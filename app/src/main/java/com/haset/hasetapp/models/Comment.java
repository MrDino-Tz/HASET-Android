package com.haset.hasetapp.models;

public class Comment {
    private String id;
    private String author;
    private String content;
    private long timestamp;

    public Comment(String id, String author, String content, long timestamp) {
        this.id = id;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}



