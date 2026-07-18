package com.egg.commerce.system.domain.ai.story;

import java.io.Serializable;

/**
 * 剧情任务导出文件对象。
 *
 * <p>控制器使用该对象统一返回文件名、内容类型和文件字节，避免导出服务直接依赖 HTTP Response。</p>
 */
public class AiStoryTaskExportFile implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 下载文件名，包含扩展名。 */
    private String fileName;

    /** HTTP Content-Type，区分 Word 和 Markdown。 */
    private String contentType;

    /** 文件二进制内容。 */
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
