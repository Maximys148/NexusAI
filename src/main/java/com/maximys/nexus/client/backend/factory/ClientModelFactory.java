package com.maximys.nexus.client.backend.factory;

import com.maximys.nexus.client.backend.dto.AiModelDto;
import com.maximys.nexus.client.backend.model.*;
import com.maximys.nexus.client.backend.model.AiModel.ClientAiModel;
import com.maximys.nexus.client.backend.model.AiModel.FreeCloudModel;
import com.maximys.nexus.client.backend.model.AiModel.LocalModel;
import com.maximys.nexus.client.backend.model.AiModel.PaidTariffModel;

public class ClientModelFactory {

    public static ClientAiModel create(AiModelDto dto) {
        if (dto == null || dto.getModelType() == null) {
            throw new IllegalArgumentException("Некорректные данные DTO модели");
        }

        return switch (dto.getModelType()) {
            case LOCAL -> new LocalModel(
                    dto.getId(),
                    dto.getName(),
                    dto.getDescription(),
                    dto.getSize(),
                    dto.getHardware()
            );
            case CLOUD_FREE -> new FreeCloudModel(
                    dto.getId(),
                    dto.getName(),
                    dto.getDescription()
            );
            case CLOUD_PAID -> new PaidTariffModel(
                    dto.getId(),
                    dto.getName(),
                    dto.getDescription(),
                    dto.getPrice()
            );
        };
    }
}
