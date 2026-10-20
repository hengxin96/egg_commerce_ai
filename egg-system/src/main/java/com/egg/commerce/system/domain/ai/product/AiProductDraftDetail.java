package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;
import java.util.List;

/** 生成页草稿响应，聚合草稿商品和已上传源图。 */
public class AiProductDraftDetail implements Serializable
{
    private static final long serialVersionUID = 1L;
    private AiProductAsset asset;
    private List<AiProductAssetImage> sourceImages;

    public AiProductAsset getAsset() { return asset; }
    public void setAsset(AiProductAsset asset) { this.asset = asset; }
    public List<AiProductAssetImage> getSourceImages() { return sourceImages; }
    public void setSourceImages(List<AiProductAssetImage> sourceImages) { this.sourceImages = sourceImages; }
}
