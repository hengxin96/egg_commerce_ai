package com.egg.commerce.system.service.ai.chat.impl;

import com.egg.commerce.system.service.ai.chat.AiChatModelService;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "egg.ai.mock-enabled", havingValue = "true")
public class AiMockChatModelServiceImpl implements AiChatModelService
{
    @Override
    public String generateAnswer(String question, List<AiVectorRecord> sources)
    {
        return "我已结合知识库资料给出建议。请检查下方来源后再用于正式商品内容。";
    }
}
