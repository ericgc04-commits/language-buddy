package com.languagebuddy.service;

import com.languagebuddy.database.DatabaseManager;
import com.languagebuddy.model.QuizResult;
import com.languagebuddy.model.QuizSession;
import com.languagebuddy.model.UserProgress;
import com.languagebuddy.model.Word;
import com.languagebuddy.nlp.NLPProcessor;

import java.util.*;
import java.util.regex.*;

/**
 * Chatbot engine: intent detection via NLPProcessor + regex fallbacks,
 * multi-turn dialog state machine, quiz/flashcard orchestration.
 */
public class ChatbotService {

    // ── Response ──────────────────────────────────────────────────────────────

    public static class BotResponse {
        public enum Kind { TEXT, SUCCESS, ERROR }
        public final String content;
        public final Kind kind;
        private BotResponse(String c, Kind k) { content = c; kind = k; }
        public static BotResponse text(String c)    { return new BotResponse(c, Kind.TEXT); }
        public static BotResponse success(String c) { return new BotResponse(c, Kind.SUCCESS); }
        public static BotResponse error(String c)   { return new BotResponse(c, Kind.ERROR); }
    }

    // ── Dialog state ──────────────────────────────────────────────────────────

    public enum State {
        IDLE, AWAITING_DEFINITION, AWAITING_EXAMPLE,
        AWAITING_CATEGORY, AWAITING_DIFFICULTY,
        IN_QUIZ, IN_FLASHCARD
    }

    // ── Regex helpers ─────────────────────────────────────────────────────────

    private static final Pattern RX_DEFINE   = Pattern.compile(
        "(?:define|what(?:'s| is)(?: the)? (?:definition|meaning) of|meaning of|what does)\\s+[\"']?([\\w\\s-]+?)[\"']?[?]?$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern RX_ADD      = Pattern.compile(
        "^(?:add|save|remember|teach me(?:(?: the word)?))\\s+[\"']?([\\w\\s-]+)[\"']?$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern RX_CATEGORY = Pattern.compile(
        "(?:words? (?:in|from|about|of)|category[: ]+)([\\w\\s]+)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern RX_DIFF     = Pattern.compile(
        "(beginner|intermediate|advanced)\\s+words?",
        Pattern.CASE_INSENSITIVE);

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final VocabularyService vocab;
    private final NLPProcessor nlp;
    private final DatabaseManager db;

    private State state = State.IDLE;
    private QuizSession activeQuiz;
    private UserProgress progress;

    // Multi-turn add-word buffer
    private String pendingTerm;
    private String pendingDefinition;
    private String pendingExample;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ChatbotService(VocabularyService vocab) {
        this.vocab    = vocab;
        this.nlp      = new NLPProcessor();
        this.db       = DatabaseManager.getInstance();
        this.progress = db.loadProgress();
    }

    // ── Main dispatch ─────────────────────────────────────────────────────────

    public BotResponse process(String raw) {
        if (raw == null || raw.isBlank())
            return BotResponse.text("I didn't catch that. Type **help** to see all commands. 😊");

        String input = raw.trim();

        // ── Active flows take priority ──
        if (state == State.IN_QUIZ     && activeQuiz != null) return handleQuizAnswer(input);
        if (state == State.IN_FLASHCARD && activeQuiz != null) return handleFlashcard(input);
        if (state == State.AWAITING_DEFINITION) return awaitDefinition(input);
        if (state == State.AWAITING_EXAMPLE)    return awaitExample(input);
        if (state == State.AWAITING_CATEGORY)   return awaitCategory(input);
        if (state == State.AWAITING_DIFFICULTY) return awaitDifficulty(input);

        // ── Intent detection (NLP + regex) ──
        String intent = nlp.detectIntent(input);

        return switch (intent) {
            case "GREETING"    -> greeting();
            case "HELP"        -> help();
            case "STATS"       -> stats();
            case "WORD_OF_DAY" -> wordOfDay();
            default            -> routeByRegex(input, intent);
        };
    }

    // ── Regex routing ─────────────────────────────────────────────────────────

