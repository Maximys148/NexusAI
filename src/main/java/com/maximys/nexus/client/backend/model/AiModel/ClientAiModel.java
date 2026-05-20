package com.maximys.nexus.client.backend.model.AiModel;

import com.maximys.nexus.client.backend.dto.ModelType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class ClientAiModel {
    private final String id;
    private final String name;
    private final String description;
    private final ModelType type;
}
