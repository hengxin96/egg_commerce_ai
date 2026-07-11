package com.egg.commerce.system.mapper.ai.chat;

import com.egg.commerce.system.domain.ai.chat.AiChatMessage;
import java.util.List;

/**
 * 智能聊天消息 MyBatis Mapper，封装消息落库和会话内消息查询。
 */
public interface AiChatMessageMapper
{
    /** 查询会话内的消息列表，按 XML 中的时间顺序返回。 */
    List<AiChatMessage> selectAiChatMessageListBySessionId(String sessionId);

    /** 保存一条用户或助手聊天消息。 */
    int insertAiChatMessage(AiChatMessage message);
}
