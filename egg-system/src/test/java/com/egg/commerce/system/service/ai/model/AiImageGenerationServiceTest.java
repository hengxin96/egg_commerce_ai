package com.egg.commerce.system.service.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.model.impl.AiDashScopeImageGenerationServiceImpl;
import com.egg.commerce.system.service.ai.model.impl.AiMockImageGenerationServiceImpl;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** 图片生成网关测试，确保业务层依赖稳定接口。 */
class AiImageGenerationServiceTest
{
    @Test
    void imageGenerationRequestAndResultKeepRuntimeFields()
    {
        AiImageGenerationRequest request = new AiImageGenerationRequest();
        request.setStage("生成中");
        request.setPositivePrompt("clean product image");
        request.setNegativePrompt("watermark");
        request.setReferenceImageUrls(List.of("https://oss/source.png"));
        request.setAspectRatio("3:4");
        request.setQuality("2K");
        request.setImageCount(3);
        request.setSequential(false);
        request.setProvider("dashscope");
        request.setModel("wan2.7-image-pro");

        assertEquals("生成中", request.getStage());
        assertEquals("clean product image", request.getPositivePrompt());
        assertEquals("watermark", request.getNegativePrompt());
        assertEquals(List.of("https://oss/source.png"), request.getReferenceImageUrls());
        assertEquals("3:4", request.getAspectRatio());
        assertEquals("2K", request.getQuality());
        assertEquals(3, request.getImageCount());
        assertEquals(false, request.getSequential());
        assertEquals("dashscope", request.getProvider());
        assertEquals("wan2.7-image-pro", request.getModel());
    }

    @Test
    void mockImageGenerationReturnsDeterministicResult()
    {
        AiImageGenerationRequest request = new AiImageGenerationRequest();
        request.setStage("生成中");
        request.setPositivePrompt("clean product image");
        request.setNegativePrompt("watermark");
        request.setReferenceImageUrls(List.of("https://oss/source.png"));
        request.setAspectRatio("3:4");
        request.setQuality("2K");

        AiImageGenerationResult result = new AiMockImageGenerationServiceImpl().generate(request);

        assertEquals("mock", result.getProvider());
        assertEquals("mock-image-model", result.getModel());
        assertEquals("mock-image-task", result.getTaskId());
        assertEquals(1, result.getImageUrls().size());
        assertEquals("https://example.invalid/mock-product-image.png", result.getImageUrls().get(0));
    }

    @Test
    void dashScopeImageGenerationFailsLoudlyOnUnknownStatus()
    {
        AiDashScopeImageGenerationServiceImpl service = new AiDashScopeImageGenerationServiceImpl(environment(),
                new QueueHttpClient(List.of(
                        "{\"output\":{\"task_id\":\"task-1\"}}",
                        "{\"output\":{\"task_status\":\"STRANGE\"}}")));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.generate(request()));

