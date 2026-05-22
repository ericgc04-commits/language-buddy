package com.languagebuddy.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Vocabulary word entity.
 * Includes SM-2 spaced-repetition fields (easiness, interval, repetitions).
 */
public class Word {

    private int id;
    private String term;
    private String definition;
    private String exampleSentence;
    private String category;
    private String difficulty;       // BEGINNER | INTERMEDIATE | ADVANCED
    private int timesReviewed;
    private int timesCorrect;
    private LocalDateTime addedDate;
    private LocalDateTime lastReviewed;

    // ── SM-2 spaced-repetition fields ─────────────────────────────────────────
    private double easinessFactor;   // default 2.5
    private int repetitions;         // consecutive correct answers
    private int intervalDays;        // days until next review

    public Word() {
        this.timesReviewed   = 0;
        this.timesCorrect    = 0;
        this.addedDate       = LocalDateTime.now();
        this.difficulty      = "BEGINNER";
        this.easinessFactor  = 2.5;
        this.repetitions     = 0;
        this.intervalDays    = 1;
    }

    public Word(String term, String definition, String exampleSentence,
                String category, String difficulty) {
        this();
        this.term            = term;
        this.definition      = definition;
        this.exampleSentence = exampleSentence;
        this.category        = category;
        this.difficulty      = difficulty;
    }

    // ── Mastery ───────────────────────────────────────────────────────────────

    public double getMasteryScore() {
        if (timesReviewed == 0) return 0.0;
        return (double) timesCorrect / timesReviewed * 100.0;
    }

    public boolean isMastered() {
        return timesReviewed >= 5 && getMasteryScore() >= 80.0;
    }

    /** SM-2 update. quality: 0–5 (>=3 = correct) */
    public void updateSM2(int quality) {
        if (quality >= 3) {
            timesCorrect++;
            repetitions++;
            if (repetitions == 1)       intervalDays = 1;
            else if (repetitions == 2)  intervalDays = 6;
            else intervalDays = (int) Math.round(intervalDays * easinessFactor);
        } else {
            repetitions  = 0;
            intervalDays = 1;
        }
        easinessFactor = Math.max(1.3, easinessFactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        timesReviewed++;
        lastReviewed = LocalDateTime.now();
    }

    public void recordCorrect()   { updateSM2(5); }
    public void recordIncorrect() { updateSM2(1); }

    public boolean isDueForReview() {
        if (lastReviewed == null) return true;
        return LocalDateTime.now().isAfter(lastReviewed.plusDays(intervalDays));
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getId()                         { return id; }
    public void setId(int id)                  { this.id = id; }

    public String getTerm()                    { return term; }
    public void setTerm(String term)           { this.term = term; }

    public String getDefinition()              { return definition; }
    public void setDefinition(String d)        { this.definition = d; }

    public String getExampleSentence()         { return exampleSentence; }
    public void setExampleSentence(String e)   { this.exampleSentence = e; }

    public String getCategory()                { return category; }
    public void setCategory(String c)          { this.category = c; }

    public String getDifficulty()              { return difficulty; }
    public void setDifficulty(String d)        { this.difficulty = d; }

    public int getTimesReviewed()              { return timesReviewed; }
    public void setTimesReviewed(int v)        { this.timesReviewed = v; }

    public int getTimesCorrect()               { return timesCorrect; }
    public void setTimesCorrect(int v)         { this.timesCorrect = v; }

    public LocalDateTime getAddedDate()        { return addedDate; }
    public void setAddedDate(LocalDateTime d)  { this.addedDate = d; }

    public LocalDateTime getLastReviewed()     { return lastReviewed; }
    public void setLastReviewed(LocalDateTime d){ this.lastReviewed = d; }

    public double getEasinessFactor()          { return easinessFactor; }
    public void setEasinessFactor(double v)    { this.easinessFactor = v; }

    public int getRepetitions()                { return repetitions; }
    public void setRepetitions(int v)          { this.repetitions = v; }

    public int getIntervalDays()               { return intervalDays; }
    public void setIntervalDays(int v)         { this.intervalDays = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Word w)) return false;
        return id == w.id && Objects.equals(term, w.term);
    }

    @Override public int hashCode() { return Objects.hash(id, term); }
    @Override public String toString() {
        return "Word{id=" + id + ", term='" + term + "'}";
    }
}
