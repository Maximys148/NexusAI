package com.maximys.nexus.client.backend.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.maximys.nexus.client.backend.model.GitHubReleaseInfo;

import java.io.IOException;

// Сервис отвечающий за переключение между окнами

@Service
public class NavigationService {

    private static final Logger log = LoggerFactory.getLogger(NavigationService.class);

    private final ApplicationContext springContext;

    private final LanguageService langService;

    public NavigationService(ApplicationContext springContext, LanguageService langService) {
        this.springContext = springContext;
        this.langService = langService;
    }

    public void load(StackPane container, String fxmlName) {
        try {
            // Формируем путь
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/maximys/nexus/" + fxmlName));

            // Позволяем Spring внедрять зависимости в новые контроллеры
            loader.setControllerFactory(springContext::getBean);

            // Передаем текущие переводы
            loader.setResources(langService.getCurrentBundle());

            Node view = loader.load();

            // Очищаем контейнер и вставляем новый экран
            container.getChildren().setAll(view);
            log.info("Экран {} успешно загружен", fxmlName);

        } catch (IOException e) {
            log.error("Не удалось загрузить FXML: " + fxmlName, e);
        }
    }

    // ==================== МОДАЛЬНОЕ ОКНО ОБНОВЛЕНИЙ ====================

    /**
     * Показывает модальное окно с информацией об обновлении.
     *
     * @param overlay StackPane-оверлей для отображения модалки
     * @param modalTitleLabel Label для заголовка модалки
     * @param changelogTextArea TextArea для отображения changelog
     * @param releaseInfo данные релиза для отображения
     */

    public void showUpdateModal(
            StackPane overlay,
            Label modalTitleLabel,
            TextArea changelogTextArea,
            GitHubReleaseInfo releaseInfo
    ) {
        if (releaseInfo == null) {
            log.warn("Попытка показать модалку обновления с null releaseInfo");
            return;
        }

        // Биндинг локализованного заголовка с параметром
        langService.bindText(modalTitleLabel, "ui.update.modal.title", releaseInfo.getTagName());

        // Парсинг и отображение чейнджлога
        changelogTextArea.setText(langService.parseChangelog(releaseInfo.getBody()));

        // Показ оверлея
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    /**
     * Скрывает модальное окно обновления.
     */
    public void hideUpdateModal(StackPane overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
    }
}

