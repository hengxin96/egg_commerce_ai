package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** Ownership-checked generated product image ready for an HTTP download response. */
public class AiProductAssetDownload implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String fileName;

    private String contentType;

    private byte[] bytes;

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public byte[] getBytes()
    {
        return bytes;
    }

    public void setBytes(byte[] bytes)
    {
        this.bytes = bytes;
    }
}
