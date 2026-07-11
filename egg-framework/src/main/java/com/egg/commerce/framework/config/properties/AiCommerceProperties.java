package com.egg.commerce.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第一阶段 AI 电商模块的集中式类型化配置。
 */
@Component
@ConfigurationProperties(prefix = "egg.ai")
public class AiCommerceProperties
{
    private boolean mockEnabled = true;

    private Storage storage = new Storage();

    private Task task = new Task();

    private Rag rag = new Rag();

    private ModelGateway modelGateway = new ModelGateway();

    private RealImageGeneration realImageGeneration = new RealImageGeneration();

    private A2a a2a = new A2a();

    public boolean isMockEnabled()
    {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled)
    {
        this.mockEnabled = mockEnabled;
    }

    public Storage getStorage()
    {
        return storage;
    }

    public void setStorage(Storage storage)
    {
        this.storage = storage;
    }

    public Task getTask()
    {
        return task;
    }

    public void setTask(Task task)
    {
        this.task = task;
    }

    public Rag getRag()
    {
        return rag;
    }

    public void setRag(Rag rag)
    {
        this.rag = rag;
    }

    public ModelGateway getModelGateway()
    {
        return modelGateway;
    }

    public void setModelGateway(ModelGateway modelGateway)
    {
        this.modelGateway = modelGateway;
    }

    public RealImageGeneration getRealImageGeneration()
    {
        return realImageGeneration;
    }

    public void setRealImageGeneration(RealImageGeneration realImageGeneration)
    {
        this.realImageGeneration = realImageGeneration;
    }

    public A2a getA2a()
    {
        return a2a;
    }

    public void setA2a(A2a a2a)
    {
        this.a2a = a2a;
    }

    public static class Storage
    {
        private String provider = "minio";

        private Minio minio = new Minio();

        public String getProvider()
        {
            return provider;
        }

        public void setProvider(String provider)
        {
            this.provider = provider;
        }

        public Minio getMinio()
        {
            return minio;
        }

        public void setMinio(Minio minio)
        {
            this.minio = minio;
        }
    }

    public static class Minio
    {
        private String endpoint;

        private String publicEndpoint;

        private String bucket;

        private String accessKey;

        private String secretKey;

        private int presignedUrlExpireMinutes = 30;

        private String objectPrefix = "users";

        public String getEndpoint()
        {
            return endpoint;
        }

        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }

        public String getPublicEndpoint()
        {
            return publicEndpoint;
        }

        public void setPublicEndpoint(String publicEndpoint)
        {
            this.publicEndpoint = publicEndpoint;
        }

        public String getBucket()
        {
            return bucket;
        }

        public void setBucket(String bucket)
        {
            this.bucket = bucket;
        }

        public String getAccessKey()
        {
            return accessKey;
        }

        public void setAccessKey(String accessKey)
        {
            this.accessKey = accessKey;
        }

        public String getSecretKey()
        {
            return secretKey;
        }

        public void setSecretKey(String secretKey)
        {
            this.secretKey = secretKey;
        }

        public int getPresignedUrlExpireMinutes()
        {
            return presignedUrlExpireMinutes;
        }

        public void setPresignedUrlExpireMinutes(int presignedUrlExpireMinutes)
        {
            this.presignedUrlExpireMinutes = presignedUrlExpireMinutes;
        }

        public String getObjectPrefix()
        {
            return objectPrefix;
        }

