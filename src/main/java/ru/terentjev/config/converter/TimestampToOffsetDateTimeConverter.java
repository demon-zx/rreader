package ru.terentjev.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;

@ReadingConverter
public class TimestampToOffsetDateTimeConverter implements Converter<Timestamp, OffsetDateTime> {
    private final Clock clock;

    public TimestampToOffsetDateTimeConverter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public OffsetDateTime convert(Timestamp source) {
        return OffsetDateTime.ofInstant(source.toInstant(), clock.getZone());
    }
}
