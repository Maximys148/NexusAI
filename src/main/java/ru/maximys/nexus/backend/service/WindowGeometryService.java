package ru.maximys.nexus.backend.service;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Service;
import ru.maximys.nexus.backend.config.AppSettings;

// Сервис отвечающий за перемещение окна приложения, изменения его размеров и т.д.

@Service
public class WindowGeometryService {

    private static final int RESIZE_MARGIN = 6;

    private double dragAnchorX, dragAnchorY;
    private double resizeAnchorX, resizeAnchorY;
    private boolean resizingLeft, resizingRight, resizingTop, resizingBottom;

    public void setupWindowGeometry(HBox titleBar, AppSettings appSettings) {
        setupDragging(titleBar);
        setupResizingListeners(titleBar, appSettings);
    }

    private void setupDragging(HBox titleBar) {
        titleBar.setOnMousePressed(e -> {
            dragAnchorX = e.getScreenX();
            dragAnchorY = e.getScreenY();
        });

        titleBar.setOnMouseDragged(e -> {
            if (isResizeCursor(titleBar.getScene().getCursor())) return;
            
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(stage.getX() + e.getScreenX() - dragAnchorX);
            stage.setY(stage.getY() + e.getScreenY() - dragAnchorY);
            
            dragAnchorX = e.getScreenX();
            dragAnchorY = e.getScreenY();
        });
    }

    private void setupResizingListeners(HBox titleBar, AppSettings appSettings) {
        titleBar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsW, oldWin, newWin) -> {
                    if (newWin instanceof Stage stage) {
                        applySavedSize(stage, appSettings);
                        setupResizing(stage, newScene);
                        setupPersistence(stage, appSettings);
                    }
                });
            }
        });
    }

    private void setupResizing(Stage stage, Scene scene) {
        scene.addEventHandler(MouseEvent.MOUSE_MOVED, e -> updateResizeCursor(scene, e));
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            resizeAnchorX = stage.getX() - e.getScreenX();
            resizeAnchorY = stage.getY() - e.getScreenY();
        });
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!isResizeCursor(scene.getCursor())) return;
            performResize(stage, e.getScreenX(), e.getScreenY());
        });
    }

    private void updateResizeCursor(Scene scene, MouseEvent e) {
        double x = e.getSceneX(), y = e.getSceneY();
        double w = scene.getWidth(), h = scene.getHeight();

        resizingLeft   = x < RESIZE_MARGIN;
        resizingRight  = x > w - RESIZE_MARGIN;
        resizingTop    = y < RESIZE_MARGIN;
        resizingBottom = y > h - RESIZE_MARGIN;

        scene.setCursor(resolveCursor());
    }

    private Cursor resolveCursor() {
        if (resizingLeft && resizingTop) return Cursor.NW_RESIZE;
        if (resizingLeft && resizingBottom) return Cursor.SW_RESIZE;
        if (resizingRight && resizingTop) return Cursor.NE_RESIZE;
        if (resizingRight && resizingBottom) return Cursor.SE_RESIZE;
        if (resizingLeft) return Cursor.W_RESIZE;
        if (resizingRight) return Cursor.E_RESIZE;
        if (resizingTop) return Cursor.N_RESIZE;
        if (resizingBottom) return Cursor.S_RESIZE;
        return Cursor.DEFAULT;
    }

    private boolean isResizeCursor(Cursor cursor) {
        return cursor != Cursor.DEFAULT;
    }

    private void performResize(Stage stage, double mouseX, double mouseY) {
        double minWidth = stage.getMinWidth(), minHeight = stage.getMinHeight();

        if (resizingRight) {
            double w = mouseX - stage.getX();
            if (w >= minWidth) stage.setWidth(w);
        }
        if (resizingLeft) {
            double w = stage.getX() + stage.getWidth() - mouseX;
            if (w >= minWidth) { stage.setX(mouseX); stage.setWidth(w); }
        }
        if (resizingBottom) {
            double h = mouseY - stage.getY();
            if (h >= minHeight) stage.setHeight(h);
        }
        if (resizingTop) {
            double h = stage.getY() + stage.getHeight() - mouseY;
            if (h >= minHeight) { stage.setY(mouseY); stage.setHeight(h); }
        }
    }

    private void applySavedSize(Stage stage, AppSettings appSettings) {
        stage.setWidth(appSettings.getWindowWidth());
        stage.setHeight(appSettings.getWindowHeight());
        stage.setMinWidth(450);
        stage.setMinHeight(350);
    }

    private void setupPersistence(Stage stage, AppSettings appSettings) {
        Runnable save = () -> {
            appSettings.setWindowWidth(stage.getWidth());
            appSettings.setWindowHeight(stage.getHeight());
            appSettings.flush();
        };
        stage.showingProperty().addListener((o, oldV, showing) -> { if (!showing) save.run(); });
        stage.setOnCloseRequest(e -> save.run());
    }
}