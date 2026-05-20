package com.maximys.nexus.client.backend.service;

import com.maximys.nexus.client.backend.model.AiModel.ClientAiModel;
import com.maximys.nexus.client.backend.model.AiModel.LocalModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ModelService modelService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private ClientAiModel currentSelectedModel = null;
    private boolean useLocalPrivacyMode = false;
    private final List<File> attachedFiles = new ArrayList<>();

    // ==================== 1. ИНИЦИАЛИЗАЦИЯ И НАСТРОЙКА VIEW ====================

    @SuppressWarnings("unchecked")
    public void initChatView(Map<String, Node> uiElements) {
        ComboBox<ClientAiModel> modelComboBox = (ComboBox<ClientAiModel>) uiElements.get("modelComboBox");
        VBox messageContainer = (VBox) uiElements.get("messageContainer");
        ScrollPane chatScrollPane = (ScrollPane) uiElements.get("chatScrollPane");

        if (modelComboBox == null) return;

        // Включаем автопрокрутку ScrollPane при добавлении новых сообщений в VBox
        if (chatScrollPane != null && messageContainer != null) {
            messageContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                    chatScrollPane.setVvalue(1.0));
        }

        List<ClientAiModel> allModels = modelService.getModels();
        if (allModels.isEmpty()) {
            modelComboBox.setPromptText("Модели недоступны");
            return;
        }

        modelComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ClientAiModel model, boolean empty) {
                super.updateItem(model, empty);
                if (empty || model == null) {
                    setText(null);
                    setDisable(false);
                } else {
                    setText(model.getName());
                    if (model instanceof LocalModel local) {
                        setDisable(!local.isDownloaded());
                    } else {
                        setDisable(false);
                    }
                }
            }
        });

        modelComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ClientAiModel model, boolean empty) {
                super.updateItem(model, empty);
                setText((empty || model == null) ? "Выберите модель" : model.getName());
            }
        });

        modelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.currentSelectedModel = newVal;
                this.useLocalPrivacyMode = newVal instanceof LocalModel;

                if (messageContainer != null) {
                    messageContainer.getChildren().clear();
                    if (useLocalPrivacyMode) {
                        addPrivacyBanner(messageContainer);
                    } else {
                        Platform.runLater(() -> loadChatHistory(messageContainer));
                    }
                }
            }
        });

        Platform.runLater(() -> {
            modelComboBox.setItems(FXCollections.observableArrayList(allModels));
            modelComboBox.getSelectionModel().selectFirst();
        });
    }

    // ==================== 2. СЕРВИС ВЛОЖЕНИЯ КРОСС ПЛАТФОРМЕННЫХ ФАЙЛОВ ====================

    public void handleFileAttachment(Label attachedFilesLabel) {
        if (attachedFilesLabel == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файлы для загрузки");

        // Открываем диалог в контексте текущего окна
        Stage stage = (Stage) attachedFilesLabel.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            attachedFiles.addAll(files);
            attachedFilesLabel.setText("📎 Прикреплено файлов: " + attachedFiles.size());
            log.info("[FILE] Успешно прикреплено файлов: {}", files.size());
        }
    }

    // ==================== 3. КОНВЕЙЕР ОТПРАВКИ И ПОЛУЧЕНИЯ СООБЩЕНИЙ ====================

    public void sendMessageWorkflow(Map<String, Node> uiElements) {
        if (currentSelectedModel == null) return;

        TextField userMessageField = (TextField) uiElements.get("userMessageField");
        VBox messageContainer = (VBox) uiElements.get("messageContainer");
        Label attachedFilesLabel = (Label) uiElements.get("attachedFilesLabel");

        if (userMessageField == null || userMessageField.getText().trim().isEmpty() || messageContainer == null) {
            return;
        }

        // Защитная валидация локальной модели
        if (currentSelectedModel instanceof LocalModel local && !local.isDownloaded()) {
            addMessageBubble("⚠️ Ошибка: Модель [" + local.getName() + "] не скачана.", false, messageContainer);
            return;
        }

        String prompt = userMessageField.getText().trim();

        // Отображаем сообщение пользователя в чате и чистим инпуты
        addMessageBubble(prompt, true, messageContainer);
        userMessageField.clear();
        attachedFiles.clear();
        if (attachedFilesLabel != null) attachedFilesLabel.setText("");

        // Создаем пустой "бабл" для будущего ответа ИИ (эффект стриминга/ожидания)
        HBox aiResponseRow = createEmptyResponseRow();
        Label aiLabel = (Label) aiResponseRow.lookup("#aiTextLabel");
        messageContainer.getChildren().add(aiResponseRow);

        // Отправляем асинхронный сетевой запрос
        sendRequestToAi(prompt, currentSelectedModel.getId(), useLocalPrivacyMode)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (aiLabel != null) {
                        aiLabel.setText(response);
                    }
                }));
    }

    // ==================== 4. СЕТЕВОЙ ОБМЕН (OLLAMA / CLOUD) ====================

    private CompletableFuture<String> sendRequestToAi(String prompt, String modelId, boolean isLocal) {
        // Логика приватности: переключаем трафик строго на Ollama (:11434) или на удаленный сервер
        String targetUrl = isLocal ? "http://localhost:11434/api/generate" : "http://localhost:8080/api/chat/send";
        log.info("[NET] Направление промпта к ИИ. URL: {}, Модель: {}", targetUrl, modelId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Упрощенный JSON-боди (адаптируйте под требования вашей структуры ChatRequest)
                String jsonBody = String.format("{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}", modelId, prompt);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return response.body(); // Тут можно вызвать ваш метод parseAiResponse()
                } else {
                    return "⚠️ Ошибка сервера: код " + response.statusCode();
                }
            } catch (Exception e) {
                log.error("[NET] Критическая ошибка сети при генерации ответа: ", e);
                return "❌ Ошибка соединения. Проверьте статус локального ИИ или интернет.";
            }
        });
    }

    // ==================== 5. ГРАФИЧЕСКИЙ РЕНДЕРИНГ ЭЛЕМЕНТОВ (ATLANTAFX) ====================

    private void addMessageBubble(String text, boolean isUser, VBox container) {
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(450.0);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-radius: 12;");

        HBox row = new HBox();
        row.setPadding(new Insets(5, 10, 5, 10));

        if (isUser) {
            row.setAlignment(Pos.CENTER_RIGHT);
            // Промпт пользователя красим в акцентный цвет темы AtlantaFX
            messageLabel.setStyle(messageLabel.getStyle() + "-fx-background-color: -color-accent-emphasis; -fx-text-fill: -color-accent-fg;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            // Ответ ИИ красим в мягкий приглушенный фоновый цвет
            messageLabel.setStyle(messageLabel.getStyle() + "-fx-background-color: -color-bg-subtle; -fx-text-fill: -color-fg-default;");
        }

        row.getChildren().add(messageLabel);
        Platform.runLater(() -> container.getChildren().add(row));
    }

    private HBox createEmptyResponseRow() {
        HBox row = new HBox();
        row.setPadding(new Insets(5, 10, 5, 10));
        row.setAlignment(Pos.CENTER_LEFT);

        Label aiLabel = new Label("Nexus думает...");
        aiLabel.setId("aiTextLabel");
        aiLabel.setWrapText(true);
        aiLabel.setMaxWidth(450.0);
        aiLabel.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-radius: 12; -fx-background-color: -color-bg-subtle; -fx-text-fill: -color-fg-muted;");

        row.getChildren().add(aiLabel);
        return row;
    }

    private void addPrivacyBanner(VBox container) {
        Label banner = new Label("🔒 Локальный приватный режим (История сообщений изолирована и не пишется на сервер)");
        banner.setStyle("-fx-text-fill: -color-success-fg; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 10 15;");
        container.getChildren().add(banner);
    }

    public void loadChatHistory(VBox container) {
        log.info("[HISTORY] Загрузка истории чата с сервера бэкенда...");
        // Сюда встанет выпрос к loadChatHistory() через HttpClient до PostgreSQL сервера
    }
}
