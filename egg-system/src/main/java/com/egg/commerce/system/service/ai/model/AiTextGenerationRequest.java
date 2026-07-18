package com.egg.commerce.system.service.ai.model;

import java.io.Serializable;

/**
 * 文本生成请求对象。
 *
 * <p>剧情生成服务只关心“哪个节点、发什么提示词、用哪个模型快照”，具体 HTTP 协议和密钥由模型网关实现封装。</p>
 */
public class AiTextGenerationRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 当前业务节点名称，例如剧情生成、剧本生成或分镜校验。 */
    private String stage;

    /** 渲染占位符后的完整提示词。 */
    private String prompt;

    /** 模型供应商快照，例如 deepseek 或 mock。 */
    private String provider;

    /** 模型名称快照，例如 deepseek-v4-pro。 */
    private String model;

    public String getStage()
    {
        return stage;
    }

    public void setStage(String stage)
    {
        this.stage = stage;
    }

    public String getPrompt()
    {
        return prompt;
    }

    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }
}
