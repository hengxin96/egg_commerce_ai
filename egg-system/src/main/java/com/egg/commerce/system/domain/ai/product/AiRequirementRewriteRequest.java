package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** AI 帮写商品图要求请求。 */
public class AiRequirementRewriteRequest implements Serializable
{
    private static final long serialVersionUID = 1L;
    private String productAssetId;
    private String assetType;
    private String targetPlatform;
    private String userRequirement;

    public String getProductAssetId() { return productAssetId; }
    public void setProductAssetId(String productAssetId) { this.productAssetId = productAssetId; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String targetPlatform) { this.targetPlatform = targetPlatform; }
    public String getUserRequirement() { return userRequirement; }
    public void setUserRequirement(String userRequirement) { this.userRequirement = userRequirement; }
}
