package com.maximys.nexus.client.ui.controller;

import com.maximys.nexus.client.backend.dto.AiModelDto;
import com.maximys.nexus.client.backend.service.ModelService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ModelsController {

    private final ModelService modelService;
    // private final ModelDownloadService downloadService;

    @FXML private ListView<AiModelDto> modelsListView;
    @FXML private ToggleButton filterAllBtn, filterLocalBtn, filterCloudBtn;
    @FXML private TextField searchField;

    // Ссылки на ваш шаблон из fx:define / bottom основного FXML файла
    @FXML private BorderPane rowTemplate;
    @FXML private Label rowNameLabel, rowTypeLabel;
    @FXML private Button rowInfoBtn, rowActionBtn;

    private List<AiModelDto> allModels = new ArrayList<>();
    private String currentFilter = "ALL";

    @FXML
    public void initialize() {
        modelsListView.setCellFactory(param -> new ListCell<>() {
            private final Button actionBtn = new Button();

            {
                actionBtn.setPrefWidth(140);
                actionBtn.setMinWidth(140);
            }

            @Override
            protected void updateItem(AiModelDto model, boolean empty) {
                super.updateItem(model, empty);

                if (empty || model == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // ИСПРАВЛЕНО: Выводим полный технический паспорт модели в текстовое поле ячейки списка
                    String rowText = String.format(
                            "%s    (%s)\n" +
                                    "• ⚖️ Вес: %s  |  💰 Цена: %s  |  ⚡ Железо: %s\n" +
                                    "• 📝 Описание: %s",
                            model.getName(),
                            model.isLocal() ? "Локальная модель" : "Облачное API",
                            model.getSize(),
                            model.getPrice(),
                            model.getRequirements(),
                            model.getDescription() != null ? model.getDescription() : "Описание отсутствует."
                    );

                    setText(rowText);
                    setStyle("-fx-font-size: 13px; -fx-padding: 10 12; -fx-line-spacing: 3;"); // Добавили красивый межстрочный интервал

                    actionBtn.getStyleClass().removeAll("success", "accent", "flat");
                    actionBtn.setOnAction(null);

                    if (!model.isLocal()) {
                        actionBtn.setText("Доступна по API");
                        actionBtn.setDisable(true);
                        actionBtn.getStyleClass().add("flat");
                    } else {
                        modelService.isModelDownloadedLocally(model.getId()).thenAccept(isDownloaded -> {
                            Platform.runLater(() -> {
                                if (isDownloaded) {
                                    actionBtn.setText("Ready");
                                    actionBtn.getStyleClass().add("success");
                                    actionBtn.setDisable(true);
                                } else {
                                    actionBtn.setText("Скачать");
                                    actionBtn.setDisable(false);
                                    actionBtn.getStyleClass().add("accent");

                                    actionBtn.setOnAction(e -> {
                                        actionBtn.setDisable(true);
                                        actionBtn.setText("Скачивание...");
                                        ProgressBar dummyPb = new ProgressBar();
                                        Label dummyLabel = new Label();
                                        dummyLabel.textProperty().addListener((obs, oldText, newText) ->
                                                Platform.runLater(() -> actionBtn.setText(newText))
                                        );
                                        // downloadService.downloadOllamaModel(model.getId(), dummyPb, dummyLabel);
                                    });
                                }
                            });
                        });
                    }
                    setGraphic(actionBtn);
                }
            }
        });
    }

        private void showModelInfoDialog(AiModelDto model) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация: " + model.getName());
        alert.setHeaderText(model.getName());
        alert.setContentText(model.getDescription() != null ? model.getDescription() : "Описание отсутствует.");
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    private void loadModels() {
        modelService.fetchModelsFromServer().thenAccept(models -> {
            this.allModels = models;
            Platform.runLater(this::applyFilterAndSearch);
        });
    }

    private void applyFilterAndSearch() {
        if (allModels == null) return;

        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        List<AiModelDto> filteredList = new ArrayList<>();

        for (AiModelDto m : allModels) {
            if (m == null) continue;
            if (currentFilter.equals("LOCAL") && !m.isLocal()) continue;
            if (currentFilter.equals("CLOUD") && m.isLocal()) continue;

            if (!query.isEmpty()) {
                boolean matches = (m.getName() != null && m.getName().toLowerCase().contains(query))
                        || (m.getDescription() != null && m.getDescription().toLowerCase().contains(query))
                        || (m.getPrice() != null && m.getPrice().toLowerCase().contains(query))
                        || (m.getSize() != null && m.getSize().toLowerCase().contains(query))
                        || (m.getRequirements() != null && m.getRequirements().toLowerCase().contains(query));
                if (!matches) continue;
            }
            filteredList.add(m);
        }

        Platform.runLater(() -> {
            modelsListView.setItems(FXCollections.observableArrayList(filteredList));
            modelsListView.getSelectionModel().clearSelection();
        });
    }
}
