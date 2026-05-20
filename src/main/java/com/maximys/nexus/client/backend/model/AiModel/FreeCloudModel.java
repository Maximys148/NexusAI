package com.maximys.nexus.client.backend.model.AiModel;

import com.maximys.nexus.client.backend.dto.ModelType;

public class FreeCloudModel extends ClientAiModel {
    public FreeCloudModel(String id, String name, String description) {
        super(id, name, description, ModelType.CLOUD_FREE);
    }
}
