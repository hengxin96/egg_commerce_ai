package com.egg.commerce.system.mapper.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import java.util.List;

/**
 * 智能聊天会话 Mapper，负责会话列表、创建和摘要更新。
 */
public interface AiChatSessionMapper
{
    /** 按会话 ID 查询聊天会话，用于归属校验和详情读取。 */
    AiChatSession selectAiChatSessionById(String id);

    /** 按查询条件返回聊天会话列表。 */
    List<AiChatSession> selectAiChatSessionList(AiChatSession query);

    /** 新建聊天会话记录。 */
    int insertAiChatSession(AiChatSession session);

    /** 更新会话摘要、消息数和最近消息时间。 */
    int updateAiChatSession(AiChatSession session);
}
