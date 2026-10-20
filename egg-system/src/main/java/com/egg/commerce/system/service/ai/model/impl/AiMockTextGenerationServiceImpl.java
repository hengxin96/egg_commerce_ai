package com.egg.commerce.system.service.ai.model.impl;

import com.egg.commerce.system.service.ai.model.AiTextGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiTextGenerationResult;
import com.egg.commerce.system.service.ai.model.AiTextGenerationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 本地 Mock 文本生成实现。
 *
 * <p>只有显式配置 {@code egg.ai.model-gateway.story.provider=mock} 时才启用该实现，用于本地无 DeepSeek
 * API Key 的流程联调。Mock 结果不会回显完整提示词，避免维护人员或用户把提示词误认为真实模型输出。</p>
 */
@Service
@ConditionalOnProperty(name = "egg.ai.model-gateway.story.provider", havingValue = "mock")
public class AiMockTextGenerationServiceImpl implements AiTextGenerationService
{
    /** Mock 模型名称，写入任务记录用于区分真实模型输出。 */
    private static final String MODEL_NAME = "mock-story-model";

    /** 返回明确的本地模拟占位内容，提示需要切换到 DeepSeek 才能得到真实生成结果。 */
    @Override
    public AiTextGenerationResult generate(AiTextGenerationRequest request)
    {
        AiTextGenerationResult result = new AiTextGenerationResult();
        result.setProvider("mock");
        result.setModel(MODEL_NAME);
        result.setContent("【" + StringUtils.defaultString(request.getStage(), "文本生成")
                + "】Mock 生成内容：当前为本地模拟结果，未调用真实 DeepSeek。请将 EGG_MODEL_STORY_PROVIDER 设置为 deepseek 并配置 EGG_DEEPSEEK_API_KEY 后重新生成真实内容。");
        return result;
    }
}
