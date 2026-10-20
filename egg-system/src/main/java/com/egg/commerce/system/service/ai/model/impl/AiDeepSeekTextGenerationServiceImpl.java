package com.egg.commerce.system.service.ai.model.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.model.AiTextGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiTextGenerationResult;
import com.egg.commerce.system.service.ai.model.AiTextGenerationService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * DeepSeek OpenAI-compatible 文本生成实现。
 *
 * <p>该类只负责 DeepSeek HTTP 调用和响应解析。剧情任务服务不直接拼 DeepSeek 请求，避免业务流程被供应商协议绑定。</p>
 */
@Service
@ConditionalOnProperty(name = "egg.ai.model-gateway.story.provider", havingValue = "deepseek", matchIfMissing = true)
public class AiDeepSeekTextGenerationServiceImpl implements AiTextGenerationService
{
    /** DeepSeek 默认 endpoint，实际部署应通过环境变量覆盖。 */
    private static final String DEFAULT_ENDPOINT = "https://api.deepseek.com";

    /** DeepSeek v4 默认模型，仍允许通过配置切换为 deepseek-v4-flash。 */
    private static final String DEFAULT_MODEL = "deepseek-v4-pro";

    /** DeepSeek API Key 的环境变量名，保留原始变量名便于 IDEA Run Configuration 直接配置。 */
    private static final String ENV_DEEPSEEK_API_KEY = "EGG_DEEPSEEK_API_KEY";

    /** DeepSeek endpoint 的环境变量名，作为 application.yml 占位符之外的兜底来源。 */
    private static final String ENV_DEEPSEEK_ENDPOINT = "EGG_DEEPSEEK_ENDPOINT";

    /** 剧情模型供应商环境变量名，便于运行期覆盖 deepseek/mock。 */
    private static final String ENV_STORY_PROVIDER = "EGG_MODEL_STORY_PROVIDER";

    /** 剧情模型名称环境变量名，便于运行期切换 DeepSeek 具体模型。 */
    private static final String ENV_STORY_MODEL = "EGG_MODEL_STORY_NAME";

    /** JDK HTTP 客户端，避免为单一模型供应商引入额外 SDK 依赖。 */
    private final HttpClient httpClient;

    /** Spring 环境配置，用于读取 endpoint、apiKey、model 和超时时间。 */
    private final Environment environment;

    /**
     * 注入 Spring 环境并创建默认 HTTP 客户端。
     */
    @Autowired
    public AiDeepSeekTextGenerationServiceImpl(Environment environment)
    {
        this(environment, HttpClient.newHttpClient());
    }

    /**
     * 测试构造函数，允许后续注入自定义 HTTP 客户端。
     */
    public AiDeepSeekTextGenerationServiceImpl(Environment environment, HttpClient httpClient)
    {
        this.environment = environment;
        this.httpClient = httpClient;
    }

    /** 调用 DeepSeek chat completions 接口并解析第一条候选回复。 */
    @Override
    public AiTextGenerationResult generate(AiTextGenerationRequest request)
    {
        ModelConfig config = modelConfig();
        JSONObject body = buildRequestBody(config.model(), request.getPrompt(), false);
        HttpRequest httpRequest = buildHttpRequest(config, body);

        try
        {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("DeepSeek 调用失败：" + response.statusCode());
            }
            return result(config, parseContent(response.body()));
        }
        catch (IOException e)
        {
            throw new ServiceException("DeepSeek 网络调用失败：" + e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("DeepSeek 调用被中断");
        }
    }

