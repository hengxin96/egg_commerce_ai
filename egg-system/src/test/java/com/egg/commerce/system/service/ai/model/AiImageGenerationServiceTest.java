package com.egg.commerce.system.service.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.model.impl.AiDashScopeImageGenerationServiceImpl;
import com.egg.commerce.system.service.ai.model.impl.AiMockImageGenerationServiceImpl;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
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
        request.setProvider("dashscope");
        request.setModel("wan2.7-image-pro");

        assertEquals("生成中", request.getStage());
        assertEquals("clean product image", request.getPositivePrompt());
        assertEquals("watermark", request.getNegativePrompt());
        assertEquals(List.of("https://oss/source.png"), request.getReferenceImageUrls());
        assertEquals("3:4", request.getAspectRatio());
        assertEquals("2K", request.getQuality());
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
        private int index;

        QueueHttpClient(List<String> responses)
        {
            this.responses = responses;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException
        {
            request.timeout().ifPresent(timeouts::add);
            String body = responses.get(index++);
            @SuppressWarnings("unchecked")
            T castBody = (T) body;
            return new SimpleHttpResponse<>(request, castBody);
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
