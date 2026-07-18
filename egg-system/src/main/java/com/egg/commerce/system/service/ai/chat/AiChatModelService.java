package com.egg.commerce.system.service.ai.chat;

import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import java.util.List;
import java.util.function.Consumer;

/**
 * 聊天模型服务接口，隔离真实模型、Mock 模型和后续网关实现。
 */
public interface AiChatModelService
{
    /** 根据用户问题和 RAG 来源生成可返回给前端的助手回答。 */
    String generateAnswer(String question, List<AiVectorRecord> sources);

    /** 根据用户问题和 RAG 来源生成助手回答，并把模型增量内容推给调用方。 */
    default String generateAnswerStream(String question, List<AiVectorRecord> sources, Consumer<String> onDelta)
    {
        String answer = generateAnswer(question, sources);
        if (answer != null && !answer.isEmpty() && onDelta != null)
        {
            onDelta.accept(answer);
        }
        return answer;
    }

    /** 根据首轮用户问题和助手回答生成简短会话标题。 */
    default String generateSessionTitle(String question, String answer)
    {
        return question;
    }
}
