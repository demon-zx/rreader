package ru.terentjev.service.render;

import java.util.List;
import java.util.Map;

public class OpenInfo extends Info {
    private final int pages;
    private final Map<Integer, Integer> positionByPage;
    private final List<Content> content;

    public OpenInfo(
            String author,
            String title,
            int pages,
            String sequenceName,
            Integer sequenceNumber,
            Map<Integer, Integer> positionByPage,
            List<Content> content
    ) {
        super(author, title, sequenceName, sequenceNumber);
        this.pages = pages;
        this.positionByPage = positionByPage;
        this.content = content;
    }

    public int getPages() {
        return pages;
    }

    public Map<Integer, Integer> getPositionByPage() {
        return positionByPage;
    }

    public List<Content> getContent() {
        return content;
    }
}
