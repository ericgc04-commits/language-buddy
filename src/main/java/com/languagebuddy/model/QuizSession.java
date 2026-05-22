package com.languagebuddy.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Active quiz session state. */
public class QuizSession {

    public enum QuizType { DEFINITION, TERM, MULTIPLE_CHOICE, FILL_IN_BLANK }

    private final List<Word> words;
    private final QuizType quizType;
    private int currentIndex;
    private int correctCount;
    private int totalAnswered;
    private boolean active;
    private Word currentWord;
    private final LocalDateTime startedAt;

    public QuizSession(List<Word> words, QuizType quizType) {
        this.words        = new ArrayList<>(words);
        this.quizType     = quizType;
        this.currentIndex = 0;
        this.active       = !words.isEmpty();
        this.startedAt    = LocalDateTime.now();
        this.currentWord  = words.isEmpty() ? null : words.get(0);
    }

    public boolean hasNext()   { return currentIndex < words.size(); }

    public Word nextWord() {
        if (!hasNext()) { active = false; return null; }
        currentWord = words.get(currentIndex++);
        return currentWord;
    }

    public void recordAnswer(boolean correct) {
        totalAnswered++;
        if (correct) correctCount++;
    }

    public double getScore() {
        return totalAnswered == 0 ? 0.0 : (double) correctCount / totalAnswered * 100.0;
    }

    public QuizResult toResult() {
        return new QuizResult(quizType.name(), totalAnswered, correctCount, startedAt);
    }

    public int getCorrectCount()    { return correctCount; }
    public int getTotalAnswered()   { return totalAnswered; }
    public int getTotalWords()      { return words.size(); }
    public QuizType getQuizType()   { return quizType; }
    public boolean isActive()       { return active; }
    public void setActive(boolean a){ active = a; }
    public Word getCurrentWord()    { return currentWord; }
    public int getCurrentIndex()    { return currentIndex; }
}
