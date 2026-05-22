package com.languagebuddy.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** A single chat message. */
public class Message {
    public enum Type { USER, BOT, SYSTEM, SUCCESS, ERROR }
    private final String content;
    private final Type type;
    private final LocalDateTime timestamp;

    public Message(String content, Type type) {
        this.content   = content;
        this.type      = type;
        this.timestamp = LocalDateTime.now();
    }
    public String getContent()       { return content; }
    public Type getType()            { return type; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public String getFormattedTime() { return timestamp.format(DateTimeFormatter.ofPattern("HH:mm")); }
}