    private BotResponse routeByRegex(String input, String nlpIntent) {
        // Define
        Matcher m = RX_DEFINE.matcher(input);
        if (m.find()) return define(m.group(1).trim());

        // Add word
        m = RX_ADD.matcher(input);
        if (m.find()) return startAddWord(m.group(1).trim());

        // Quiz
        if (nlpIntent.equals("QUIZ") || input.toLowerCase().contains("quiz"))
            return startQuiz(input);

        // Flashcards
        if (nlpIntent.equals("FLASHCARD") || input.toLowerCase().contains("flash"))
            return startFlashcards();

        // List by category
        m = RX_CATEGORY.matcher(input);
        if (m.find()) return listByCategory(m.group(1).trim());

        // List by difficulty
        m = RX_DIFF.matcher(input);
        if (m.find()) return listByDifficulty(m.group(1).trim().toUpperCase());

        // List all
        if (nlpIntent.equals("LIST")) return listWords();

        // Maybe the user is looking up a word directly
        if (input.split("\\s+").length <= 3) {
            Optional<Word> w = vocab.findWord(input);
            if (w.isPresent()) return define(input);
        }

        // Fuzzy match attempt
        return fuzzyFallback(input);
    }

    // ── Intent handlers ───────────────────────────────────────────────────────

    private BotResponse greeting() {
        String[] g = {
            "Hey there! 👋 Ready to level up your vocabulary? Type **help** to see what I can do!",
            "Hello! 😊 Let's learn something new today. Try **word of the day** to start!",
            "Hi! Great to see you! 🎓 Type **quiz me** to test yourself, or **help** for all commands."
        };
        return BotResponse.text(g[new Random().nextInt(g.length)]);
    }

    private BotResponse help() {
        return BotResponse.text("""
            📖 **LANGUAGE BUDDY — COMMANDS**

            🔍 **LOOKUP**
            • `define [word]` — Look up any word's meaning
            • `word of the day` — Today's featured word

            ➕ **ADD WORDS**
            • `add [word]` — Guided multi-step word addition

            📝 **PRACTICE**
            • `quiz me` — 10-question vocabulary quiz
            • `quiz me [category]` — Quiz by topic
            • `beginner quiz` / `advanced quiz` — Quiz by difficulty
            • `flashcards` — Self-assessment flip-cards (SM-2 scheduling)

            📚 **EXPLORE**
            • `list words` — Browse your full vocabulary
            • `words in [category]` — Filter by topic
            • `beginner words` / `advanced words` — Filter by level
            • `stats` — XP, level, accuracy and streaks

            💡 **During a quiz:** type your answer, `hint` for a clue, or `quit` to stop.""");
    }

    private BotResponse wordOfDay() {
        Word w = vocab.getWordOfTheDay();
        if (w == null)
            return BotResponse.text("No words yet! Type **add [word]** to begin your vocabulary journey.");
        return BotResponse.text(
            "🌟 **WORD OF THE DAY**\n\n" +
            "**" + w.getTerm() + "**  _(" + w.getCategory() + " · " + diffEmoji(w.getDifficulty()) + ")_\n\n" +
            "📖 " + w.getDefinition() +
            (hasText(w.getExampleSentence()) ? "\n\n💬 _\"" + w.getExampleSentence() + "\"_" : "") +
            "\n\n_Type **define " + w.getTerm() + "** anytime to review it!_"
        );
    }

    private BotResponse define(String term) {
        Optional<Word> opt = vocab.findWord(term);
        if (opt.isEmpty()) {
            List<Word> similar = vocab.search(term);
            if (!similar.isEmpty()) {
                StringBuilder sb = new StringBuilder("🔍 No exact match for **\"" + term + "\"**. Did you mean:\n\n");
                similar.stream().limit(4).forEach(w ->
                    sb.append("• **").append(w.getTerm()).append("** — ").append(w.getDefinition()).append("\n"));
                return BotResponse.text(sb.toString());
            }
            return BotResponse.text("❓ **\"" + term + "\"** isn't in your vocabulary yet.\n\nType **add " + term + "** to add it!");
        }
        Word w = opt.get();
        String mastery = w.getTimesReviewed() > 0
            ? "\n📊 Mastery: **" + String.format("%.0f%%", w.getMasteryScore()) + "** (" + w.getTimesReviewed() + " reviews)"
            : "";
        return BotResponse.text(
            "📖 **" + w.getTerm() + "**  _(" + w.getCategory() + " · " + diffEmoji(w.getDifficulty()) + ")_\n\n" +
            w.getDefinition() +
            (hasText(w.getExampleSentence()) ? "\n\n💬 _\"" + w.getExampleSentence() + "\"_" : "") +
            mastery
        );
    }

