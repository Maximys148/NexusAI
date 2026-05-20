package com.maximys.nexus.client.backend.service;

import atlantafx.base.theme.*;
import jakarta.annotation.PostConstruct;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.maximys.nexus.client.backend.config.AppSettings;

import java.util.LinkedHashMap;
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

    private final DoubleProperty currentFontSize = new SimpleDoubleProperty(14.0);

    // Карта: Текст в UI -> Размер в пикселях
    private final Map<String, Double> scaleMap = new LinkedHashMap<>() {{
        put("75%", 11.0);
        put("100%", 14.0);
        put("125%", 18.0);
        put("150%", 21.0);
        put("200%", 28.0);
    }};


    public SettingService(LanguageService langService, AppSettings appSettings) {
        this.langService = langService;
        this.appSettings = appSettings;
    }

    @PostConstruct
    public void init() {
        Platform.runLater(() -> {
            try {
                // 1. Применяем сохраненную тему
                String savedTheme = appSettings.getSavedTheme();
                applyTheme(savedTheme);

                // 2. Применяем сохраненный масштаб
                double savedScale = appSettings.getSavedScale();
                applyScaling(savedScale,null); // null, так как конкретного узла еще нет

                log.info("Настройки загружены: Тема=" + savedTheme + ", Масштаб=" + savedScale);
            } catch (Exception e) {
            }
        });
    }

    public void initSettingsView(Map<String, Node> nodes) {
        // 1. Локализация
        bindUserInterface(nodes);

        // 2. Смена языка
        if (nodes.get("languageComboBox") instanceof ComboBox<?> combo) {
            setupLanguageSelection((ComboBox<String>) combo);
        }

        // 3. Смена темы
        if (nodes.get("themeComboBox") instanceof ComboBox<?> combo) {
            setupThemeSelection((ComboBox<String>) combo);
        }

        // 4. Логика масштаба
        currentFontSize.set(appSettings.getSavedScale());
        if (nodes.get("scalingComboBox") instanceof ComboBox<?> combo) {
            setupScalingLogic((ComboBox<String>) combo);
        }

        // 5. Логика автозагрузки (ИСПРАВЛЕНО: вызываем правильный метод)
        if (nodes.get("startupCheckBox") instanceof CheckBox checkBox) {
            setupStartupLogic(checkBox);
        }
    }

    private void bindUserInterface(Map<String, Node> elements) {
        // Используем безопасный метод из твоего LanguageService
        if (elements.get("settingsHeaderLabel") instanceof Labeled l) langService.bindText(l, "ui.settings.header");
        if (elements.get("themeTitleLabel") instanceof Labeled l) langService.bindText(l, "ui.settings.theme");
        if (elements.get("languageTitleLabel") instanceof Labeled l) langService.bindText(l, "ui.settings.language");
        if (elements.get("scalingTitleLabel") instanceof Labeled l) langService.bindText(l, "ui.settings.scaling");
        if (elements.get("startupTitleLabel") instanceof Labeled l) langService.bindText(l, "ui.settings.startup");
    }


    private void setupLanguageSelection(ComboBox<String> combo) {
        combo.getItems().setAll(languages.keySet());
        String currentCode = langService.getCurrentLangCode();
        languages.forEach((name, code) -> {
            if (code.equals(currentCode)) combo.setValue(name);
        });

        combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                langService.saveLanguage(languages.get(newVal));
            }
        });
    }

    private void setupThemeSelection(ComboBox<String> combo) {
        combo.getItems().setAll("Primer Light", "Primer Dark", "Nord Light", "Nord Dark", "Cupertino Light", "Cupertino Dark", "Dracula");
        combo.setValue(appSettings.getSavedTheme());
        combo.setOnAction(event -> {
            String selected = combo.getValue();
            applyTheme(selected);
            appSettings.saveTheme(selected);
        });
    }

    private void setupScalingLogic(ComboBox<String> combo) {
        combo.getItems().setAll(scaleMap.keySet());

        double savedPx = appSettings.getSavedScale();

        // Находим текст процента для сохраненных пикселей
        scaleMap.forEach((percent, px) -> {
            if (Math.abs(px - savedPx) < 0.1) {
                combo.setValue(percent);
            }
        });

        // ВАЖНО: Явно вызываем применение масштаба сразу после установки значения
        applyScaling(savedPx, combo);

        combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                double fontSize = scaleMap.get(newVal);
                applyScaling(fontSize, combo);
                appSettings.saveScale(fontSize);
            }
        });
    }

    // Метод для привязки масштаба к любому контейнеру
    public void bindScaling(Parent root) {
        if (root != null) {
            // Привязываем стиль fontSize к нашему свойству
            root.styleProperty().bind(Bindings.createStringBinding(
                    () -> "-fx-font-size: " + currentFontSize.get() + "px;",
                    currentFontSize
            ));
        }
    }

    // Метод для изменения (вызывается из ComboBox)
    public void updateScaling(double newValue) {
        currentFontSize.set(newValue);
        appSettings.saveScale(newValue);
    }

    // Новый метод для обработки CheckBox автозагрузки
    private void setupStartupLogic(CheckBox checkBox) {
        checkBox.setSelected(appSettings.isStartupEnabled());
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            setAutoStart(newVal);
            appSettings.saveStartup(newVal);
        });
    }

    // Убираем попытки достать Stage из Spring. Работаем с тем, что передали.
    public void applyScaling(double fontSize, Node anyNode) {
        if (anyNode == null) return;

        Platform.runLater(() -> {
            try {
                Scene scene = anyNode.getScene();
                if (scene != null) {
                    scene.getRoot().setStyle("-fx-font-size: " + fontSize + "px;");
                    log.info("Масштаб {}px применен через Node: {}", fontSize, anyNode.getId());
                }
            } catch (Exception e) {
                log.error("Ошибка применения масштаба: {}", e.getMessage());
            }
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
    }

    private void setAutoStart(boolean enable) {
        // ВАЖНО: user.dir работает только для установленного EXE
        String path = System.getProperty("user.dir") + "\\Nexus.exe";
        String key = "NexusAI";
        try {
            ProcessBuilder pb;
            if (enable) {
                pb = new ProcessBuilder("reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", key, "/t", "REG_SZ", "/d", path, "/f");
            } else {
                pb = new ProcessBuilder("reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", key, "/f");
            }
            pb.start();
        } catch (Exception e) {
            log.error("Registry error: " + e.getMessage());
        }
    }
}
