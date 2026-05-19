package com.maximys.nexus.client.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModelDto {
    private String id;               // Техническое имя (например, "deepseek-r1:8b")
    private String name;             // Красивое имя ("DeepSeek R1 8B")
    private boolean isLocal;         // Локальная или Облачная
    private String price;            // "Бесплатно" или "$0.002 / 1k tokens"
    private String size;             // Вес: "4.7 GB" или "— (Cloud)"
    private String requirements;     // Мощность: "8GB RAM / GTX 1060" или "Любой ПК"
    private String description;      // Описание возможностей модели
}
