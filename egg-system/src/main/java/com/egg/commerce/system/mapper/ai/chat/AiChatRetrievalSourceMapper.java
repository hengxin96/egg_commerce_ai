package com.egg.commerce.system.mapper.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatRetrievalSource;
import java.util.List;

public interface AiChatRetrievalSourceMapper
{
    List<AiChatRetrievalSource> selectAiChatRetrievalSourceListByMessageId(String messageId);

    int insertAiChatRetrievalSource(AiChatRetrievalSource source);
}
