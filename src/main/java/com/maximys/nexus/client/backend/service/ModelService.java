package com.maximys.nexus.client.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maximys.nexus.client.backend.dto.AiModelDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ModelService implements InitializingBean {

    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<AiModelDto> cachedModels = new CopyOnWriteArrayList<>();

    @Override
    public void afterPropertiesSet() {
        fetchModelsFromServer();
    }

    public CompletableFuture<List<AiModelDto>> fetchModelsFromServer() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/api/models/list"))
                .GET()
                .timeout(Duration.ofSeconds(3))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            List<AiModelDto> serverModels = mapper.readValue(
                                    response.body(),
                                    mapper.getTypeFactory().constructCollectionType(List.class, AiModelDto.class)
                            );
                            if (!serverModels.isEmpty()) {
                                cachedModels.clear();
                                cachedModels.addAll(serverModels);
                                return cachedModels;
                            }
                        } catch (Exception e) {
                            log.error("[ModelService] Ошибка парсинга моделей, уходим в оффлайн.");
                        }
                    }
                    return fetchOnlyDownloadedOllamaModels();
                }).exceptionally(ex -> {
                    log.warn("[ModelService] Сервер оффлайн. Считываем локальные модели Ollama.");
                    return fetchOnlyDownloadedOllamaModels();
                });
    }

    private List<AiModelDto> fetchOnlyDownloadedOllamaModels() {
        List<AiModelDto> localDownloaded = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode modelsNode = root.get("models");
                if (modelsNode != null && modelsNode.isArray()) {
                    for (JsonNode m : modelsNode) {
                        String fullOllamaName = m.get("name").asText();
                        String cleanName = fullOllamaName.contains(":") ? fullOllamaName.split(":")[0] : fullOllamaName;

                        localDownloaded.add(new AiModelDto(
                                fullOllamaName,
                                cleanName.toUpperCase() + " (Локальная)",
                                true,
                                "Бесплатно",
                                "Скачано",
                                "Ваш ПК",
                                "Автономная модель."
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[ModelService] Локальная утилита Ollama не запущена.");
        }

        cachedModels.clear();
        cachedModels.addAll(localDownloaded);
        return localDownloaded;
    }

    public CompletableFuture<Boolean> isModelDownloadedLocally(String modelId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/tags"))
                .GET()
                .timeout(Duration.ofSeconds(2))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode root = mapper.readTree(response.body());
                            JsonNode modelsNode = root.get("models");
                            if (modelsNode != null && modelsNode.isArray()) {
                                for (JsonNode model : modelsNode) {
                                    if (model.get("name").asText().startsWith(modelId)) {
                                        return true;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Ошибка парсинга тегов Ollama");
                        }
                    }
                    return false;
                }).exceptionally(ex -> false);
    }

    /**
     * Теперь возвращает ПОЛНЫЙ список всех моделей сервера для чата.
     * Локальные модели проверяются на скачанность, но не удаляются из списка в случае отсутствия.
     */
    public CompletableFuture<List<AiModelDto>> fetchActiveModelsForChat() {
        return fetchModelsFromServer().exceptionally(ex -> {
            log.warn("[ModelService] Сервер оффлайн, чат использует локальные теги Ollama");
            return fetchOnlyDownloadedOllamaModels();
        });
    }

    public List<AiModelDto> getCachedModels() {
        return new ArrayList<>(this.cachedModels);
    }

    public boolean checkIfModelIsLocal(String modelId) {
        if (modelId == null) return false;
        return cachedModels.stream()
                .filter(m -> modelId.equals(m.getId()))
                .findFirst()
                .map(AiModelDto::isLocal)
                .orElse(false);
    }
}
