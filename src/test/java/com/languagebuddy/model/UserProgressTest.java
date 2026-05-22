package com.languagebuddy.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserProgress XP, level, and accuracy calculations.
 */
@DisplayName("User Progress Tests")
class UserProgressTest {

    private UserProgress progress;

    @BeforeEach
    void setUp() { progress = new UserProgress(); }

    @Test @DisplayName("New user starts at Seedling level")
    void newUserIsSeedling() {
        assertEquals("🌱 Seedling", progress.getLevel());
        assertEquals(0, progress.getLevelIndex());
    }

    @Test @DisplayName("Apprentice level at 100 XP")
    void apprenticeAt100() {
        progress.setTotalXP(100);
        assertEquals("📚 Apprentice", progress.getLevel());
    }

    @Test @DisplayName("Scholar level at 300 XP")
    void scholarAt300() {
        progress.setTotalXP(300);
        assertEquals("🎓 Scholar", progress.getLevel());
    }

    @Test @DisplayName("Expert level at 700 XP")
    void expertAt700() {
        progress.setTotalXP(700);
        assertEquals("⭐ Expert", progress.getLevel());
    }

    @Test @DisplayName("Master level at 1500 XP")
    void masterAt1500() {
        progress.setTotalXP(1500);
        assertEquals("🏆 Master", progress.getLevel());
    }

    @Test @DisplayName("Accuracy is zero when no quizzes taken")
    void zeroAccuracyWhenNoQuizzes() {
        assertEquals(0.0, progress.getOverallAccuracy(), 0.001);
    }

    @Test @DisplayName("Accuracy is 80% for 8/10 correct")
    void accuracyCalculation() {
        progress.setTotalQuizzesTaken(10);
        progress.setTotalCorrectAnswers(8);
        assertEquals(80.0, progress.getOverallAccuracy(), 0.001);
    }

    @Test @DisplayName("addXP accumulates correctly")
    void addXpAccumulates() {
        progress.addXP(50); progress.addXP(30);
        assertEquals(80, progress.getTotalXP());
    }

    @Test @DisplayName("Level progress is between 0 and 1")
    void levelProgressBounds() {
        progress.setTotalXP(150); // between 100 and 300 = apprentice
        double lp = progress.getLevelProgress();
        assertTrue(lp > 0.0 && lp < 1.0);
    }

    @Test @DisplayName("Max level has XPForNextLevel = 0")
    void maxLevelXp() {
        progress.setTotalXP(1500);
        assertEquals(0, progress.getXPForNextLevel());
    }
}
