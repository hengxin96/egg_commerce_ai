package com.egg.commerce.system.service.ai.model;

import java.io.Serializable;

/**
 * 文本生成结果对象。
 *
 * <p>除模型输出正文外，也保存供应商和模型名称，方便剧情任务把模型调用快照写入任务记录。</p>
 */
public class AiTextGenerationResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 模型供应商快照。 */
    private String provider;

    /** 实际调用或 Mock 使用的模型名称。 */
    private String model;

    /** 模型返回的正文内容。 */
    private String content;

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

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }
}
