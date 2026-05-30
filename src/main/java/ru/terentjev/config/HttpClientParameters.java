package ru.terentjev.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;

@Validated
public class HttpClientParameters {

    @NotEmpty
    private String url;
    private Duration connectionTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(15);

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = StringUtils.removeEnd(url, "/");
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getConnectionTimeoutMillis() {
        return (int) connectionTimeout.toMillis();
    }

    public int getReadTimeoutMillis() {
        return (int) readTimeout.toMillis();
    }
}
