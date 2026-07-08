package com.egg.commerce.system.service.ai.chat.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.chat.AiChatMessage;
import com.egg.commerce.system.domain.ai.chat.AiChatResponse;
import com.egg.commerce.system.domain.ai.chat.AiChatRetrievalSource;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import com.egg.commerce.system.mapper.ai.chat.AiChatMessageMapper;
import com.egg.commerce.system.mapper.ai.chat.AiChatRetrievalSourceMapper;
import com.egg.commerce.system.mapper.ai.chat.AiChatSessionMapper;
import com.egg.commerce.system.service.ai.chat.AiChatModelService;
import com.egg.commerce.system.service.ai.chat.IAiChatService;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatServiceImpl implements IAiChatService
{
    private static final String STATUS_ACTIVE = "active";

    private static final String ROLE_USER = "user";

    private static final String ROLE_ASSISTANT = "assistant";

    private final AiChatSessionMapper chatSessionMapper;

    private final AiChatMessageMapper chatMessageMapper;

    private final AiChatRetrievalSourceMapper retrievalSourceMapper;

    private final AiVectorStoreService vectorStoreService;

    private final AiChatModelService chatModelService;

    private final ObjectMapper objectMapper;

    private final int topK;

    private final double minScore;

    @Autowired
    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            ObjectMapper objectMapper,
            Environment environment)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                objectMapper,
                environment.getProperty("egg.ai.rag.top-k", Integer.class, 6),
                environment.getProperty("egg.ai.rag.min-score", Double.class, 0.62D));
    }

    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            Environment environment)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                new ObjectMapper(), environment);
    }

    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            ObjectMapper objectMapper)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                objectMapper, 6, 0.62D);
    }

    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                new ObjectMapper(), 6, 0.62D);
    }

    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            ObjectMapper objectMapper,
            int topK,
            double minScore)
    {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.retrievalSourceMapper = retrievalSourceMapper;
        this.vectorStoreService = vectorStoreService;
        this.chatModelService = chatModelService;
        this.objectMapper = objectMapper;
        this.topK = topK;
        this.minScore = minScore;
    }

    @Override
    public List<AiChatSession> selectSessionList(Long userId)
    {
        AiChatSession query = new AiChatSession();
        query.setUserId(userId);
        return chatSessionMapper.selectAiChatSessionList(query);
    }

    @Override
    public AiChatSession createSession(Long userId, String title)
    {
        Date now = new Date();
        AiChatSession session = new AiChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle(StringUtils.defaultIfBlank(StringUtils.trim(title), "新建对话"));
        session.setStatus(STATUS_ACTIVE);
        session.setMessageCount(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        chatSessionMapper.insertAiChatSession(session);
        return session;
    }

    @Override
    public List<AiChatMessage> selectMessages(String sessionId, Long userId)
    {
        requireOwnedSession(sessionId, userId);
        List<AiChatMessage> messages = chatMessageMapper.selectAiChatMessageListBySessionId(sessionId);
        attachSources(messages);
        return messages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponse sendMessage(String sessionId, Long userId, String content)
    {
        AiChatSession session = requireOwnedSession(sessionId, userId);
        if (StringUtils.isBlank(content))
        {
            throw new ServiceException("Chat content cannot be blank");
        }

        Date now = new Date();
        chatMessageMapper.insertAiChatMessage(buildUserMessage(sessionId, userId, content, now));

        // Keep the RAG workflow traceable: assistant content and retrieval sources are stored as a single answer unit.
        List<AiVectorRecord> records = vectorStoreService.search(content, topK, minScore);
        String answer = chatModelService.generateAnswer(content, records);
        AiChatMessage assistantMessage = buildAssistantMessage(sessionId, userId, answer, now);
        chatMessageMapper.insertAiChatMessage(assistantMessage);

        List<AiChatRetrievalSource> sources = insertRetrievalSources(assistantMessage.getId(), records, now);
        updateSessionAfterReply(session, answer, now);

        AiChatResponse response = new AiChatResponse();
        response.setSessionId(sessionId);
        response.setMessageId(assistantMessage.getId());
        response.setAnswer(answer);
        response.setSources(sources);
        return response;
    }

    private AiChatSession requireOwnedSession(String sessionId, Long userId)
    {
        if (StringUtils.isBlank(sessionId))
        {
            throw new ServiceException("Chat session does not exist");
        }
        AiChatSession session = chatSessionMapper.selectAiChatSessionById(sessionId);
        if (session == null || userId == null || !userId.equals(session.getUserId()))
        {
            throw new ServiceException("Chat session does not exist");
        }
        return session;
    }

    private AiChatMessage buildUserMessage(String sessionId, Long userId, String content, Date createdAt)
    {
        AiChatMessage message = new AiChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ROLE_USER);
        message.setContent(StringUtils.trim(content));
        message.setRagUsed(Boolean.FALSE);
        message.setCreatedAt(createdAt);
        return message;
    }

    private AiChatMessage buildAssistantMessage(String sessionId, Long userId, String answer, Date createdAt)
    {
        AiChatMessage message = new AiChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ROLE_ASSISTANT);
        message.setContent(answer);
        message.setRagUsed(Boolean.TRUE);
        message.setCreatedAt(createdAt);
        return message;
    }

    private List<AiChatRetrievalSource> insertRetrievalSources(String messageId, List<AiVectorRecord> records, Date now)
    {
        List<AiChatRetrievalSource> sources = new ArrayList<>();
        if (records == null)
        {
            return sources;
        }
        for (int i = 0; i < records.size(); i++)
        {
            AiVectorRecord record = records.get(i);
            AiChatRetrievalSource source = new AiChatRetrievalSource();
            source.setId(UUID.randomUUID().toString());
            source.setMessageId(messageId);
            source.setDocumentId(record.getDocumentId());
            source.setSourceRank(i + 1);
            source.setChunkIndex(record.getChunkIndex());
            source.setSourceTitle(record.getSourceTitle());
            source.setSnippet(record.getContent());
            source.setMetadata(writeMetadata(record.getMetadata()));
            source.setScore(record.getScore() == null ? null : BigDecimal.valueOf(record.getScore()));
            source.setCreatedAt(now);
            retrievalSourceMapper.insertAiChatRetrievalSource(source);
            sources.add(source);
        }
        return sources;
    }

    // History responses should reuse the persisted source rows so the API does not depend on caller-provided source data.
    private void attachSources(List<AiChatMessage> messages)
    {
        if (messages == null)
        {
            return;
        }
        for (AiChatMessage message : messages)
        {
            if (message == null)
            {
                continue;
            }
            if (Boolean.TRUE.equals(message.getRagUsed()))
            {
                message.setSources(retrievalSourceMapper.selectAiChatRetrievalSourceListByMessageId(message.getId()));
            }
            else
            {
                message.setSources(Collections.emptyList());
            }
        }
    }

    private String writeMetadata(Map<String, Object> metadata)
    {
        try
        {
            return objectMapper.writeValueAsString(metadata == null || metadata.isEmpty() ? Collections.emptyMap()
                    : metadata);
        }
        catch (JsonProcessingException ex)
        {
            throw new ServiceException("Failed to serialize retrieval source metadata");
        }
    }

    private void updateSessionAfterReply(AiChatSession session, String answer, Date now)
    {
        session.setLastMessagePreview(StringUtils.abbreviate(StringUtils.defaultString(answer), 500));
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        session.setUpdatedAt(now);
        session.setLastMessageAt(now);
        chatSessionMapper.updateAiChatSession(session);
    }
}
