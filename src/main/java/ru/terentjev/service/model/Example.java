package ru.terentjev.service.model;

public class Example {

    private final int id;
    private final String name;

    public Example(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
