package ru.terentjev.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.mapping.JdbcValue;

import java.sql.SQLType;

@WritingConverter
public class ToEnumConverter implements Converter<Enum<?>, JdbcValue> {
    private SQLType columnType;

    /**
     * @param columnType Тип целевой колонки в базе данных, если тип enum (есть в
     *                   postgres) то указать JdbcValue.OTHER, в sql-server будет
     *                   varchar для него указать JdbcValue.VARCHAR,
     */
    public ToEnumConverter(SQLType columnType) {
        this.columnType = columnType;
    }

    @Override
    public JdbcValue convert(Enum<?> source) {
        return JdbcValue.of(source.name(), columnType);
    }
}
