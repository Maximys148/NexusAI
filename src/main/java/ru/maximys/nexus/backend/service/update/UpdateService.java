package ru.maximys.nexus.backend.service.update;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.maximys.nexus.MainApplication;
import ru.maximys.nexus.backend.model.GitHubReleaseInfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// Сервис управления обновлениями через GitHub Releases API

@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);

    @Getter
    @Value("${app.version}")
    private String currentVersion;

    @Value("${app.github.api.url}")
    private String githubApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==================== ПУБЛИЧНЫЙ API ====================

    /**
     * Инициализирует UI-элементы обновлений и запускает фоновую проверку.
     *
     * @param uiElements карта элементов интерфейса
     * @param onReleaseFound колбэк, вызываемый при обнаружении нового релиза
     */
    public void initUpdateFeature(Map<String, Node> uiElements, Consumer<GitHubReleaseInfo> onReleaseFound) {
        bindVersionLabel(uiElements);
        hideUpdateButton(uiElements);
        checkForUpdates(uiElements, onReleaseFound);
    }

    /**
     * Запускает процесс загрузки и установки обновления.
     */
    public void processUpdate() {
        new Thread(this::executeUpdateFlow).start();
    }

    // ==================== ВНУТРЕННЯЯ ЛОГИКА ====================

    public Map<String, Object> getLatestReleaseInfo() {
        try {
            return restTemplate.getForObject(githubApiUrl, Map.class);
        } catch (Exception e) {
            log.error("Ошибка при обращении к GitHub API: {}", e.getMessage());
            return null;
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
        CompletableFuture.runAsync(() -> {
            try {
                var response = getLatestReleaseInfo();
                if (response != null && response.containsKey("tag_name")) {
                    String remoteTag = (String) response.get("tag_name");
                    String body = (String) response.getOrDefault("body", "Описание изменений отсутствует.");

                    if (isNewerVersion(remoteTag)) {
                        var releaseInfo = new GitHubReleaseInfo();
                        releaseInfo.setTagName(remoteTag);
                        releaseInfo.setBody(body);

                        // Обновляем UI в JavaFX-потоке
                        Platform.runLater(() -> showUpdateButton(uiElements, releaseInfo));
                        // Уведомляем фасад о найденном релизе
                        onReleaseFound.accept(releaseInfo);
                    }
                }
            } catch (Exception e) {
                log.error("Фоновая проверка обновлений провалена: {}", e.getMessage());
            }
        });
    }

    private void showUpdateButton(Map<String, Node> uiElements, GitHubReleaseInfo release) {
        if (uiElements.get("updateButton") instanceof Button btn) {
            btn.setVisible(true);
            btn.setManaged(true);
        }
        if (uiElements.get("versionLabel") instanceof Label label) {
            label.setText("Доступна " + release.getTagName());
        }
    }

    private boolean isNewerVersion(String remoteTag) {
        String v1 = currentVersion.replace("v", "").trim();
        String v2 = remoteTag.replace("v", "").trim();
        return v2.compareTo(v1) > 0;
    }

    private void executeUpdateFlow() {
        try {
            log.info("Запуск процесса обновления...");

            var latest = getLatestReleaseInfo();
            if (latest == null || !latest.containsKey("assets")) {
                log.error("Не удалось получить информацию о релизе");
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assets = (List<Map<String, Object>>) latest.get("assets");
            if (assets.isEmpty()) {
                log.error("Нет доступных файлов для загрузки");
                return;
            }

            String downloadUrl = (String) assets.get(0).get("browser_download_url");
            String tempJar = Paths.get(System.getProperty("java.io.tmpdir"), "update.jar").toString();
            String currentJar = getCurrentJarPath();
            String exePath = getExePath(currentJar);

            downloadFile(downloadUrl, tempJar);
            createUpdaterScript(tempJar, currentJar, exePath);
            launchUpdater();

        } catch (Exception e) {
            log.error("Критическая ошибка при обновлении: ", e);
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