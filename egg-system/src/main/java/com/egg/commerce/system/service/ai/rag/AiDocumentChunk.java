package com.egg.commerce.system.service.ai.rag;

/**
 * 知识文档切片对象，承载向量化前的文本片段和顺序。
 */
public class AiDocumentChunk
{
    private String chunkId;

    private String content;

    private Integer chunkIndex;

    public String getChunkId()
    {
        return chunkId;
    }

    public void setChunkId(String chunkId)
    {
        this.chunkId = chunkId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getChunkIndex()
    {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex)
    {
        this.chunkIndex = chunkIndex;
    }
}
