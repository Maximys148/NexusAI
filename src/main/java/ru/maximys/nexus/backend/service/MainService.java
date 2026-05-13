package ru.maximys.nexus.backend.service;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.maximys.nexus.backend.config.AppSettings;
import ru.maximys.nexus.backend.service.update.UpdateService;

import java.util.Map;

// Сервис отвечающий за бизнес логику контролера MainController

@Service
public class MainService {

    private static final Logger log = LoggerFactory.getLogger(MainService.class);

    private final LanguageService langService;
    private final UpdateService updateService;
    private final NavigationService navService;
    private final SettingService settingService;
    private final AppSettings appSettings;

    private double xOffset = 0;
    private double yOffset = 0;

    public MainService(LanguageService langService, UpdateService updateService, NavigationService navService, SettingService settingService, AppSettings appSettings) {
        this.langService = langService;
        this.updateService = updateService;
        this.navService = navService;
        this.settingService = settingService;
        this.appSettings = appSettings;
    }

    public void initMainView(HBox titleBar, Map<String, Labeled> uiElements) {

        // 1. Настраиваем перетаскивание
        makeDraggable(titleBar);

        // 2. Локализация
        bindUserInterface(uiElements);

        // 3. Масштабирование
        Platform.runLater(() -> {
            settingService.applyScaling(appSettings.getSavedScale(), titleBar);
        });

        // 4. Обновления
        Labeled versionLabel = uiElements.get("versionLabel");
        versionLabel.setText("v" + updateService.getCurrentVersion());
        Labeled updateButton = uiElements.get("updateButton");
        updateService.checkUpdates(versionInfo -> {
            updateButton.setVisible(true);
            updateButton.setManaged(true);
            versionLabel.setText(versionInfo);
        });


    }

    private void makeDraggable(Node node) {
        node.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        node.setOnMouseDragged(event -> {
            Stage stage = (Stage) node.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    // Управляет всеми ключами переводов
    private void bindUserInterface(Map<String, Labeled> elements) {

        langService.bindText(elements.get("functions"), "ui.sidebar.functions");
        langService.bindText(elements.get("profile"), "ui.titlebar.profile");
        langService.bindText(elements.get("welcome"), "ui.main.welcome");
        langService.bindText(elements.get("textButton"), "ui.sidebar.text");
        langService.bindText(elements.get("imageButton"), "ui.sidebar.image");
        langService.bindText(elements.get("videoButton"), "ui.sidebar.video");
        langService.bindText(elements.get("audioButton"), "ui.sidebar.audio");
        langService.bindText(elements.get("settingsButton"), "ui.sidebar.settings");
        langService.bindText(elements.get("updateButton"), "ui.sidebar.update");
    }

    // --- МЕТОДЫ ПЕРЕХОДА МЕЖДУ ЭКРАНАМИ ---

    public void navigateToChat(StackPane area) {
        log.info("Переход в раздел текст");
        navService.load(area, "chat-view.fxml");
    }

    public void navigateToSettings(StackPane area) {
        log.info("Переход в раздел настроек");
        navService.load(area, "setting-view.fxml");
    }

    public void navigateToImage(StackPane area) {
        log.info("Переход в раздел изображения");
        navService.load(area, "image-view.fxml");
    }

    public void navigateToAudio(StackPane area) {
        log.info("Переход в раздел аудио");
        navService.load(area, "audio-view.fxml");
    }

    public void handleUpdateAction() {
        updateService.processUpdate();
    }

    // --- УПРАВЛЕНИЕ СОСТОЯНИЕМ ОКНА ---
    public void minimize(Button btn) {
        getStage(btn).setIconified(true);
    }

    public void maximize(Button btn) {
        Stage stage = getStage(btn);
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            btn.setText("🗖");
        } else {
            stage.setMaximized(true);
            btn.setText("❐");
        }
    }

    public void exit() {
        Platform.exit();
        System.exit(0);
    }

    private Stage getStage(Node node) {
        return (Stage) node.getScene().getWindow();
    }
}
