package com.egg.commerce.system.domain.ai.knowledge;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class AiKnowledgeDocument
{
    private String id;

    private String fileName;

    private String fileExt;

    private String mimeType;

    private Long fileSize;

    private String ossBucket;

    private String ossObjectKey;

    private String ossUrl;

    private String vectorStore;

    private String vectorCollection;

    private String vectorIds;

    private Integer chunkCount;

    private String status;

    private String failureReason;

    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completedAt;

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

    public String getFileExt()
    {
        return fileExt;
    }

    public void setFileExt(String fileExt)
    {
        this.fileExt = fileExt;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getOssBucket()
    {
        return ossBucket;
    }

    public void setOssBucket(String ossBucket)
    {
        this.ossBucket = ossBucket;
    }

    public String getOssObjectKey()
    {
        return ossObjectKey;
    }

    public void setOssObjectKey(String ossObjectKey)
    {
        this.ossObjectKey = ossObjectKey;
    }

    public String getOssUrl()
    {
        return ossUrl;
    }

    public void setOssUrl(String ossUrl)
    {
        this.ossUrl = ossUrl;
    }

    public String getVectorStore()
    {
        return vectorStore;
    }

    public void setVectorStore(String vectorStore)
    {
        this.vectorStore = vectorStore;
    }

    public String getVectorCollection()
    {
        return vectorCollection;
    }

    public void setVectorCollection(String vectorCollection)
    {
        this.vectorCollection = vectorCollection;
    }

    public String getVectorIds()
    {
        return vectorIds;
    }

    public void setVectorIds(String vectorIds)
    {
        this.vectorIds = vectorIds;
    }

    public Integer getChunkCount()
    {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount)
    {
        this.chunkCount = chunkCount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getFailureReason()
    {
        return failureReason;
    }

    public void setFailureReason(String failureReason)
    {
        this.failureReason = failureReason;
    }

    public Long getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy)
    {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public Date getCompletedAt()
    {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt)
    {
        this.completedAt = completedAt;
    }
}