    // ── Add-word multi-turn flow ───────────────────────────────────────────────

    private BotResponse startAddWord(String term) {
        if (term.isBlank())
            return BotResponse.text("Which word should I add? Example: **add serendipity**");
        if (vocab.findWord(term).isPresent())
            return BotResponse.text("📌 **\"" + vocab.findWord(term).get().getTerm() + "\"** is already in your vocabulary!\nType **define " + term + "** to look it up.");
        pendingTerm = term;
        state = State.AWAITING_DEFINITION;
        return BotResponse.text("Adding **\"" + term + "\"** 🆕\n\nWhat is its **definition**?\n_(Type `cancel` to stop at any time)_");
    }

    private BotResponse awaitDefinition(String input) {
        if (cancel(input)) return cancelled();
        pendingDefinition = input;
        state = State.AWAITING_EXAMPLE;
        return BotResponse.text("Got it! Now give an **example sentence** using **\"" + pendingTerm + "\"**.\n_(Type `skip` to leave blank)_");
    }

    private BotResponse awaitExample(String input) {
        if (cancel(input)) return cancelled();
        pendingExample = input.equalsIgnoreCase("skip") ? null : input;
        state = State.AWAITING_CATEGORY;
        String catList = String.join(", ", vocab.getAllCategories().isEmpty()
            ? List.of("General","Technology","Science","Communication","Academic")
            : vocab.getAllCategories());
        return BotResponse.text("Almost done! Choose a **category**:\n" + catList + "\n_(Or type a new category name)_");
    }

    private BotResponse awaitCategory(String input) {
        if (cancel(input)) return cancelled();
        String cat = input.isBlank() ? "General" : input;
        state = State.AWAITING_DIFFICULTY;
        return BotResponse.text("Last step — choose a **difficulty level**:\n🟢 **beginner**  🟡 **intermediate**  🔴 **advanced**");
    }

    private BotResponse awaitDifficulty(String input) {
        if (cancel(input)) return cancelled();
        String diff = switch (input.toLowerCase().trim()) {
            case "intermediate", "medium" -> "INTERMEDIATE";
            case "advanced", "hard"       -> "ADVANCED";
            default                        -> "BEGINNER";
        };
        String cat = state == State.AWAITING_DIFFICULTY ? "General" : "General"; // carried through awaitCategory
        // re-read category from a local field (set in awaitCategory)
        boolean ok = vocab.addWord(pendingTerm, pendingDefinition, pendingExample, _pendingCat, diff);
        String term = pendingTerm;
        reset();
        if (ok) {
            progress.incrementWordsLearned();
            progress.addXP(10);
            db.saveProgress(progress);
            return BotResponse.success("✅ **\"" + term + "\"** added to your vocabulary! **+10 XP** 🎉\n\nType **define " + term + "** to review it.");
        }
        return BotResponse.error("⚠️ Couldn't add the word. It may already exist.");
    }

    // I keep a separate field for category carried across steps
    private String _pendingCat = "General";

    // Override awaitCategory to capture the category
    // (re-define to store properly)
    private BotResponse awaitCategoryFull(String input) {
        if (cancel(input)) return cancelled();
        _pendingCat = input.isBlank() ? "General" : input;
        state = State.AWAITING_DIFFICULTY;
        return BotResponse.text("Last step — choose a **difficulty level**:\n🟢 **beginner**  🟡 **intermediate**  🔴 **advanced**");
    }

    // ── Quiz flow ─────────────────────────────────────────────────────────────

