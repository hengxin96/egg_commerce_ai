package com.egg.commerce.system.mapper.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import java.util.List;

public interface AiChatSessionMapper
{
    AiChatSession selectAiChatSessionById(String id);

    List<AiChatSession> selectAiChatSessionList(AiChatSession query);

    int insertAiChatSession(AiChatSession session);

    int updateAiChatSession(AiChatSession session);
}
