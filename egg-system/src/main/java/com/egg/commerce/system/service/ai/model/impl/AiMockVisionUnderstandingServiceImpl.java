package com.egg.commerce.system.service.ai.model.impl;

import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingRequest;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingResult;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingService;
import java.util.LinkedHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 本地 Mock 视觉理解实现，用于没有 DashScope Key 的开发和单元测试环境。 */
@Service
@ConditionalOnProperty(name = "egg.ai.model-gateway.vision.provider", havingValue = "mock")
public class AiMockVisionUnderstandingServiceImpl implements AiVisionUnderstandingService
{
    @Override
    public AiVisionUnderstandingResult analyze(AiVisionUnderstandingRequest request)
    {
        JSONObject summary = new JSONObject(new LinkedHashMap<>());
        summary.put("productNameGuess", "Mock 商品");
        summary.put("categoryGuess", "通用品类");
        summary.put("identityGuards", java.util.List.of("保持主体颜色", "保持结构", "保持 logo 和数量"));

        JSONObject output = new JSONObject(new LinkedHashMap<>());
        output.put("schemaVersion", "product_image_analysis_v1");
        output.put("sourceSummary", summary);

        AiVisionUnderstandingResult result = new AiVisionUnderstandingResult();
        result.setProvider("mock");
        result.setModel("mock-vision-model");
        result.setContent(output.toJSONString());
        result.setRawResponse(output.toJSONString());
        return result;
    }
}