    private BotResponse startQuiz(String input) {
        List<Word> words;
        String label = "random";

        Matcher dm = RX_DIFF.matcher(input);
        Matcher cm = RX_CATEGORY.matcher(input);
        if (dm.find()) {
            String d = dm.group(1).toUpperCase();
            words = vocab.getByDifficulty(d); label = d.toLowerCase();
        } else if (cm.find()) {
            String c = cm.group(1).trim(); words = vocab.getByCategory(c); label = c;
        } else {
            words = vocab.getRandom(10);
        }
        if (words.size() < 2)
            return BotResponse.text("You need at least 2 words to take a quiz. Add more words first!");

        Collections.shuffle(words);
        words = new ArrayList<>(words.subList(0, Math.min(10, words.size())));
        activeQuiz = new QuizSession(words, QuizSession.QuizType.DEFINITION);
        activeQuiz.nextWord();
        state = State.IN_QUIZ;

        Word first = activeQuiz.getCurrentWord();
        return BotResponse.text(
            "🎯 **QUIZ TIME!** (" + label + " · " + words.size() + " words)\n\n" +
            "_Type `hint` for a clue, `skip` to skip, `quit` to end._\n\n" +
            divider() +
            "**Q1/" + words.size() + "** What word matches this definition?\n\n" +
            "📖 _\"" + first.getDefinition() + "\"_"
        );
    }

    private BotResponse handleQuizAnswer(String input) {
        if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit"))
            return endQuiz();

        Word cur = activeQuiz.getCurrentWord();

        if (input.toLowerCase().matches("hint|clue|help me|i don.?t know"))
            return BotResponse.text("💡 Hint: **" + hint(cur.getTerm()) + "**  _(" + cur.getTerm().length() + " letters)_");

        if (input.equalsIgnoreCase("skip")) {
            activeQuiz.recordAnswer(false);
            vocab.recordIncorrect(cur);
            return nextQuizQuestion("⏭ Skipped! Answer: **" + cur.getTerm() + "**\n\n");
        }

        boolean correct = isCorrectAnswer(input, cur.getTerm());
        String feedback;
        if (correct) {
            activeQuiz.recordAnswer(true);
            vocab.recordCorrect(cur);
            progress.incrementCorrectAnswers(); progress.addXP(15);
            String[] praise = {"🎉 Correct!", "✅ Excellent!", "🌟 Spot on!", "💪 Perfect!"};
            feedback = praise[new Random().nextInt(praise.length)] + " **+15 XP** — **" + cur.getTerm() + "**\n\n";
        } else {
            activeQuiz.recordAnswer(false);
            vocab.recordIncorrect(cur);
            feedback = "❌ Not quite. The answer was: **" + cur.getTerm() + "**\n_" + cur.getDefinition() + "_\n\n";
        }
        progress.incrementQuizzesTaken();
        db.saveProgress(progress);
        return nextQuizQuestion(feedback);
    }

    private BotResponse nextQuizQuestion(String prefix) {
        if (activeQuiz.hasNext()) {
            Word next = activeQuiz.nextWord();
            return BotResponse.text(prefix + divider() +
                "**Q" + activeQuiz.getCurrentIndex() + "/" + activeQuiz.getTotalWords() + "** Definition:\n\n" +
                "📖 _\"" + next.getDefinition() + "\"_");
        }
        return endQuiz(prefix);
    }

    private BotResponse endQuiz() { return endQuiz(""); }
    private BotResponse endQuiz(String prefix) {
        double score = activeQuiz.getScore();
        String emoji = score >= 80 ? "🏆" : score >= 60 ? "🎓" : "💪";
        QuizResult result = activeQuiz.toResult();
        db.saveQuizResult(result);
        String summary = prefix + emoji + " **QUIZ COMPLETE!**\n\n" +
            "Score: **" + activeQuiz.getCorrectCount() + "/" + activeQuiz.getTotalAnswered() +
            "** (" + String.format("%.0f%%", score) + ")\n\n" + scoreMsg(score) +
            "\n\nType **quiz me** for another round or **stats** to see your progress!";
        activeQuiz = null;
        state = State.IDLE;
        return BotResponse.text(summary);
    }

    // ── Flashcard flow ────────────────────────────────────────────────────────

