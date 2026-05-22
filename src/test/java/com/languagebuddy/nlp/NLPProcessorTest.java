package com.languagebuddy.nlp;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the NLPProcessor.
 * Covers tokenisation, intent detection, fuzzy matching, and normalisation.
 */
@DisplayName("NLP Processor Tests")
class NLPProcessorTest {

    private NLPProcessor nlp;

    @BeforeEach
    void setUp() { nlp = new NLPProcessor(); }

    // ── Tokenisation ──────────────────────────────────────────────────────────

    @Test @DisplayName("Tokenises simple sentence correctly")
    void tokenisesSimple() {
        String[] tokens = nlp.tokenize("define eloquent");
        assertArrayEquals(new String[]{"define","eloquent"}, tokens);
    }

    @Test @DisplayName("Returns empty array for blank input")
    void blankInputReturnsEmpty() {
        assertEquals(0, nlp.tokenize("").length);
        assertEquals(0, nlp.tokenize(null).length);
    }

    @Test @DisplayName("Stop words are removed")
    void stopWordsRemoved() {
        String[] tokens = nlp.removeStopWords(new String[]{"what","is","the","meaning","of","hello"});
        assertArrayEquals(new String[]{"meaning","hello"}, tokens);
    }

    // ── Intent detection ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "input=''{0}'' → intent=''{1}''")
    @CsvSource({
        "hello, GREETING",
        "hi there, GREETING",
        "define eloquent, DEFINE",
        "what is the meaning of verbose, DEFINE",
        "add serendipity, ADD",
        "quiz me, QUIZ",
        "practice now, QUIZ",
        "flashcard, FLASHCARD",
        "my stats, STATS",
        "show progress, STATS",
        "list all words, LIST",
        "word of the day, WORD_OF_DAY",
        "help me, HELP"
    })
    @DisplayName("Intent detection maps correctly")
    void intentDetection(String input, String expected) {
        assertEquals(expected, nlp.detectIntent(input));
    }

    @Test @DisplayName("Unknown input returns UNKNOWN intent")
    void unknownIntent() {
        assertEquals("UNKNOWN", nlp.detectIntent("xyzzy frobnicator"));
    }

    // ── Similarity / fuzzy matching ───────────────────────────────────────────

    @Test @DisplayName("Identical strings have similarity 1.0")
    void perfectSimilarity() {
        assertEquals(1.0, nlp.similarity("eloquent","eloquent"), 0.001);
    }

    @Test @DisplayName("Completely different strings have low similarity")
    void lowSimilarity() {
        assertTrue(nlp.similarity("abc","xyz") < 0.5);
    }

    @Test @DisplayName("Single typo maintains high similarity")
    void typoSimilarity() {
        // "elequent" vs "eloquent" — one character off
        assertTrue(nlp.similarity("elequent","eloquent") > 0.7);
    }

    @Test @DisplayName("Null inputs return 0.0 similarity")
    void nullSimilarity() {
        assertEquals(0.0, nlp.similarity(null,"word"), 0.001);
        assertEquals(0.0, nlp.similarity("word",null), 0.001);
    }

    // ── Target extraction ─────────────────────────────────────────────────────

    @Test @DisplayName("Extracts target word after keyword")
    void extractsTarget() {
        String target = nlp.extractTarget("define serendipity", "define");
        assertEquals("serendipity", target);
    }

    @Test @DisplayName("Strips surrounding quotes from target")
    void stripsQuotes() {
        String target = nlp.extractTarget("add \"ubiquitous\"", "add");
        assertEquals("ubiquitous", target);
    }
}
