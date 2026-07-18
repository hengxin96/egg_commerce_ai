package com.egg.commerce.framework.ai.model;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashScopeEmbeddingModelTest
{
    private HttpServer server;

    @AfterEach
    void stopServer()
    {
        if (server != null)
        {
            server.stop(0);
        }
    }

    @Test
    void callsDashScopeEmbeddingEndpointAndKeepsVectorOrder() throws Exception
    {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/embedding", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, """
                    {
                      "output": {
                        "embeddings": [
                          {"text_index": 1, "embedding": [0.3, 0.4]},
                          {"text_index": 0, "embedding": [0.1, 0.2]}
                        ]
                      }
                    }
                    """);
        });
        server.start();

        String endpoint = "http://localhost:" + server.getAddress().getPort() + "/embedding";
        DashScopeEmbeddingModel model = new DashScopeEmbeddingModel(endpoint, "test-key", "text-embedding-v4", 1536);

        EmbeddingResponse response = model.call(new EmbeddingRequest(List.of("hello", "world"),
                EmbeddingOptions.builder().build()));

        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        assertThat(requestBody.get()).contains("\"model\":\"text-embedding-v4\"");
        assertThat(requestBody.get()).contains("\"texts\":[\"hello\",\"world\"]");
        assertThat(requestBody.get()).contains("\"text_type\":\"document\"");
        assertThat(requestBody.get()).contains("\"dimension\":1536");
        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getOutput()).containsExactly(0.1F, 0.2F);
        assertThat(response.getResults().get(1).getOutput()).containsExactly(0.3F, 0.4F);
        assertThat(model.dimensions()).isEqualTo(1536);
    }

    @Test
    void failsFastWhenApiKeyIsMissing()
    {
        DashScopeEmbeddingModel model = new DashScopeEmbeddingModel("http://localhost:18080/embedding", "", "text-embedding-v4", 1536);

        assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of("hello"), EmbeddingOptions.builder().build())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EGG_DASHSCOPE_API_KEY");
    }

    private void respond(HttpExchange exchange, String body) throws IOException
    {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
