package ru.terentjev.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.terentjev.repository.model.ExampleEntity;

@Repository
public interface ExampleRepository extends CrudRepository<ExampleEntity, Integer> {
}
