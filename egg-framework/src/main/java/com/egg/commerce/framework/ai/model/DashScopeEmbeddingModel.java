package com.egg.commerce.framework.ai.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Minimal DashScope embedding adapter used by Spring AI vector stores.
 */
public class DashScopeEmbeddingModel implements EmbeddingModel
{
    private static final String DEFAULT_ENDPOINT =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

    private static final String DEFAULT_MODEL = "text-embedding-v4";

    private final HttpClient httpClient;

    private final String endpoint;

    private final String apiKey;

    private final String model;

    private final int dimensions;

    public DashScopeEmbeddingModel(String endpoint, String apiKey, String model, int dimensions)
    {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.endpoint = StringUtils.defaultIfBlank(endpoint, DEFAULT_ENDPOINT);
        this.apiKey = apiKey;
        this.model = StringUtils.defaultIfBlank(model, DEFAULT_MODEL);
        this.dimensions = dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request)
    {
        List<String> texts = request == null ? List.of() : request.getInstructions();
        if (texts == null || texts.isEmpty())
        {
            return new EmbeddingResponse(List.of());
        }
        if (StringUtils.isBlank(apiKey))
        {
            throw new IllegalStateException("DashScope embedding api key is missing. Set EGG_DASHSCOPE_API_KEY.");
        }

        JSONObject payload = buildPayload(texts, request.getOptions());
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString(), StandardCharsets.UTF_8))
                .build();

        try
        {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new IllegalStateException("DashScope embedding call failed with HTTP " + response.statusCode()
                        + ": " + StringUtils.abbreviate(response.body(), 500));
            }
            return parseResponse(response.body(), texts.size());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("DashScope embedding call failed because the remote service was unreachable.", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DashScope embedding call was interrupted.", e);
        }
    }

    @Override
    public float[] embed(Document document)
    {
        return embed(StringUtils.defaultString(getEmbeddingContent(document)));
    }

    @Override
    public int dimensions()
    {
        return dimensions > 0 ? dimensions : EmbeddingModel.super.dimensions();
    }

    private JSONObject buildPayload(List<String> texts, EmbeddingOptions options)
    {
        String requestModel = options == null ? null : options.getModel();
        Integer requestDimensions = options == null ? null : options.getDimensions();

        JSONObject input = new JSONObject();
        input.put("texts", texts);

        JSONObject payload = new JSONObject();
        payload.put("model", StringUtils.defaultIfBlank(requestModel, model));
        payload.put("input", input);

        int effectiveDimensions = requestDimensions == null ? dimensions : requestDimensions;
        JSONObject parameters = new JSONObject();
        // 知识库入库使用文档向量类型，避免 DashScope 按查询短文本参数处理长文档切片。
        parameters.put("text_type", "document");
        // Knowledge chunks are stored as document embeddings, not short query embeddings.
        parameters.put("text_type", "document");
        if (effectiveDimensions > 0)
        {
            parameters.put("dimension", effectiveDimensions);
        }
        payload.put("parameters", parameters);
        return payload;
    }

    private EmbeddingResponse parseResponse(String body, int expectedCount)
    {
        JSONObject root = JSON.parseObject(body);
        JSONObject output = root == null ? null : root.getJSONObject("output");
        JSONArray embeddings = output == null ? null : output.getJSONArray("embeddings");
        if (embeddings == null)
        {
            throw new IllegalStateException("DashScope embedding response did not include output.embeddings.");
        }

        List<Embedding> ordered = new ArrayList<>();
        for (int i = 0; i < expectedCount; i++)
        {
            ordered.add(null);
        }

        for (int i = 0; i < embeddings.size(); i++)
        {
            JSONObject item = embeddings.getJSONObject(i);
            Integer textIndex = item.getInteger("text_index");
            int index = textIndex == null ? i : textIndex;
            if (index < 0 || index >= expectedCount)
            {
                throw new IllegalStateException("DashScope embedding response contained an invalid text_index: " + index);
            }
            ordered.set(index, new Embedding(toFloatArray(item.getJSONArray("embedding")), index));
        }

        if (ordered.stream().anyMatch(item -> item == null))
        {
            throw new IllegalStateException("DashScope embedding response count did not match the request count.");
        }
        return new EmbeddingResponse(ordered);
    }

    private float[] toFloatArray(JSONArray values)
    {
        if (values == null)
        {
            throw new IllegalStateException("DashScope embedding item did not include an embedding vector.");
        }
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++)
        {
            Object value = values.get(i);
            if (!(value instanceof Number number))
            {
                throw new IllegalStateException("DashScope embedding vector contained a non-numeric value.");
            }
            vector[i] = number.floatValue();
        }
        return vector;
    }
}
