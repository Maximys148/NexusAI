package ru.maximys.nexusai.backend.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/maximys/nexusai/" + fxmlName));

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
}

