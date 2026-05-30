package ru.terentjev.config;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.core.mapping.JdbcValue;
import ru.terentjev.config.converter.ToEnumConverter;
import ru.terentjev.config.converter.TimestampToOffsetDateTimeConverter;

import javax.sql.DataSource;
import java.sql.JDBCType;
import java.time.Clock;
import java.util.List;

@Configuration
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final Clock clock;

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .build();
    }

    public JdbcConfiguration(Clock clock) {
        this.clock = clock;
    }

    @NonNull
    @Override
    protected List<?> userConverters() {
        return List.of(
                new ToEnumConverter(JDBCType.OTHER),
                new TimestampToOffsetDateTimeConverter(clock)
        );
    }
}
