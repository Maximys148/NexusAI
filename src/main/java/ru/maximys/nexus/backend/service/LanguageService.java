package ru.maximys.nexus.backend.service;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

// Сервис отвечающий за локализацию приложения

@Service
public class LanguageService {

    private final Preferences prefs = Preferences.userNodeForPackage(LanguageService.class);
    private final ObjectProperty<ResourceBundle> resources = new SimpleObjectProperty<>();
    private String currentLangCode;

    public LanguageService() {
        setLanguage(prefs.get("app_lang", "en"));
    }

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
        this.currentLangCode = langCode;
        Locale locale = new Locale(langCode);
        ResourceBundle bundle = ResourceBundle.getBundle("i18n." + langCode + ".messages", locale);
        resources.set(bundle);
    }

    public void bindText(Labeled element, String key, Object... args) {
        if (element != null) {
            element.textProperty().unbind();
            element.textProperty().bind(Bindings.createStringBinding(() -> {
                try {
                    String pattern = resources.get().getString(key);
                    if (args == null || args.length == 0) {
                        return pattern;
                    }
                    return java.text.MessageFormat.format(pattern, args);
                } catch (Exception e) {
                    return "[MISSING: " + key + "]";
                }
            }, resources));
        }
    }

    // Центральный метод локализации интерфейса main-view
    public void bindMainUserInterface(Map<String, Node> elements) {
        if (elements == null) return;

        bindNodeText(elements.get("functions"), "ui.sidebar.functions");
        bindNodeText(elements.get("profile"), "ui.titlebar.profile");
        bindNodeText(elements.get("welcome"), "ui.main.welcome");
        bindNodeText(elements.get("textButton"), "ui.sidebar.text");
        bindNodeText(elements.get("imageButton"), "ui.sidebar.image");
        bindNodeText(elements.get("videoButton"), "ui.sidebar.video");
        bindNodeText(elements.get("audioButton"), "ui.sidebar.audio");
        bindNodeText(elements.get("settingsButton"), "ui.sidebar.settings");
        bindNodeText(elements.get("updateButton"), "ui.sidebar.update");
        bindNodeText(elements.get("closeModalButton"), "ui.update.modal.btn.later");
        bindNodeText(elements.get("installUpdateButton"), "ui.update.modal.btn.install");
    }

    /**
     * Безопасное извлечение Labeled-свойств из абстрактной ноды (Паттерн-матчинг Java 21)
     */
    private void bindNodeText(Node node, String key) {
        if (node instanceof Labeled labeled) {
            bindText(labeled, key);
        }
    }

    public String parseChangelog(String rawBody) {
        if (rawBody == null) return "";
        String langCode = getCurrentLangCode().toUpperCase();
        String langKey = "[" + langCode + "]";

        if (!rawBody.contains(langKey)) {
            return rawBody;
        }
        try {
            int startIdx = rawBody.indexOf(langKey) + langKey.length();
            int endIdx = rawBody.indexOf("[", startIdx);
            if (endIdx != -1) {
                return rawBody.substring(startIdx, endIdx).trim();
            } else {
                return rawBody.substring(startIdx).trim();
            }
        } catch (Exception e) {
            return rawBody;
        }
    }

    public ResourceBundle getCurrentBundle() {
        return resources.get();
    }
}
