package ru.maximys.nexus.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.maximys.nexus.backend.service.SettingService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

// Контролер отвечающий за отображения страницы Настроек (setting-view.fxml)

@Component
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    private final SettingService settingService;

    @FXML
    private Label settingsHeaderLabel;

    @FXML
    private Label themeTitleLabel;

    @FXML
    private Label languageTitleLabel;

    @FXML
    private Label scalingTitleLabel;

    @FXML
    private Label scalingValueLabel;

    @FXML
    private Label startupTitleLabel;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    private ComboBox<String> themeComboBox;

    @FXML
    private ComboBox<String> scalingComboBox;

    @FXML
    private CheckBox startupCheckBox;


    public SettingsController(SettingService settingService) {
        this.settingService = settingService;
    }

    @FXML
    public void initialize() {
        Map<String, Node> uiElements = getXmlNodes();
        settingService.initSettingsView(uiElements);
    }

    private Map<String, Node> getXmlNodes() {
        Map<String, Node> nodes = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(FXML.class) && Node.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true); // Даем доступ к private полям
                    Node node = (Node) field.get(this);
                    if (node != null) {
                        nodes.put(field.getName(), node);
                    }
                } catch (IllegalAccessException e) {
                    log.error("Ошибка при сборке карты узлов: {}", e.getMessage());
                }
            }
        }
        return nodes;
    }
}
