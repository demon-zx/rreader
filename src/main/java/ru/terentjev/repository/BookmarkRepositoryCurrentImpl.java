package ru.terentjev.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import ru.terentjev.repository.model.BookmarkEntity;

import java.util.List;

/**
 * Custom fragment for {@link BookmarkRepository}. Uses the fluent {@link Criteria} API so that
 * {@link Pageable} (limit/offset + sort) is handled natively — unlike {@code @Query} methods.
 * Name = Interface + Impl
 */
public class BookmarkRepositoryCurrentImpl implements BookmarkRepositoryCurrent {
    private final JdbcAggregateTemplate template;

    public BookmarkRepositoryCurrentImpl(JdbcAggregateTemplate template) {
        this.template = template;
    }

    @Override
    public List<BookmarkEntity> findCurrent(String username, String title, Pageable pageable) {
        Criteria criteria = Criteria.where("username")
                .is(username)
                .and("title")
                .is(title);
        return template.findAll(Query.query(criteria).with(pageable), BookmarkEntity.class);
    }
}
