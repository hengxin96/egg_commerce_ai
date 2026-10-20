package com.egg.commerce.system.service.ai.product;

import com.alibaba.fastjson2.JSONObject;

/** 后端文字合成结果，包含最终图片字节和排版元数据。 */
public class AiProductImageCompositionResult
{
    private byte[] imageBytes;

    private String extension;

    private boolean applied;

    private JSONObject metadata;

    public byte[] getImageBytes() { return imageBytes; }
    public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
    public JSONObject getMetadata() { return metadata; }
    public void setMetadata(JSONObject metadata) { this.metadata = metadata; }
}
