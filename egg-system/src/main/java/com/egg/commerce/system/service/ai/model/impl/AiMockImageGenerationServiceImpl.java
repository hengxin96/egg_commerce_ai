package com.egg.commerce.system.service.ai.model.impl;

import com.egg.commerce.system.service.ai.model.AiImageGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiImageGenerationResult;
import com.egg.commerce.system.service.ai.model.AiImageGenerationService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 本地 Mock 图片生成实现，仅用于测试和本地联调。 */
@Service
@ConditionalOnProperty(name = "egg.ai.model-gateway.image.provider", havingValue = "mock")
public class AiMockImageGenerationServiceImpl implements AiImageGenerationService
{
    private static final String MODEL_NAME = "mock-image-model";

    @Override
    public AiImageGenerationResult generate(AiImageGenerationRequest request)
    {
        AiImageGenerationResult result = new AiImageGenerationResult();
        result.setProvider("mock");
        result.setModel(MODEL_NAME);
        result.setTaskId("mock-image-task");
        result.setImageUrls(List.of("https://example.invalid/mock-product-image.png"));
        result.setRawResponse("{\"mock\":true}");
        return result;
    }
}
