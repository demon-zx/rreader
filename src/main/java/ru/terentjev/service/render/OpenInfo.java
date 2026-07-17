package ru.terentjev.service.render;

import java.util.Map;

public class OpenInfo extends Info {

    private final int pages;
    private final Map<Integer, Integer> positionByPage;


    public OpenInfo(
            String author,
            String title,
            int pages,
            String sequenceName,
            Integer sequenceNumber,
            Map<Integer, Integer> positionByPage
    ) {
        super(author, title, sequenceName, sequenceNumber);
        this.pages = pages;
        this.positionByPage = positionByPage;
    }

    public int getPages() {
        return pages;
    }
    
    public Map<Integer, Integer> getPositionByPage() {
        return positionByPage;
    }
    
}
