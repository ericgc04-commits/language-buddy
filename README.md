# 📖 Language Buddy
### Java-Powered Vocabulary Learning Chatbot
> York St John University — Dissertation Project  
> **Eric González Ceballos** · Student Number: 250158429

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | **17+** | https://adoptium.net |
| Maven | **3.8+** | https://maven.apache.org |
| IntelliJ IDEA (recommended) | Any | https://jetbrains.com/idea |

### Run

```bash
cd language-buddy
mvn javafx:run
```

### Build fat JAR

```bash
mvn clean package -DskipTests
java -jar target/language-buddy-1.0.0.jar
```

### Run unit tests

```bash
mvn test
```

---

## 🏗️ Architecture (MVC)

```
┌─────────────────────────────────────────────────────────┐
│                     VIEW (JavaFX)                        │
│   MainView.java — Chat · Vocabulary · Statistics UI      │
└──────────────────────┬──────────────────────────────────┘
                       │ Events / Responses
┌──────────────────────▼──────────────────────────────────┐
│                  CONTROLLER                              │
│   MainController.java — bridges View ↔ Services         │
└──────────┬───────────────────────────┬──────────────────┘
           │                           │
┌──────────▼──────────┐   ┌───────────▼──────────────────┐
│  VocabularyService  │   │       ChatbotService          │
│  CRUD, SM-2, Stats  │   │  Intent routing, Quiz/Cards   │
└──────────┬──────────┘   └───────────┬──────────────────┘
           │                          │
┌──────────▼──────────────────────────▼──────────────────┐
│                    NLPProcessor                          │
│  Tokenisation · Intent detection · Fuzzy matching        │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                DatabaseManager (SQLite/JDBC)             │
│   words · user_progress · quiz_results tables           │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
language-buddy/
├── pom.xml
└── src/
    ├── main/java/com/languagebuddy/
    │   ├── App.java                        ← Entry point
    │   ├── module-info.java
    │   ├── model/
    │   │   ├── Word.java                   ← SM-2 spaced repetition
    │   │   ├── Message.java
    │   │   ├── UserProgress.java           ← XP / levels / streaks
    │   │   ├── QuizSession.java
    │   │   └── QuizResult.java             ← Persisted quiz history
    │   ├── database/
    │   │   └── DatabaseManager.java        ← SQLite/JDBC singleton
    │   ├── nlp/
    │   │   └── NLPProcessor.java           ← Apache OpenNLP + Levenshtein
    │   ├── service/
    │   │   ├── VocabularyService.java
    │   │   └── ChatbotService.java         ← Dialog state machine
    │   ├── controller/
    │   │   └── MainController.java
    │   └── view/
    │       └── MainView.java               ← Premium dark JavaFX UI
    └── test/java/com/languagebuddy/
        ├── model/
        │   ├── WordTest.java               ← SM-2, mastery, equality
        │   └── UserProgressTest.java       ← XP, levels, accuracy
        └── nlp/
            └── NLPProcessorTest.java       ← Tokenise, intent, fuzzy
```

---

## 💬 Chat Commands

| Command | Action |
|---------|--------|
| `define [word]` | Look up a word's definition |
| `word of the day` | Today's featured word |
| `add [word]` | Guided multi-step word addition |
| `quiz me` | 10-question vocabulary quiz |
| `quiz me [category]` | Quiz by topic |
| `beginner quiz` / `advanced quiz` | Quiz by difficulty |
| `flashcards` | SM-2 scheduled self-assessment |
| `list words` | Browse all vocabulary |
| `words in [category]` | Filter by topic |
| `beginner words` / `advanced words` | Filter by level |
| `stats` | XP, level, accuracy, streaks |
| `help` | All commands |

---

## 🗄️ Database Schema

```sql
-- Vocabulary with SM-2 fields
CREATE TABLE words (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    term             TEXT NOT NULL UNIQUE,
    definition       TEXT NOT NULL,
    example_sentence TEXT,
    category         TEXT DEFAULT 'General',
    difficulty       TEXT DEFAULT 'BEGINNER',
    times_reviewed   INTEGER DEFAULT 0,
    times_correct    INTEGER DEFAULT 0,
    easiness_factor  REAL    DEFAULT 2.5,   -- SM-2
    repetitions      INTEGER DEFAULT 0,      -- SM-2
    interval_days    INTEGER DEFAULT 1,      -- SM-2
    added_date       TEXT,
    last_reviewed    TEXT
);

-- Learning progress
CREATE TABLE user_progress (
    id INTEGER PRIMARY KEY DEFAULT 1,
    total_words_learned   INTEGER DEFAULT 0,
    total_quizzes_taken   INTEGER DEFAULT 0,
    total_correct_answers INTEGER DEFAULT 0,
    current_streak        INTEGER DEFAULT 0,
    longest_streak        INTEGER DEFAULT 0,
    total_xp              INTEGER DEFAULT 0
);

-- Quiz history
CREATE TABLE quiz_results (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    quiz_type       TEXT,
    total_questions INTEGER,
    correct_answers INTEGER,
    taken_at        TEXT
);
```

---

## 🛠️ Technology Stack

| Component | Technology | Notes |
|-----------|-----------|-------|
| Language | Java 17 | Modern features (records, switch expressions, text blocks) |
| UI | JavaFX 21 | Premium dark theme, animations |
| Database | SQLite via JDBC | WAL mode, foreign keys |
| NLP | Apache OpenNLP 2.3 | Tokenisation + intent detection |
| Build | Apache Maven | Fat JAR with shade plugin |
| Testing | JUnit 5 | Parameterised tests |
| Algorithm | SM-2 (SuperMemo) | Spaced repetition scheduling |
| Pattern | MVC | Clean separation of concerns |

---

## ✅ Dissertation Checklist

- [x] Java 17 + Spring-style OOP principles
- [x] JavaFX frontend with premium dark UI
- [x] Apache OpenNLP (tokeniser, intent detection, Levenshtein fuzzy matching)
- [x] Maven dependency management
- [x] SQLite with JDBC (3 tables, WAL mode)
- [x] MVC architecture
- [x] Conversational chatbot with dialog state machine
- [x] Quiz mode with scoring and hints
- [x] Flashcard mode with SM-2 spaced repetition
- [x] Guided multi-turn word addition flow
- [x] XP + level progression system (5 levels)
- [x] Quiz history persisted to database
- [x] Statistics dashboard with category/difficulty breakdown
- [x] Unit tests (JUnit 5) — Word, UserProgress, NLPProcessor
- [x] 25 pre-loaded vocabulary words across 8 categories
- [x] Javadoc on all public classes and methods
