package com.egg.commerce.system.domain.ai.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 智能聊天会话实体，记录用户会话摘要、状态和最近消息时间。
 */
public class AiChatSession
{
    private String id;

    private Long userId;

    private String title;

    private String lastMessagePreview;

    private String status;

    private Integer messageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageAt;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getLastMessagePreview()
    {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview)
    {
        this.lastMessagePreview = lastMessagePreview;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getMessageCount()
    {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount)
    {
        this.messageCount = messageCount;
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

    public Date getLastMessageAt()
    {
        return lastMessageAt;
    }

    public void setLastMessageAt(Date lastMessageAt)
    {
        this.lastMessageAt = lastMessageAt;
    }
}
