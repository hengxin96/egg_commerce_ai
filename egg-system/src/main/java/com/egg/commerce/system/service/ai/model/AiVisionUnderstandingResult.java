package com.egg.commerce.system.service.ai.model;

import java.io.Serializable;

/** 视觉理解模型返回结果，保留结构化内容和供应商原始响应摘要。 */
public class AiVisionUnderstandingResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String provider;

    private String model;

    private String content;

    private String rawResponse;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
}
