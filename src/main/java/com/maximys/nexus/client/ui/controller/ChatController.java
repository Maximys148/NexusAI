package com.maximys.nexus.client.ui.controller;

import com.maximys.nexus.client.backend.model.AiModel.ClientAiModel;
import com.maximys.nexus.client.backend.service.ChatService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextField userMessageField;
    @FXML private ComboBox<ClientAiModel> modelComboBox; // Хранит готовые объекты полиморфных моделей
    @FXML private Label attachedFilesLabel;

    @FXML
    public void initialize() {
        // Контроллер чата просто собирает свои FXML-узлы и отдает их сервису чата
        chatService.initChatView(getXmlNodes());
    }

    @FXML
    public void onAttachFile() {
        chatService.handleFileAttachment(attachedFilesLabel);
    }

    @FXML
    public void onSendMessage() {
        chatService.sendMessageWorkflow(getXmlNodes());
    }

    public Map<String, Node> getXmlNodes() {
        Map<String, Node> nodes = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(FXML.class) && Node.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    Node node = (Node) field.get(this);
                    if (node != null) nodes.put(field.getName(), node);
                } catch (IllegalAccessException e) {
                    log.error("Ошибка сборки карты узлов ChatView: {}", e.getMessage());
                }
            }
        }
        return nodes;
    }
}
