package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;
import java.util.List;

/** 商品库详情响应，聚合商品、源图和生成图。 */
public class AiProductAssetDetail implements Serializable
{
    private static final long serialVersionUID = 1L;
    private AiProductAsset asset;
    private List<AiProductAssetImage> sourceImages;
    private List<AiProductAssetImage> generatedImages;

    public AiProductAsset getAsset() { return asset; }
    public void setAsset(AiProductAsset asset) { this.asset = asset; }
    public List<AiProductAssetImage> getSourceImages() { return sourceImages; }
    public void setSourceImages(List<AiProductAssetImage> sourceImages) { this.sourceImages = sourceImages; }
    public List<AiProductAssetImage> getGeneratedImages() { return generatedImages; }
    public void setGeneratedImages(List<AiProductAssetImage> generatedImages) { this.generatedImages = generatedImages; }
}
