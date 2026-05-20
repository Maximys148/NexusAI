package com.maximys.nexus.client.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.maximys.nexus.client.backend.service.LocalModelUiService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Component
public class LocalModelController {

    private static final Logger log = LoggerFactory.getLogger(LocalModelController.class);
    private final LocalModelUiService localModelUiService;

    @FXML private Label localHeaderLabel;
    @FXML private Label localSubHeaderLabel;
    @FXML private VBox modelsContainer;

    public LocalModelController(LocalModelUiService localModelUiService) {
        this.localModelUiService = localModelUiService;
    }

    @FXML
    public void initialize() {
        localModelUiService.initLocalModelsView(getXmlNodes());
    }

    private Map<String, Node> getXmlNodes() {
        Map<String, Node> nodes = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(FXML.class) && Node.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    Node node = (Node) field.get(this);
                    if (node != null) {
                        nodes.put(field.getName(), node);
                    }
                } catch (IllegalAccessException e) {
                    log.error("Ошибка при сборке карты узлов LocalModel: {}", e.getMessage());
                }
            }
        }
        return nodes;
    }
}
