package com.egg.commerce.system.service.ai.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.system.service.ai.model.impl.AiDeepSeekTextGenerationServiceImpl;
import com.egg.commerce.system.service.ai.model.impl.AiMockTextGenerationServiceImpl;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

/**
 * 文本生成网关测试，确保剧情服务依赖稳定接口，而不是直接依赖具体模型供应商。
 */
class AiTextGenerationServiceTest
{
    /**
     * 验证请求对象能保存节点、提示词和模型参数快照。
     */
    @Test
    void textGenerationRequestKeepsStagePromptAndModelMetadata()
    {
        AiTextGenerationRequest request = new AiTextGenerationRequest();
        request.setStage("剧情生成");
        request.setPrompt("请生成剧情");
        request.setProvider("deepseek");
        request.setModel("deepseek-v4-pro");

        assertEquals("剧情生成", request.getStage());
        assertEquals("请生成剧情", request.getPrompt());
        assertEquals("deepseek", request.getProvider());
        assertEquals("deepseek-v4-pro", request.getModel());
    }

    /**
     * 验证 Mock 模型只返回明确占位内容，不再把完整提示词冒充为阶段输出。
     */
    @Test
    void mockTextGenerationReturnsSafePlaceholderWithoutEchoingPrompt()
    {
        AiMockTextGenerationServiceImpl service = new AiMockTextGenerationServiceImpl();
        AiTextGenerationRequest request = new AiTextGenerationRequest();
        request.setStage("剧本生成");
        request.setPrompt("剧情内容");

        AiTextGenerationResult result = service.generate(request);

        assertEquals("mock", result.getProvider());
        assertEquals("mock-story-model", result.getModel());
        assertTrue(result.getContent().contains("剧本生成"));
        assertTrue(result.getContent().contains("Mock 生成内容"));
        assertFalse(result.getContent().contains("剧情内容"));
    }

    /**
     * 验证剧情链路按 story provider 选择 DeepSeek 或 Mock，不再被全局 mock 开关误导。
     */
    @Test
    void storyTextGenerationBeansAreSelectedByStoryProvider()
    {
        ConditionalOnProperty deepseek = AiDeepSeekTextGenerationServiceImpl.class
                .getAnnotation(ConditionalOnProperty.class);
        assertArrayEquals(new String[] {"egg.ai.model-gateway.story.provider"}, deepseek.name());
        assertEquals("deepseek", deepseek.havingValue());
        assertTrue(deepseek.matchIfMissing());

        ConditionalOnProperty mock = AiMockTextGenerationServiceImpl.class.getAnnotation(ConditionalOnProperty.class);
        assertArrayEquals(new String[] {"egg.ai.model-gateway.story.provider"}, mock.name());
        assertEquals("mock", mock.havingValue());
        assertFalse(mock.matchIfMissing());
    }

    /**
     * 验证 DeepSeek 服务在真实 Spring 容器中只需要 Environment 也能创建，避免本地没有 HttpClient Bean 时启动失败。
     */
    @Test
    void deepSeekTextGenerationBeanStartsWithDefaultHttpClient()
    {
        new ApplicationContextRunner()
                .withBean(AiDeepSeekTextGenerationServiceImpl.class)
                .run(context -> {
                    assertEquals(null, context.getStartupFailure());
                    assertTrue(context.getBean(AiDeepSeekTextGenerationServiceImpl.class) != null);
                });
    }

    /**
     * 验证 DeepSeek Key 可以从原始环境变量名读取，避免 application.yml 占位符展开为空时直接失败。
     */
    @Test
    void deepSeekApiKeyCanFallbackToRawEnvironmentName() throws Exception
    {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("EGG_DEEPSEEK_API_KEY", "sk-test-key");
        AiDeepSeekTextGenerationServiceImpl service = new AiDeepSeekTextGenerationServiceImpl(environment);

        assertEquals("sk-test-key", resolveConfigValue(service, "egg.ai.model-gateway.story.api-key",
                "EGG_DEEPSEEK_API_KEY", ""));
    }

    /**
     * 调用私有配置读取方法，让测试聚焦配置来源优先级，不触发真实 DeepSeek 网络请求。
     */
    private String resolveConfigValue(AiDeepSeekTextGenerationServiceImpl service, String propertyName,
            String environmentName, String defaultValue) throws Exception
    {
        Method method = AiDeepSeekTextGenerationServiceImpl.class.getDeclaredMethod("resolveConfigValue",
                String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, propertyName, environmentName, defaultValue);
    }
}
