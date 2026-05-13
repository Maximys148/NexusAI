package ru.maximys.nexus.backend.config;

import java.util.prefs.Preferences;
import org.springframework.stereotype.Component;

@Component
public class AppSettings {
    private final Preferences prefs = Preferences.userRoot().node("ru.maximys.nexusai");
    private static final String THEME_KEY = "selected_theme";

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
}
