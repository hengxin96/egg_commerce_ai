package com.egg.commerce.system.mapper.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatRetrievalSource;
import java.util.List;

/**
 * 聊天检索来源 Mapper，负责保存和查询回答关联的 RAG 来源。
 */
public interface AiChatRetrievalSourceMapper
{
    /** 查询指定助手消息关联的 RAG 检索来源。 */
    List<AiChatRetrievalSource> selectAiChatRetrievalSourceListByMessageId(String messageId);

    /** 保存一条回答引用的 RAG 来源记录。 */
    int insertAiChatRetrievalSource(AiChatRetrievalSource source);
}
