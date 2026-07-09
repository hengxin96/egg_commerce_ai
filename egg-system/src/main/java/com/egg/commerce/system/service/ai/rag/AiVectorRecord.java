package com.egg.commerce.system.service.ai.rag;

import java.util.HashMap;
import java.util.Map;

public class AiVectorRecord
{
    private String vectorId;

    private String documentId;

    private String chunkId;

    private Integer chunkIndex;

    private String sourceTitle;

    private String content;

    private Double score;

    private Map<String, Object> metadata = new HashMap<>();

    public String getVectorId()
    {
        return vectorId;
    }

    public void setVectorId(String vectorId)
    {
        this.vectorId = vectorId;
    }

    public String getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(String documentId)
    {
        this.documentId = documentId;
    }

    public String getChunkId()
    {
        return chunkId;
    }

    public void setChunkId(String chunkId)
    {
        this.chunkId = chunkId;
    }

    public Integer getChunkIndex()
    {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex)
    {
        this.chunkIndex = chunkIndex;
    }

    public String getSourceTitle()
    {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle)
    {
        this.sourceTitle = sourceTitle;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Double getScore()
    {
        return score;
    }

    public void setScore(Double score)
    {
        this.score = score;
    }

    public Map<String, Object> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata)
    {
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }
}
