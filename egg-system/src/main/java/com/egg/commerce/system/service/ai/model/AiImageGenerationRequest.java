package com.egg.commerce.system.service.ai.model;

import java.util.List;

/** 图片生成模型请求，业务层只传已渲染的提示词和参考图信息。 */
public class AiImageGenerationRequest
{
    private String stage;

    private String positivePrompt;

    private String negativePrompt;

    private List<String> referenceImageUrls;

    private String aspectRatio;

    private String quality;

    private Integer imageCount;

    private Boolean sequential;

    private String provider;

    private String model;

    public String getStage()
    {
        return stage;
    }

    public void setStage(String stage)
    {
        this.stage = stage;
    }

    public String getPositivePrompt()
    {
        return positivePrompt;
    }

    public void setPositivePrompt(String positivePrompt)
    {
        this.positivePrompt = positivePrompt;
    }

    public String getNegativePrompt()
    {
        return negativePrompt;
    }

    public void setNegativePrompt(String negativePrompt)
    {
        this.negativePrompt = negativePrompt;
    }

    public List<String> getReferenceImageUrls()
    {
        return referenceImageUrls;
    }

    public void setReferenceImageUrls(List<String> referenceImageUrls)
    {
        this.referenceImageUrls = referenceImageUrls;
    }

    public String getAspectRatio()
    {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio)
    {
        this.aspectRatio = aspectRatio;
    }

    public String getQuality()
    {
        return quality;
    }

    public void setQuality(String quality)
    {
        this.quality = quality;
    }

    public Integer getImageCount()
    {
        return imageCount;
    }

    public void setImageCount(Integer imageCount)
    {
        this.imageCount = imageCount;
    }

    public Boolean getSequential()
    {
        return sequential;
    }

    public void setSequential(Boolean sequential)
    {
        this.sequential = sequential;
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
