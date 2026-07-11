package com.egg.commerce.system.service.ai.chat;

import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import java.util.List;

/**
 * 聊天模型服务接口，隔离真实模型、Mock 模型和后续网关实现。
 */
public interface AiChatModelService
{
    /** 根据用户问题和 RAG 来源生成可返回给前端的助手回答。 */
    String generateAnswer(String question, List<AiVectorRecord> sources);
}
