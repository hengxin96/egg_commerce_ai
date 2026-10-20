package com.egg.commerce.system.service.ai.model;

/** 图片生成模型网关接口。 */
public interface AiImageGenerationService
{
    /** 根据提示词和参考图调用图片模型并返回生成图片 URL。 */
    AiImageGenerationResult generate(AiImageGenerationRequest request);
}
