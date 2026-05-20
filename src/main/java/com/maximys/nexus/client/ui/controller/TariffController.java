package com.maximys.nexus.client.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.maximys.nexus.client.backend.service.TariffUiService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Component
public class TariffController {

    private static final Logger log = LoggerFactory.getLogger(TariffController.class);
    private final TariffUiService tariffUiService;

    @FXML private Label tariffHeaderLabel;
    @FXML private Label tariffSubHeaderLabel;
    @FXML private FlowPane tariffsContainer;

    public TariffController(TariffUiService tariffUiService) {
        this.tariffUiService = tariffUiService;
    }

    @FXML
    public void initialize() {
        tariffUiService.initTariffsView(getXmlNodes());
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
                    log.error("Ошибка при сборке карты узлов Tariff: {}", e.getMessage());
                }
            }
        }
        return nodes;
    }
}
