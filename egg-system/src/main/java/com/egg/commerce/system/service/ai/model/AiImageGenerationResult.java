package com.egg.commerce.system.service.ai.model;

import java.util.List;

/** 图片生成模型结果，保存供应商任务 ID、图片 URL 和原始响应。 */
public class AiImageGenerationResult
{
    private String provider;

    private String model;

    private String taskId;

    private List<String> imageUrls;

    private String rawResponse;

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

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public List<String> getImageUrls()
    {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls)
    {
        this.imageUrls = imageUrls;
    }

    public String getRawResponse()
    {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse)
    {
        this.rawResponse = rawResponse;
    }
}
