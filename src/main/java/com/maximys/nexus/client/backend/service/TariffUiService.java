package com.maximys.nexus.client.backend.service;

import com.maximys.nexus.client.backend.dto.ModelType;
import com.maximys.nexus.client.backend.model.AiModel.ClientAiModel;
import com.maximys.nexus.client.backend.model.AiModel.PaidTariffModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;

@Service
public class TariffUiService {

    private static final Logger log = LoggerFactory.getLogger(TariffUiService.class);

    private final ModelService modelService;
    private final LanguageService langService;

    public TariffUiService(ModelService modelService, LanguageService langService) {
        this.modelService = modelService;
        this.langService = langService;
    }

    public void initTariffsView(Map<String, Node> nodes) {
        // Статическая локализация заголовков страницы
        if (nodes.get("tariffHeaderLabel") instanceof Label l) langService.bindText(l, "ui.tariff.header");
        if (nodes.get("tariffSubHeaderLabel") instanceof Label l) langService.bindText(l, "ui.tariff.subheader");

        if (nodes.get("tariffsContainer") instanceof FlowPane container) {
            container.getChildren().clear();

            // Извлекаем облачные модели из глобального реестра
            List<ClientAiModel> cloudModels = modelService.getModels().stream()
                    .filter(m -> m.getType() == ModelType.CLOUD_FREE || m.getType() == ModelType.CLOUD_PAID)
                    .toList();

            if (cloudModels.isEmpty()) {
                Label emptyLabel = new Label();
                langService.bindText(emptyLabel, "ui.tariff.empty_list");
                container.getChildren().add(emptyLabel);
                return;
            }

            for (ClientAiModel model : cloudModels) {
                container.getChildren().add(createTariffCard(model));
            }
        }
    }

    private VBox createTariffCard(ClientAiModel model) {
        // Контейнер карточки с фиксированной шириной и высотой для ровной сетки
        VBox card = new VBox(16);
        card.setPrefWidth(290);
        card.setMinWidth(290);
        card.setPrefHeight(320);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -color-border-default;");

        // --- БЕЙДЖ ТИПА ТАРИФА ---
        Label typeBadge = new Label();
        typeBadge.setStyle("-fx-padding: 2 8 2 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

        if (model.getType() == ModelType.CLOUD_FREE) {
            typeBadge.getStyleClass().add("badge-neutral");
            langService.bindText(typeBadge, "ui.tariff.badge.free");
        } else {
            typeBadge.getStyleClass().add("badge-danger");
            langService.bindText(typeBadge, "ui.tariff.badge.premium");
        }

        // --- НАЗВАНИЕ МОДЕЛИ С МИНИ-ИКОНКОЙ ---
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(model.getName());
        titleLabel.getStyleClass().add("title-3");

        headerBox.getChildren().addAll(titleLabel);

        // --- ОПИСАНИЕ МОДЕЛИ (ИСПРАВЛЕНО ОТОБРАЖЕНИЕ) ---
        Label descLabel = new Label();
        descLabel.setWrapText(true);
        descLabel.setMinHeight(70);
        descLabel.setMaxHeight(70);
        descLabel.setAlignment(Pos.TOP_LEFT);
        descLabel.getStyleClass().add("text-muted");

        /*// ШАГ 1: Гарантируем немедленный вывод текста до срабатывания слушателей i18n
        descLabel.setText(model.getDescription(langService.getCurrentLangCode()));

        // ШАГ 2: Привязываем реактивное изменение текста из БД при смене языка в рантайме
        descLabel.textProperty().bind(Bindings.createStringBinding(
                () -> model.getDescription(langService.getCurrentLangCode()),
                langService.getCurrentBundle() == null ? Bindings.createStringBinding(() -> "") : langService.getCurrentBundle()
        ));*/

        // Пружина, прижимающая цену и кнопку вниз
        Pane spacer = new Pane();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // --- БЛОК С ЦЕНОЙ ---
        HBox priceBox = new HBox(4);
        priceBox.setAlignment(Pos.BASELINE_LEFT);

        Label priceValueLabel = new Label();
        priceValueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900;");

        Label pricePeriodLabel = new Label();
        pricePeriodLabel.getStyleClass().add("text-muted");

        if (model.getType() == ModelType.CLOUD_FREE) {
            priceValueLabel.setText("0$");
            langService.bindText(pricePeriodLabel, "ui.tariff.price.forever");
        } else if (model instanceof PaidTariffModel paidModel) {
            priceValueLabel.setText(paidModel.getPrice());
            langService.bindText(pricePeriodLabel, "ui.tariff.price.month");
        }
        priceBox.getChildren().addAll(priceValueLabel, pricePeriodLabel);

        // --- КНОПКА ДЕЙСТВИЯ С ИКОНКОЙ ---
        Button actionButton = new Button();
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setPrefHeight(38);

        if (model.getType() == ModelType.CLOUD_FREE) {
            actionButton.getStyleClass().add("button-outlined");
            langService.bindText(actionButton, "ui.tariff.btn.use");
        } else {
            actionButton.getStyleClass().add("button-primary");
            langService.bindText(actionButton, "ui.tariff.btn.buy");
        }

        actionButton.setOnAction(e -> {
            log.info("Выбран тариф облачной модели: {}", model.getId());
            // Логика использования или покупки
        });

        // Собираем всё в карточку
        card.getChildren().addAll(typeBadge, headerBox, descLabel, spacer, priceBox, actionButton);
        return card;
    }
}
