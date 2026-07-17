package ru.terentjev.service.render;

import java.util.Map;

public class Info {

    private final int pages;
    private final String author;
    private final String title;
    private final String sequenceName;
    private final Integer sequenceNumber;
    private final Map<Integer, Integer> positionByPage;

    public Info(int pages, String author, String title, String sequenceName, Integer sequenceNumber, Map<Integer, Integer> positionByPage) {
        this.pages = pages;
        this.author = author;
        this.title = title;
        this.sequenceName = sequenceName;
        this.sequenceNumber = sequenceNumber;
        this.positionByPage = positionByPage;
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
    
    public Map<Integer, Integer> getPositionByPage() {
        return positionByPage;
    }
    
}
