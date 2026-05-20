package com.maximys.nexus.client.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maximys.nexus.client.backend.dto.AiModelDto;
import com.maximys.nexus.client.backend.factory.ClientModelFactory;
import com.maximys.nexus.client.backend.model.AiModel.ClientAiModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ГЛОБАЛЬНЫЙ СПИСОК (Как хранилище настроек AppSettings)
    private final List<ClientAiModel> globalModelsRegistry = new ArrayList<>();

    /**
     * Геттер для моментального синхронного доступа из других сервисов
     */
    public List<ClientAiModel> getModels() {
        return this.globalModelsRegistry;
    }

    /**
     * ВЫЗЫВАЕТСЯ ОДИН РАЗ ПРИ СТАРТЕ ПРИЛОЖЕНИЯ (в MainService / AppInitializer)
     * Скачивает данные и сохраняет их в глобальный реестр памяти.
     */
    public CompletableFuture<Void> syncModelsFromServer() {
        String targetUrl = "http://localhost:8080/api/models/list";
        log.info("[HTTP-GET] Первичная синхронизация каталога моделей: {}", targetUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            List<AiModelDto> dtoList = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<List<AiModelDto>>() {}
                            );

                            List<ClientAiModel> clientModels = dtoList.stream()
                                    .map(ClientModelFactory::create)
                                    .toList();

                            // АТОМАРНО ОБНОВЛЯЕМ ГЛОБАЛЬНЫЙ СПИСОК
                            synchronized (globalModelsRegistry) {
                                globalModelsRegistry.clear();
                                globalModelsRegistry.addAll(clientModels);
                            }

                            log.info("[HTTP-CLIENT] Глобальный реестр моделей успешно заполнен. Всего: {} шт.", globalModelsRegistry.size());
                        } catch (Exception e) {
                            log.error("[HTTP-GET] Критическая ошибка парсинга каталога моделей: ", e);
                        }
                    } else {
                        log.warn("[HTTP-GET] Сервер вернул ошибку каталога. Статус: {}", response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    log.error("[HTTP-GET] Бэкенд недоступен. Работаем в оффлайн-режиме: {}", ex.getMessage());
                    return null;
                });
    }
}