        assertEquals(true, exception.getMessage().contains("未知状态"));
    }

    @Test
    void dashScopeImageGenerationExtractsChoiceImageUrl()
    {
        QueueHttpClient httpClient = new QueueHttpClient(List.of(
                        "{\"output\":{\"task_id\":\"task-1\"}}",
                        "{\"output\":{\"task_status\":\"SUCCEEDED\",\"choices\":[{\"message\":{\"content\":[{\"image\":\"https://oss/generated.png\"}]}}]}}"));
        AiDashScopeImageGenerationServiceImpl service = new AiDashScopeImageGenerationServiceImpl(environment(),
                httpClient);

        AiImageGenerationResult result = service.generate(request());

        assertEquals(List.of("https://oss/generated.png"), result.getImageUrls());
        assertEquals(true, httpClient.timeouts.stream().allMatch(timeout -> timeout.toMillis() <= 5000L));
    }

    @Test
    void dashScopeImageGenerationSubmitsWan27MessagesBodyWithAllReferenceImages()
    {
        QueueHttpClient httpClient = new QueueHttpClient(List.of(
                "{\"output\":{\"task_id\":\"task-1\"}}",
                "{\"output\":{\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://oss/generated-1.png\"},{\"url\":\"https://oss/generated-2.png\"}]}}"));
        AiDashScopeImageGenerationServiceImpl service = new AiDashScopeImageGenerationServiceImpl(environment(),
                httpClient);
        AiImageGenerationRequest request = request();
        request.setNegativePrompt("no watermark");
        request.setReferenceImageUrls(List.of("https://oss/source-1.png", "https://oss/source-2.png"));
        request.setImageCount(2);
        request.setSequential(false);

        service.generate(request);

        assertEquals("https://dashscope.test/api/v1/services/aigc/image-generation/generation",
                httpClient.requests.get(0).uri().toString());
        String body = httpClient.requestBodies.get(0);
        assertFalse(body.contains("ref_img"));
        assertFalse(body.contains("reference_image_urls"));
        assertFalse(body.contains("negative_prompt"));
        assertFalse(body.contains("prompt_extend"));
        JSONObject json = JSONObject.parseObject(body);
        assertEquals("wan2.7-image-pro", json.getString("model"));
        JSONArray content = json.getJSONObject("input").getJSONArray("messages")
                .getJSONObject(0).getJSONArray("content");
        assertEquals(true, content.getJSONObject(0).getString("text").contains("clean product image"));
        assertEquals(true, content.getJSONObject(0).getString("text").contains("no watermark"));
        assertEquals("https://oss/source-1.png", content.getJSONObject(1).getString("image"));
        assertEquals("https://oss/source-2.png", content.getJSONObject(2).getString("image"));
        JSONObject parameters = json.getJSONObject("parameters");
        assertEquals(2, parameters.getIntValue("n"));
        assertEquals(false, parameters.getBooleanValue("enable_sequential"));
        assertEquals(true, parameters.containsKey("watermark"));
        assertEquals(false, parameters.getBooleanValue("watermark"));
    }

    @Test
    void dashScopeImageGenerationUsesQualityAware2KSize()
    {
        QueueHttpClient httpClient = new QueueHttpClient(List.of(
                "{\"output\":{\"task_id\":\"task-1\"}}",
                "{\"output\":{\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://oss/generated.png\"}]}}"));
        AiDashScopeImageGenerationServiceImpl service = new AiDashScopeImageGenerationServiceImpl(environment(),
                httpClient);
        AiImageGenerationRequest request = request();
        request.setAspectRatio("3:4");
        request.setQuality("2K");

        service.generate(request);

        JSONObject json = JSONObject.parseObject(httpClient.requestBodies.get(0));
        assertEquals("1536*2048", json.getJSONObject("parameters").getString("size"));
    }

    private AiImageGenerationRequest request()
    {
        AiImageGenerationRequest request = new AiImageGenerationRequest();
        request.setPositivePrompt("clean product image");
        request.setAspectRatio("3:4");
        return request;
    }

    private MockEnvironment environment()
    {
        return new MockEnvironment()
                .withProperty("egg.ai.model-gateway.image.api-key", "sk-test")
                .withProperty("egg.ai.model-gateway.image.provider", "dashscope")
                .withProperty("egg.ai.model-gateway.image.model", "wan2.7-image-pro")
                .withProperty("egg.ai.model-gateway.image.endpoint", "https://dashscope.test")
                .withProperty("egg.ai.model-gateway.timeout-seconds", "5")
                .withProperty("egg.ai.model-gateway.image.poll-interval-millis", "0")
                .withProperty("egg.ai.model-gateway.image.max-polls", "2");
    }

    private static class QueueHttpClient extends HttpClient
    {
        private final List<String> responses;
        private final List<Duration> timeouts = new ArrayList<>();
        private final List<HttpRequest> requests = new ArrayList<>();
        private final List<String> requestBodies = new ArrayList<>();
        private int index;

        QueueHttpClient(List<String> responses)
        {
            this.responses = responses;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException
        {
            requests.add(request);
            requestBodies.add(readBody(request));
            request.timeout().ifPresent(timeouts::add);
            String body = responses.get(index++);
            @SuppressWarnings("unchecked")
            T castBody = (T) body;
            return new SimpleHttpResponse<>(request, castBody);
        }

        private String readBody(HttpRequest request)
        {
            return request.bodyPublisher()
                    .map(BodyCollector::collect)
                    .orElse("");
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<java.net.CookieHandler> cookieHandler()
        {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout()
        {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects()
        {
            return Redirect.NEVER;
        }

        @Override
        public Optional<java.net.ProxySelector> proxy()
        {
            return Optional.empty();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext()
        {
            return null;
        }

        @Override
        public javax.net.ssl.SSLParameters sslParameters()
        {
            return null;
        }

        @Override
        public Optional<java.net.Authenticator> authenticator()
        {
            return Optional.empty();
        }

        @Override
        public Version version()
        {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<java.util.concurrent.Executor> executor()
        {
            return Optional.empty();
        }
    }

    private static class BodyCollector implements Flow.Subscriber<ByteBuffer>
    {
        private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        private final CompletableFuture<String> result = new CompletableFuture<>();

        static String collect(HttpRequest.BodyPublisher publisher)
        {
            BodyCollector collector = new BodyCollector();
            publisher.subscribe(collector);
            return collector.result.join();
        }

        @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
        @Override public void onNext(ByteBuffer item)
        {
            byte[] chunk = new byte[item.remaining()];
            item.get(chunk);
            bytes.writeBytes(chunk);
        }
        @Override public void onError(Throwable throwable) { result.completeExceptionally(throwable); }
        @Override public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
    }

    private static class SimpleHttpResponse<T> implements HttpResponse<T>
    {
        private final HttpRequest request;
        private final T body;

        SimpleHttpResponse(HttpRequest request, T body)
        {
            this.request = request;
            this.body = body;
        }

        @Override public int statusCode() { return 200; }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(java.util.Map.of(), (key, value) -> true); }
        @Override public T body() { return body; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
