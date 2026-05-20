package com.maximys.nexus.client.backend.model.AiModel;

import com.maximys.nexus.client.backend.dto.ModelType;
import lombok.Getter;

@Getter
public class PaidTariffModel extends ClientAiModel {
    private final String price;

    public PaidTariffModel(String id, String name, String description, String price) {
        super(id, name, description, ModelType.CLOUD_PAID);
        this.price = price;
    }
}
