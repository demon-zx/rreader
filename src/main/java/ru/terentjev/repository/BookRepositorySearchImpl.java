package ru.terentjev.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import ru.terentjev.repository.model.BookEntity;

import java.util.List;

/**
 * Custom fragment for {@link BookRepository}. Uses the fluent {@link Criteria} API so a single
 * search term can be matched against several columns without a derived-name explosion, and so
 * {@link Pageable} (with its count query) is handled natively — unlike {@code @Query} methods.
 * Name = Interface + Impl
 */
public class BookRepositorySearchImpl implements BookRepositorySearch {

    private final JdbcAggregateTemplate template;

    public BookRepositorySearchImpl(JdbcAggregateTemplate template) {
        this.template = template;
    }

    @Override
    public Page<BookEntity> search(String q, Pageable pageable) {
        String like = "%" + q + "%";
        Criteria criteria = Criteria.where("author").like(like).ignoreCase(true)
                .or("title").like(like).ignoreCase(true)
                .or("sequenceName").like(like).ignoreCase(true);
        Query query = Query.query(criteria);
        long total = template.count(query, BookEntity.class);
        List<BookEntity> content = template.findAll(query.with(pageable), BookEntity.class);
        return new PageImpl<>(content, pageable, total);
    }
}
