package com.languagebuddy.model;

/** Tracks global learning progress, XP and streaks. */
public class UserProgress {

    private int totalWordsLearned;
    private int totalQuizzesTaken;
    private int totalCorrectAnswers;
    private int currentStreak;
    private int longestStreak;
    private int totalXP;

    public UserProgress() {}

    // ── Computed ──────────────────────────────────────────────────────────────

    public double getOverallAccuracy() {
        return totalQuizzesTaken == 0 ? 0.0
               : (double) totalCorrectAnswers / totalQuizzesTaken * 100.0;
    }

    public String getLevel() {
        if (totalXP <  100) return "🌱 Seedling";
        if (totalXP <  300) return "📚 Apprentice";
        if (totalXP <  700) return "🎓 Scholar";
        if (totalXP < 1500) return "⭐ Expert";
        return "🏆 Master";
    }

    public int getLevelIndex() {
        if (totalXP <  100) return 0;
        if (totalXP <  300) return 1;
        if (totalXP <  700) return 2;
        if (totalXP < 1500) return 3;
        return 4;
    }

    public int getXPForNextLevel() {
        if (totalXP <  100) return 100  - totalXP;
        if (totalXP <  300) return 300  - totalXP;
        if (totalXP <  700) return 700  - totalXP;
        if (totalXP < 1500) return 1500 - totalXP;
        return 0;
    }

    public double getLevelProgress() {
        int[] thresholds = {0, 100, 300, 700, 1500};
        int lvl = getLevelIndex();
        if (lvl >= 4) return 1.0;
        int lo = thresholds[lvl], hi = thresholds[lvl + 1];
        return (double)(totalXP - lo) / (hi - lo);
    }

    public void addXP(int xp) { totalXP += xp; }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getTotalWordsLearned()           { return totalWordsLearned; }
    public void setTotalWordsLearned(int v)     { totalWordsLearned = v; }
    public void incrementWordsLearned()         { totalWordsLearned++; }

    public int getTotalQuizzesTaken()           { return totalQuizzesTaken; }
    public void setTotalQuizzesTaken(int v)     { totalQuizzesTaken = v; }
    public void incrementQuizzesTaken()         { totalQuizzesTaken++; }

    public int getTotalCorrectAnswers()         { return totalCorrectAnswers; }
    public void setTotalCorrectAnswers(int v)   { totalCorrectAnswers = v; }
    public void incrementCorrectAnswers()       { totalCorrectAnswers++; }

    public int getCurrentStreak()               { return currentStreak; }
    public void setCurrentStreak(int v)         { currentStreak = v; }

    public int getLongestStreak()               { return longestStreak; }
    public void setLongestStreak(int v)         { longestStreak = v; }

    public int getTotalXP()                     { return totalXP; }
    public void setTotalXP(int v)               { totalXP = v; }
}