    /** 调用 DeepSeek 流式 chat completions 接口，逐段转发 assistant delta。 */
    @Override
    public AiTextGenerationResult generateStream(AiTextGenerationRequest request, Consumer<String> onDelta)
    {
        ModelConfig config = modelConfig();
        JSONObject body = buildRequestBody(config.model(), request.getPrompt(), true);
        HttpRequest httpRequest = buildHttpRequest(config, body);

        try
        {
            HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            try (Stream<String> lines = response.body())
            {
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                {
                    String bodyText = lines.collect(Collectors.joining("\n"));
                    throw new ServiceException("DeepSeek 调用失败：" + response.statusCode()
                            + (StringUtils.isBlank(bodyText) ? "" : " " + bodyText));
                }
                StringBuilder content = new StringBuilder();
                lines.forEach(line -> appendStreamDelta(line, content, onDelta));
                if (StringUtils.isBlank(content.toString()))
                {
                    throw new ServiceException("DeepSeek 返回内容为空");
                }
                return result(config, content.toString());
            }
        }
        catch (IOException e)
        {
            throw new ServiceException("DeepSeek 网络调用失败：" + e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("DeepSeek 调用被中断");
        }
    }

    private ModelConfig modelConfig()
    {
        String apiKey = resolveConfigValue("egg.ai.model-gateway.story.api-key", ENV_DEEPSEEK_API_KEY, "");
        if (StringUtils.isBlank(apiKey))
        {
            throw new ServiceException("DeepSeek API Key 未配置：当前 JVM 没有读取到 egg.ai.model-gateway.story.api-key 或 EGG_DEEPSEEK_API_KEY，请在 IDEA 运行配置的 Environment variables 中配置 EGG_DEEPSEEK_API_KEY，或使用 -Degg.ai.model-gateway.story.api-key=你的Key");
        }

        String provider = resolveConfigValue("egg.ai.model-gateway.story.provider", ENV_STORY_PROVIDER, "deepseek");
        String model = resolveConfigValue("egg.ai.model-gateway.story.model", ENV_STORY_MODEL, DEFAULT_MODEL);
        String endpoint = StringUtils.removeEnd(resolveConfigValue("egg.ai.model-gateway.story.endpoint",
                ENV_DEEPSEEK_ENDPOINT, DEFAULT_ENDPOINT), "/");
        int timeoutSeconds = environment.getProperty("egg.ai.model-gateway.timeout-seconds", Integer.class, 120);
        return new ModelConfig(apiKey, provider, model, endpoint, timeoutSeconds);
    }

    private HttpRequest buildHttpRequest(ModelConfig config, JSONObject body)
    {
        return HttpRequest.newBuilder(URI.create(config.endpoint() + "/chat/completions"))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();
    }

    private AiTextGenerationResult result(ModelConfig config, String content)
    {
        AiTextGenerationResult result = new AiTextGenerationResult();
        result.setProvider(config.provider());
        result.setModel(config.model());
        result.setContent(content);
        return result;
    }

    /** 按 Spring 配置、原始环境变量、JVM -D 参数的顺序读取模型配置，兼容 IDEA 本地运行和服务器部署。 */
    private String resolveConfigValue(String propertyName, String environmentName, String defaultValue)
    {
        String value = environment.getProperty(propertyName);
        if (StringUtils.isBlank(value))
        {
            value = environment.getProperty(environmentName);
        }
        if (StringUtils.isBlank(value))
        {
            value = System.getProperty(propertyName);
        }
        if (StringUtils.isBlank(value))
        {
            value = System.getProperty(environmentName);
        }
        if (StringUtils.isBlank(value))
        {
            value = System.getenv(environmentName);
        }
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    /** 构造 OpenAI-compatible chat completions 请求体。 */
    private JSONObject buildRequestBody(String model, String prompt, boolean stream)
    {
        JSONObject body = new JSONObject();
        body.put("model", model);
        if (stream)
        {
            body.put("stream", true);
        }
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", StringUtils.defaultString(prompt));
        messages.add(userMessage);
        body.put("messages", messages);
        return body;
    }

    /** 从 DeepSeek 响应中取第一条 assistant 消息正文。 */
    private String parseContent(String responseBody)
    {
        JSONObject json = JSONObject.parseObject(responseBody);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            throw new ServiceException("DeepSeek 未返回候选内容");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message == null ? null : message.getString("content");
        if (StringUtils.isBlank(content))
        {
            throw new ServiceException("DeepSeek 返回内容为空");
        }
        return content;
    }

    private void appendStreamDelta(String line, StringBuilder content, Consumer<String> onDelta)
    {
        if (StringUtils.isBlank(line) || !line.startsWith("data:"))
        {
            return;
        }
        String data = StringUtils.trim(line.substring("data:".length()));
        if ("[DONE]".equals(data))
        {
            return;
        }
        String delta = parseStreamDelta(data);
        if (StringUtils.isBlank(delta))
        {
            return;
        }
        content.append(delta);
        if (onDelta != null)
        {
            onDelta.accept(delta);
        }
    }

    private String parseStreamDelta(String eventData)
    {
        JSONObject json = JSONObject.parseObject(eventData);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            return "";
        }
        JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
        return delta == null ? "" : StringUtils.defaultString(delta.getString("content"));
    }

    private record ModelConfig(String apiKey, String provider, String model, String endpoint, int timeoutSeconds)
    {
    }
}
