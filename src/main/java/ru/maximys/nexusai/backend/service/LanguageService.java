package ru.maximys.nexusai.backend.service;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Labeled;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

// Сервис отвечающий за локализацию приложения

@Service
public class LanguageService {

    private final Preferences prefs = Preferences.userNodeForPackage(LanguageService.class);

    private final ObjectProperty<ResourceBundle> resources = new SimpleObjectProperty<>();

    private String currentLangCode;

    public LanguageService() {
        setLanguage(prefs.get("app_lang", "en")); // Язык по умолчанию
    }

    // Сохраняем язык в настройках OC и меняем сам язык
    public void saveLanguage(String langCode) {
        this.currentLangCode = langCode;
        prefs.put("app_lang", langCode);
        setLanguage(langCode);
    }

    public String getCurrentLangCode() {
        if (currentLangCode == null) {
            currentLangCode = prefs.get("app_lang", "en");
        }
        return currentLangCode;
    }

    public void setLanguage(String langCode) {
        Locale locale = new Locale(langCode);
        ResourceBundle bundle = ResourceBundle.getBundle("i18n." + langCode + ".messages", locale);
        resources.set(bundle);
    }

    public StringBinding createStringBinding(String key) {
        return Bindings.createStringBinding(
                () -> resources.get().getString(key),
                resources
        );
    }

    /**
     * Привязывает текст любого элемента (Button, Label и т.д.) к ключу в словаре.
     * Текст будет меняться автоматически при смене языка.
     */
    public void bindText(Labeled element, String key) {
        if (element != null) {
            element.textProperty().bind(Bindings.createStringBinding(() -> {
                try {
                    return resources.get().getString(key);
                } catch (Exception e) {
                    return "[MISSING: " + key + "]";
                }
            }, resources));
        }
    }

    public ResourceBundle getCurrentBundle() {
        return resources.get();
    }

    public ObjectProperty<ResourceBundle> resourcesProperty() {
        return resources;
    }

    public String getString(String key) {
        return resources.get().getString(key);
    }
}
