package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;
import java.util.Date;

/** 商品图商品资产实体。 */
public class AiProductAsset implements Serializable
{
    private static final long serialVersionUID = 1L;
    /** 商品资产主键。 */ private String id;
    /** 前台用户 ID。 */ private Long userId;
    /** 商品名称。 */ private String name;
    /** 资产状态。 */ private String status;
    /** 是否在商品库展示。 */ private Boolean visible;
    /** 生成类型。 */ private String assetType;
    /** 目标平台。 */ private String targetPlatform;
    /** 图片比例。 */ private String aspectRatio;
    /** 图片清晰度。 */ private String quality;
    /** 用户要求。 */ private String userRequirement;
    /** 来源商品资产 ID。 */ private String sourceProductId;
    /** 封面图片 ID。 */ private String coverImageId;
    /** 最近一次任务 ID。 */ private String lastTaskId;
    /** 创建时间。 */ private Date createdAt;
    /** 更新时间。 */ private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
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
    public String getSourceProductId() { return sourceProductId; }
    public void setSourceProductId(String sourceProductId) { this.sourceProductId = sourceProductId; }
    public String getCoverImageId() { return coverImageId; }
    public void setCoverImageId(String coverImageId) { this.coverImageId = coverImageId; }
    public String getLastTaskId() { return lastTaskId; }
    public void setLastTaskId(String lastTaskId) { this.lastTaskId = lastTaskId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
