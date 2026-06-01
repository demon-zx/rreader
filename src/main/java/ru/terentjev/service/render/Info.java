package ru.terentjev.service.render;

public class Info {

    private final int pages;
    private final String author;
    private final String title;

    public Info(int pages, String author, String title) {
        this.pages = pages;
        this.author = author;
        this.title = title;
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
}
