package com.egg.commerce.system.service.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.chat.AiChatMessage;
import com.egg.commerce.system.domain.ai.chat.AiChatResponse;
import com.egg.commerce.system.domain.ai.chat.AiChatRetrievalSource;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import com.egg.commerce.system.mapper.ai.chat.AiChatMessageMapper;
import com.egg.commerce.system.mapper.ai.chat.AiChatRetrievalSourceMapper;
import com.egg.commerce.system.mapper.ai.chat.AiChatSessionMapper;
import com.egg.commerce.system.service.ISysRoleService;
import com.egg.commerce.system.service.ai.chat.impl.AiChatServiceImpl;
import com.egg.commerce.system.service.ai.knowledge.AiKnowledgeDocumentService;
import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import com.egg.commerce.system.service.ai.sensitive.AiSensitiveWordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 智能聊天服务测试，覆盖 RAG 回答落库、来源持久化、会话归属校验和权限过滤。
 */
class AiChatServiceImplTest
{
    private static final String SESSION_ID = "11111111-1111-1111-1111-111111111111";

    private static final String DOCUMENT_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    void sendMessageStoresUserAssistantAndSourcesWithMetadata()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeChatModel chatModel = new FakeChatModel("建议突出平台规则。");
        AiChatServiceImpl service = service(sessionMapper, messageMapper, sourceMapper, vectorStore, chatModel);

        AiChatResponse response = service.sendMessage(SESSION_ID, 100L, "主图怎么做？");

