package com.egg.commerce.system.service.ai.model;

import java.util.function.Consumer;

/**
 * 文本生成模型网关接口。
 *
 * <p>业务服务依赖该接口而不是 DeepSeek 或千问的 HTTP 细节，后续切换供应商、增加调用日志或重试策略时，
 * 不需要改剧情生成主流程。</p>
 */
public interface AiTextGenerationService
{
    /** 根据已渲染提示词调用文本模型并返回正文结果。 */
    AiTextGenerationResult generate(AiTextGenerationRequest request);

    /** 根据已渲染提示词调用文本模型，并在供应商支持时把原始增量正文推给调用方。 */
    default AiTextGenerationResult generateStream(AiTextGenerationRequest request, Consumer<String> onDelta)
    {
        AiTextGenerationResult result = generate(request);
        if (result != null && result.getContent() != null && !result.getContent().isEmpty() && onDelta != null)
        {
            onDelta.accept(result.getContent());
        }
        return result;
    }
}
