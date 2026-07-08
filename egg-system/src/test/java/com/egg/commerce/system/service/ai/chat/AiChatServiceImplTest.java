package com.egg.commerce.system.service.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.chat.AiChatMessage;
import com.egg.commerce.system.domain.ai.chat.AiChatResponse;
import com.egg.commerce.system.domain.ai.chat.AiChatRetrievalSource;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import com.egg.commerce.system.mapper.ai.chat.AiChatMessageMapper;
import com.egg.commerce.system.mapper.ai.chat.AiChatRetrievalSourceMapper;
import com.egg.commerce.system.mapper.ai.chat.AiChatSessionMapper;
import com.egg.commerce.system.service.ai.chat.impl.AiChatServiceImpl;
import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiChatServiceImplTest
{
    private static final String SESSION_ID = "11111111-1111-1111-1111-111111111111";

    private static final String DOCUMENT_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    void sendMessageStoresUserAssistantAndSourcesWithMetadata()
    {
        FakeChatSessionMapper sessionMapper = new FakeChatSessionMapper();
        AiChatSession session = new AiChatSession();
        session.setId(SESSION_ID);
        session.setUserId(100L);
        session.setTitle("Chat");
        session.setMessageCount(0);
        sessionMapper.sessions.put(session.getId(), session);
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeChatModel chatModel = new FakeChatModel("建议突出平台规则。");
        AiChatServiceImpl service = new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper, vectorStore,
                chatModel, new ObjectMapper());

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
    void sendMessageRejectsMissingSessionBeforeSaving()
    {
        FakeChatSessionMapper sessionMapper = new FakeChatSessionMapper();
        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeChatModel chatModel = new FakeChatModel("unused");
        AiChatServiceImpl service = new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper, vectorStore,
                chatModel, new ObjectMapper());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.sendMessage("missing-session", 100L, "主图怎么做？"));

        assertEquals("Chat session does not exist", exception.getMessage());
        assertTrue(messageMapper.inserted.isEmpty());
        assertTrue(sourceMapper.inserted.isEmpty());
    }

    @Test
    void selectMessagesAttachesPersistedSourcesForAssistantMessages()
    {
        FakeChatSessionMapper sessionMapper = new FakeChatSessionMapper();
        AiChatSession session = new AiChatSession();
        session.setId(SESSION_ID);
        session.setUserId(100L);
        sessionMapper.sessions.put(session.getId(), session);

        FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setId("33333333-3333-3333-3333-333333333333");
        userMessage.setSessionId(SESSION_ID);
        userMessage.setUserId(100L);
        userMessage.setRole("user");
        userMessage.setContent("question");
        userMessage.setRagUsed(Boolean.FALSE);
        userMessage.setCreatedAt(new Date());
        messageMapper.messagesBySession.add(userMessage);

        AiChatMessage assistantMessage = new AiChatMessage();
        assistantMessage.setId("44444444-4444-4444-4444-444444444444");
        assistantMessage.setSessionId(SESSION_ID);
        assistantMessage.setUserId(100L);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent("answer");
        assistantMessage.setRagUsed(Boolean.TRUE);
        assistantMessage.setCreatedAt(new Date());
        messageMapper.messagesBySession.add(assistantMessage);

        FakeSourceMapper sourceMapper = new FakeSourceMapper();
        AiChatRetrievalSource source = new AiChatRetrievalSource();
        source.setId("55555555-5555-5555-5555-555555555555");
        source.setMessageId(assistantMessage.getId());
        source.setDocumentId(DOCUMENT_ID);
        source.setMetadata("{\"channel\":\"marketplace\"}");
        sourceMapper.sourcesByMessageId.put(assistantMessage.getId(), List.of(source));

        AiChatServiceImpl service = new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper,
                new FakeVectorStore(), new FakeChatModel("unused"), new ObjectMapper());

        List<AiChatMessage> messages = service.selectMessages(SESSION_ID, 100L);

        assertEquals(2, messages.size());
        assertEquals(0, messages.get(0).getSources().size());
        assertEquals(1, messages.get(1).getSources().size());
        assertEquals("{\"channel\":\"marketplace\"}", messages.get(1).getSources().get(0).getMetadata());
    }

    private static class FakeChatSessionMapper implements AiChatSessionMapper
    {
        private final Map<String, AiChatSession> sessions = new LinkedHashMap<>();

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
    }

    private static class FakeChatMessageMapper implements AiChatMessageMapper
    {
        private final List<AiChatMessage> inserted = new ArrayList<>();

        private final List<AiChatMessage> messagesBySession = new ArrayList<>();

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
    }

    private static class FakeSourceMapper implements AiChatRetrievalSourceMapper
    {
        private final List<AiChatRetrievalSource> inserted = new ArrayList<>();

        private final Map<String, List<AiChatRetrievalSource>> sourcesByMessageId = new LinkedHashMap<>();

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
            return List.of(record);
        }

        @Override
        public void deleteVectors(List<String> vectorIds)
        {
        }
    }

    private static class FakeChatModel implements AiChatModelService
    {
        private final String answer;

        private FakeChatModel(String answer)
        {
            this.answer = answer;
        }

        @Override
        public String generateAnswer(String question, List<AiVectorRecord> sources)
        {
            return answer;
        }
    }
}
