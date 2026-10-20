package com.egg.commerce.system.service.ai.model;

/** 视觉理解模型网关，供商品图分析和生成后审核复用。 */
public interface AiVisionUnderstandingService
{
    AiVisionUnderstandingResult analyze(AiVisionUnderstandingRequest request);
}
