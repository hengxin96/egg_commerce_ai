package com.egg.commerce.system.domain.ai.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 智能聊天消息实体，保存用户或助手内容以及 RAG 来源挂载结果。
 */
public class AiChatMessage
{
    private String id;

    private String sessionId;

    private Long userId;

    private String role;

    private String content;

    private String modelName;

    private String promptVersion;

    private Boolean ragUsed;

    private List<AiChatRetrievalSource> sources = new ArrayList<>();

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

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getPromptVersion()
    {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion)
    {
        this.promptVersion = promptVersion;
    }

    public Boolean getRagUsed()
    {
        return ragUsed;
    }

    public void setRagUsed(Boolean ragUsed)
    {
        this.ragUsed = ragUsed;
    }

    public List<AiChatRetrievalSource> getSources()
    {
        return sources;
    }

    public void setSources(List<AiChatRetrievalSource> sources)
    {
        this.sources = sources == null ? new ArrayList<>() : new ArrayList<>(sources);
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
