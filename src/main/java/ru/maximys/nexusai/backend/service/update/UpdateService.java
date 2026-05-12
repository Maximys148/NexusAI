package ru.maximys.nexusai.backend.service.update;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.maximys.nexusai.MainApplication;
import ru.maximys.nexusai.ui.controller.MainController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Сервис для проверки доступных обновлений и само обновление программы

@Service
public class UpdateService {
    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);

    @Value("${app.version}")
    private String currentVersion;

    @Value("${app.github.api.url}")
    private String GITHUB_API_URL;

    public String getCurrentVersion() {
        return currentVersion;
    }

    // --- ПРОВЕРКА НАЛИЧИЯ ОБНОВЛЕНИЙ ---

    public void checkUpdates(Consumer<String> onUpdateAvailable) {
        new Thread(() -> {
            try {
                Map<String, Object> latest = getLatestReleaseInfo();
                if (latest != null) {
                    String latestTagName = (String) latest.get("tag_name");
                    String latestVer = latestTagName.replaceAll("[^0-9.]", "");
                    String currentVer = currentVersion.replaceAll("[^0-9.]", "");

                    if (!currentVer.equals(latestVer)) {
                        String versionInfo = "v" + currentVersion + " -> " + latestTagName;
                        Platform.runLater(() -> onUpdateAvailable.accept(versionInfo));
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка в фоновой проверке обновлений", e);
            }
        }).start();
    }

    public Map<String, Object> getLatestReleaseInfo() {
        log.info("Запрос данных о релизе по адресу: {}", GITHUB_API_URL);
        try {
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(GITHUB_API_URL, Map.class);
        } catch (Exception e) {
            log.error("Ошибка при обращении к GitHub API: {}", e.getMessage());
            return null;
        }
    }

    // --- ЛОГИКА УСТАНОВКИ ---

    public void processUpdate() {
        new Thread(() -> {
            try {
                log.info("Запуск процесса обновления...");

                // 1. Получаем данные и ссылку
                Map<String, Object> latest = getLatestReleaseInfo();
                List<Map<String, Object>> assets = (List<Map<String, Object>>) latest.get("assets");
                String downloadUrl = (String) assets.get(0).get("browser_download_url");

                // 2. Пути
                String tempJarPath = Paths.get(System.getProperty("java.io.tmpdir"), "update.jar").toString();
                String currentJarPath = new File(MainApplication.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).getPath();
                File exeFile = new File(currentJarPath).getParentFile().getParentFile();
                String exePath = new File(exeFile, "NexusAI.exe").getAbsolutePath();

                // 3. Скачивание
                downloadNewVersion(downloadUrl, tempJarPath);

                // 4. Формирование батника
                List<String> commands = Arrays.asList(
                        "@echo off",
                        "chcp 65001 > nul",
                        "echo [Nexus AI Update] Ожидание закрытия программы...",
                        "timeout /t 3 /nobreak > nul",
                        "echo [Nexus AI Update] Замена файлов...",
                        "move /y \"" + tempJarPath + "\" \"" + currentJarPath + "\"",
                        "echo [Nexus AI Update] Перезапуск...",
                        "start \"\" \"" + exePath + "\"",
                        "del \"%~f0\""
                );

                Files.write(Paths.get("updater.bat"), commands, StandardCharsets.UTF_8);

                // 5. Запуск скрипта и выход
                new ProcessBuilder("cmd", "/c", "start", "updater.bat").start();
                System.exit(0);

            } catch (Exception e) {
                log.error("Критическая ошибка при обновлении: ", e);
            }
        }).start();
    }

    private void downloadNewVersion(String downloadUrl, String tempPath) throws IOException {
        log.info("Начинаем скачивание: {}", downloadUrl);
        java.net.URL url = new java.net.URL(downloadUrl);
        try (java.io.InputStream in = url.openStream()) {
            java.nio.file.Files.copy(in, Paths.get(tempPath), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

