package com.egg.commerce.system.service.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatMessage;
import com.egg.commerce.system.domain.ai.chat.AiChatResponse;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import java.util.List;

/**
 * 智能聊天业务服务，负责会话归属校验、消息保存和 RAG 回答流程。
 */
public interface AiChatService
{
    /** 查询用户自己的智能聊天会话列表。 */
    List<AiChatSession> selectSessionList(Long userId);

    /** 创建用户智能聊天会话，并补齐默认标题和初始状态。 */
    AiChatSession createSession(Long userId, String title);

    /** 查询会话消息历史，同时校验会话归属。 */
    List<AiChatMessage> selectMessages(String sessionId, Long userId);

    /** 保存用户问题、执行 RAG 检索并返回助手回答。 */
    AiChatResponse sendMessage(String sessionId, Long userId, String content);
}
