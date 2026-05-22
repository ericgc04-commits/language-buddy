package com.languagebuddy.service;

import com.languagebuddy.database.DatabaseManager;
import com.languagebuddy.model.Word;

import java.util.*;

/**
 * Business-logic layer for vocabulary.
 * Wraps DatabaseManager; adds SM-2 spaced-repetition routing.
 */
public class VocabularyService {

    private final DatabaseManager db;

    public VocabularyService() { this.db = DatabaseManager.getInstance(); }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean addWord(String term, String definition,
                           String example, String category, String difficulty) {
        if (isBlank(term) || isBlank(definition)) return false;
        Word w = new Word(
            capitalise(term.trim()),
            capitalise(definition.trim()),
            example != null ? example.trim() : null,
            isBlank(category) ? "General" : category.trim(),
            isBlank(difficulty) ? "BEGINNER" : difficulty.trim()
        );
        return db.insertWord(w);
    }

    public boolean deleteWord(int id)    { return db.deleteWord(id); }
    public boolean updateWord(Word word) { return db.updateWord(word); }

    public Optional<Word> findWord(String term) {
        return Optional.ofNullable(db.findByTerm(term.trim()));
    }

    public List<Word> getAllWords()                       { return db.getAllWords(); }
    public List<Word> getByCategory(String cat)          { return db.getWordsByCategory(cat); }
    public List<Word> getByDifficulty(String diff)       { return db.getWordsByDifficulty(diff); }
    public List<String> getAllCategories()                { return db.getAllCategories(); }
    public List<Word> getRandom(int n)                   { return db.getRandom(n); }
    public List<Word> getDueForReview(int n)             { return db.getDueForReview(n); }

    public List<Word> search(String query) {
        return isBlank(query) ? getAllWords() : db.search(query.trim());
    }

    // ── Spaced-Repetition (SM-2) ──────────────────────────────────────────────

    public void recordCorrect(Word word) {
        word.recordCorrect();
        db.updateWord(word);
    }

    public void recordIncorrect(Word word) {
        word.recordIncorrect();
        db.updateWord(word);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public int getTotalCount()    { return db.countWords(); }
    public int getMasteredCount() { return db.countMastered(); }

    public Word getWordOfTheDay() {
        List<Word> all = getAllWords();
        if (all.isEmpty()) return null;
        int idx = (int)((System.currentTimeMillis() / 86_400_000L) % all.size());
        return all.get(idx);
    }

    public Map<String, Integer> countByCategory() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String c : getAllCategories()) map.put(c, getByCategory(c).size());
        return map;
    }

    public Map<String, Integer> countByDifficulty() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String d : List.of("BEGINNER","INTERMEDIATE","ADVANCED"))
            map.put(d, getByDifficulty(d).size());
        return map;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
