package com.languagebuddy.controller;

import com.languagebuddy.database.DatabaseManager;
import com.languagebuddy.model.QuizResult;
import com.languagebuddy.model.UserProgress;
import com.languagebuddy.model.Word;
import com.languagebuddy.service.ChatbotService;
import com.languagebuddy.service.ChatbotService.BotResponse;
import com.languagebuddy.service.VocabularyService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MVC Controller: bridges the View and the Service/Model layers.
 * All UI events are routed through this class.
 */
public class MainController {

    private final VocabularyService vocab;
    private final ChatbotService    chat;

    public MainController() {
        this.vocab = new VocabularyService();
        this.chat  = new ChatbotService(vocab);
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    public BotResponse sendMessage(String input)    { return chat.process(input); }

    public String getWelcomeMessage() {
        UserProgress p = getProgress();
        return "👋 Welcome back!\n\n" +
               "Level: **" + p.getLevel() + "** · XP: **" + p.getTotalXP() + "** · " +
               "Words: **" + vocab.getTotalCount() + "**\n\n" +
               "Type **help** to see all commands, or try **word of the day** 🌟";
    }

    // ── Vocabulary ────────────────────────────────────────────────────────────

    public List<Word> getAllWords()                  { return vocab.getAllWords(); }
    public List<Word> searchWords(String q)         { return vocab.search(q); }
    public List<Word> getByCategory(String cat)     { return vocab.getByCategory(cat); }
    public List<Word> getByDifficulty(String diff)  { return vocab.getByDifficulty(diff); }
    public List<String> getCategories()             { return vocab.getAllCategories(); }
    public Optional<Word> findWord(String term)     { return vocab.findWord(term); }
    public boolean updateWord(Word w)               { return vocab.updateWord(w); }
    public boolean deleteWord(int id)               { return vocab.deleteWord(id); }

    public boolean addWord(String term, String def, String ex, String cat, String diff) {
        boolean ok = vocab.addWord(term, def, ex, cat, diff);
        if (ok) {
            UserProgress p = getProgress();
            p.incrementWordsLearned(); p.addXP(10);
            DatabaseManager.getInstance().saveProgress(p);
        }
        return ok;
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public UserProgress getProgress() {
        chat.refreshProgress();
        return chat.getProgress();
    }

    public int getTotalWordCount()              { return vocab.getTotalCount(); }
    public int getMasteredWordCount()           { return vocab.getMasteredCount(); }
    public Map<String,Integer> byCategory()    { return vocab.countByCategory(); }
    public Map<String,Integer> byDifficulty()  { return vocab.countByDifficulty(); }
    public Word getWordOfTheDay()              { return vocab.getWordOfTheDay(); }
    public List<QuizResult> getRecentQuizzes() {
        return DatabaseManager.getInstance().getRecentQuizResults(10);
    }
}
