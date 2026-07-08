package com.egg.commerce.system.domain.ai.chat;

import java.util.List;

public class AiChatResponse
{
    private String sessionId;

    private String messageId;

    private String answer;

    private List<AiChatRetrievalSource> sources;

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public List<AiChatRetrievalSource> getSources()
    {
        return sources;
    }

    public void setSources(List<AiChatRetrievalSource> sources)
    {
        this.sources = sources;
    }
}
