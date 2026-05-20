package com.maximys.nexus.client.backend.model.AiModel;

import com.maximys.nexus.client.backend.dto.ModelType;
import lombok.Getter;
import lombok.Setter;

@Getter
public class LocalModel extends ClientAiModel {
    private final String size;
    private final String hardware;

    @Setter
    private boolean downloaded;

    public LocalModel(String id, String name, String description, String size, String hardware) {
        super(id, name, description, ModelType.LOCAL);
        this.size = size;
        this.hardware = hardware;
        this.downloaded = false; // По умолчанию false, пока не проверит Ollama
    }
}
