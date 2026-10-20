package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** 创建商品图任务请求。 */
public class AiProductImageTaskRequest implements Serializable
{
    private static final long serialVersionUID = 1L;
    private String productAssetId;
    private String assetType;
    private String targetPlatform;
    private String targetLanguage;
    private Boolean textEnabled;
    private String selectedModel;
    private String aspectRatio;
    private String quality;
    private Integer imageCount;
    private String userRequirement;

    public String getProductAssetId() { return productAssetId; }
    public void setProductAssetId(String productAssetId) { this.productAssetId = productAssetId; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String targetPlatform) { this.targetPlatform = targetPlatform; }
    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
    public Boolean getTextEnabled() { return textEnabled; }
    public void setTextEnabled(Boolean textEnabled) { this.textEnabled = textEnabled; }
    public String getSelectedModel() { return selectedModel; }
    public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }
    public String getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public Integer getImageCount() { return imageCount; }
    public void setImageCount(Integer imageCount) { this.imageCount = imageCount; }
    public String getUserRequirement() { return userRequirement; }
    public void setUserRequirement(String userRequirement) { this.userRequirement = userRequirement; }
}
