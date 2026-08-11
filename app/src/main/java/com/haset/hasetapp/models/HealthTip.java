package com.haset.hasetapp.models;

public class HealthTip {
    private final String id;
    private final String text;
    private final String author;
    private final long timestamp;

    public HealthTip(String id, String text, String author, long timestamp) {
        this.id = id;
        this.text = text;
        this.author = author;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getAuthor() { return author; }
    public long getTimestamp() { return timestamp; }
}
