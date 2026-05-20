package com.maximys.nexus.client.backend.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Objects;

// Сервис отвечающий за бизнес логику MainApplication

@Service
public class MainApplicationService {

    private final LanguageService langService;
    private final ApplicationContext springContext;

    public MainApplicationService(ApplicationContext springContext, LanguageService langService) {
        this.springContext = springContext;
        this.langService = langService;
    }

    public void showMainScene(Stage stage) throws Exception {
        // Настройка стиля и иконки
        stage.initStyle(StageStyle.UNDECORATED);
        
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon.png")));
        stage.getIcons().add(icon);

        // Загрузка FXML через Spring
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/maximys/nexus/main-view.fxml"));
        loader.setControllerFactory(springContext::getBean);
        loader.setResources(langService.getCurrentBundle());

        Parent root = loader.load();
        Scene scene = new Scene(root);
        
        // Настройка прозрачности
        scene.setFill(Color.TRANSPARENT);
        // stage.initStyle(StageStyle.TRANSPARENT); // Внимание: можно вызвать только один раз!

        stage.setScene(scene);
        stage.show();
    }
}
