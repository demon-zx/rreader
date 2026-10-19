package ru.terentjev.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("settings")
public class SettingsHtmlController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(SettingsHtmlController.class);

    public SettingsHtmlController() {
        super(log);
    }

    @GetMapping
    public String settings() {
        return "settings";
    }
}
