package com.maximys.nexus.client.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String prompt;
    private String model;
    private List<String> attachedFiles; // Имена или Base64-содержимое файлов
    private boolean isLocal;            // Флаг: слать на удаленный сервер или локальную Ollama
}
