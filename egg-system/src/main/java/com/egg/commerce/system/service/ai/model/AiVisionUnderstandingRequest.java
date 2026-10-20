package com.egg.commerce.system.service.ai.model;

import java.io.Serializable;
import java.util.List;

/**
 * 视觉理解请求对象。
 *
 * <p>商品图工作流只描述业务阶段、图片 URL 和结构化输出要求，具体供应商协议由模型网关实现隐藏。</p>
 */
public class AiVisionUnderstandingRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 当前业务节点，例如商品图视觉分析或生成后视觉审核。 */
    private String stage;

    /** 可选 system 角色提示词；在 DashScope OpenAI 兼容接口里会作为 messages 的一条消息传入。 */
    private String systemPrompt;

    /** 用户消息中的文本指令，必须明确要求 JSON 输出。 */
    private String prompt;

    /** 后端生成的 OSS 签名 URL，不能传本地路径或前端临时 URL。 */
    private List<String> imageUrls;

    /** 模型供应商快照，例如 dashscope 或 mock。 */
    private String provider;

    /** 模型名称快照，例如 qwen3.7-plus。 */
    private String model;

    /** 是否使用 OpenAI-compatible JSON Mode。 */
    private Boolean jsonResponse;

    /** 是否启用阿里视觉高分辨率图片理解参数。 */
    private Boolean highResolution;

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Boolean getJsonResponse() { return jsonResponse; }
    public void setJsonResponse(Boolean jsonResponse) { this.jsonResponse = jsonResponse; }
    public Boolean getHighResolution() { return highResolution; }
    public void setHighResolution(Boolean highResolution) { this.highResolution = highResolution; }
}
