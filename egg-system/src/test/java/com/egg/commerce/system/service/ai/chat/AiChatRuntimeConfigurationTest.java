package com.egg.commerce.system.service.ai.chat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 智能聊天运行配置约束测试，覆盖真实模型启用和 SSE 异步分发安全配置。
 */
class AiChatRuntimeConfigurationTest
{
    @Test
    void chatModelMockModeIsDisabledByDefault() throws Exception
    {
        String applicationYaml = Files.readString(Path.of("../egg-admin/src/main/resources/application.yml"));

        assertTrue(applicationYaml.contains("mock-enabled: ${EGG_AI_MOCK_ENABLED:false}"));
    }

    @Test
    void securityConfigPermitsAsyncDispatcherForSseResponses() throws Exception
    {
        String source = Files.readString(Path.of("../egg-framework/src/main/java/com/egg/commerce/framework/config/SecurityConfig.java"));

        assertTrue(source.contains("dispatcherTypeMatchers"));
        assertTrue(source.contains("DispatcherType.ASYNC"));
        assertTrue(source.contains("DispatcherType.ERROR"));
    }
}
