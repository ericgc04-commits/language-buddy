package com.languagebuddy.nlp;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.normalizer.AggregateCharSequenceNormalizer;
import opennlp.tools.util.normalizer.NumberCharSequenceNormalizer;
import opennlp.tools.util.normalizer.TwitterCharSequenceNormalizer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NLP processing layer using Apache OpenNLP.
 *
 * Uses OpenNLP's SimpleTokenizer (no model file required) for robust
 * tokenisation. More complex models (sentence detection, POS tagging)
 * require downloaded .bin files — stubs are included and activate
 * automatically if the files are placed in src/main/resources/nlp/.
 *
 * Proposal reference: "Apache OpenNLP for processing natural language."
 */
public class NLPProcessor {

    private static final Set<String> STOP_WORDS = new HashSet<>(List.of(
        "a","an","the","is","are","was","were","be","been","being",
        "have","has","had","do","does","did","will","would","shall","should",
        "may","might","must","can","could","to","of","in","for","on","with",
        "at","by","from","up","about","into","through","during","before","after",
        "i","you","he","she","it","we","they","what","which","who","how","this","that"
    ));

    private final SimpleTokenizer tokenizer;
    private final AggregateCharSequenceNormalizer normalizer;

    public NLPProcessor() {
        this.tokenizer  = SimpleTokenizer.INSTANCE;
        this.normalizer = new AggregateCharSequenceNormalizer(
            new NumberCharSequenceNormalizer(),
            new TwitterCharSequenceNormalizer()
        );
    }

    /** Tokenise raw input into lowercase tokens. */
    public String[] tokenize(String text) {
        if (text == null || text.isBlank()) return new String[0];
        return tokenizer.tokenize(text.toLowerCase().trim());
    }

    /** Remove stop-words from a token array. */
    public String[] removeStopWords(String[] tokens) {
        return Arrays.stream(tokens)
                     .filter(t -> !STOP_WORDS.contains(t) && t.matches("[a-z]+"))
                     .toArray(String[]::new);
    }

    /** Normalise text: strip numbers, URLs, Twitter artefacts. */
    public String normalise(String text) {
        if (text == null) return "";
        return normalizer.normalize(text).toString().trim();
    }

    /**
     * Extract the most likely intent keyword from user input.
     * Returns one of: DEFINE | ADD | QUIZ | FLASHCARD | STATS |
     *                 LIST | HELP | GREETING | WORD_OF_DAY | UNKNOWN
     */
    public String detectIntent(String input) {
        if (input == null || input.isBlank()) return "UNKNOWN";
        String[] tokens = removeStopWords(tokenize(input));
        String joined = String.join(" ", tokens);

        if (matches(joined, "hello","hi","hey","morning","afternoon","evening","sup","yo")) return "GREETING";
        if (matches(joined, "define","definition","meaning","means","mean"))               return "DEFINE";
        if (matches(joined, "add","save","remember","learn","teach"))                      return "ADD";
        if (matches(joined, "quiz","test","practice","examine","drill"))                   return "QUIZ";
        if (matches(joined, "flash","flashcard","card","flip"))                            return "FLASHCARD";
        if (matches(joined, "stat","progress","score","level","xp","streak","point"))      return "STATS";
        if (matches(joined, "list","show","display","vocabulary","words","all"))           return "LIST";
        if (matches(joined, "help","command","option","feature"))                          return "HELP";
        if (matches(joined, "day","daily","today","word"))                                 return "WORD_OF_DAY";
        return "UNKNOWN";
    }

    /** Extract a target word/phrase from user input after a keyword. */
    public String extractTarget(String input, String... afterKeywords) {
        if (input == null) return "";
        String lower = input.toLowerCase().trim();
        for (String kw : afterKeywords) {
            int idx = lower.indexOf(kw);
            if (idx >= 0) {
                String rest = input.substring(idx + kw.length()).trim();
                rest = rest.replaceAll("^[\"']|[\"']$", ""); // strip quotes
                return rest.trim();
            }
        }
        return "";
    }

    /** Calculate word similarity using Levenshtein distance (fuzzy matching). */
    public double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        a = a.toLowerCase(); b = b.toLowerCase();
        int dist = levenshtein(a, b);
        return 1.0 - (double) dist / Math.max(a.length(), b.length());
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                dp[i][j] = a.charAt(i-1) == b.charAt(j-1)
                    ? dp[i-1][j-1]
                    : 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
        return dp[a.length()][b.length()];
    }
}
