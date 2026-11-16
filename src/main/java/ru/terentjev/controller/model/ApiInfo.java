package ru.terentjev.controller.model;

import java.util.List;
import java.util.Map;

public class ApiInfo {
    private final int pages;
    private final String author;
    private final String title;
    private final Map<Integer, Integer> positionByPage;
    private final List<ApiContent> content;

    public ApiInfo(
            int pages,
            String author,
            String title,
            Map<Integer, Integer> positionByPage,
            List<ApiContent> content
    ) {
        this.pages = pages;
        this.author = author;
        this.title = title;
        this.positionByPage = positionByPage;
        this.content = content;
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

    public Map<Integer, Integer> getPositionByPage() {
        return positionByPage;
    }

    public List<ApiContent> getContent() {
        return content;
    }
}
