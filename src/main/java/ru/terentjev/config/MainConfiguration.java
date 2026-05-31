package ru.terentjev.config;


import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.terentjev.repository.BookRepository;
import ru.terentjev.service.BookService;
import ru.terentjev.service.BookUploadService;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;


@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConfigurationPropertiesScan(basePackageClasses = MainConfiguration.class)
public class MainConfiguration implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("api/v1", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Bean
    public Clock clock(@Value("${application.zone-id:Europe/Moscow}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }

    /**
     * Логирование входящих запросов
     */
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("");
        filter.setAfterMessageSuffix("");
        return filter;
    }

    /**
     * Дополнительные поля в ответ об ошибке
     */
    @Bean
    public ErrorAttributes errorAttributes() {
        List<String> exposeMdcKeys = List.of("traceId", "spanId");
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
                for (String key : exposeMdcKeys) {
                    errorAttributes.put(key, MDC.get(key));
                }
                return errorAttributes;
            }
        };
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

}


