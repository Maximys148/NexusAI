package ru.maximys.nexusai.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;
import ru.maximys.nexusai.backend.service.ChatService;

// Контролер отвечающий за отображения страницы чата (chat-view.fxml)

@Component
public class ChatController {

    private final ChatService chatService;

    @FXML private VBox messageContainer;
    @FXML private TextField userMessageField;
    @FXML private ScrollPane chatScrollPane;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @FXML
    public void onSendMessage() {
        String userText = userMessageField.getText();
        if (userText.isEmpty()) return;

        // 1. Добавляем сообщение пользователя (справа)
        chatService.addMessage(userText, true, chatScrollPane, messageContainer);

        // Очищаем поле
        userMessageField.clear();

        // 2. Имитируем ответ нейросети (слева)
        // Позже здесь будет вызов aiService.ask(userText)
        chatService.addMessage("Ответ от Nexus AI на ваш запрос...", false, chatScrollPane, messageContainer);
    }

}
