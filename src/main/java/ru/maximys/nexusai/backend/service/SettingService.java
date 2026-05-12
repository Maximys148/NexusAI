package ru.maximys.nexusai.backend.service;

import atlantafx.base.theme.*;
import jakarta.annotation.PostConstruct;
import javafx.application.Application;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.maximys.nexusai.backend.config.AppSettings;

import java.util.HashMap;
import java.util.Map;

// Сервис отвечающий за бизнес логику контролера SettingController

@Service
public class SettingService {

    private static final Logger log = LoggerFactory.getLogger(SettingService.class);

    private final LanguageService langService;
    private final AppSettings appSettings;

    private final Map<String, String> languages = Map.of(
            "Русский", "ru",
            "English", "en"
    );

    public SettingService(LanguageService langService, AppSettings appSettings) {
        this.langService = langService;
        this.appSettings = appSettings;
    }

    // Загружает сохраненную тему
    @PostConstruct
    public void init() {
        String savedTheme = appSettings.getSavedTheme();
        applyTheme(savedTheme);
    }

    public void initSettingsView(ComboBox<String> langCombo, ComboBox<String> themeCombo, Map<String, Labeled> uiElements) {

        // 1. Локализация
        bindUserInterface(uiElements);

        // 2. Смена языка
        setupLanguageSelection(langCombo);

        // 3. Смена языка
        setupThemeSelection(themeCombo);

    }

    private void bindUserInterface(Map<String, Labeled> elements) {
        langService.bindText(elements.get("header"), "ui.settings.header");
        langService.bindText(elements.get("themeTitleLabel"), "ui.settings.theme");
        langService.bindText(elements.get("languageTitleLabel"), "ui.settings.language");
    }

    private void setupLanguageSelection(ComboBox<String> combo) {
        combo.getItems().addAll(languages.keySet());

        // Установка текущего значения
        String currentCode = langService.getCurrentLangCode();
        languages.forEach((name, code) -> {
            if (code.equals(currentCode)) combo.setValue(name);
        });

        // Слушатель смены языка
        combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String code = languages.get(newVal);
                langService.saveLanguage(code);
                log.info("Язык изменен на: {}", newVal);
            }
        });
    }

    private void setupThemeSelection(ComboBox<String> combo) {
        combo.getItems().addAll("Primer Light", "Primer Dark", "Nord Light", "Nord Dark", "Cupertino Light", "Cupertino Dark", "Dracula");

        combo.setValue(appSettings.getSavedTheme());

        combo.setOnAction(event -> {
            String selected = combo.getValue();
            applyTheme(selected);
            appSettings.saveTheme(selected);
            log.info("Тема изменена на: {}", selected);
        });
    }

    public void applyTheme(String themeName) {
        Map<String, Theme> themes = Map.of(
                "Primer Light", new PrimerLight(),
                "Primer Dark", new PrimerDark(),
                "Nord Light", new NordLight(),
                "Nord Dark", new NordDark(),
                "Cupertino Light", new CupertinoLight(),
                "Cupertino Dark", new CupertinoDark(),
                "Dracula", new Dracula()
        );

        Theme selected = themes.getOrDefault(themeName, new NordDark());
        Application.setUserAgentStylesheet(selected.getUserAgentStylesheet());
        log.info("Тема {} успешно применена", themeName);
    }
}
