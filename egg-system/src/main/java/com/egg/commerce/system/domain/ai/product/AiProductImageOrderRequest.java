package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;
import java.util.List;

/** 当前草稿源图排序请求，第一张会作为主参考图。 */
public class AiProductImageOrderRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private List<String> imageIds;

    private String primaryImageId;

    public List<String> getImageIds() { return imageIds; }
    public void setImageIds(List<String> imageIds) { this.imageIds = imageIds; }
    public String getPrimaryImageId() { return primaryImageId; }
    public void setPrimaryImageId(String primaryImageId) { this.primaryImageId = primaryImageId; }
}
