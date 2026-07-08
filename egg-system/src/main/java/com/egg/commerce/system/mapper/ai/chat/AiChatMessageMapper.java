package com.egg.commerce.system.mapper.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatMessage;
import java.util.List;

public interface AiChatMessageMapper
{
    List<AiChatMessage> selectAiChatMessageListBySessionId(String sessionId);

    int insertAiChatMessage(AiChatMessage message);
}
