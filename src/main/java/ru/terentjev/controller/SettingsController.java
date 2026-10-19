package ru.terentjev.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.terentjev.security.SecurityUtil;
import ru.terentjev.service.SettingsService;
import ru.terentjev.service.model.KeyValue;
import ru.terentjev.service.model.Settings;
import ru.terentjev.service.model.SettingsScope;

import io.micrometer.core.annotation.Timed;

import java.util.List;
import java.util.UUID;

@Timed
@RestController
@RequestMapping("settings")
public class SettingsController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        super(log);
        this.settingsService = settingsService;
    }

    @GetMapping("fonts")
    public List<String> fonts() {
        return run(
            () -> {
                log.debug("fonts");
                return settingsService.fonts();
            },
            b -> b
        );
    }

    @GetMapping("definitions")
    public List<String> definitions(@RequestParam SettingsScope scope) {
        return run(
            () -> {
                log.debug("definitions scope={}", scope);
                return settingsService.definitions(scope);
            },
            b -> b
        );
    }

    @GetMapping
    public Settings read(
            @RequestParam UUID deviceUuid,
            @RequestParam SettingsScope scope
    ) {
        return run(
            () -> {
                log.debug("read settings deviceUuid={} scope={}", deviceUuid, scope);
                return settingsService.read(deviceUuid, scope);
            },
            b -> b
        );
    }

    @PutMapping
    public void save(
            @RequestParam UUID deviceUuid,
            @RequestParam SettingsScope scope,
            @RequestBody List<KeyValue> values
    ) {
        run(() -> {
            log.debug("save settings deviceUuid={} scope={}", deviceUuid, scope);
            settingsService.save(
                    deviceUuid,
                    scope,
                    values,
                    SecurityUtil.userRequired()
            );
        });
    }

}
