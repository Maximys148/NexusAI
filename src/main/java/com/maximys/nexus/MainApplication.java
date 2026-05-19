package com.maximys.nexus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import com.maximys.nexus.client.backend.service.MainApplicationService;

// Класс запуска приложения

@SpringBootApplication
@EnableAsync
public class MainApplication extends Application {

    @Getter
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

    public static void main(String[] args) {
        launch(args);
    }
}
