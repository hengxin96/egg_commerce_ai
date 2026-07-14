package com.egg.commerce.system.service.ai.model.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.model.AiImageGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiImageGenerationResult;
import com.egg.commerce.system.service.ai.model.AiImageGenerationService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * DashScope / 通义万相图片生成实现。
 *
 * <p>业务服务只依赖 {@link AiImageGenerationService}。该类封装 DashScope 异步任务提交、轮询和响应解析。</p>
 */
@Service
@ConditionalOnProperty(name = "egg.ai.model-gateway.image.provider", havingValue = "dashscope", matchIfMissing = true)
public class AiDashScopeImageGenerationServiceImpl implements AiImageGenerationService
{
    private static final String DEFAULT_ENDPOINT = "https://dashscope.aliyuncs.com";

    private static final String DEFAULT_MODEL = "wan2.7-image-pro";

    private static final String ENV_DASHSCOPE_API_KEY = "EGG_DASHSCOPE_API_KEY";

    private static final String ENV_DASHSCOPE_ENDPOINT = "EGG_DASHSCOPE_ENDPOINT";

    private static final String ENV_IMAGE_PROVIDER = "EGG_MODEL_IMAGE_PROVIDER";

    private static final String ENV_IMAGE_MODEL = "EGG_MODEL_IMAGE_NAME";

    private final Environment environment;

    private final HttpClient httpClient;

    @Autowired
    public AiDashScopeImageGenerationServiceImpl(Environment environment)
    {
        this(environment, HttpClient.newHttpClient());
    }

    public AiDashScopeImageGenerationServiceImpl(Environment environment, HttpClient httpClient)
    {
        this.environment = environment;
        this.httpClient = httpClient;
    }

