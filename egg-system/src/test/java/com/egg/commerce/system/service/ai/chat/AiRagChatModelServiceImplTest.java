package com.egg.commerce.system.service.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.service.ai.chat.impl.AiRagChatModelServiceImpl;
import com.egg.commerce.system.service.ai.prompt.IAiPromptTemplateService;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * RAG 聊天模型测试，覆盖系统提示词和知识库来源整理。
 */
class AiRagChatModelServiceImplTest
{
    /**
     * 验证聊天模型会读取 rag-chat 发布模板，并把用户问题与知识库来源交给模型客户端。
     */
    @Test
    void generateAnswerUsesPublishedRagPromptAndKnowledgeSources()
    {
        FakeChatCompletionClient completionClient = new FakeChatCompletionClient();
        FakePromptTemplateService promptTemplateService = new FakePromptTemplateService();
        AiPromptTemplate template = new AiPromptTemplate();
        template.setContent("{\"format\":\"markdown\",\"body\":\"你只能依据知识库回答。\"}");
        promptTemplateService.publishedTemplate = template;
        AiRagChatModelServiceImpl service = new AiRagChatModelServiceImpl(completionClient, promptTemplateService);
        AiVectorRecord source = new AiVectorRecord();
        source.setDocumentId("doc-1");
        source.setSourceTitle("rules.pdf");
        source.setChunkIndex(3);
        source.setScore(0.91);
        source.setContent("source content");

        String answer = service.generateAnswer("应该怎么处理？", List.of(source));

        assertEquals("模型回答", answer);
        assertTrue(completionClient.systemPrompt.contains("只能依据知识库"));
        assertTrue(completionClient.userPrompt.contains("用户问题：应该怎么处理？"));
        assertTrue(completionClient.userPrompt.contains("来源[1]"));
        assertTrue(completionClient.userPrompt.contains("rules.pdf"));
        assertTrue(completionClient.userPrompt.contains("source content"));
        assertFalse(completionClient.userPrompt.contains("- 切片序号"));
        assertFalse(completionClient.userPrompt.contains("- 相似度"));
        assertFalse(completionClient.userPrompt.contains("- 文档ID"));
    }

    /**
     * 即使本轮没有检索到知识库资料，也要把问题交给模型，由系统提示词约束模型自然说明无法依据知识库回答。
     */
    @Test
    void generateAnswerStillCallsModelWhenKnowledgeSourcesAreEmpty()
    {
        FakeChatCompletionClient completionClient = new FakeChatCompletionClient();
        FakePromptTemplateService promptTemplateService = new FakePromptTemplateService();
        AiRagChatModelServiceImpl service = new AiRagChatModelServiceImpl(completionClient, promptTemplateService);

        String answer = service.generateAnswer("hello", List.of());

        assertEquals("模型回答", answer);
        assertEquals(1, completionClient.completeCalls);
        assertTrue(completionClient.userPrompt.contains("hello"));
        assertTrue(completionClient.userPrompt.contains("没有检索到可引用的知识库资料"));
    }

    private static class FakeChatCompletionClient implements AiChatCompletionClient
    {
        private String systemPrompt;

        private String userPrompt;

        private int completeCalls;

        @Override
        public String complete(String systemPrompt, String userPrompt)
        {
            completeCalls++;
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            return "模型回答";
        }
    }

    private static class FakePromptTemplateService implements IAiPromptTemplateService
    {
        private AiPromptTemplate publishedTemplate;

        @Override
        public AiPromptTemplate selectAiPromptTemplateById(String id)
        {
            return null;
        }

        @Override
        public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate)
        {
            return List.of();
        }

        @Override
        public AiPromptTemplate selectPublishedTemplateByCategory(String category)
        {
            return publishedTemplate;
        }

        @Override
        public int insertAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            return 0;
        }

        @Override
        public int updateAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            return 0;
        }

        @Override
        public int deleteAiPromptTemplateByIds(String[] ids)
        {
            return 0;
        }
    }
}
