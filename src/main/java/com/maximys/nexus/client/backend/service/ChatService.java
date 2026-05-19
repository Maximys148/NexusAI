package com.maximys.nexus.client.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maximys.nexus.client.backend.model.ChatRequest;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
@Slf4j
public class ChatService {

    @Value("${app.ai.server.url}")
    private String serverUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public CompletableFuture<String> sendRequestToAi(String prompt, String model, List<String> files, boolean isLocal) {
        try {
            String targetUrl = isLocal ? "http://localhost:11434/api/generate" : serverUrl;
            log.info("[HTTP] Формирование запроса к AI. Целевой URL: {}, Модель: {}", targetUrl, model);

            ChatRequest chatRequest = new ChatRequest(prompt, model, files, isLocal);
            String jsonBody = objectMapper.writeValueAsString(chatRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        log.info("[HTTP] Получен ответ от сервера. Статус-код: {}", response.statusCode());
                        if (response.statusCode() == 200) {
                            return parseAiResponse(response.body(), isLocal);
                        } else {
                            log.warn("[HTTP] Сервер ответил ошибкой: {}", response.body());
                            return "Ошибка сервера: код " + response.statusCode();
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("[HTTP] Сбой сетевого соединения: ", ex);
                        return "Ошибка сети: " + ex.getMessage();
                    });

        } catch (Exception e) {
            log.error("[HTTP] Ошибка сборки JSON-запроса: ", e);
            return CompletableFuture.completedFuture("Ошибка построения запроса: " + e.getMessage());
        }
    }

    // Добавьте это поле в начало ChatService
    private final List<String> attachedFiles = new ArrayList<>();

    /**
     * Логика выбора файлов и безопасного кроссплатформенного рендеринга текста прикреплений.
     */
    public void handleFileAttachment(Label attachedFilesLabel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файлы");

        List<java.io.File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null) {
            for (java.io.File file : selectedFiles) {
                attachedFiles.add(file.getAbsolutePath());
            }

            // Безопасное обновление текста без крашей с индексами
            if (attachedFiles.isEmpty()) {
                attachedFilesLabel.setText("");
            } else if (attachedFiles.size() == 1) {
                java.io.File singleFile = new java.io.File(attachedFiles.get(0));
                attachedFilesLabel.setText("📎 " + singleFile.getName());
            } else {
                attachedFilesLabel.setText(attachedFiles.size() + " файл(ов) прикреплено");
            }
            log.info("[Service] Файлы закешированы. Всего: {}", attachedFiles.size());
        }
    }

    /**
     * Единый воркфлоу отправки сообщения, очистки полей и асинхронного рендеринга ответа.
     */
    public void sendMessageWorkflow(String text, String model, boolean isLocal,
                                    TextField inputField, Label filesLabel,
                                    ScrollPane scrollPane, VBox container) {

        if (text.isEmpty() && attachedFiles.isEmpty()) return;

        // Фиксируем баббл пользователя на экране
        addMessage(text, true, scrollPane, container);
        inputField.clear();

        // Скопировали файлы для отправки и очистили кэш прикреплений
        List<String> filesToSend = new ArrayList<>(attachedFiles);
        attachedFiles.clear();
        filesLabel.setText("");

        // Асинхронный сетевой запрос к серверу (или Ollama)
        sendRequestToAi(text, model, filesToSend, isLocal)
                .thenAccept(aiResponse -> {
                    // Рендерим ответ ИИ
                    addMessage(aiResponse, false, scrollPane, container);
                });
    }


    private String parseAiResponse(String responseBody, boolean isLocal) {
        try {
            Map<?, ?> map = objectMapper.readValue(responseBody, Map.class);
            if (isLocal && map.containsKey("response")) return map.get("response").toString();
            if (map.containsKey("output")) return map.get("output").toString();
            if (map.containsKey("text")) return map.get("text").toString();
            return responseBody;
        } catch (Exception e) {
            log.warn("[HTTP] Не удалось распарсить JSON, возврат сырого текста ответа");
            return responseBody;
        }
    }

    public void addMessage(String text, boolean isUser, ScrollPane chatScrollPane, VBox messageContainer) {
        HBox messageBox = new HBox();
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(400);
        label.getStyleClass().add("user-message-base");

        if (isUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            label.getStyleClass().addAll("accent", "user-message");
            label.setStyle("-fx-background-radius: 15 15 2 15; -fx-padding: 8 12; -fx-font-size: 14px;");
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            label.getStyleClass().addAll("secondary", "ai-message");
            label.setStyle("-fx-background-radius: 15 15 15 2; -fx-padding: 8 12; -fx-font-size: 14px;");
        }

        messageBox.getChildren().add(label);

        Platform.runLater(() -> {
            messageContainer.getChildren().add(messageBox);
            chatScrollPane.setVvalue(1.0);
            log.debug("[UI] Новое сообщение отрендерено на экране. Автор: {}", isUser ? "USER" : "AI");
        });
    }

    /**
     * Асинхронно выгружает историю сообщений из PostgreSQL бэкенда
     * и рендерит баблы переписки на экране JavaFX.
     */
    public void loadChatHistory(ScrollPane chatScrollPane, VBox messageContainer) {
        try {
            log.info("[ChatService] Запрос истории переписки с сервера...");
            // Формируем URL эндпоинта истории: http://localhost:8080/api/chat/history
            String historyUrl = serverUrl.replace("/send", "/history");

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(historyUrl))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                // Парсим JSON-массив сообщений из БД
                                List<Map<?, ?>> messages = objectMapper.readValue(response.body(), List.class);

                                // Возвращаемся в UI поток JavaFX для отрисовки всей пачки сообщений
                                javafx.application.Platform.runLater(() -> {
                                    messageContainer.getChildren().clear(); // Чистим старый контейнер
                                    for (Map<?, ?> msg : messages) {
                                        String content = msg.get("content").toString();
                                        boolean isUser = "USER".equals(msg.get("senderType").toString());
                                        // Вызываем ваш стандартный метод отрисовки баблов
                                        addMessage(content, isUser, chatScrollPane, messageContainer);
                                    }
                                    log.info("[ChatService] Успешно восстановлено {} сообщений из базы данных.", messages.size());
                                });
                            } catch (Exception e) {
                                log.error("[ChatService] Ошибка десериализации истории сообщений: ", e);
                            }
                        } else {
                            log.warn("[ChatService] Сервер вернул код {} при запросе истории", response.statusCode());
                        }
                    }).exceptionally(ex -> {
                        log.warn("[ChatService] Не удалось загрузить историю чата (Бэкенд недоступен или оффлайн)");
                        return null;
                    });
        } catch (Exception e) {
            log.error("[ChatService] Ошибка построения запроса истории: ", e);
        }
    }

}
