package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;
import java.util.List;

/** Selected generated image IDs for batch download. */
public class AiProductImageDownloadRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private List<String> imageIds;

    public List<String> getImageIds()
    {
        return imageIds;
    }

    public void setImageIds(List<String> imageIds)
    {
        this.imageIds = imageIds;
    }
}