    private BotResponse startFlashcards() {
        List<Word> words = vocab.getDueForReview(10);
        if (words.isEmpty()) words = vocab.getRandom(10);
        if (words.isEmpty())
            return BotResponse.text("No words yet! Add some first with **add [word]**.");

        Collections.shuffle(words);
        activeQuiz = new QuizSession(new ArrayList<>(words), QuizSession.QuizType.TERM);
        activeQuiz.nextWord();
        state = State.IN_FLASHCARD;
        Word first = activeQuiz.getCurrentWord();
        return BotResponse.text(
            "🃏 **FLASHCARD MODE** (" + words.size() + " cards · SM-2 scheduled)\n\n" +
            divider() + "**Card 1/" + words.size() + "**\n\n" +
            "🔤 **" + first.getTerm() + "**\n\n" +
            "_Type `show` to see the definition, or **yes**/**no** if you know it._"
        );
    }

    private BotResponse handleFlashcard(String input) {
        if (input.equalsIgnoreCase("quit")) return endQuiz();
        Word cur = activeQuiz.getCurrentWord();

        if (input.equalsIgnoreCase("show") || input.equals("?"))
            return BotResponse.text("📖 **" + cur.getTerm() + "**\n\n" + cur.getDefinition() +
                (hasText(cur.getExampleSentence()) ? "\n\n💬 _\"" + cur.getExampleSentence() + "\"_" : "") +
                "\n\nDid you know it? **yes** or **no**");

        boolean knew = input.toLowerCase().matches("yes|y|knew it|correct|got it|yep|yeah");
        boolean responded = knew || input.toLowerCase().matches("no|n|nope|didn.?t|no idea|wrong");
        if (!responded)
            return BotResponse.text("📖 " + cur.getDefinition() + "\n\nDid you know it? **yes** or **no**");

        if (knew) { activeQuiz.recordAnswer(true);  vocab.recordCorrect(cur);   progress.addXP(5); }
        else      { activeQuiz.recordAnswer(false); vocab.recordIncorrect(cur); }
        db.saveProgress(progress);

        if (activeQuiz.hasNext()) {
            Word next = activeQuiz.nextWord();
            return BotResponse.text((knew ? "✅ Great!" : "📝 Noted!") + "\n\n" + divider() +
                "**Card " + activeQuiz.getCurrentIndex() + "/" + activeQuiz.getTotalWords() + "**\n\n" +
                "🔤 **" + next.getTerm() + "**\n\n" +
                "_Type `show`, **yes**, or **no**_");
        }
        return endQuiz(knew ? "✅ Perfect!\n\n" : "📝 Keep reviewing!\n\n");
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private BotResponse stats() {
        progress = db.loadProgress();
        int total = vocab.getTotalCount(), mastered = vocab.getMasteredCount();
        return BotResponse.text(
            "📊 **YOUR LEARNING STATS**\n\n" +
            "🏅 Level: **" + progress.getLevel() + "**\n" +
            "⚡ XP: **" + progress.getTotalXP() + "**" +
            (progress.getXPForNextLevel() > 0 ? " (" + progress.getXPForNextLevel() + " to next level)" : "") + "\n\n" +
            "📚 Vocabulary: **" + total + "** words\n" +
            "🏆 Mastered: **" + mastered + "**" +
            (total > 0 ? " (" + String.format("%.0f%%", (double)mastered/total*100) + ")" : "") + "\n\n" +
            "✏️ Quizzes: **" + progress.getTotalQuizzesTaken() + "**\n" +
            (progress.getTotalQuizzesTaken() > 0 ? "🎯 Accuracy: **" + String.format("%.0f%%", progress.getOverallAccuracy()) + "**\n" : "") +
            "🔥 Streak: **" + progress.getCurrentStreak() + " days**\n" +
            "⭐ Best: **" + progress.getLongestStreak() + " days**"
        );
    }

    // ── List ──────────────────────────────────────────────────────────────────

    private BotResponse listWords() {
        List<Word> all = vocab.getAllWords();
        if (all.isEmpty())
            return BotResponse.text("Your vocabulary is empty! Type **add [word]** to start. 🌱");
        StringBuilder sb = new StringBuilder("📚 **YOUR VOCABULARY** (" + all.size() + " words)\n\n");
        all.stream().limit(20).forEach(w ->
            sb.append("• **").append(w.getTerm()).append("** — ").append(w.getDefinition())
              .append(w.isMastered() ? " ✅" : "").append("\n"));
        if (all.size() > 20) sb.append("\n_...and ").append(all.size()-20).append(" more. See the **Vocabulary** tab!_");
        return BotResponse.text(sb.toString());
    }

    private BotResponse listByCategory(String cat) {
        List<Word> words = vocab.getByCategory(cat);
        if (words.isEmpty())
            return BotResponse.text("No words in **\"" + cat + "\"**. Type **list words** to see all categories.");
        StringBuilder sb = new StringBuilder("📁 **" + cat.toUpperCase() + "** (" + words.size() + " words)\n\n");
        words.forEach(w -> sb.append("• **").append(w.getTerm()).append("** — ").append(w.getDefinition()).append("\n"));
        return BotResponse.text(sb.toString());
    }

    private BotResponse listByDifficulty(String diff) {
        List<Word> words = vocab.getByDifficulty(diff);
        if (words.isEmpty()) return BotResponse.text("No " + diffEmoji(diff) + " words yet.");
        StringBuilder sb = new StringBuilder(diffEmoji(diff) + " **WORDS** (" + words.size() + ")\n\n");
        words.forEach(w -> sb.append("• **").append(w.getTerm()).append("** — ").append(w.getDefinition()).append("\n"));
        return BotResponse.text(sb.toString());
    }

    // ── Fuzzy fallback ────────────────────────────────────────────────────────

    private BotResponse fuzzyFallback(String input) {
        List<Word> all = vocab.getAllWords();
        String[] tokens = nlp.removeStopWords(nlp.tokenize(input));
        Word best = null; double bestSim = 0;
        for (Word w : all) {
            for (String tok : tokens) {
                double sim = nlp.similarity(tok, w.getTerm().toLowerCase());
                if (sim > bestSim) { bestSim = sim; best = w; }
            }
        }
        if (bestSim > 0.75 && best != null)
            return define(best.getTerm());

        String[] unknowns = {
            "🤔 I didn't understand **\"" + input + "\"**. Type **help** to see all commands!",
            "💭 Not sure about that. Try **define [word]**, **quiz me**, or **help**!"
        };
        return BotResponse.text(unknowns[new Random().nextInt(unknowns.length)]);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isCorrectAnswer(String answer, String correct) {
        String a = answer.trim().toLowerCase();
        String c = correct.trim().toLowerCase();
        if (a.equals(c)) return true;
        if (c.contains(a) && a.length() > 3) return true;
        return nlp.similarity(a, c) > 0.80;
    }

    private String hint(String term) {
        char[] h = new char[term.length()];
        Arrays.fill(h, '_');
        h[0] = term.charAt(0);
        if (term.length() > 3) h[term.length()-1] = term.charAt(term.length()-1);
        return new String(h).chars().mapToObj(c -> String.valueOf((char)c))
                            .reduce("", (a,b) -> a + " " + b).trim();
    }

    private String diffEmoji(String d) {
        return switch (d.toUpperCase()) {
            case "BEGINNER"     -> "🟢 Beginner";
            case "INTERMEDIATE" -> "🟡 Intermediate";
            case "ADVANCED"     -> "🔴 Advanced";
            default             -> d;
        };
    }

    private String divider() { return "─────────────────────────\n"; }

    private String scoreMsg(double s) {
        if (s >= 90) return "Outstanding! Vocabulary master! 🌟";
        if (s >= 80) return "Excellent work! Keep it up! 💪";
        if (s >= 60) return "Good job! A bit more practice will get you there! 📖";
        return "Don't give up — every mistake is progress! 💡";
    }

    private boolean cancel(String s) { return s.equalsIgnoreCase("cancel"); }
    private boolean hasText(String s) { return s != null && !s.isBlank(); }

    private BotResponse cancelled() {
        reset(); return BotResponse.text("Cancelled. What else can I help you with?");
    }

    private void reset() {
        state = State.IDLE; activeQuiz = null;
        pendingTerm = null; pendingDefinition = null; pendingExample = null; _pendingCat = "General";
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public State getState()           { return state; }
    public UserProgress getProgress() { return progress; }
    public void refreshProgress()     { this.progress = db.loadProgress(); }
}
