package com.egg.commerce.system.service.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatMessage;
import com.egg.commerce.system.domain.ai.chat.AiChatResponse;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import java.util.List;

public interface AiChatService
{
    List<AiChatSession> selectSessionList(Long userId);

    AiChatSession createSession(Long userId, String title);

    List<AiChatMessage> selectMessages(String sessionId, Long userId);

    AiChatResponse sendMessage(String sessionId, Long userId, String content);
}
