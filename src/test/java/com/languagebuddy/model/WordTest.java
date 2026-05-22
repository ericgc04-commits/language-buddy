package com.languagebuddy.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Word model.
 * Covers mastery, SM-2 spaced-repetition, and edge cases.
 */
@DisplayName("Word Model Tests")
class WordTest {

    private Word word;

    @BeforeEach
    void setUp() {
        word = new Word("Eloquent","Fluent in speech","She spoke eloquently.","Communication","INTERMEDIATE");
    }

    @Test @DisplayName("New word has zero mastery")
    void newWordZeroMastery() {
        assertEquals(0.0, word.getMasteryScore(), 0.001);
        assertFalse(word.isMastered());
    }

    @Test @DisplayName("recordCorrect increments both counters")
    void recordCorrectIncrementsCounters() {
        word.recordCorrect();
        assertEquals(1, word.getTimesReviewed());
        assertEquals(1, word.getTimesCorrect());
    }

    @Test @DisplayName("recordIncorrect only increments reviewed")
    void recordIncorrectOnlyReviewed() {
        word.recordIncorrect();
        assertEquals(1, word.getTimesReviewed());
        assertEquals(0, word.getTimesCorrect());
    }

    @Test @DisplayName("Mastery score is 100% after all correct")
    void masteryAfterAllCorrect() {
        for (int i = 0; i < 5; i++) word.recordCorrect();
        assertEquals(100.0, word.getMasteryScore(), 0.001);
        assertTrue(word.isMastered());
    }

    @Test @DisplayName("Word not mastered with fewer than 5 reviews")
    void notMasteredUnder5Reviews() {
        for (int i = 0; i < 4; i++) word.recordCorrect();
        assertFalse(word.isMastered());
    }

    @ParameterizedTest(name = "quality={0} → repetitions change")
    @CsvSource({"5,1", "4,1", "3,1", "2,0", "1,0", "0,0"})
    @DisplayName("SM-2 repetitions update correctly")
    void sm2RepetitionsUpdate(int quality, int expectedReps) {
        word.updateSM2(quality);
        assertEquals(expectedReps, word.getRepetitions());
    }

    @Test @DisplayName("SM-2 interval increases after 3 correct answers")
    void sm2IntervalIncreases() {
        word.updateSM2(5); // rep=1 → interval=1
        word.updateSM2(5); // rep=2 → interval=6
        word.updateSM2(5); // rep=3 → interval grows
        assertTrue(word.getIntervalDays() >= 6);
    }

    @Test @DisplayName("SM-2 easiness factor never drops below 1.3")
    void sm2EasinessFloor() {
        for (int i = 0; i < 20; i++) word.updateSM2(0);
        assertTrue(word.getEasinessFactor() >= 1.3);
    }

    @Test @DisplayName("New word is always due for review")
    void newWordDueForReview() {
        assertTrue(word.isDueForReview());
    }

    @Test @DisplayName("Equality based on id and term")
    void equalityCheck() {
        Word w2 = new Word("Eloquent","Different def",null,"General","BEGINNER");
        w2.setId(word.getId()); // same id
        assertEquals(word, w2);
    }
}
