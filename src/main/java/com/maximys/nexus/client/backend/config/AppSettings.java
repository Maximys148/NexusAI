package com.maximys.nexus.client.backend.config;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Запоминает настройки пользователя

@Component
public class AppSettings {
    private final Preferences prefs = Preferences.userRoot().node("ru.maximys.nexusai");

    private static final Logger log = LoggerFactory.getLogger(AppSettings.class);

    private static final String KEY_WIDTH = "window_width";
    private static final String KEY_HEIGHT = "window_height";

    private static final String THEME_KEY = "selected_theme";

    public double getWindowWidth() {
        return prefs.getDouble(KEY_WIDTH, 1024.0);
    }

    public void setWindowWidth(double width) {
        prefs.putDouble(KEY_WIDTH, width);
    }

    public double getWindowHeight() {
        return prefs.getDouble(KEY_HEIGHT, 768.0);
    }

    public void setWindowHeight(double height) {
        prefs.putDouble(KEY_HEIGHT, height);
    }

    public void saveTheme(String themeName) {
        prefs.put(THEME_KEY, themeName);
    }

    public String getSavedTheme() {
        // По умолчанию возвращаем Nord Dark, если ничего не сохранено
        return prefs.get(THEME_KEY, "Nord Dark");
    }

    // Масштаб
    public double getSavedScale() {
        return prefs.getDouble("ui_scale", 14.0);
    }

    public void saveScale(double value) {
        prefs.putDouble("ui_scale", value);
    }

    // Автозагрузка
    public boolean isStartupEnabled() {
        return prefs.getBoolean("startup", false);
    }

    public void saveStartup(boolean value) {
        prefs.putBoolean("startup", value);
    }

    public void flush() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            // Логируйте ошибку или оставьте пустой, если логирование еще не настроено
            log.error("Failed to save Nexus settings: " + e.getMessage());
        }
    }
}
