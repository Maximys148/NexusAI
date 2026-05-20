package com.maximys.nexus.client.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.maximys.nexus.client.backend.service.MainService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

// Контролер отвечающий за отображения главной страницы (main-view.fxml)

@Component
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final MainService mainService;

    @FXML private Label versionLabel;
    @FXML private HBox titleBar;
    @FXML private Label welcomeLabel;
    @FXML private Button profileButton;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;
    @FXML private Button textButton;
    @FXML private Button imageButton;
    @FXML private Button videoButton;
    @FXML private Button audioButton;
    @FXML private Button localModelsButton;
    @FXML private Button tariffsButton;
    @FXML private Button settingsButton;
    @FXML private Button updateButton;
    @FXML private StackPane updateOverlay;
    @FXML private TextArea changelogTextArea;
    @FXML private Button aboutButton;
    @FXML private Button exitButton;
    @FXML private StackPane contentArea;
    @FXML private Label modalTitleLabel;
    @FXML private Button closeModalButton;
    @FXML private Button installUpdateButton;


    public MainController(MainService mainService) {
        this.mainService = mainService;
    }

    @FXML
    public void initialize() {
        mainService.initMainView(titleBar, getXmlNodes());
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

    @FXML
    public void onChatButtonClick() {
        mainService.navigateToChat(contentArea);
    }

    @FXML
    public void onImageButtonClick() {
        mainService.navigateToImage(contentArea);
    }

    @FXML
    public void onLocalModelsButtonClick() {
        mainService.navigateToLocalModels(contentArea);
    }

    @FXML
    public void onTariffsButtonClick() {
        mainService.navigateToTariffs(contentArea);
    }

    @FXML
    private void onSettingButtonClick() {
        mainService.navigateToSettings(contentArea);
    }

    @FXML
    private void onUpdateClick() {
        mainService.showUpdateModal(updateOverlay, modalTitleLabel, changelogTextArea);
    }

    @FXML
    private void onCloseModalClick() {
        mainService.hideUpdateModal(updateOverlay);
    }

    @FXML
    private void onInstallUpdateClick() {
        mainService.startInstallation();
    }

    @FXML
    protected void onExitButtonClick() {
        mainService.exit(titleBar);
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
        mainService.exit(titleBar);
    }
}