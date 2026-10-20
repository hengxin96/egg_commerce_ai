package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;
import java.util.Date;

/** 商品图图片资产实体。 */
public class AiProductAssetImage implements Serializable
{
    private static final long serialVersionUID = 1L;
    /** 图片主键。 */ private String id;
    /** 商品资产 ID。 */ private String productAssetId;
    /** 前台用户 ID。 */ private Long userId;
    /** 关联任务 ID。 */ private String taskId;
    /** 图片角色。 */ private String imageRole;
    /** OSS bucket。 */ private String ossBucket;
    /** OSS object key。 */ private String ossObjectKey;
    /** 图片访问地址。 */ private String ossUrl;
    /** MIME 类型。 */ private String mimeType;
    /** 文件大小。 */ private Long fileSize;
    /** 图片宽度。 */ private Integer width;
    /** 图片高度。 */ private Integer height;
    /** 排序。 */ private Integer sortOrder;
    /** AI 识别用途。 */ private String analysisRole;
    /** 创建时间。 */ private Date createdAt;
    /** 更新时间。 */ private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductAssetId() { return productAssetId; }
    public void setProductAssetId(String productAssetId) { this.productAssetId = productAssetId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getImageRole() { return imageRole; }
    public void setImageRole(String imageRole) { this.imageRole = imageRole; }
    public String getOssBucket() { return ossBucket; }
    public void setOssBucket(String ossBucket) { this.ossBucket = ossBucket; }
    public String getOssObjectKey() { return ossObjectKey; }
    public void setOssObjectKey(String ossObjectKey) { this.ossObjectKey = ossObjectKey; }
    public String getOssUrl() { return ossUrl; }
    public void setOssUrl(String ossUrl) { this.ossUrl = ossUrl; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getAnalysisRole() { return analysisRole; }
    public void setAnalysisRole(String analysisRole) { this.analysisRole = analysisRole; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
