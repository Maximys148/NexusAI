package com.maximys.nexus.client.ui.controller;

import com.maximys.nexus.client.backend.dto.AiModelDto;
import com.maximys.nexus.client.backend.service.ChatService;
import com.maximys.nexus.client.backend.service.ModelService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ModelService modelService;

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextField userMessageField;
    @FXML private ComboBox<String> modelComboBox;
    @FXML private Label attachedFilesLabel;

    private String currentModel = null;
    private boolean useLocalModel = false;

    @FXML
    public void initialize() {
        // Наполняем ComboBox настоящим списком моделей из общей памяти клиента
        modelService.fetchActiveModelsForChat().thenAccept(allModels -> {
            Platform.runLater(() -> {
                modelComboBox.getItems().clear();

                if (allModels.isEmpty()) {
                    modelComboBox.setPromptText("Модели недоступны");
                    return;
                }

                // Переводчик ID -> Человеческое имя для списка ячеек
                modelComboBox.setCellFactory(lv -> new ListCell<String>() {
                    @Override
                    protected void updateItem(String id, boolean empty) {
                        super.updateItem(id, empty);
                        if (empty || id == null) {
                            setText(null);
                            setDisable(false);
                        } else {
                            AiModelDto dto = modelService.getCachedModels().stream()
                                    .filter(m -> id.equals(m.getId()))
                                    .findFirst().orElse(null);

                            if (dto != null) {
                                setText(dto.getName());
                                // Если локальная модель не скачана — отключаем саму ячейку (disabled)
                                // CSS автоматически подхватит селектор :disabled и сделает её серой
                                if (dto.isLocal()) {
                                    boolean isDownloaded = modelService.isModelDownloadedLocally(id).join();
                                    setDisable(!isDownloaded);
                                } else {
                                    setDisable(false);
                                }
                            } else {
                                setText(id);
                                setDisable(false);
                            }
                        }
                    }
                });

                // Переводчик для отображения уже выбранного элемента
                modelComboBox.setButtonCell(new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Выберите модель");
                        } else {
                            String name = modelService.getCachedModels().stream()
                                    .filter(m -> item.equals(m.getId()))
                                    .findFirst().map(AiModelDto::getName).orElse(item);
                            setText(name);
                        }
                    }
                });

                // Добавляем ID моделей в список
                allModels.forEach(model -> modelComboBox.getItems().add(model.getId()));
                modelComboBox.getSelectionModel().selectFirst();
            });
        });

        // Реактивное управление логикой приватности при смене модели
        modelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.currentModel = newVal;
                this.useLocalModel = modelService.checkIfModelIsLocal(newVal);

                if (useLocalModel) {
                    messageContainer.getChildren().clear();
                    messageContainer.getChildren().add(new Label("🔒 Локальный приватный режим (История не сохраняется на сервере)"));
                } else {
                    Platform.runLater(() -> chatService.loadChatHistory(chatScrollPane, messageContainer));
                }
            }
        });
    }

    @FXML
    public void onAttachFile() {
        chatService.handleFileAttachment(attachedFilesLabel);
    }

    @FXML
    public void onSendMessage() {
        if (currentModel == null) return;

        // Валидация скачанности локальной модели
        if (useLocalModel) {
            boolean isDownloaded = modelService.isModelDownloadedLocally(currentModel).join();
            if (!isDownloaded) {
                chatService.addMessage("⚠️ Модель [" + currentModel + "] еще не скачана. Скачайте её во вкладке 'Хаб моделей'.", false, chatScrollPane, messageContainer);
                return;
            }
        }

        String text = userMessageField.getText().trim();
        chatService.sendMessageWorkflow(text, currentModel, useLocalModel, userMessageField, attachedFilesLabel, chatScrollPane, messageContainer);
    }
}
