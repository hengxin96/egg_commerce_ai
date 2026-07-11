package com.egg.commerce.system.domain.ai.chat;

/**
 * 智能聊天提问请求对象，仅承载前端提交的用户问题内容。
 */
public class AiChatRequest
{
    private String content;

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }
}
