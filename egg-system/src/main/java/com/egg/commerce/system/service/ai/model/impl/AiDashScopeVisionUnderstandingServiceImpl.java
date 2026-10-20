package com.egg.commerce.system.service.ai.model.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingRequest;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingResult;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * DashScope OpenAI-compatible 视觉理解实现。
 *
 * <p>阿里兼容接口把 system prompt 作为 messages 里的 system 角色消息传入；图片使用
 * {@code image_url} content item，不传自造的顶层 systemPrompt 或本地文件路径。</p>
 */
@Service
@ConditionalOnProperty(name = "egg.ai.model-gateway.vision.provider", havingValue = "dashscope", matchIfMissing = true)
public class AiDashScopeVisionUnderstandingServiceImpl implements AiVisionUnderstandingService
{
    private static final String DEFAULT_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String DEFAULT_MODEL = "qwen3.7-plus";
    private static final String ENV_DASHSCOPE_API_KEY = "EGG_DASHSCOPE_API_KEY";
    private static final String ENV_DASHSCOPE_COMPAT_ENDPOINT = "EGG_DASHSCOPE_COMPAT_ENDPOINT";
    private static final String ENV_VISION_PROVIDER = "EGG_MODEL_VISION_PROVIDER";
    private static final String ENV_VISION_MODEL = "EGG_MODEL_VISION_NAME";

    private final Environment environment;
    private final HttpClient httpClient;

    @Autowired
    public AiDashScopeVisionUnderstandingServiceImpl(Environment environment)
    {
        this(environment, HttpClient.newHttpClient());
    }

    public AiDashScopeVisionUnderstandingServiceImpl(Environment environment, HttpClient httpClient)
    {
        this.environment = environment;
        this.httpClient = httpClient;
    }

    @Override
    public AiVisionUnderstandingResult analyze(AiVisionUnderstandingRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getPrompt()))
        {
            throw new ServiceException("视觉理解提示词不能为空");
        }
        if (request.getImageUrls() == null || request.getImageUrls().isEmpty())
        {
            throw new ServiceException("视觉理解图片不能为空");
        }

        ModelConfig config = modelConfig(request);
        JSONObject body = buildRequestBody(config.model(), request);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(config.endpoint() + "/chat/completions"))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();

        try
        {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("DashScope 视觉理解调用失败：" + response.statusCode());
            }
            String content = parseContent(response.body());
            AiVisionUnderstandingResult result = new AiVisionUnderstandingResult();
            result.setProvider(config.provider());
            result.setModel(config.model());
            result.setContent(content);
            result.setRawResponse(response.body());
            return result;
        }
        catch (IOException e)
        {
            throw new ServiceException("DashScope 视觉理解网络调用失败：" + e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("DashScope 视觉理解调用被中断");
        }
    }

    private JSONObject buildRequestBody(String model, AiVisionUnderstandingRequest request)
    {
        JSONObject body = new JSONObject();
        body.put("model", model);
        if (Boolean.TRUE.equals(request.getJsonResponse()))
        {
            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object");
            body.put("response_format", responseFormat);
        }
        if (Boolean.TRUE.equals(request.getHighResolution()))
        {
            body.put("vl_high_resolution_images", true);
        }

        JSONArray messages = new JSONArray();
        if (StringUtils.isNotBlank(request.getSystemPrompt()))
        {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", request.getSystemPrompt());
            messages.add(system);
        }
        JSONObject user = new JSONObject();
        user.put("role", "user");
        JSONArray content = new JSONArray();
        for (String imageUrl : request.getImageUrls())
        {
            if (StringUtils.isBlank(imageUrl))
            {
                continue;
            }
            JSONObject image = new JSONObject();
            image.put("type", "image_url");
            JSONObject imageUrlObject = new JSONObject();
            imageUrlObject.put("url", imageUrl);
            image.put("image_url", imageUrlObject);
            content.add(image);
        }
        JSONObject text = new JSONObject();
        text.put("type", "text");
        text.put("text", request.getPrompt());
        content.add(text);
        user.put("content", content);
        messages.add(user);
        body.put("messages", messages);
        return body;
    }

    private String parseContent(String responseBody)
    {
        JSONObject json = JSONObject.parseObject(responseBody);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            throw new ServiceException("DashScope 视觉理解未返回候选内容");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message == null ? null : message.getString("content");
        if (StringUtils.isBlank(content))
        {
            throw new ServiceException("DashScope 视觉理解返回内容为空");
        }
        return content;
    }

    private ModelConfig modelConfig(AiVisionUnderstandingRequest request)
    {
        String apiKey = resolveConfigValue("egg.ai.model-gateway.vision.api-key", ENV_DASHSCOPE_API_KEY, "");
        if (StringUtils.isBlank(apiKey))
        {
            throw new ServiceException("DashScope 视觉理解 API Key 未配置");
        }
        String provider = resolveConfigValue("egg.ai.model-gateway.vision.provider", ENV_VISION_PROVIDER, "dashscope");
        String model = StringUtils.defaultIfBlank(request.getModel(),
                resolveConfigValue("egg.ai.model-gateway.vision.model", ENV_VISION_MODEL, DEFAULT_MODEL));
        String endpoint = StringUtils.removeEnd(resolveConfigValue("egg.ai.model-gateway.vision.endpoint",
                ENV_DASHSCOPE_COMPAT_ENDPOINT, DEFAULT_ENDPOINT), "/");
        int timeoutSeconds = environment.getProperty("egg.ai.model-gateway.timeout-seconds", Integer.class, 240);
        return new ModelConfig(apiKey, provider, model, endpoint, timeoutSeconds);
    }

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

    private record ModelConfig(String apiKey, String provider, String model, String endpoint, int timeoutSeconds)
    {
    }
}
