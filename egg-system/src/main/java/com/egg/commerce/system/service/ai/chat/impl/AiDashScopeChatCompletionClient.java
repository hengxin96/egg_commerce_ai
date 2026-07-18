package com.egg.commerce.system.service.ai.chat.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.chat.AiChatCompletionClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * DashScope 通义千问 OpenAI-compatible 聊天补全客户端。
 */
@Service
@ConditionalOnProperty(name = "egg.ai.model-gateway.text.provider", havingValue = "dashscope", matchIfMissing = true)
public class AiDashScopeChatCompletionClient implements AiChatCompletionClient
{
    private static final String DEFAULT_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private static final String DEFAULT_MODEL = "qwen-plus";

    private static final String ENV_DASHSCOPE_API_KEY = "EGG_DASHSCOPE_API_KEY";

    private static final String ENV_DASHSCOPE_ENDPOINT = "EGG_DASHSCOPE_ENDPOINT";

    private static final String ENV_TEXT_PROVIDER = "EGG_MODEL_TEXT_PROVIDER";

    private static final String ENV_TEXT_MODEL = "EGG_MODEL_TEXT_NAME";

    private final Environment environment;

    private final HttpClient httpClient;

    @Autowired
    public AiDashScopeChatCompletionClient(Environment environment)
    {
        this(environment, HttpClient.newHttpClient());
    }

    public AiDashScopeChatCompletionClient(Environment environment, HttpClient httpClient)
    {
        this.environment = environment;
        this.httpClient = httpClient;
    }

    /**
     * 调用 DashScope 兼容模式 chat completions，并返回 assistant 正文。
     */
    @Override
    public String complete(String systemPrompt, String userPrompt)
    {
        String apiKey = resolveConfigValue("egg.ai.model-gateway.text.api-key", ENV_DASHSCOPE_API_KEY, "");
        if (StringUtils.isBlank(apiKey))
        {
            throw new ServiceException("DashScope API Key 未配置：请配置 egg.ai.model-gateway.text.api-key 或 EGG_DASHSCOPE_API_KEY");
        }

        String model = resolveConfigValue("egg.ai.model-gateway.text.model", ENV_TEXT_MODEL, DEFAULT_MODEL);
        String endpoint = StringUtils.removeEnd(resolveConfigValue("egg.ai.model-gateway.text.endpoint",
                ENV_DASHSCOPE_ENDPOINT, DEFAULT_ENDPOINT), "/");
        int timeoutSeconds = environment.getProperty("egg.ai.model-gateway.timeout-seconds", Integer.class, 120);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/chat/completions"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(model, systemPrompt, userPrompt, false)
                        .toJSONString()))
                .build();

        try
        {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("DashScope 调用失败：" + response.statusCode());
            }
            return parseContent(response.body());
        }
        catch (IOException e)
        {
            throw new ServiceException("DashScope 网络调用失败：" + e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("DashScope 调用被中断");
        }
    }

    /**
     * 调用 DashScope 兼容模式流式 chat completions，并把 delta.content 增量推给调用方。
     */
    @Override
    public String completeStream(String systemPrompt, String userPrompt, Consumer<String> onDelta)
    {
        String apiKey = resolveConfigValue("egg.ai.model-gateway.text.api-key", ENV_DASHSCOPE_API_KEY, "");
        if (StringUtils.isBlank(apiKey))
        {
            throw new ServiceException("DashScope API Key 未配置：请配置 egg.ai.model-gateway.text.api-key 或 EGG_DASHSCOPE_API_KEY");
        }

        String model = resolveConfigValue("egg.ai.model-gateway.text.model", ENV_TEXT_MODEL, DEFAULT_MODEL);
        String endpoint = StringUtils.removeEnd(resolveConfigValue("egg.ai.model-gateway.text.endpoint",
                ENV_DASHSCOPE_ENDPOINT, DEFAULT_ENDPOINT), "/");
        int timeoutSeconds = environment.getProperty("egg.ai.model-gateway.timeout-seconds", Integer.class, 120);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/chat/completions"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(model, systemPrompt, userPrompt, true)
                        .toJSONString()))
                .build();

        try
        {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("DashScope 调用失败：" + response.statusCode());
            }
            return parseStreamContent(response.body(), onDelta);
        }
        catch (IOException e)
        {
            throw new ServiceException("DashScope 网络调用失败：" + e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("DashScope 调用被中断");
        }
    }

    /**
     * 按 Spring 配置、原始环境变量、JVM -D 参数的顺序读取模型配置。
     */
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

    /**
     * 构造 OpenAI-compatible 请求体。
     */
    private JSONObject buildRequestBody(String model, String systemPrompt, String userPrompt, boolean stream)
    {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", 0.2);
        if (stream)
        {
            body.put("stream", true);
        }

        JSONArray messages = new JSONArray();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        body.put("messages", messages);
        return body;
    }

    private JSONObject message(String role, String content)
    {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", StringUtils.defaultString(content));
        return message;
    }

    /**
     * 从兼容模式响应中解析第一条 assistant 消息正文。
     */
    private String parseContent(String responseBody)
    {
        JSONObject json = JSONObject.parseObject(responseBody);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            throw new ServiceException("DashScope 未返回候选内容");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message == null ? null : message.getString("content");
        if (StringUtils.isBlank(content))
        {
            throw new ServiceException("DashScope 返回内容为空");
        }
        return content;
    }

    /**
     * 解析 OpenAI-compatible SSE 流，每个 data 事件中读取 choices[0].delta.content。
     */
    private String parseStreamContent(InputStream responseBody, Consumer<String> onDelta) throws IOException
    {
        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (!line.startsWith("data:"))
                {
                    continue;
                }
                String data = StringUtils.trim(StringUtils.removeStart(line, "data:"));
                if ("[DONE]".equals(data))
                {
                    break;
                }
                appendDeltaContent(data, answer, onDelta);
            }
        }
        if (answer.isEmpty())
        {
            throw new ServiceException("DashScope 返回内容为空");
        }
        return answer.toString();
    }

    private void appendDeltaContent(String data, StringBuilder answer, Consumer<String> onDelta)
    {
        JSONObject json = JSONObject.parseObject(data);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            return;
        }
        JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
        String content = delta == null ? null : delta.getString("content");
        if (StringUtils.isEmpty(content))
        {
            return;
        }
        answer.append(content);
        if (onDelta != null)
        {
            onDelta.accept(content);
        }
    }
}
