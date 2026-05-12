package ru.maximys.nexusai.backend.service;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Service;

// Сервис отвечающий за работу раздела чата

@Service
public class ChatService {

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
        messageContainer.getChildren().add(messageBox);

        // Скролл вниз
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
}
