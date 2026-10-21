package ru.terentjev.service.model;

import java.util.List;

public class Page<T> {
    private final List<T> items;
    private final int all;
    private final int allPages;

    public Page(List<T> items, int all, int allPages) {
        this.items = items;
        this.all = all;
        this.allPages = allPages;
    }

    public List<T> getItems() {
        return items;
    }

    public int getAll() {
        return all;
    }

    public int getAllPages() {
        return allPages;
    }
}
