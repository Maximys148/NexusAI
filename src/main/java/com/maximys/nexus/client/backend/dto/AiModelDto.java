package com.maximys.nexus.client.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiModelDto {
    private String id;
    private String name;
    private String description;
    private ModelType modelType; // Соответствует серверному Enum

    // Специфично для LOCAL
    private String size;
    private String hardware;
    private boolean downloaded;  // Этот флаг клиент проставит сам после пинга Ollama

    // Специфично для CLOUD (тарифов)
    private String price;
}
