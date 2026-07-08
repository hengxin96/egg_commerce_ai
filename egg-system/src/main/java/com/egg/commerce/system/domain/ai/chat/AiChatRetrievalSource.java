package com.egg.commerce.system.domain.ai.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

public class AiChatRetrievalSource
{
    private String id;

    private String messageId;

    private String documentId;

    private Integer sourceRank;

    private Integer chunkIndex;

    private String sourceTitle;

    private String snippet;

    private BigDecimal score;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    public String getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(String documentId)
    {
        this.documentId = documentId;
    }

    public Integer getSourceRank()
    {
        return sourceRank;
    }

    public void setSourceRank(Integer sourceRank)
    {
        this.sourceRank = sourceRank;
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

    public String getSnippet()
    {
        return snippet;
    }

    public void setSnippet(String snippet)
    {
        this.snippet = snippet;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }
}
