package ru.terentjev;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import ru.terentjev.service.FirstRunService;

import java.util.Collection;

@SpringBootApplication
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private final Collection<FirstRunService> services;

    public Application(Collection<FirstRunService> services) {
        this.services = services;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        log.info("Startup...");
        for (FirstRunService service : services) {
            String name = StringUtils.substringBefore(
                    service.getClass()
                            .getSimpleName(),
                    "$"
            ) + "#firstRun()";
            log.info("Service {} run", name);
            service.firstRun();
            log.info("Service {} complete", name);
        }
        log.info("Startup complete");
    }
}
