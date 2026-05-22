package com.languagebuddy.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Immutable record of a completed quiz — stored in DB for history. */
public class QuizResult {

    private int id;
    private final String quizType;
    private final int totalQuestions;
    private final int correctAnswers;
    private final LocalDateTime takenAt;

    public QuizResult(String quizType, int totalQuestions, int correctAnswers, LocalDateTime takenAt) {
        this.quizType       = quizType;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.takenAt        = takenAt;
    }

    public double getScorePercent() {
        return totalQuestions == 0 ? 0.0 : (double) correctAnswers / totalQuestions * 100.0;
    }

    public String getFormattedDate() {
        return takenAt.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"));
    }

    public int getId()              { return id; }
    public void setId(int id)       { this.id = id; }
    public String getQuizType()     { return quizType; }
    public int getTotalQuestions()  { return totalQuestions; }
    public int getCorrectAnswers()  { return correctAnswers; }
    public LocalDateTime getTakenAt(){ return takenAt; }
}