    @Override
    public AiImageGenerationResult generate(AiImageGenerationRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getPositivePrompt()))
        {
            throw new ServiceException("图片生成提示词不能为空");
        }

        String apiKey = resolveConfigValue("egg.ai.model-gateway.image.api-key", ENV_DASHSCOPE_API_KEY, "");
        if (StringUtils.isBlank(apiKey))
        {
            throw new ServiceException("DashScope API Key 未配置");
        }

        String provider = resolveConfigValue("egg.ai.model-gateway.image.provider", ENV_IMAGE_PROVIDER, "dashscope");
        String model = StringUtils.defaultIfBlank(request.getModel(),
                resolveConfigValue("egg.ai.model-gateway.image.model", ENV_IMAGE_MODEL, DEFAULT_MODEL));
        String endpoint = StringUtils.removeEnd(resolveConfigValue("egg.ai.model-gateway.image.endpoint",
                ENV_DASHSCOPE_ENDPOINT, DEFAULT_ENDPOINT), "/");
        int timeoutSeconds = environment.getProperty("egg.ai.model-gateway.timeout-seconds", Integer.class, 240);
        int pollIntervalMillis = environment.getProperty("egg.ai.model-gateway.image.poll-interval-millis",
                Integer.class, 3000);
        int maxPolls = environment.getProperty("egg.ai.model-gateway.image.max-polls", Integer.class, 80);
        long deadlineMillis = System.currentTimeMillis() + timeoutSeconds * 1000L;

        JSONObject submitResponse = submitTask(endpoint, apiKey, model, request, remainingDuration(deadlineMillis));
        String taskId = extractTaskId(submitResponse);
        JSONObject finalResponse = pollTask(endpoint, apiKey, taskId, deadlineMillis, pollIntervalMillis, maxPolls);

        AiImageGenerationResult result = new AiImageGenerationResult();
        result.setProvider(provider);
        result.setModel(model);
        result.setTaskId(taskId);
        result.setImageUrls(extractImageUrls(finalResponse));
        result.setRawResponse(finalResponse.toJSONString());
        return result;
    }

    private JSONObject submitTask(String endpoint, String apiKey, String model, AiImageGenerationRequest request,
            Duration timeout)
    {
        JSONObject body = new JSONObject();
        body.put("model", model);
        JSONObject input = new JSONObject();
        input.put("prompt", request.getPositivePrompt());
        if (StringUtils.isNotBlank(request.getNegativePrompt()))
        {
            input.put("negative_prompt", request.getNegativePrompt());
        }
        if (request.getReferenceImageUrls() != null && !request.getReferenceImageUrls().isEmpty())
        {
            input.put("ref_img", request.getReferenceImageUrls().get(0));
            input.put("reference_image_urls", request.getReferenceImageUrls());
        }
        body.put("input", input);

        JSONObject parameters = new JSONObject();
        String size = resolveSize(request.getAspectRatio());
        if (StringUtils.isNotBlank(size))
        {
            parameters.put("size", size);
        }
        if (StringUtils.isNotBlank(request.getQuality()))
        {
            parameters.put("quality", request.getQuality());
        }
        parameters.put("n", 1);
        body.put("parameters", parameters);

        HttpRequest httpRequest = HttpRequest.newBuilder(
                URI.create(endpoint + "/api/v1/services/aigc/text2image/image-synthesis"))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();
        return sendJson(httpRequest, "DashScope 图片生成任务提交失败");
    }

    private JSONObject pollTask(String endpoint, String apiKey, String taskId, long deadlineMillis,
            int pollIntervalMillis, int maxPolls)
    {
        for (int index = 0; index < maxPolls; index++)
        {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint + "/api/v1/tasks/" + taskId))
                    .timeout(remainingDuration(deadlineMillis))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            JSONObject response = sendJson(httpRequest, "DashScope 图片生成任务查询失败");
            String status = taskStatus(response);
            if ("SUCCEEDED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status))
            {
                return response;
            }
            if ("FAILED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)
                    || "UNKNOWN".equalsIgnoreCase(status))
            {
                throw new ServiceException("DashScope 图片生成失败：" + status);
            }
            if (!isRunningStatus(status))
            {
                throw new ServiceException("DashScope 图片生成返回未知状态：" + status);
            }
            sleep(Math.min(pollIntervalMillis, Math.max(0L, deadlineMillis - System.currentTimeMillis())));
        }
        throw new ServiceException("DashScope 图片生成超时");
    }

    private boolean isRunningStatus(String status)
    {
        return "PENDING".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status)
                || "SUSPENDED".equalsIgnoreCase(status);
    }

    private Duration remainingDuration(long deadlineMillis)
    {
        long remainingMillis = deadlineMillis - System.currentTimeMillis();
        if (remainingMillis <= 0)
        {
            throw new ServiceException("DashScope 图片生成超时");
        }
        return Duration.ofMillis(remainingMillis);
    }

    private JSONObject sendJson(HttpRequest request, String errorMessage)
    {
        try
        {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException(errorMessage + "：" + response.statusCode());
            }
            return JSONObject.parseObject(response.body());
        }
        catch (IOException e)
        {
            throw new ServiceException(errorMessage + "：" + e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException(errorMessage + "：调用被中断");
        }
    }

    private String extractTaskId(JSONObject response)
    {
        String taskId = response.getString("task_id");
        if (StringUtils.isBlank(taskId) && response.getJSONObject("output") != null)
        {
            taskId = response.getJSONObject("output").getString("task_id");
        }
        if (StringUtils.isBlank(taskId))
        {
            throw new ServiceException("DashScope 未返回图片生成任务 ID");
        }
        return taskId;
    }

    private String taskStatus(JSONObject response)
    {
        JSONObject output = response.getJSONObject("output");
        String status = output == null ? response.getString("task_status") : output.getString("task_status");
        return StringUtils.defaultString(status);
    }

    private List<String> extractImageUrls(JSONObject response)
    {
        List<String> imageUrls = new ArrayList<>();
        JSONObject output = response.getJSONObject("output");
        if (output == null)
        {
            output = response;
        }
        JSONArray results = output.getJSONArray("results");
        if (results != null)
        {
            for (int i = 0; i < results.size(); i++)
            {
                JSONObject item = results.getJSONObject(i);
                String url = item == null ? null : StringUtils.defaultIfBlank(item.getString("url"),
                        item.getString("image_url"));
                if (StringUtils.isNotBlank(url))
                {
                    imageUrls.add(url);
                }
            }
        }
        if (imageUrls.isEmpty() && StringUtils.isNotBlank(output.getString("url")))
        {
            imageUrls.add(output.getString("url"));
        }
        JSONArray choices = output.getJSONArray("choices");
        if (imageUrls.isEmpty() && choices != null)
        {
            for (int i = 0; i < choices.size(); i++)
            {
                JSONObject message = choices.getJSONObject(i).getJSONObject("message");
                JSONArray content = message == null ? null : message.getJSONArray("content");
                if (content == null)
                {
                    continue;
                }
                for (int j = 0; j < content.size(); j++)
                {
                    String image = content.getJSONObject(j).getString("image");
                    if (StringUtils.isNotBlank(image))
                    {
                        imageUrls.add(image);
                    }
                }
            }
        }
        if (imageUrls.isEmpty())
        {
            throw new ServiceException("DashScope 未返回生成图片");
        }
        return imageUrls;
    }

    private String resolveSize(String aspectRatio)
    {
        if (StringUtils.isBlank(aspectRatio))
        {
            return null;
        }
        if (aspectRatio.contains("*"))
        {
            return aspectRatio;
        }
        if ("3:4".equals(aspectRatio))
        {
            return "768*1024";
        }
        if ("4:3".equals(aspectRatio))
        {
            return "1024*768";
        }
        if ("9:16".equals(aspectRatio))
        {
            return "720*1280";
        }
        if ("16:9".equals(aspectRatio))
        {
            return "1280*720";
        }
        if ("1:1".equals(aspectRatio))
        {
            return "1024*1024";
        }
        return aspectRatio;
    }

    private void sleep(long pollIntervalMillis)
    {
        try
        {
            Thread.sleep(Math.max(0, pollIntervalMillis));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("DashScope 图片生成查询被中断");
        }
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
}
