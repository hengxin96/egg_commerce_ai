package com.egg.commerce.system.service.ai.chat;

import java.util.function.Consumer;

/**
 * 聊天补全客户端，隔离 DashScope 等模型供应商 HTTP 细节。
 */
public interface AiChatCompletionClient
{
    /** 使用系统提示词和用户提示词生成助手回答。 */
    String complete(String systemPrompt, String userPrompt);

    /** 使用系统提示词和用户提示词生成助手回答，并把模型增量内容推给调用方。 */
    default String completeStream(String systemPrompt, String userPrompt, Consumer<String> onDelta)
    {
        String answer = complete(systemPrompt, userPrompt);
        if (answer != null && !answer.isEmpty() && onDelta != null)
        {
            onDelta.accept(answer);
        }
        return answer;
    }
}
