package com.egg.commerce.system.service.ai.chat;

import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import java.util.List;

public interface AiChatModelService
{
    String generateAnswer(String question, List<AiVectorRecord> sources);
}
