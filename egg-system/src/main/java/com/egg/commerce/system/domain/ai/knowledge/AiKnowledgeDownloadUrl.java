package com.egg.commerce.system.domain.ai.knowledge;

/**
 * 暴露给后台知识库页面的下载地址视图。
 *
 * <p>服务只返回下载所需字段，避免向前端暴露完整文档记录。</p>
 */
public class AiKnowledgeDownloadUrl
{
    private String id;

    private String fileName;

    private String url;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}
