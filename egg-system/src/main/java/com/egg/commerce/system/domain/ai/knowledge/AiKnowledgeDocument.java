package com.egg.commerce.system.domain.ai.knowledge;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * AI 知识库文档实体。
 *
 * <p>一条记录代表后台上传到 RAG 知识库的一份源文件，同时保存对象存储位置、向量库写入结果、
 * 切片数量和处理状态，便于管理员追踪知识文件是否已经可用于智能问答检索。</p>
 */
public class AiKnowledgeDocument
{
    /** 文档主键，使用 UUID 字符串，便于同时作为向量 metadata 中的 documentId。 */
    private String id;

    /** 上传时的原始文件名，用于后台展示和解析器识别文件类型。 */
    private String fileName;

    /** 文件扩展名，例如 pdf、docx、txt。 */
    private String fileExt;

    /** 上传文件的 MIME 类型，用于排查解析器兼容性问题。 */
    private String mimeType;

    /** 文件大小，单位字节，用于后台展示和上传限制排查。 */
    private Long fileSize;

    /** 对象存储 bucket 名称。 */
    private String ossBucket;

    /** 对象存储 object key，删除文档时按此字段清理源文件。 */
    private String ossObjectKey;

    /** 对象存储访问地址，重试向量化时会根据该地址重新下载源文件。 */
    private String ossUrl;

    /** 当前文档写入的向量存储类型或名称，例如 local、milvus。 */
    private String vectorStore;

    /** 向量库集合/collection 名称，便于后续多集合隔离时追踪数据位置。 */
    private String vectorCollection;

    /** 向量库返回的向量 ID JSON 数组，删除文档时用于精准清理 RAG 数据。 */
    private String vectorIds;

    /** 可访问该文档的角色 key，逗号分隔；上传默认仅 admin，可批量扩展到 common。 */
    private String accessRoles;

    /** 文档解析后生成的文本切片数量。 */
    private Integer chunkCount;

    /** 向量化状态，例如 processing、completed、failed。 */
    private String status;

    /** 向量化失败时面向后台展示的简短原因。 */
    private String failureReason;

    /** 创建该知识文档的后台管理员用户 ID。 */
    private Long createdBy;

    /** 文档元数据创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 文档记录最近更新时间，状态回写和重试都会刷新。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 向量化完成时间，仅成功完成后写入。 */
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

    public String getAccessRoles()
    {
        return accessRoles;
    }

    public void setAccessRoles(String accessRoles)
    {
        this.accessRoles = accessRoles;
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
