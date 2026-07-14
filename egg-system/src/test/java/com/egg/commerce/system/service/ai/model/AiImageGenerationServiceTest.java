package com.egg.commerce.system.service.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.egg.commerce.system.service.ai.model.impl.AiMockImageGenerationServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 图片生成网关测试，确保业务层依赖稳定接口。 */
class AiImageGenerationServiceTest
{
    @Test
    void imageGenerationRequestAndResultKeepRuntimeFields()
    {
        AiImageGenerationRequest request = new AiImageGenerationRequest();
        request.setStage("生成中");
        request.setPositivePrompt("clean product image");
        request.setNegativePrompt("watermark");
        request.setReferenceImageUrls(List.of("https://oss/source.png"));
        request.setAspectRatio("3:4");
        request.setQuality("2K");
        request.setProvider("dashscope");
        request.setModel("wan2.7-image-pro");

        assertEquals("生成中", request.getStage());
        assertEquals("clean product image", request.getPositivePrompt());
        assertEquals("watermark", request.getNegativePrompt());
        assertEquals(List.of("https://oss/source.png"), request.getReferenceImageUrls());
        assertEquals("3:4", request.getAspectRatio());
        assertEquals("2K", request.getQuality());
        assertEquals("dashscope", request.getProvider());
        assertEquals("wan2.7-image-pro", request.getModel());
    }

    @Test
    void mockImageGenerationReturnsDeterministicResult()
    {
        AiImageGenerationRequest request = new AiImageGenerationRequest();
        request.setStage("生成中");
        request.setPositivePrompt("clean product image");
        request.setNegativePrompt("watermark");
        request.setReferenceImageUrls(List.of("https://oss/source.png"));
        request.setAspectRatio("3:4");
        request.setQuality("2K");

        AiImageGenerationResult result = new AiMockImageGenerationServiceImpl().generate(request);

        assertEquals("mock", result.getProvider());
        assertEquals("mock-image-model", result.getModel());
        assertEquals("mock-image-task", result.getTaskId());
        assertEquals(1, result.getImageUrls().size());
        assertEquals("https://example.invalid/mock-product-image.png", result.getImageUrls().get(0));
    }
}
