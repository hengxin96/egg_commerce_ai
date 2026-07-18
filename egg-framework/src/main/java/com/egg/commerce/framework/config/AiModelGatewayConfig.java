package com.egg.commerce.framework.config;

import com.egg.commerce.framework.ai.model.DashScopeEmbeddingModel;
import com.egg.commerce.framework.config.properties.AiCommerceProperties;
import com.egg.commerce.framework.config.properties.AiCommerceProperties.Provider;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将项目模型网关配置桥接为 Spring AI 模型 Bean。
 */
@Configuration
public class AiModelGatewayConfig
{
    /**
     * 按模型网关配置创建 DashScope embedding 适配器，供 Spring AI 向量库复用。
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(name = "egg.ai.model-gateway.embedding.provider", havingValue = "dashscope")
    public EmbeddingModel dashScopeEmbeddingModel(AiCommerceProperties properties)
    {
        Provider embedding = properties.getModelGateway().getEmbedding();
        return new DashScopeEmbeddingModel(embedding.getEndpoint(), embedding.getApiKey(), embedding.getModel(),
                properties.getRag().getEmbeddingDimension());
    }
}
