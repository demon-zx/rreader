package ru.terentjev.service.render;

public class Info {

    private final int pages;
    private final String author;
    private final String title;
    private final String sequenceName;
    private final Integer sequenceNumber;

    public Info(int pages, String author, String title, String sequenceName, Integer sequenceNumber) {
        this.pages = pages;
        this.author = author;
        this.title = title;
        this.sequenceName = sequenceName;
        this.sequenceNumber = sequenceNumber;
    }

    public int getPages() {
        return pages;
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
