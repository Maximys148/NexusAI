package com.maximys.nexus.client.backend.service;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.maximys.nexus.client.backend.config.AppSettings;
import com.maximys.nexus.client.backend.model.GitHubReleaseInfo;
import com.maximys.nexus.client.backend.service.update.UpdateService;

import java.util.Map;

/**
 * Фасад-координатор для MainController.
 * Только делегирует вызовы специализированным сервисам.
 */
@Service
public class MainService {

    private static final Logger log = LoggerFactory.getLogger(MainService.class);

    private final LanguageService langService;
    private final UpdateService updateService;
    private final NavigationService navService;
    private final SettingService settingService;
    private final WindowGeometryService windowGeometryService;
    private final AppSettings appSettings;
    private final ModelService modelService;

    // Состояние для модального окна обновлений
    private GitHubReleaseInfo activeReleaseInfo;

    public MainService(
            LanguageService langService,
            UpdateService updateService,
            NavigationService navService,
            SettingService settingService,
            WindowGeometryService windowGeometryService,
            AppSettings appSettings, ModelService modelService
    ) {
        this.langService = langService;
        this.updateService = updateService;
        this.navService = navService;
        this.settingService = settingService;
        this.windowGeometryService = windowGeometryService;
        this.appSettings = appSettings;
        this.modelService = modelService;
    }

    // ==================== МЕТОД ИНИЦИАЛИЗАЦИИ ====================

    public void initMainView(HBox titleBar, Map<String, Node> uiElements) {
        windowGeometryService.setupWindowGeometry(titleBar, appSettings);
        langService.bindMainUserInterface(uiElements);
        Platform.runLater(() -> settingService.applyScaling(appSettings.getSavedScale(), titleBar));
        updateService.initUpdateFeature(uiElements, this::storeActiveRelease);

        modelService.syncModelsFromServer();
    }

    private void storeActiveRelease(GitHubReleaseInfo info) {
        this.activeReleaseInfo = info;
    }

    // ==================== МОДАЛЬНОЕ ОКНО ОБНОВЛЕНИЙ ====================

    // Инжектируйте UpdateService в конструктор MainService, если его там еще нет
// Перепишите метод showUpdateModal в MainService.java:

    // Измените метод showUpdateModal в вашем MainService.java:

    public void showUpdateModal(StackPane updateOverlay, Label modalTitleLabel, TextArea changelogTextArea) {
        // Вызываем строго один метод из специализированного UpdateService
        updateService.bindAndShowUpdateModal(updateOverlay, modalTitleLabel, changelogTextArea, activeReleaseInfo);
    }

    public void hideUpdateModal(StackPane updateOverlay) {
        updateOverlay.setVisible(false);
        updateOverlay.setManaged(false);
    }
    // ==================== ОБНОВЛЕНИЯ ====================

    public void startInstallation() {
        updateService.processUpdate();
    }

    // ==================== НАВИГАЦИЯ ====================

    public void navigateToChat(StackPane area) {
        log.info("Переход в раздел текст");
        navService.load(area, "chat-view.fxml");
    }

    public void navigateToImage(StackPane area) {
        log.info("Переход в раздел изображения");
        navService.load(area, "image-view.fxml");
    }

    public void navigateToAudio(StackPane area) {
        log.info("Переход в раздел аудио");
        navService.load(area, "audio-view.fxml");
    }

    public void navigateToLocalModels(StackPane area) {
        log.info("Переход в раздел локальных моделей");
        navService.load(area,"local-model-view.fxml");
    }

    public void navigateToTariffs(StackPane area) {
        log.info("Переход в раздел облачных моделей");
        navService.load(area,"tariff-view.fxml");
    }
    public void navigateToSettings(StackPane area) {
        log.info("Переход в раздел настроек");
        navService.load(area, "setting-view.fxml");
    }

    // ==================== УПРАВЛЕНИЕ ОКНОМ ====================

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

    public void exit(Node node) {
        if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage stage) {
            appSettings.setWindowWidth(stage.getWidth());
            appSettings.setWindowHeight(stage.getHeight());
            appSettings.flush();
        }
        Platform.exit();
        System.exit(0);
    }

    private Stage getStage(Node node) {
        return (Stage) node.getScene().getWindow();
    }
}