        public void setObjectPrefix(String objectPrefix)
        {
            this.objectPrefix = objectPrefix;
        }
    }

    public static class Task
    {
        private String queueProvider = "redis";

        private String queueKey;

        private String progressKeyPrefix;

        private String duplicateLockPrefix;

        private int duplicateLockSeconds = 600;

        private int userRateLimitPerMinute = 12;

        private int ipRateLimitPerMinute = 60;

        public String getQueueProvider()
        {
            return queueProvider;
        }

        public void setQueueProvider(String queueProvider)
        {
            this.queueProvider = queueProvider;
        }

        public String getQueueKey()
        {
            return queueKey;
        }

        public void setQueueKey(String queueKey)
        {
            this.queueKey = queueKey;
        }

        public String getProgressKeyPrefix()
        {
            return progressKeyPrefix;
        }

        public void setProgressKeyPrefix(String progressKeyPrefix)
        {
            this.progressKeyPrefix = progressKeyPrefix;
        }

        public String getDuplicateLockPrefix()
        {
            return duplicateLockPrefix;
        }

        public void setDuplicateLockPrefix(String duplicateLockPrefix)
        {
            this.duplicateLockPrefix = duplicateLockPrefix;
        }

        public int getDuplicateLockSeconds()
        {
            return duplicateLockSeconds;
        }

        public void setDuplicateLockSeconds(int duplicateLockSeconds)
        {
            this.duplicateLockSeconds = duplicateLockSeconds;
        }

        public int getUserRateLimitPerMinute()
        {
            return userRateLimitPerMinute;
        }

        public void setUserRateLimitPerMinute(int userRateLimitPerMinute)
        {
            this.userRateLimitPerMinute = userRateLimitPerMinute;
        }

        public int getIpRateLimitPerMinute()
        {
            return ipRateLimitPerMinute;
        }

        public void setIpRateLimitPerMinute(int ipRateLimitPerMinute)
        {
            this.ipRateLimitPerMinute = ipRateLimitPerMinute;
        }
    }

    public static class Rag
    {
        private boolean enabled = true;

        private String vectorStore = "pgvector";

        private int chunkSize = 800;

        private int chunkOverlap = 120;

        private int topK = 6;

        private double minScore = 0.62;

        private int embeddingDimension = 1536;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getVectorStore()
        {
            return vectorStore;
        }

        public void setVectorStore(String vectorStore)
        {
            this.vectorStore = vectorStore;
        }

        public int getChunkSize()
        {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize)
        {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap()
        {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap)
        {
            this.chunkOverlap = chunkOverlap;
        }

        public int getTopK()
        {
            return topK;
        }

        public void setTopK(int topK)
        {
            this.topK = topK;
        }

        public double getMinScore()
        {
            return minScore;
        }

        public void setMinScore(double minScore)
        {
            this.minScore = minScore;
        }

        public int getEmbeddingDimension()
        {
            return embeddingDimension;
        }

        public void setEmbeddingDimension(int embeddingDimension)
        {
            this.embeddingDimension = embeddingDimension;
        }
    }

    public static class ModelGateway
    {
        private int timeoutSeconds = 120;

        private int maxRetries = 2;

        private boolean costLogEnabled = true;

        private Provider text = new Provider();

        private Provider vision = new Provider();

        private Provider image = new Provider();

        private Provider embedding = new Provider();

        private Coze coze = new Coze();

        public int getTimeoutSeconds()
        {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds)
        {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxRetries()
        {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries)
        {
            this.maxRetries = maxRetries;
        }

        public boolean isCostLogEnabled()
        {
            return costLogEnabled;
        }

        public void setCostLogEnabled(boolean costLogEnabled)
        {
            this.costLogEnabled = costLogEnabled;
        }

        public Provider getText()
        {
            return text;
        }

        public void setText(Provider text)
        {
            this.text = text;
        }

        public Provider getVision()
        {
            return vision;
        }

        public void setVision(Provider vision)
        {
            this.vision = vision;
        }

        public Provider getImage()
        {
            return image;
        }

        public void setImage(Provider image)
        {
            this.image = image;
        }

        public Provider getEmbedding()
        {
            return embedding;
        }

        public void setEmbedding(Provider embedding)
        {
            this.embedding = embedding;
        }

        public Coze getCoze()
        {
            return coze;
        }

        public void setCoze(Coze coze)
        {
            this.coze = coze;
        }
    }

    public static class Provider
    {
        private String provider = "mock";

        private String endpoint;

        private String model;

        private String apiKey;

        public String getProvider()
        {
            return provider;
        }

        public void setProvider(String provider)
        {
            this.provider = provider;
        }

        public String getEndpoint()
        {
            return endpoint;
        }

        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }

        public String getModel()
        {
            return model;
        }

        public void setModel(String model)
        {
            this.model = model;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(String apiKey)
        {
            this.apiKey = apiKey;
        }
    }

    public static class Coze extends Provider
    {
        private boolean enabled;

        private String botId;

        private String workflowId;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getBotId()
        {
            return botId;
        }

        public void setBotId(String botId)
        {
            this.botId = botId;
        }

        public String getWorkflowId()
        {
            return workflowId;
        }

        public void setWorkflowId(String workflowId)
        {
            this.workflowId = workflowId;
        }
    }

    public static class RealImageGeneration
    {
        private boolean required = true;

        private boolean allowModelTemporaryUrl;

        private boolean persistGeneratedImage = true;

        private boolean requireProductIdentityPreservation = true;

        private boolean requireManualReviewForHighRisk = true;

        public boolean isRequired()
        {
            return required;
        }

        public void setRequired(boolean required)
        {
            this.required = required;
        }

        public boolean isAllowModelTemporaryUrl()
        {
            return allowModelTemporaryUrl;
        }

        public void setAllowModelTemporaryUrl(boolean allowModelTemporaryUrl)
        {
            this.allowModelTemporaryUrl = allowModelTemporaryUrl;
        }

        public boolean isPersistGeneratedImage()
        {
            return persistGeneratedImage;
        }

        public void setPersistGeneratedImage(boolean persistGeneratedImage)
        {
            this.persistGeneratedImage = persistGeneratedImage;
        }

        public boolean isRequireProductIdentityPreservation()
        {
            return requireProductIdentityPreservation;
        }

        public void setRequireProductIdentityPreservation(boolean requireProductIdentityPreservation)
        {
            this.requireProductIdentityPreservation = requireProductIdentityPreservation;
        }

        public boolean isRequireManualReviewForHighRisk()
        {
            return requireManualReviewForHighRisk;
        }

        public void setRequireManualReviewForHighRisk(boolean requireManualReviewForHighRisk)
        {
            this.requireManualReviewForHighRisk = requireManualReviewForHighRisk;
        }
    }

    public static class A2a
    {
        private boolean enabled;

        private String phase = "terminal";

        private String endpoint;

        private String apiKey;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getPhase()
        {
            return phase;
        }

        public void setPhase(String phase)
        {
            this.phase = phase;
        }

        public String getEndpoint()
        {
            return endpoint;
        }

        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(String apiKey)
        {
            this.apiKey = apiKey;
        }
    }
}
