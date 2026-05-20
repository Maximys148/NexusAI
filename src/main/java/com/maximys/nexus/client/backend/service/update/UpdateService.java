package com.maximys.nexus.client.backend.service.update;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.maximys.nexus.MainApplication;
import com.maximys.nexus.client.backend.model.GitHubReleaseInfo;
import com.maximys.nexus.client.backend.service.LanguageService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// Сервис управления накопительными обновлениями через GitHub Releases API
@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);

    @Getter
    @Value("${app.version}")
    private String currentVersion;

    @Value("${app.github.api.url}")
    private String githubApiUrl; // Ссылка должна вести строго на .../releases (без /latest на конце)

    private final RestTemplate restTemplate = new RestTemplate();
    private final LanguageService langService;

    private String latestDownloadUrl;

    public UpdateService(LanguageService langService) {
        this.langService = langService;
    }

    // ==================== ПУБЛИЧНЫЙ API ====================

    public void initUpdateFeature(Map<String, Node> uiElements, Consumer<GitHubReleaseInfo> onReleaseFound) {
        bindVersionLabel(uiElements);
        hideUpdateButton(uiElements);
        checkForUpdates(uiElements, onReleaseFound);
    }

    public void processUpdate() {
        new Thread(this::executeUpdateFlow).start();
    }

    /**
     * Единственный метод управления модальным окном обновлений.
     * Вызывается из MainService. Полностью настраивает UI и подписывается на смену языка.
     */
    /**
     * Единственный метод управления модальным окном обновлений.
     * Вызывается из MainService. Полностью настраивает UI и подписывается на смену языка.
     */
    public void bindAndShowUpdateModal(StackPane updateOverlay, Label modalTitleLabel, TextArea changelogTextArea, GitHubReleaseInfo activeReleaseInfo) {
        if (activeReleaseInfo == null || activeReleaseInfo.getSkippedReleases() == null) {
            log.warn("[UPDATE] Нет данных для отображения модального окна обновлений.");
            return;
        }

        // 1. Сбрасываем старые привязки текста (на всякий случай)
        changelogTextArea.textProperty().unbind();

        // 2. Первичное заполнение интерфейса при открытии окна
        langService.bindText(modalTitleLabel, "ui.update.modal.title", activeReleaseInfo.getTagName());
        changelogTextArea.setText(buildAccumulatedChangelog(activeReleaseInfo.getSkippedReleases()));

        // 3. РЕАКТИВНЫЙ СЛУШАТЕЛЬ: Теперь ресурсы гарантированно отслеживаются!
        langService.resourcesProperty().addListener((observable, oldBundle, newBundle) -> {
            if (newBundle != null) {
                // Перерисовываем UI в безопасном потоке JavaFX на лету
                Platform.runLater(() -> {
                    langService.bindText(modalTitleLabel, "ui.update.modal.title", activeReleaseInfo.getTagName());
                    changelogTextArea.setText(buildAccumulatedChangelog(activeReleaseInfo.getSkippedReleases()));
                    log.info("[UPDATE] История изменений успешно переведена на лету после смены языка в настройках.");
                });
            }
        });

        // 4. Отображение оверлея
        updateOverlay.setVisible(true);
        updateOverlay.setManaged(true);
    }

    // ==================== ВНУТРЕННЯЯ ЛОГИКА ====================

    public List<Map<String, Object>> getAllReleasesInfo() {
        try {
            log.info("[UPDATE] Запрос истории обновлений по адресу: {}", githubApiUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(githubApiUrl, String.class);
            String json = response.getBody();

            if (json == null || json.isBlank()) return List.of();

            ObjectMapper mapper = new ObjectMapper();
            json = json.trim();

            if (json.startsWith("[")) {
                return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            }

            if (json.startsWith("{")) {
                log.warn("[UPDATE] URL вернул одиночный объект вместо списка. Авто-обертывание.");
                Map<String, Object> singleRelease = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                return List.of(singleRelease);
            }

            return List.of();
        } catch (Exception e) {
            log.error("[UPDATE] Ошибка при получении или десериализации списка релизов: {}", e.getMessage());
            return List.of();
        }
    }

    private void bindVersionLabel(Map<String, Node> uiElements) {
        if (uiElements.get("versionLabel") instanceof Label label) {
            label.setText("v" + getCurrentVersion());
        }
    }

    private void hideUpdateButton(Map<String, Node> uiElements) {
        if (uiElements.get("updateButton") instanceof Button btn) {
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }

    private void checkForUpdates(Map<String, Node> uiElements, Consumer<GitHubReleaseInfo> onReleaseFound) {
        log.info("[UPDATE] Запуск фоновой проверки обновлений...");
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> allReleases = getAllReleasesInfo();
                if (allReleases.isEmpty()) return;

                // Отбираем только те релизы, которые новее текущей версии
                List<Map<String, Object>> skippedReleases = allReleases.stream()
                        .filter(release -> isNewerVersion((String) release.get("tag_name")))
                        .toList();

                if (!skippedReleases.isEmpty()) {
                    // Самый верхний релиз в списке — самый свежий
                    Map<String, Object> latestRelease = skippedReleases.get(0);
                    String absoluteLatestTag = (String) latestRelease.get("tag_name");

                    // Сохраняем ссылку на скачивание
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> assets = (List<Map<String, Object>>) latestRelease.get("assets");
                    if (assets != null && !assets.isEmpty()) {
                        this.latestDownloadUrl = (String) assets.get(0).get("browser_download_url");
                    }

                    // Формируем объект информации о релизах
                    var releaseInfo = new GitHubReleaseInfo();
                    releaseInfo.setTagName(absoluteLatestTag);
                    releaseInfo.setSkippedReleases(skippedReleases); // Сохраняем СЫРЫЕ данные!

                    log.info("[UPDATE] Найдено пропущенных обновлений: {} шт.", skippedReleases.size());
                    Platform.runLater(() -> showUpdateButton(uiElements, releaseInfo));
                    onReleaseFound.accept(releaseInfo);
                } else {
                    log.info("[UPDATE] Клиент использует последнюю версию. Обновления не требуются.");
                }

            } catch (Exception e) {
                log.error("[UPDATE] Ошибка выполнения цепочки обновлений: ", e);
            }
        });
    }

    /**
     * Динамически собирает накопительный текст обновлений под ТЕКУЩИЙ язык приложения
     */
    public String buildAccumulatedChangelog(List<Map<String, Object>> skippedReleases) {
        if (skippedReleases == null || skippedReleases.isEmpty()) return "";

        StringBuilder combinedChangelog = new StringBuilder();
        String currentLang = langService.getCurrentLangCode().toLowerCase(); // "ru" или "en"

        // Извлекаем актуальный перевод слова "Версия"
        String versionWord = "Version";
        try {
            if (langService.getCurrentBundle() != null) {
                versionWord = langService.getCurrentBundle().getString("ui.update.version_prefix");
            }
        } catch (Exception e) {
            if (currentLang.equals("ru")) versionWord = "Версия";
        }

        for (Map<String, Object> release : skippedReleases) {
            String tag = (String) release.get("tag_name");
            String rawBody = (String) release.getOrDefault("body", "Описание изменений отсутствует.");

            // Переводим прямо в рантайме
            String localizedBody = parseSingleReleaseChangelog(rawBody, currentLang);

            combinedChangelog.append("### ").append(versionWord).append(" ").append(tag).append("\n")
                    .append(localizedBody).append("\n\n")
                    .append("--------------------------------------------------\n\n");
        }

        return combinedChangelog.toString().trim();
    }


    private String parseSingleReleaseChangelog(String rawBody, String langCode) {
        if (rawBody == null) return "";

        // Делаем поиск независимым от регистра (работает и с [RU], и с [ru])
        String bodyLower = rawBody.toLowerCase();
        String langKey = "[" + langCode.toLowerCase() + "]";

        if (!bodyLower.contains("[ru]") && !bodyLower.contains("[en]")) {
            return rawBody.trim();
        }

        if (bodyLower.contains(langKey)) {
            try {
                int startIdx = bodyLower.indexOf(langKey) + langKey.length();
                int endIdx = bodyLower.indexOf("[", startIdx);

                if (endIdx != -1) {
                    return rawBody.substring(startIdx, endIdx).trim();
                } else {
                    return rawBody.substring(startIdx).trim();
                }
            } catch (Exception e) {
                return rawBody.trim();
            }
        }

        // Фолбэк на случай, если перевод не найден
        return langCode.equalsIgnoreCase("ru")
                ? "Описание изменений недоступно на вашем языке."
                : "Changelog is not available in your language.";
    }


    private void showUpdateButton(Map<String, Node> uiElements, GitHubReleaseInfo release) {
        // Показываем кнопку "Update" на сайдбаре
        if (uiElements.get("updateButton") instanceof Button btn) {
            btn.setVisible(true);
            btn.setManaged(true);
        }
    }


    private boolean isNewerVersion(String remoteTag) {
        if (currentVersion == null || remoteTag == null) return false;
        String v1 = currentVersion.replace("v", "").trim();
        String v2 = remoteTag.replace("v", "").trim();
        return compareVersionStrings(v2, v1) > 0;
    }

    private int compareVersionStrings(String v1, String v2) {
        String[] vals1 = v1.split("\\.");
        String[] vals2 = v2.split("\\.");
        int i = 0;

        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }

        if (i < vals1.length && i < vals2.length) {
            try {
                int num1 = Integer.parseInt(vals1[i].trim());
                int num2 = Integer.parseInt(vals2[i].trim());
                return Integer.compare(num1, num2);
            } catch (NumberFormatException e) {
                return Integer.signum(vals1[i].compareTo(vals2[i]));
            }
        }

        return Integer.signum(vals1.length - vals2.length);
    }

    private void executeUpdateFlow() {
        try {
            log.info("Запуск скачивания накопительного обновления...");
            if (this.latestDownloadUrl == null || this.latestDownloadUrl.isEmpty()) {
                log.error("Ссылка на ассет загрузки последнего релиза не найдена.");
                return;
            }

            String tempJar = Paths.get(System.getProperty("java.io.tmpdir"), "update.jar").toString();
            String currentJar = getCurrentJarPath();
            String exePath = getExePath(currentJar);

            downloadFile(this.latestDownloadUrl, tempJar);
            createUpdaterScript(tempJar, currentJar, exePath);
            launchUpdater();
        } catch (Exception e) {
            log.error("Критическая ошибка при накатывании обновления: ", e);
        }
    }

    private String getCurrentJarPath() throws Exception {
        return new File(MainApplication.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getPath();
    }

    private String getExePath(String jarPath) {
        File parent = new File(jarPath).getParentFile().getParentFile();
        return new File(parent, "Nexus.exe").getAbsolutePath();
    }

    private void downloadFile(String url, String dest) throws IOException {
        try (var in = new java.net.URL(url).openStream()) {
            Files.copy(in, Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void createUpdaterScript(String tempJar, String currentJar, String exePath) throws IOException {
        var script = Arrays.asList(
                "@echo off",
                "chcp 65001 > nul",
                "echo [Nexus Update] Ожидание закрытия программы...",
                "timeout /t 3 /nobreak > nul",
                "move /y \"" + tempJar + "\" \"" + currentJar + "\"",
                "start \"\" \"" + exePath + "\"",
                "exit"
        );
        Files.write(Paths.get("updater.bat"), script, StandardCharsets.UTF_8);
    }

    private void launchUpdater() throws IOException {
        new ProcessBuilder("cmd", "/c", "start", "updater.bat").start();
        System.exit(0);
    }
}
