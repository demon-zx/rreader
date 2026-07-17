package ru.terentjev.service.render;

public class Info {

    private final String author;
    private final String title;
    private final String sequenceName;
    private final Integer sequenceNumber;

    public Info(String author, String title, String sequenceName, Integer sequenceNumber) {
        this.author = author;
        this.title = title;
        this.sequenceName = sequenceName;
        this.sequenceNumber = sequenceNumber;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

}
