package com.languagebuddy.database;

import com.languagebuddy.model.QuizResult;
import com.languagebuddy.model.UserProgress;
import com.languagebuddy.model.Word;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton SQLite/JDBC data layer.
 * Tables: words, user_progress, quiz_results.
 */
public class DatabaseManager {

    private static final String DB_URL  = "jdbc:sqlite:language_buddy.db";
    private static DatabaseManager instance;
    private Connection conn;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(true);
            applyPragmas();
            createTables();
            seedVocabulary();
        } catch (Exception e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    private void applyPragmas() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA foreign_keys=ON");
        }
    }

    private void createTables() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS words (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    term             TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    definition       TEXT NOT NULL,
                    example_sentence TEXT,
                    category         TEXT NOT NULL DEFAULT 'General',
                    difficulty       TEXT NOT NULL DEFAULT 'BEGINNER',
                    times_reviewed   INTEGER NOT NULL DEFAULT 0,
                    times_correct    INTEGER NOT NULL DEFAULT 0,
                    easiness_factor  REAL    NOT NULL DEFAULT 2.5,
                    repetitions      INTEGER NOT NULL DEFAULT 0,
                    interval_days    INTEGER NOT NULL DEFAULT 1,
                    added_date       TEXT NOT NULL,
                    last_reviewed    TEXT
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS user_progress (
                    id                    INTEGER PRIMARY KEY DEFAULT 1,
                    total_words_learned   INTEGER NOT NULL DEFAULT 0,
                    total_quizzes_taken   INTEGER NOT NULL DEFAULT 0,
                    total_correct_answers INTEGER NOT NULL DEFAULT 0,
                    current_streak        INTEGER NOT NULL DEFAULT 0,
                    longest_streak        INTEGER NOT NULL DEFAULT 0,
                    total_xp              INTEGER NOT NULL DEFAULT 0
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS quiz_results (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    quiz_type       TEXT NOT NULL,
                    total_questions INTEGER NOT NULL,
                    correct_answers INTEGER NOT NULL,
                    taken_at        TEXT NOT NULL
                )""");
            s.execute("INSERT OR IGNORE INTO user_progress (id) VALUES (1)");
        }
    }

    // ── Seed ─────────────────────────────────────────────────────────────────

    private void seedVocabulary() throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM words")) {
            if (rs.getInt(1) > 0) return;
        }

        List<Word> seed = List.of(
            w("Eloquent",      "Fluent or persuasive in speaking or writing",               "She gave an eloquent speech that moved the entire room.",           "Communication", "INTERMEDIATE"),
            w("Ubiquitous",    "Present or found everywhere",                               "Smartphones have become ubiquitous in modern everyday life.",        "General",       "ADVANCED"),
            w("Serendipity",   "A happy chance discovery",                                  "It was pure serendipity that they met at the coffee shop.",          "General",       "INTERMEDIATE"),
            w("Algorithm",     "A step-by-step procedure for solving a problem",            "The search algorithm returns results in milliseconds.",              "Technology",    "BEGINNER"),
            w("Ephemeral",     "Lasting for a very short time",                             "The ephemeral beauty of cherry blossoms lasts only weeks.",          "General",       "ADVANCED"),
            w("Pragmatic",     "Dealing with things sensibly and realistically",            "We need a pragmatic approach to solve this crisis.",                 "General",       "INTERMEDIATE"),
            w("Verbose",       "Using more words than necessary; wordy",                    "The verbose report could have been a single paragraph.",             "Communication", "INTERMEDIATE"),
            w("Catalyst",      "Something that speeds up a process or change",              "The discovery was a catalyst for a new era of science.",             "Science",       "INTERMEDIATE"),
            w("Ambiguous",     "Open to more than one interpretation",                      "The instructions were ambiguous, causing widespread confusion.",      "Communication", "BEGINNER"),
            w("Iterate",       "To perform or repeat a process in a loop",                  "We iterate over the array to find the target element.",              "Technology",    "BEGINNER"),
            w("Abstract",      "Existing as an idea rather than a concrete thing",          "Freedom is an abstract concept that means different things.",         "Philosophy",    "BEGINNER"),
            w("Resilient",     "Able to recover quickly from difficulties",                 "Children are often more resilient than we expect.",                  "Psychology",    "INTERMEDIATE"),
            w("Paradigm",      "A typical example, pattern, or model",                      "Object-oriented is the dominant programming paradigm.",              "Technology",    "ADVANCED"),
            w("Deduce",        "Arrive at a conclusion by logical reasoning",               "From the clues, the detective could deduce the culprit.",            "Logic",         "BEGINNER"),
            w("Synthesis",     "Combining parts into a coherent whole",                     "The essay requires a synthesis of multiple scholarly sources.",       "Academic",      "INTERMEDIATE"),
            w("Meticulous",    "Very careful and precise about details",                    "She was meticulous in recording every step of her experiment.",      "General",       "INTERMEDIATE"),
            w("Hierarchy",     "A system ranked into grades or classes",                    "The corporate hierarchy has many distinct management layers.",        "Organization",  "BEGINNER"),
            w("Coherent",      "Logical, consistent, and easy to understand",               "Please write a coherent summary of the main arguments.",             "Communication", "BEGINNER"),
            w("Heuristic",     "A practical problem-solving approach using rules of thumb", "The teacher used heuristic methods to guide student discovery.",     "Education",     "ADVANCED"),
            w("Concise",       "Giving information clearly and briefly",                    "Keep your answers concise and directly to the point.",               "Communication", "BEGINNER"),
            w("Anomaly",       "Something that deviates from the norm",                     "The data showed a clear anomaly in the second quarter results.",      "Science",       "INTERMEDIATE"),
            w("Diligent",      "Having or showing care and effort in work",                 "The diligent student reviewed her notes every evening.",             "General",       "BEGINNER"),
            w("Nuance",        "A subtle difference in meaning or expression",              "He understood the nuance between confidence and arrogance.",          "Communication", "ADVANCED"),
            w("Empirical",     "Based on observation and experiment, not theory",           "The study requires empirical evidence to support its claims.",        "Science",       "ADVANCED"),
            w("Inference",     "A conclusion reached from evidence and reasoning",          "The inference drawn from the data was surprising.",                  "Logic",         "INTERMEDIATE")
        );
        seed.forEach(this::insertWord);
    }

    private Word w(String term, String def, String ex, String cat, String diff) {
        return new Word(term, def, ex, cat, diff);
    }

    // ── Word CRUD ─────────────────────────────────────────────────────────────

    public boolean insertWord(Word word) {
        String sql = """
            INSERT OR IGNORE INTO words
              (term,definition,example_sentence,category,difficulty,
               times_reviewed,times_correct,easiness_factor,repetitions,interval_days,added_date)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, word.getTerm());
            ps.setString(2, word.getDefinition());
            ps.setString(3, word.getExampleSentence());
            ps.setString(4, word.getCategory());
            ps.setString(5, word.getDifficulty());
            ps.setInt(6, 0); ps.setInt(7, 0);
            ps.setDouble(8, 2.5); ps.setInt(9, 0); ps.setInt(10, 1);
            ps.setString(11, LocalDateTime.now().toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("insertWord: " + e.getMessage()); return false; }
    }

    public boolean updateWord(Word word) {
        String sql = """
            UPDATE words SET definition=?,example_sentence=?,category=?,difficulty=?,
              times_reviewed=?,times_correct=?,easiness_factor=?,repetitions=?,
              interval_days=?,last_reviewed=? WHERE id=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, word.getDefinition());
            ps.setString(2, word.getExampleSentence());
            ps.setString(3, word.getCategory());
            ps.setString(4, word.getDifficulty());
            ps.setInt(5, word.getTimesReviewed());
            ps.setInt(6, word.getTimesCorrect());
            ps.setDouble(7, word.getEasinessFactor());
            ps.setInt(8, word.getRepetitions());
            ps.setInt(9, word.getIntervalDays());
            ps.setString(10, word.getLastReviewed() != null ? word.getLastReviewed().toString() : null);
            ps.setInt(11, word.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("updateWord: " + e.getMessage()); return false; }
    }

    public boolean deleteWord(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM words WHERE id=?")) {
            ps.setInt(1, id); return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public List<Word> getAllWords() {
        return query("SELECT * FROM words ORDER BY term ASC", null);
    }

    public List<Word> getWordsByCategory(String cat) {
        return query("SELECT * FROM words WHERE category=? ORDER BY term ASC", new Object[]{cat});
    }

    public List<Word> getWordsByDifficulty(String diff) {
        return query("SELECT * FROM words WHERE difficulty=? ORDER BY term ASC", new Object[]{diff});
    }

    public Word findByTerm(String term) {
        List<Word> r = query("SELECT * FROM words WHERE term=? COLLATE NOCASE", new Object[]{term});
        return r.isEmpty() ? null : r.get(0);
    }

    public List<Word> search(String q) {
        String p = "%" + q + "%";
        return query("SELECT * FROM words WHERE term LIKE ? OR definition LIKE ? ORDER BY term ASC",
                     new Object[]{p, p});
    }

    public List<Word> getRandom(int limit) {
        return query("SELECT * FROM words ORDER BY RANDOM() LIMIT ?", new Object[]{limit});
    }

    public List<Word> getDueForReview(int limit) {
        return query("""
            SELECT * FROM words
             WHERE last_reviewed IS NULL
                OR date(last_reviewed, '+' || interval_days || ' days') <= date('now')
             ORDER BY RANDOM() LIMIT ?""", new Object[]{limit});
    }

    public List<String> getAllCategories() {
        List<String> cats = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT DISTINCT category FROM words ORDER BY category ASC")) {
            while (rs.next()) cats.add(rs.getString(1));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return cats;
    }

    public int countWords() {
        return scalar("SELECT COUNT(*) FROM words");
    }

    public int countMastered() {
        return scalar("SELECT COUNT(*) FROM words WHERE times_reviewed>=5 AND CAST(times_correct AS REAL)/times_reviewed>=0.8");
    }

    // ── Quiz Results ──────────────────────────────────────────────────────────

    public void saveQuizResult(QuizResult r) {
        String sql = "INSERT INTO quiz_results(quiz_type,total_questions,correct_answers,taken_at) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getQuizType());
            ps.setInt(2, r.getTotalQuestions());
            ps.setInt(3, r.getCorrectAnswers());
            ps.setString(4, r.getTakenAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("saveQuizResult: " + e.getMessage()); }
    }

    public List<QuizResult> getRecentQuizResults(int limit) {
        List<QuizResult> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz_results ORDER BY taken_at DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuizResult r = new QuizResult(
                        rs.getString("quiz_type"),
                        rs.getInt("total_questions"),
                        rs.getInt("correct_answers"),
                        LocalDateTime.parse(rs.getString("taken_at"))
                    );
                    r.setId(rs.getInt("id"));
                    list.add(r);
                }
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    // ── User Progress ─────────────────────────────────────────────────────────

    public UserProgress loadProgress() {
        UserProgress p = new UserProgress();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM user_progress WHERE id=1")) {
            if (rs.next()) {
                p.setTotalWordsLearned(rs.getInt("total_words_learned"));
                p.setTotalQuizzesTaken(rs.getInt("total_quizzes_taken"));
                p.setTotalCorrectAnswers(rs.getInt("total_correct_answers"));
                p.setCurrentStreak(rs.getInt("current_streak"));
                p.setLongestStreak(rs.getInt("longest_streak"));
                p.setTotalXP(rs.getInt("total_xp"));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return p;
    }

    public void saveProgress(UserProgress p) {
        String sql = """
            UPDATE user_progress SET
              total_words_learned=?,total_quizzes_taken=?,total_correct_answers=?,
              current_streak=?,longest_streak=?,total_xp=? WHERE id=1""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,p.getTotalWordsLearned()); ps.setInt(2,p.getTotalQuizzesTaken());
            ps.setInt(3,p.getTotalCorrectAnswers()); ps.setInt(4,p.getCurrentStreak());
            ps.setInt(5,p.getLongestStreak()); ps.setInt(6,p.getTotalXP());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Word> query(String sql, Object[] params) {
        List<Word> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null) for (int i = 0; i < params.length; i++) ps.setObject(i+1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWord(rs));
            }
        } catch (SQLException e) { System.err.println("query: " + e.getMessage()); }
        return list;
    }

    private Word mapWord(ResultSet rs) throws SQLException {
        Word w = new Word();
        w.setId(rs.getInt("id"));
        w.setTerm(rs.getString("term"));
        w.setDefinition(rs.getString("definition"));
        w.setExampleSentence(rs.getString("example_sentence"));
        w.setCategory(rs.getString("category"));
        w.setDifficulty(rs.getString("difficulty"));
        w.setTimesReviewed(rs.getInt("times_reviewed"));
        w.setTimesCorrect(rs.getInt("times_correct"));
        w.setEasinessFactor(rs.getDouble("easiness_factor"));
        w.setRepetitions(rs.getInt("repetitions"));
        w.setIntervalDays(rs.getInt("interval_days"));
        String added = rs.getString("added_date");
        if (added != null) w.setAddedDate(LocalDateTime.parse(added));
        String lastR = rs.getString("last_reviewed");
        if (lastR != null) w.setLastReviewed(LocalDateTime.parse(lastR));
        return w;
    }

    private int scalar(String sql) {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.getInt(1);
        } catch (SQLException e) { return 0; }
    }

    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }
}