        assertEquals(SESSION_ID, response.getSessionId());
        assertEquals("建议突出平台规则。", response.getAnswer());
        assertEquals(2, messageMapper.inserted.size());
        assertEquals("user", messageMapper.inserted.get(0).getRole());
        assertEquals("assistant", messageMapper.inserted.get(1).getRole());
        assertTrue(Boolean.TRUE.equals(messageMapper.inserted.get(1).getRagUsed()));
        assertEquals(1, sourceMapper.inserted.size());
        assertEquals(messageMapper.inserted.get(1).getId(), sourceMapper.inserted.get(0).getMessageId());
        assertEquals(DOCUMENT_ID, sourceMapper.inserted.get(0).getDocumentId());
        assertEquals("{\"channel\":\"marketplace\",\"ruleCode\":\"MAIN_IMAGE\"}",
                sourceMapper.inserted.get(0).getMetadata());
        assertEquals(2, sessionMapper.sessions.get(SESSION_ID).getMessageCount());
    }

    @Test
    void sendMessageGeneratesSessionTitleAfterFirstReply()
    {
        AiChatSession session = ownedSession();
        session.setTitle("新建对话");
        FakeChatSessionMapper sessionMapper = sessionMapperWith(session);
        FakeChatModel chatModel = new FakeChatModel("建议突出平台规则。");
        chatModel.title = "主图规则建议";
        AiChatServiceImpl service = service(sessionMapper, new FakeChatMessageMapper(), new FakeSourceMapper(),
                new FakeVectorStore(), chatModel);

        AiChatResponse response = service.sendMessage(SESSION_ID, 100L, "主图怎么做？");

        assertEquals("主图规则建议", sessionMapper.sessions.get(SESSION_ID).getTitle());
        assertEquals("主图规则建议", response.getTitle());
        assertEquals("主图怎么做？", chatModel.titleQuestion);
        assertEquals("建议突出平台规则。", chatModel.titleAnswer);
    }

    @Test
    void sendMessageFallsBackToFirstQuestionWhenTitleGenerationFails()
    {
        AiChatSession session = ownedSession();
        session.setTitle("新建对话");
        FakeChatSessionMapper sessionMapper = sessionMapperWith(session);
        FakeChatModel chatModel = new FakeChatModel("建议突出平台规则。");
        chatModel.throwTitle = true;
        AiChatServiceImpl service = service(sessionMapper, new FakeChatMessageMapper(), new FakeSourceMapper(),
                new FakeVectorStore(), chatModel);

        AiChatResponse response = service.sendMessage(SESSION_ID, 100L, "这个商品主图应该怎么做才能更合规？");

        assertEquals("这个商品主图应该怎么做才能更合规？", sessionMapper.sessions.get(SESSION_ID).getTitle());
        assertEquals("这个商品主图应该怎么做才能更合规？", response.getTitle());
    }

    @Test
    void streamMessageDoesNotWaitIndefinitelyForSessionTitleGeneration()
    {
        AiChatSession session = ownedSession();
        session.setTitle("新建对话");
        FakeChatSessionMapper sessionMapper = sessionMapperWith(session);
        SlowTitleStreamingChatModel chatModel = new SlowTitleStreamingChatModel("建议", "优化主图。");
        AiChatServiceImpl service = service(sessionMapper, new FakeChatMessageMapper(), new FakeSourceMapper(),
                new FakeVectorStore(), chatModel);

        assertTimeoutPreemptively(Duration.ofSeconds(3),
                () -> service.streamMessage(SESSION_ID, 100L, "主图怎么做？", chunk -> { }));
    }

    @Test
    void deleteSessionPhysicallyDeletesSourcesMessagesAndSession()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiChatServiceImpl service = service(sessionMapper, messageMapper, sourceMapper, new FakeVectorStore(),
                new FakeChatModel("unused"));

        service.deleteSession(SESSION_ID, 100L);

        assertEquals(List.of(SESSION_ID), sourceMapper.deletedSessionIds);
        assertEquals(List.of(SESSION_ID), messageMapper.deletedSessionIds);
        assertEquals(List.of(SESSION_ID), sessionMapper.deletedSessionIds);
        assertTrue(sessionMapper.sessions.isEmpty());
    }

    @Test
    void streamMessageEmitsDeltasAndStoresFinalAssistantReply()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        FakeStreamingChatModel chatModel = new FakeStreamingChatModel("建议", "优化主图。");
        AiChatServiceImpl service = service(sessionMapper, messageMapper, sourceMapper, new FakeVectorStore(),
                chatModel);
        List<String> deltas = new ArrayList<>();

        AiChatResponse response = service.streamMessage(SESSION_ID, 100L, "主图怎么做？", deltas::add);

        assertEquals(List.of("建议", "优化主图。"), deltas);
        assertEquals("建议优化主图。", response.getAnswer());
        assertEquals("建议优化主图。", messageMapper.inserted.get(1).getContent());
        assertEquals(1, response.getSources().size());
    }

    @Test
    void sendMessageRejectsMissingSessionBeforeSaving()
    {
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiChatServiceImpl service = service(new FakeChatSessionMapper(), messageMapper, sourceMapper,
                new FakeVectorStore(), new FakeChatModel("unused"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.sendMessage("missing-session", 100L, "主图怎么做？"));

        assertEquals("Chat session does not exist", exception.getMessage());
        assertTrue(messageMapper.inserted.isEmpty());
        assertTrue(sourceMapper.inserted.isEmpty());
    }

    @Test
    void sendMessageRejectsSensitiveContentBeforeRagAndModel()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiVectorStoreService vectorStore = mock(AiVectorStoreService.class);
        AiChatModelService chatModel = mock(AiChatModelService.class);
        AiSensitiveWordService sensitiveWordService = mock(AiSensitiveWordService.class);
        AiKnowledgeDocumentService knowledgeDocumentService = mock(AiKnowledgeDocumentService.class);
        ISysRoleService roleService = mock(ISysRoleService.class);
        when(sensitiveWordService.containsEnabledSensitiveWord("敏感问题")).thenReturn(true);
        AiChatServiceImpl service = new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper, vectorStore,
                chatModel, sensitiveWordService, knowledgeDocumentService, roleService, new ObjectMapper(), 6, 0.62D);

        AiChatResponse response = service.sendMessage(SESSION_ID, 100L, "敏感问题");

        assertEquals("您的问题包含敏感词，无法回答。", response.getAnswer());
        assertEquals(0, response.getSources().size());
        verify(vectorStore, never()).search(anyString(), anyInt(), anyDouble(), anyList());
        verify(chatModel, never()).generateAnswer(anyString(), anyList());
    }

    @Test
    void sendMessageRoutesGreetingThroughRagAndModel()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiVectorStoreService vectorStore = mock(AiVectorStoreService.class);
        AiChatModelService chatModel = mock(AiChatModelService.class);
        when(vectorStore.search(eq("hello"), anyInt(), anyDouble())).thenReturn(List.of(vectorRecord()));
        when(chatModel.generateAnswer(eq("hello"), anyList())).thenReturn("模型友好回答");
        AiChatServiceImpl service = service(sessionMapper, messageMapper, sourceMapper, vectorStore, chatModel);

        AiChatResponse response = service.sendMessage(SESSION_ID, 100L, "hello");

        assertEquals("模型友好回答", response.getAnswer());
        assertEquals(1, response.getSources().size());
        verify(vectorStore).search("hello", 6, 0.62D);
        verify(chatModel).generateAnswer(eq("hello"), anyList());
    }

    @Test
    void sendMessageCallsModelWhenUserHasNoAccessibleDocuments()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiVectorStoreService vectorStore = mock(AiVectorStoreService.class);
        AiChatModelService chatModel = mock(AiChatModelService.class);
        AiSensitiveWordService sensitiveWordService = mock(AiSensitiveWordService.class);
        AiKnowledgeDocumentService knowledgeDocumentService = mock(AiKnowledgeDocumentService.class);
        ISysRoleService roleService = mock(ISysRoleService.class);
        Set<String> roleKeys = Set.of("common");
        when(roleService.selectRolePermissionByUserId(100L)).thenReturn(roleKeys);
        when(knowledgeDocumentService.selectAccessibleCompletedDocumentIds(roleKeys)).thenReturn(List.of());
        when(chatModel.generateAnswer(eq("hello"), eq(List.of()))).thenReturn("模型说明暂无资料");
        AiChatServiceImpl service = new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper, vectorStore,
                chatModel, sensitiveWordService, knowledgeDocumentService, roleService, new ObjectMapper(), 6, 0.62D);

        AiChatResponse response = service.sendMessage(SESSION_ID, 100L, "hello");

        assertEquals("模型说明暂无资料", response.getAnswer());
        assertEquals(0, response.getSources().size());
        verify(vectorStore, never()).search(anyString(), anyInt(), anyDouble(), anyList());
        verify(chatModel).generateAnswer("hello", List.of());
    }

    @Test
    void sendMessageSearchesOnlyAccessibleDocumentsForUserRoles()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiVectorStoreService vectorStore = mock(AiVectorStoreService.class);
        AiChatModelService chatModel = mock(AiChatModelService.class);
        AiSensitiveWordService sensitiveWordService = mock(AiSensitiveWordService.class);
        AiKnowledgeDocumentService knowledgeDocumentService = mock(AiKnowledgeDocumentService.class);
        ISysRoleService roleService = mock(ISysRoleService.class);
        Set<String> roleKeys = Set.of("common");
        List<String> accessibleDocumentIds = List.of(DOCUMENT_ID);
        when(roleService.selectRolePermissionByUserId(100L)).thenReturn(roleKeys);
        when(knowledgeDocumentService.selectAccessibleCompletedDocumentIds(roleKeys)).thenReturn(accessibleDocumentIds);
        when(vectorStore.search(eq("主图怎么做？"), anyInt(), anyDouble(), eq(accessibleDocumentIds)))
                .thenReturn(List.of(vectorRecord()));
        when(chatModel.generateAnswer(eq("主图怎么做？"), anyList())).thenReturn("建议突出平台规则。");
        AiChatServiceImpl service = new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper, vectorStore,
                chatModel, sensitiveWordService, knowledgeDocumentService, roleService, new ObjectMapper(), 6, 0.62D);

        AiChatResponse response = service.sendMessage(SESSION_ID, 100L, "主图怎么做？");

        assertEquals("建议突出平台规则。", response.getAnswer());
        assertEquals(1, response.getSources().size());
        verify(vectorStore).search("主图怎么做？", 6, 0.62D, accessibleDocumentIds);
    }

    @Test
    void selectMessagesAttachesPersistedSourcesForAssistantMessages()
    {
        FakeChatSessionMapper sessionMapper = sessionMapperWith(ownedSession());
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        AiChatMessage userMessage = message("33333333-3333-3333-3333-333333333333", "user", "question", false);
        AiChatMessage assistantMessage = message("44444444-4444-4444-4444-444444444444", "assistant", "answer", true);
        messageMapper.messagesBySession.add(userMessage);
        messageMapper.messagesBySession.add(assistantMessage);
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiChatRetrievalSource source = new AiChatRetrievalSource();
        source.setId("55555555-5555-5555-5555-555555555555");
        source.setMessageId(assistantMessage.getId());
        source.setDocumentId(DOCUMENT_ID);
        source.setMetadata("{\"channel\":\"marketplace\"}");
        sourceMapper.sourcesByMessageId.put(assistantMessage.getId(), List.of(source));
        AiChatServiceImpl service = service(sessionMapper, messageMapper, sourceMapper, new FakeVectorStore(),
                new FakeChatModel("unused"));

        List<AiChatMessage> messages = service.selectMessages(SESSION_ID, 100L);

        assertEquals(2, messages.size());
        assertEquals(0, messages.get(0).getSources().size());
        assertEquals(1, messages.get(1).getSources().size());
        assertEquals("{\"channel\":\"marketplace\"}", messages.get(1).getSources().get(0).getMetadata());
    }

    private AiChatServiceImpl service(FakeChatSessionMapper sessionMapper, FakeChatMessageMapper messageMapper,
            FakeSourceMapper sourceMapper, AiVectorStoreService vectorStore, AiChatModelService chatModel)
    {
        return new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper, vectorStore, chatModel,
                new ObjectMapper());
    }

    private FakeChatSessionMapper sessionMapperWith(AiChatSession session)
    {
        FakeChatSessionMapper mapper = new FakeChatSessionMapper();
        mapper.sessions.put(session.getId(), session);
        return mapper;
    }

    private AiChatSession ownedSession()
    {
        AiChatSession session = new AiChatSession();
        session.setId(SESSION_ID);
        session.setUserId(100L);
        session.setTitle("Chat");
        session.setMessageCount(0);
        return session;
    }

    private AiChatMessage message(String id, String role, String content, boolean ragUsed)
    {
        AiChatMessage message = new AiChatMessage();
        message.setId(id);
        message.setSessionId(SESSION_ID);
        message.setUserId(100L);
        message.setRole(role);
        message.setContent(content);
        message.setRagUsed(ragUsed);
        message.setCreatedAt(new Date());
        return message;
    }

    private static AiVectorRecord vectorRecord()
    {
        AiVectorRecord record = new AiVectorRecord();
        record.setDocumentId(DOCUMENT_ID);
        record.setChunkIndex(0);
        record.setSourceTitle("平台规则");
        record.setContent("主图需要突出主体并避免违规承诺。");
        record.setScore(0.91D);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("channel", "marketplace");
        metadata.put("ruleCode", "MAIN_IMAGE");
        record.setMetadata(metadata);
        return record;
    }

    private static class FakeChatSessionMapper implements AiChatSessionMapper
    {
        private final Map<String, AiChatSession> sessions = new LinkedHashMap<>();

        private final List<String> deletedSessionIds = new ArrayList<>();

        @Override
        public AiChatSession selectAiChatSessionById(String id)
        {
            return sessions.get(id);
        }

        @Override
        public List<AiChatSession> selectAiChatSessionList(AiChatSession query)
        {
            List<AiChatSession> list = new ArrayList<>();
            for (AiChatSession session : sessions.values())
            {
                if (query == null || query.getUserId() == null || query.getUserId().equals(session.getUserId()))
                {
                    list.add(session);
                }
            }
            return list;
        }

        @Override
        public int insertAiChatSession(AiChatSession session)
        {
            sessions.put(session.getId(), session);
            return 1;
        }

        @Override
        public int updateAiChatSession(AiChatSession session)
        {
            sessions.put(session.getId(), session);
            return 1;
        }

        @Override
        public int deleteAiChatSessionById(String id)
        {
            deletedSessionIds.add(id);
            sessions.remove(id);
            return 1;
        }
    }

    private static class FakeChatMessageMapper implements AiChatMessageMapper
    {
        private final List<AiChatMessage> inserted = new ArrayList<>();

        private final List<AiChatMessage> messagesBySession = new ArrayList<>();

        private final List<String> deletedSessionIds = new ArrayList<>();

        @Override
        public List<AiChatMessage> selectAiChatMessageListBySessionId(String sessionId)
        {
            return messagesBySession;
        }

        @Override
        public int insertAiChatMessage(AiChatMessage message)
        {
            inserted.add(message);
            return 1;
        }

        @Override
        public int deleteAiChatMessagesBySessionId(String sessionId)
        {
            deletedSessionIds.add(sessionId);
            return 1;
        }
    }

    private static class FakeSourceMapper implements AiChatRetrievalSourceMapper
    {
        private final List<AiChatRetrievalSource> inserted = new ArrayList<>();

        private final Map<String, List<AiChatRetrievalSource>> sourcesByMessageId = new LinkedHashMap<>();

        private final List<String> deletedSessionIds = new ArrayList<>();

        @Override
        public List<AiChatRetrievalSource> selectAiChatRetrievalSourceListByMessageId(String messageId)
        {
            return sourcesByMessageId.getOrDefault(messageId, Collections.emptyList());
        }

        @Override
        public int insertAiChatRetrievalSource(AiChatRetrievalSource source)
        {
            inserted.add(source);
            return 1;
        }

        @Override
        public int deleteAiChatRetrievalSourcesBySessionId(String sessionId)
        {
            deletedSessionIds.add(sessionId);
            return 1;
        }
    }

    private static class FakeVectorStore implements AiVectorStoreService
    {
        @Override
        public List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks)
        {
            return Collections.emptyList();
        }

        @Override
        public List<AiVectorRecord> search(String query, int topK, double minScore)
        {
            return List.of(vectorRecord());
        }

        @Override
        public void deleteVectors(List<String> vectorIds)
        {
        }
    }

    private static class FakeChatModel implements AiChatModelService
    {
        private final String answer;

        private String title;

        private String titleQuestion;

        private String titleAnswer;

        private boolean throwTitle;

        private FakeChatModel(String answer)
        {
            this.answer = answer;
        }

        @Override
        public String generateAnswer(String question, List<AiVectorRecord> sources)
        {
            return answer;
        }

        @Override
        public String generateSessionTitle(String question, String answer)
        {
            titleQuestion = question;
            titleAnswer = answer;
            if (throwTitle)
            {
                throw new ServiceException("title failed");
            }
            return title;
        }
    }

    private static class FakeStreamingChatModel implements AiChatModelService
    {
        private final List<String> chunks;

        private FakeStreamingChatModel(String... chunks)
        {
            this.chunks = List.of(chunks);
        }

        @Override
        public String generateAnswer(String question, List<AiVectorRecord> sources)
        {
            return String.join("", chunks);
        }

        @Override
        public String generateAnswerStream(String question, List<AiVectorRecord> sources,
                java.util.function.Consumer<String> onDelta)
        {
            chunks.forEach(onDelta);
            return String.join("", chunks);
        }
    }

    private static class SlowTitleStreamingChatModel extends FakeStreamingChatModel
    {
        private SlowTitleStreamingChatModel(String... chunks)
        {
            super(chunks);
        }

        @Override
        public String generateSessionTitle(String question, String answer)
        {
            try
            {
                Thread.sleep(5_000L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            return "慢标题";
        }
    }
}
