package ru.terentjev.repository.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("Example")
public class ExampleEntity {

    @Id
    @Column("id")
    private final int id;
    @Column("name")
    private final String name;

    public ExampleEntity(int id, String name) {
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
