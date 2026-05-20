package com.maximys.nexus.client.backend.service;

import com.maximys.nexus.client.backend.dto.ModelType;
import com.maximys.nexus.client.backend.model.AiModel.LocalModel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LocalModelUiService {

    private static final Logger log = LoggerFactory.getLogger(LocalModelUiService.class);

    private final ModelService modelService;
    private final LanguageService langService;

    public LocalModelUiService(ModelService modelService, LanguageService langService) {
        this.modelService = modelService;
        this.langService = langService;
    }

    public void initLocalModelsView(Map<String, Node> nodes) {
        // Статические элементы страницы привязываем через ваш стандартный механизм
        if (nodes.get("localHeaderLabel") instanceof Label l) langService.bindText(l, "ui.local.header");
        if (nodes.get("localSubHeaderLabel") instanceof Label l) langService.bindText(l, "ui.local.subheader");

        if (nodes.get("modelsContainer") instanceof VBox modelsContainer) {
            modelsContainer.getChildren().clear();

            List<LocalModel> localModels = modelService.getModels().stream()
                    .filter(model -> model.getType() == ModelType.LOCAL)
                    .map(model -> (LocalModel) model)
                    .toList();

            if (localModels.isEmpty()) {
                Label emptyLabel = new Label();
                langService.bindText(emptyLabel, "ui.local.empty_list");
                emptyLabel.getStyleClass().add("text-muted");
                modelsContainer.getChildren().add(emptyLabel);
                return;
            }

            for (LocalModel model : localModels) {
                modelsContainer.getChildren().add(createModelCard(model));
            }
        }
    }

    private VBox createModelCard(LocalModel model) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 16; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: -color-border-default;");

        // --- ШАПКА КАРТОЧКИ ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(model.getName());
        titleLabel.getStyleClass().add("title-4");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Динамический реактивный статус-бейдж (переводится на лету)
        Label statusBadge = new Label();
        statusBadge.setStyle("-fx-padding: 2 8 2 8; -fx-background-radius: 4;");

        // Переключение CSS-классов в зависимости от состояния модели
        updateBadgeStyle(model, statusBadge);

        header.getChildren().addAll(titleLabel, spacer, statusBadge);

        // --- ОПИСАНИЕ МОДЕЛИ ---
        Label descLabel = new Label(model.getDescription());
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("text-muted");

        // --- ХАРАКТЕРИСТИКИ ---
        HBox specsBox = new HBox(25);
        specsBox.setAlignment(Pos.CENTER_LEFT);
        specsBox.setStyle("-fx-padding: 5 0 5 0;");

        // Характеристика: Вес
        VBox sizeBox = new VBox(4);
        Label sizeTitle = new Label();
        sizeTitle.getStyleClass().add("text-small");
        sizeTitle.setStyle("-fx-opacity: 0.6;");
        langService.bindText(sizeTitle, "ui.local.card.weight"); // Ключ: "Вес модели:"

        Label sizeValue = new Label(model.getSize());
        sizeValue.setStyle("-fx-font-weight: bold;");
        sizeBox.getChildren().addAll(sizeTitle, sizeValue);

        // Характеристика: Железо
        VBox hardwareBox = new VBox(4);
        Label hardwareTitle = new Label();
        hardwareTitle.getStyleClass().add("text-small");
        hardwareTitle.setStyle("-fx-opacity: 0.6;");
        langService.bindText(hardwareTitle, "ui.local.card.hardware"); // Ключ: "Рекомендуемое железо:"

        Label hardwareValue = new Label(model.getHardware());
        hardwareValue.setStyle("-fx-font-weight: bold;");
        hardwareBox.getChildren().addAll(hardwareTitle, hardwareValue);

        specsBox.getChildren().addAll(sizeBox, hardwareBox);

        // --- КНОПКА ДЕЙСТВИЯ ---
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button actionButton = new Button();
        updateButtonState(model, actionButton);

        actionButton.setOnAction(e -> handleModelAction(model, actionButton, statusBadge));
        actions.getChildren().add(actionButton);

        card.getChildren().addAll(header, descLabel, specsBox, actions);
        return card;
    }

    private void handleModelAction(LocalModel model, Button actionButton, Label statusBadge) {
        if (!model.isDownloaded()) {
            log.info("Старт скачивания локальной модели через Ollama: {}", model.getId());

            // Отвязываем старый текст на время загрузки и пишем прогресс
            actionButton.textProperty().unbind();
            actionButton.setDisable(true);
            actionButton.setText("Загрузка...");

            // Имитируем успешное скачивание (заменить на вызов реального сервиса)
            model.setDownloaded(true);

            // Обновляем визуальное состояние компонентов
            updateBadgeStyle(model, statusBadge);
            updateButtonState(model, actionButton);
            actionButton.setDisable(false);
        } else {
            log.info("Запуск локальной сессии: {}", model.getId());
            // Сюда вставляется логика выбора активной модели для чата
        }
    }

    // Вспомогательный метод для обновления стилей и текстов бейджа с поддержкой i18n
    private void updateBadgeStyle(LocalModel model, Label badge) {
        if (model.isDownloaded()) {
            badge.getStyleClass().setAll("badge-success");
            langService.bindText(badge, "ui.local.status.ready"); // Ключ: "Готова к работе"
        } else {
            badge.getStyleClass().setAll("badge-neutral");
            langService.bindText(badge, "ui.local.status.available"); // Ключ: "Доступна для загрузки"
        }
    }

    // Вспомогательный метод для динамической локализации кнопок
    private void updateButtonState(LocalModel model, Button button) {
        if (model.isDownloaded()) {
            button.getStyleClass().setAll("button-primary");
            langService.bindText(button, "ui.local.btn.run"); // Ключ: "Запустить"
        } else {
            button.getStyleClass().setAll("button-outlined");
            langService.bindText(button, "ui.local.btn.download"); // Ключ: "Скачать модель"
        }
    }
}
