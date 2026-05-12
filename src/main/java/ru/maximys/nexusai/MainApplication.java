package ru.maximys.nexusai;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import ru.maximys.nexusai.backend.config.AppSettings;
import ru.maximys.nexusai.backend.service.LanguageService;
import ru.maximys.nexusai.backend.service.MainApplicationService;
import ru.maximys.nexusai.backend.service.SettingService;

import java.util.Objects;

// Класс запуска приложения

@SpringBootApplication
public class MainApplication extends Application {

    private static ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(MainApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        MainApplicationService mainApplicationService = springContext.getBean(MainApplicationService.class);
        mainApplicationService.showMainScene(stage);
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }

    public static ConfigurableApplicationContext getContext() {
        return springContext;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
