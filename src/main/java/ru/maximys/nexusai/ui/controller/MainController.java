package ru.maximys.nexusai.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Component
@Controller
public class MainController {

    @FXML
    private HBox titleBar;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    @FXML
    private Button closeButton;

    @FXML
    private Button searchButton;

    @FXML
    private Button historyButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button aboutButton;

    @FXML
    private Button exitButton;

    // Переменные для перетаскивания окна
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        System.out.println("Главное меню загружено");

        // ✅ Делаем заголовок перетаскиваемым
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    protected void onSearchButtonClick() {
        // Здесь будет переход к окну поиска
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Поиск");
        alert.setHeaderText(null);
        alert.setContentText("Здесь будет окно поиска");
        alert.showAndWait();
    }

    @FXML
    protected void onHistoryButtonClick() {
        // Здесь будет переход к истории поиска
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("История");
        alert.setHeaderText(null);
        alert.setContentText("Здесь будет история запросов");
        alert.showAndWait();
    }

    @FXML
    protected void onSettingsButtonClick() {
        // Здесь будет переход к настройкам
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Настройки");
        alert.setHeaderText(null);
        alert.setContentText("Здесь будут настройки приложения");
        alert.showAndWait();
    }

    @FXML
    protected void onAboutButtonClick() {
        // Показываем информацию о программе
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("AiSearchFX");
        alert.setContentText("Версия 1.0\n\nПриложение для интеллектуального поиска\nс использованием AI");
        alert.showAndWait();
    }

    @FXML
    protected void onExitButtonClick() {
        // Закрываем приложение
        Platform.exit();
    }

    @FXML
    protected void onMinimizeClick() {
        // Сворачиваем окно
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        stage.setIconified(true);
    }
    @FXML
    protected void onMaximizeClick() {
        // Переключаем между развернутым и оконным режимом
        Stage stage = (Stage) maximizeButton.getScene().getWindow();

        if (stage.isMaximized()) {
            // Если уже развернуто — возвращаем в оконный режим
            stage.setMaximized(false);
            maximizeButton.setText("🗖");  // значок развернуть
        } else {
            // Разворачиваем на весь экран
            stage.setMaximized(true);
            maximizeButton.setText("❐");  // значок восстановить (два квадрата)
        }
    }

    @FXML
    protected void onCloseClick() {
        // Закрываем приложение
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}