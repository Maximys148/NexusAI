package ru.maximys.nexusai.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import org.springframework.stereotype.Component;
import ru.maximys.nexusai.backend.service.SettingService;

import java.util.HashMap;
import java.util.Map;

// Контролер отвечающий за отображения страницы Настроек (setting-view.fxml)

@Component
public class SettingsController {

    private final SettingService settingService;

    @FXML
    private Label settingsHeaderLabel;

    @FXML
    private Label themeTitleLabel;

    @FXML
    private Label languageTitleLabel;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    private ComboBox<String> themeComboBox;

    public SettingsController(SettingService settingService) {
        this.settingService = settingService;
    }

    @FXML
    public void initialize() {
        Map<String, Labeled> uiElements = new HashMap<>();
        uiElements.put("header", settingsHeaderLabel);
        uiElements.put("themeTitleLabel", themeTitleLabel);
        uiElements.put("languageTitleLabel", languageTitleLabel);

        settingService.initSettingsView(languageComboBox, themeComboBox, uiElements);
    }
}
