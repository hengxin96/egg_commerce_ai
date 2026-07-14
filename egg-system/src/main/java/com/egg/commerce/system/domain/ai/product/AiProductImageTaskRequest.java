package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** 创建商品图任务请求。 */
public class AiProductImageTaskRequest implements Serializable
{
    private static final long serialVersionUID = 1L;
    private String productAssetId;
    private String assetType;
    private String targetPlatform;
    private String aspectRatio;
    private String quality;
    private String userRequirement;

    public String getProductAssetId() { return productAssetId; }
    public void setProductAssetId(String productAssetId) { this.productAssetId = productAssetId; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String targetPlatform) { this.targetPlatform = targetPlatform; }
    public String getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public String getUserRequirement() { return userRequirement; }
    public void setUserRequirement(String userRequirement) { this.userRequirement = userRequirement; }
}
