package com.egg.commerce.system.service.ai.chat.impl;

import com.egg.commerce.system.service.ai.chat.AiChatModelService;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 简单聊天模型实现，基于第一条 RAG 来源拼装可追溯的临时回答。
 */
@Service
@ConditionalOnProperty(name = "egg.ai.chat.simple-enabled", havingValue = "true")
public class AiSimpleChatModelServiceImpl implements AiChatModelService
{
    /**
     * 用第一条检索来源拼装临时回答，真实模型接入后可替换该实现。
     */
    @Override
    public String generateAnswer(String question, List<AiVectorRecord> sources)
    {
        if (sources == null || sources.isEmpty())
        {
            return "未检索到直接知识来源，请结合商品实际信息谨慎处理。";
        }
        AiVectorRecord firstSource = sources.get(0);
        String title = StringUtils.defaultIfBlank(firstSource.getSourceTitle(), "知识库来源");
        String snippet = StringUtils.abbreviate(StringUtils.defaultString(firstSource.getContent()), 120);
        return "根据" + title + "，建议优先处理这条信息：" + snippet;
    }
}
