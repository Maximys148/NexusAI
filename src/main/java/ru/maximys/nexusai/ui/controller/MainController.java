package ru.maximys.nexusai.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import ru.maximys.nexusai.backend.service.MainService;

import java.util.HashMap;
import java.util.Map;

// Контролер отвечающий за отображения главной страницы (main-view.fxml)

@Component
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final MainService mainService;

    @FXML
    private Label versionLabel;

    @FXML
    private HBox titleBar;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label functionsLabel;

    @FXML
    private Button profileButton;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    @FXML
    private Button closeButton;

    @FXML
    private Button textButton;

    @FXML
    private Button imageButton;

    @FXML
    private Button videoButton;

    @FXML
    private Button audioButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button aboutButton;

    @FXML
    private Button exitButton;

    @FXML
    private StackPane contentArea;

    public MainController(MainService mainService) {
        this.mainService = mainService;
    }

    @FXML
    public void initialize() {
        Map<String, Labeled> uiElements = new HashMap<>();
        uiElements.put("functions", functionsLabel);
        uiElements.put("profile", profileButton);
        uiElements.put("welcome", welcomeLabel);
        uiElements.put("textButton", textButton);
        uiElements.put("imageButton", imageButton);
        uiElements.put("videoButton", videoButton);
        uiElements.put("audioButton", audioButton);
        uiElements.put("settingsButton", settingsButton);
        uiElements.put("updateButton", updateButton);
        uiElements.put("versionLabel", versionLabel);

        mainService.initMainView(titleBar, uiElements);
    }

    @FXML
    public void onChatButtonClick() {
        mainService.navigateToChat(contentArea);
    }

    @FXML
    public void onImageButtonClick() {
        mainService.navigateToImage(contentArea);
    }

    @FXML
    private void onSettingButtonClick() {
        mainService.navigateToSettings(contentArea);
    }

    @FXML
    private void onUpdateClick() {
        mainService.handleUpdateAction();
    }

    @FXML
    protected void onExitButtonClick() {
        mainService.exit();
    }

    @FXML
    protected void onMinimizeClick() {
        mainService.minimize(minimizeButton);
    }

    @FXML
    protected void onMaximizeClick() {
        mainService.maximize(maximizeButton);
    }

    @FXML
    protected void onCloseClick() {
        mainService.exit();
    }
}