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
import com.egg.commerce.system.service.ISysRoleService;
import com.egg.commerce.system.service.ai.chat.AiChatModelService;
import com.egg.commerce.system.service.ai.chat.AiChatService;
import com.egg.commerce.system.service.ai.knowledge.AiKnowledgeDocumentService;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import com.egg.commerce.system.service.ai.sensitive.AiSensitiveWordService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatServiceImpl implements AiChatService
{
    private static final String STATUS_ACTIVE = "active";

    private static final String DEFAULT_SESSION_TITLE = "新建对话";

    private static final String ROLE_USER = "user";

    private static final String ROLE_ASSISTANT = "assistant";

    private static final long TITLE_GENERATION_TIMEOUT_MILLIS = 1500L;

    private static final String SENSITIVE_REJECTION_ANSWER = "您的问题包含敏感词，无法回答。";

    private static final String GREETING_ANSWER = "你好，我在。你可以问我知识库里的平台规则、商品内容规范、审核规则或生成建议。";

    private static final String IDENTITY_ANSWER = "我是龙蛋 AI 的智能聊天助手，可以帮你查询和整理知识库里的平台规则、商品内容规范、审核规则和生成建议。涉及具体知识的问题，我会依据知识库回答。";

    private static final String CAPABILITY_ANSWER = "我可以帮你整理知识库里的平台规则、商品内容规范、审核规则、图片或文案风险，以及基于资料给出生成建议。具体知识类问题需要以知识库内容为依据。";

    private static final String THANKS_ANSWER = "不客气。你可以继续问我知识库里的规则、规范或生成建议。";

    private static final String GOODBYE_ANSWER = "好的，随时回来问我知识库里的问题。";

    private static final String NO_ACCESSIBLE_KNOWLEDGE_ANSWER = "我现在还没有可访问的知识库资料，所以不能回答知识类问题。你可以先让管理员上传或授权相关文档后再问。";

    private static final String NO_RETRIEVAL_SOURCE_ANSWER = "这个问题需要依据知识库回答，但我没有检索到可引用资料。你可以换个问法，或请管理员补充、授权相关知识文档后再问。";

    private final AiChatSessionMapper chatSessionMapper;

    private final AiChatMessageMapper chatMessageMapper;

    private final AiChatRetrievalSourceMapper retrievalSourceMapper;

    private final AiVectorStoreService vectorStoreService;

    private final AiChatModelService chatModelService;

    private final AiSensitiveWordService sensitiveWordService;

    private final AiKnowledgeDocumentService knowledgeDocumentService;

    private final ISysRoleService roleService;

    private final ObjectMapper objectMapper;

    private final int topK;

    private final double minScore;

    /**
     * Spring 运行时构造函数，从环境配置读取 RAG 检索数量和最低分数。
     */
    @Autowired
    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            AiSensitiveWordService sensitiveWordService,
            AiKnowledgeDocumentService knowledgeDocumentService,
            ISysRoleService roleService,
            Environment environment)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                sensitiveWordService, knowledgeDocumentService, roleService,
                new ObjectMapper(),
                environment.getProperty("egg.ai.rag.top-k", Integer.class, 6),
                environment.getProperty("egg.ai.rag.min-score", Double.class, 0.62D));
    }

    /**
     * 测试构造函数，允许测试注入自定义 JSON 序列化器。
     */
    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            ObjectMapper objectMapper)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                null, null, null, objectMapper, 6, 0.62D);
    }

    /**
     * 默认测试构造函数，使用本地 ObjectMapper 和默认 RAG 参数。
     */
    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                null, null, null, new ObjectMapper(), 6, 0.62D);
    }

    /**
     * 完整构造函数，集中设置依赖和 RAG 检索参数。
     */
    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            ObjectMapper objectMapper,
            int topK,
            double minScore)
    {
        this(chatSessionMapper, chatMessageMapper, retrievalSourceMapper, vectorStoreService, chatModelService,
                null, null, null, objectMapper, topK, minScore);
    }

    /**
     * 完整构造函数，集中设置依赖和 RAG 检索参数。
     */
    public AiChatServiceImpl(AiChatSessionMapper chatSessionMapper,
            AiChatMessageMapper chatMessageMapper,
            AiChatRetrievalSourceMapper retrievalSourceMapper,
            AiVectorStoreService vectorStoreService,
            AiChatModelService chatModelService,
            AiSensitiveWordService sensitiveWordService,
            AiKnowledgeDocumentService knowledgeDocumentService,
            ISysRoleService roleService,
            ObjectMapper objectMapper,
            int topK,
            double minScore)
    {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.retrievalSourceMapper = retrievalSourceMapper;
        this.vectorStoreService = vectorStoreService;
        this.chatModelService = chatModelService;
        this.sensitiveWordService = sensitiveWordService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.roleService = roleService;
        this.objectMapper = objectMapper;
        this.topK = topK;
        this.minScore = minScore;
    }

    /**
     * 查询当前用户的聊天会话列表，避免跨用户读取历史会话。
     */
    @Override
    public List<AiChatSession> selectSessionList(Long userId)
    {
        AiChatSession query = new AiChatSession();
        query.setUserId(userId);
        return chatSessionMapper.selectAiChatSessionList(query);
    }

    /**
     * 创建聊天会话并补齐默认标题、状态和时间字段。
     */
    @Override
    public AiChatSession createSession(Long userId, String title)
    {
        Date now = new Date();
        AiChatSession session = new AiChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle(StringUtils.defaultIfBlank(StringUtils.trim(title), DEFAULT_SESSION_TITLE));
        session.setStatus(STATUS_ACTIVE);
        session.setMessageCount(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        chatSessionMapper.insertAiChatSession(session);
        return session;
    }

    /**
     * 校验会话归属后读取历史消息，并挂载已持久化的 RAG 来源。
     */
    @Override
    public List<AiChatMessage> selectMessages(String sessionId, Long userId)
    {
        requireOwnedSession(sessionId, userId);
        List<AiChatMessage> messages = chatMessageMapper.selectAiChatMessageListBySessionId(sessionId);
        attachSources(messages);
        return messages;
    }

    /**
     * 保存用户问题、执行 RAG 检索、生成助手回答，并持久化答案来源。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponse sendMessage(String sessionId, Long userId, String content)
    {
        return sendMessageInternal(sessionId, userId, content, null);
    }

    /**
     * 保存用户问题、执行 RAG 检索、流式生成助手回答，并持久化完整答案来源。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponse streamMessage(String sessionId, Long userId, String content, Consumer<String> onDelta)
    {
        return sendMessageInternal(sessionId, userId, content, onDelta);
    }

    /**
     * 物理删除会话、消息和回答来源，并复用归属校验避免越权删除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, Long userId)
    {
        AiChatSession session = requireOwnedSession(sessionId, userId);
        retrievalSourceMapper.deleteAiChatRetrievalSourcesBySessionId(session.getId());
        chatMessageMapper.deleteAiChatMessagesBySessionId(session.getId());
        chatSessionMapper.deleteAiChatSessionById(session.getId());
    }

    private AiChatResponse sendMessageInternal(String sessionId, Long userId, String content, Consumer<String> onDelta)
    {
        AiChatSession session = requireOwnedSession(sessionId, userId);
        if (StringUtils.isBlank(content))
        {
            throw new ServiceException("Chat content cannot be blank");
        }

        Date now = new Date();
        String normalizedContent = StringUtils.trim(content);
        chatMessageMapper.insertAiChatMessage(buildUserMessage(sessionId, userId, normalizedContent, now));

        if (containsSensitiveWord(normalizedContent))
        {
            emitDelta(onDelta, SENSITIVE_REJECTION_ANSWER);
            return saveAssistantReply(session, sessionId, userId, SENSITIVE_REJECTION_ANSWER, List.of(), false, now,
                    normalizedContent);
        }

        List<String> accessibleDocumentIds = selectAccessibleDocumentIds(userId);
        List<AiVectorRecord> records;
        if (accessibleDocumentIds != null && accessibleDocumentIds.isEmpty())
        {
            records = Collections.emptyList();
        }
        else
        {
            records = accessibleDocumentIds == null
                    ? vectorStoreService.search(normalizedContent, topK, minScore)
                    : vectorStoreService.search(normalizedContent, topK, minScore, accessibleDocumentIds);
            records = records == null ? Collections.emptyList() : records;
        }
        String answer = onDelta == null
                ? chatModelService.generateAnswer(normalizedContent, records)
                : chatModelService.generateAnswerStream(normalizedContent, records, onDelta);
        return saveAssistantReply(session, sessionId, userId, answer, records, !records.isEmpty(), now,
                normalizedContent);
    }

    private void emitDelta(Consumer<String> onDelta, String answer)
    {
        if (onDelta != null && StringUtils.isNotEmpty(answer))
        {
            onDelta.accept(answer);
        }
    }

    /**
     * 统一校验会话存在性和用户归属，避免越权读取或写入聊天记录。
     */
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

    /**
     * 构造用户消息记录，用户消息不标记 RAG 来源。
     */
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

    /**
     * 构造助手消息记录，答案默认来自 RAG 流程并需记录来源。
     */
    private AiChatMessage buildAssistantMessage(String sessionId, Long userId, String answer, Boolean ragUsed,
            Date createdAt)
    {
        AiChatMessage message = new AiChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ROLE_ASSISTANT);
        message.setContent(answer);
        message.setRagUsed(ragUsed);
        message.setCreatedAt(createdAt);
        return message;
    }

    /**
     * 保存助手回复、可选来源和会话摘要，统一构造前端响应。
     */
    private AiChatResponse saveAssistantReply(AiChatSession session, String sessionId, Long userId, String answer,
            List<AiVectorRecord> records, Boolean ragUsed, Date now)
    {
        return saveAssistantReply(session, sessionId, userId, answer, records, ragUsed, now, null);
    }

    private AiChatResponse saveAssistantReply(AiChatSession session, String sessionId, Long userId, String answer,
            List<AiVectorRecord> records, Boolean ragUsed, Date now, String userQuestion)
    {
        AiChatMessage assistantMessage = buildAssistantMessage(sessionId, userId, answer, ragUsed, now);
        chatMessageMapper.insertAiChatMessage(assistantMessage);

        List<AiChatRetrievalSource> sources = Boolean.TRUE.equals(ragUsed)
                ? insertRetrievalSources(assistantMessage.getId(), records, now)
                : Collections.emptyList();
        updateSessionAfterReply(session, answer, now, userQuestion);

        AiChatResponse response = new AiChatResponse();
        response.setSessionId(sessionId);
        response.setMessageId(assistantMessage.getId());
        response.setTitle(session.getTitle());
        response.setAnswer(answer);
        response.setSources(sources);
        return response;
    }

    /**
     * 用户提问命中敏感词时拒答，且不泄露具体词条。
     */
    private boolean containsSensitiveWord(String content)
    {
        return sensitiveWordService != null && sensitiveWordService.containsEnabledSensitiveWord(content);
    }

    /**
     * 寒暄类输入不属于知识问答，不需要强行触发 RAG 拒答。
     */
    private String basicFriendlyReply(String content)
    {
        String normalized = StringUtils.deleteWhitespace(StringUtils.defaultString(content)).toLowerCase();
        if ("你好".equals(normalized)
                || "您好".equals(normalized)
                || "hello".equals(normalized)
                || "hi".equals(normalized)
                || "嗨".equals(normalized)
                || "在吗".equals(normalized)
                || "在不在".equals(normalized))
        {
            return GREETING_ANSWER;
        }
        if ("你是谁".equals(stripTrailingPunctuation(normalized))
                || "你叫什么".equals(stripTrailingPunctuation(normalized)))
        {
            return IDENTITY_ANSWER;
        }
        if ("你能做什么".equals(stripTrailingPunctuation(normalized))
                || "你会什么".equals(stripTrailingPunctuation(normalized))
                || "有什么功能".equals(stripTrailingPunctuation(normalized))
                || "你的功能".equals(stripTrailingPunctuation(normalized)))
        {
            return CAPABILITY_ANSWER;
        }
        if ("谢谢".equals(stripTrailingPunctuation(normalized))
                || "感谢".equals(stripTrailingPunctuation(normalized))
                || "谢谢你".equals(stripTrailingPunctuation(normalized)))
        {
            return THANKS_ANSWER;
        }
        if ("再见".equals(stripTrailingPunctuation(normalized))
                || "拜拜".equals(stripTrailingPunctuation(normalized)))
        {
            return GOODBYE_ANSWER;
        }
        return null;
    }

    private String stripTrailingPunctuation(String value)
    {
        return StringUtils.stripEnd(value, "？?。.!！");
    }

    /**
     * 通过用户角色计算可访问的已完成知识库文档 ID。
     */
    private List<String> selectAccessibleDocumentIds(Long userId)
    {
        if (knowledgeDocumentService == null || roleService == null)
        {
            return null;
        }
        Set<String> roleKeys = roleService.selectRolePermissionByUserId(userId);
        return knowledgeDocumentService.selectAccessibleCompletedDocumentIds(roleKeys);
    }

    /**
     * 将本轮检索命中的知识片段保存为回答来源，支撑前端溯源展示。
     */
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

    // 历史消息复用已持久化的来源记录，避免接口依赖调用方传入的来源数据。
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

    /**
     * 将向量库元数据转成 JSON 字符串，方便存入 jsonb 字段。
     */
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

    /**
     * 回写会话摘要和消息计数，让会话列表展示最新回答状态。
     */
    private void updateSessionAfterReply(AiChatSession session, String answer, Date now, String userQuestion)
    {
        boolean shouldGenerateTitle = (session.getMessageCount() == null || session.getMessageCount() == 0)
                && StringUtils.isNotBlank(userQuestion)
                && (StringUtils.isBlank(session.getTitle()) || DEFAULT_SESSION_TITLE.equals(session.getTitle()));
        if (shouldGenerateTitle)
        {
            session.setTitle(resolveSessionTitle(userQuestion, answer));
        }
        session.setLastMessagePreview(StringUtils.abbreviate(StringUtils.defaultString(answer), 500));
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        session.setUpdatedAt(now);
        session.setLastMessageAt(now);
        chatSessionMapper.updateAiChatSession(session);
    }

    private String resolveSessionTitle(String question, String answer)
    {
        try
        {
            String title = CompletableFuture.supplyAsync(() -> chatModelService.generateSessionTitle(question, answer))
                    .completeOnTimeout(null, TITLE_GENERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .exceptionally(e -> null)
                    .join();
            return normalizeSessionTitle(title, question);
        }
        catch (Exception e)
        {
            return fallbackSessionTitle(question);
        }
    }

    private String normalizeSessionTitle(String title, String question)
    {
        String normalized = StringUtils.trimToEmpty(title)
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "");
        if (StringUtils.isBlank(normalized) || DEFAULT_SESSION_TITLE.equals(normalized))
        {
            return fallbackSessionTitle(question);
        }
        return abbreviateTitle(normalized);
    }

    private String fallbackSessionTitle(String question)
    {
        return abbreviateTitle(StringUtils.defaultIfBlank(StringUtils.trim(question), "未命名对话"));
    }

    private String abbreviateTitle(String value)
    {
        String normalized = StringUtils.deleteWhitespace(StringUtils.defaultString(value));
        if (normalized.length() <= 18)
        {
            return normalized;
        }
        return normalized.substring(0, 18);
    }
}
