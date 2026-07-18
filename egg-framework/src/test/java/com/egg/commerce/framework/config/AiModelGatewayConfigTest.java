package com.egg.commerce.framework.config;

import com.egg.commerce.framework.config.properties.AiCommerceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模型网关配置测试，确认 DashScope embedding 配置能注册 Spring AI 模型 Bean。
 */
class AiModelGatewayConfigTest
{
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiModelGatewayConfig.class)
            .withBean(AiCommerceProperties.class, this::embeddingProperties)
            .withPropertyValues("egg.ai.model-gateway.embedding.provider=dashscope");

    /**
     * 验证 DashScope embedding 配置会注册唯一的 Spring AI EmbeddingModel。
     */
    @Test
    void createsDashScopeEmbeddingModelFromModelGatewayConfiguration()
    {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EmbeddingModel.class);
            assertThat(context.getBean(EmbeddingModel.class).dimensions()).isEqualTo(1536);
        });
    }

    /**
     * 构造测试用 embedding 配置，避免测试依赖外部配置文件。
     */
    private AiCommerceProperties embeddingProperties()
    {
        AiCommerceProperties properties = new AiCommerceProperties();
        properties.getModelGateway().getEmbedding().setProvider("dashscope");
        properties.getModelGateway().getEmbedding().setEndpoint("http://localhost:18080/embedding");
        properties.getModelGateway().getEmbedding().setModel("text-embedding-v4");
        properties.getModelGateway().getEmbedding().setApiKey("test-key");
        properties.getRag().setEmbeddingDimension(1536);
        return properties;
    }
}
