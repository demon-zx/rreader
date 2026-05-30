package ru.terentjev.controller.model;

public class ApiExample extends ApiExampleDetails {

    private final int id;

    public ApiExample(int id, String name) {
        super(name);
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
