package com.sap.cloudwatch.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final long READ_TIMEOUT_MS = 50_000L;
    private static final long WRITE_TIMEOUT_MS = 5_000L;

    @Bean
    @Scope("prototype")
    public WebClient defaultWebClient(@Autowired HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(getCodecMemorySizeExchangeStrategy())
            .build();
    }

    @Bean
    @Scope("prototype")
    public HttpClient defaultHttpClient() {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
            .responseTimeout(Duration.ofMillis(READ_TIMEOUT_MS))
            .doOnConnected(connection ->
                connection
                    .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
            );
    }

    @Bean
    @Scope("prototype")
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .filter((request, next) -> {
                logRequest(request);
                return next
                    .exchange(interceptRequestBody(request))
                    .doOnNext(this::logResponse)
                    .map(this::interceptResponseBody);
            })
            .exchangeStrategies(getCodecMemorySizeExchangeStrategy());
    }


    private ClientRequest interceptRequestBody(ClientRequest request) {
        return ClientRequest.from(request)
            .body((outputMessage, context) -> request.body().insert(new ClientHttpRequestDecorator(outputMessage) {
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    return super.writeWith(Mono.from(body)
                        .doOnNext(dataBuffer -> logRequestBody(dataBuffer)));
                }
            }, context))
            .build();
    }

    private ClientResponse interceptResponseBody(ClientResponse response) {
        return response.mutate()
            .body(data -> data.doOnNext(this::logResponseBody))
            .build();
    }

    private void logRequest(ClientRequest request) {
        try {
            MDC.put("http_method", request.method().toString());
            MDC.put("request_url", request.url().toString());
            MDC.put("request_headers", request.headers().toString());
            log.info("logRequest");
        } finally {
            MDC.remove("http_method");
            MDC.remove("request_url");
            MDC.remove("request_headers");
        }
    }

    private void logRequestBody(DataBuffer dataBuffer) {
        try {
            MDC.put("request_body", toPrettyJson(dataBuffer.toString(StandardCharsets.UTF_8)));
            log.info("logRequestBody");
        } finally {
            MDC.remove("request_body");
        }
    }

    private void logResponse(ClientResponse response) {
        try {
            MDC.put("http_status", String.valueOf(response.statusCode().value()));
            MDC.put("response_headers", response.headers().asHttpHeaders().toString());
            log.info("logResponse");
        } finally {
            MDC.remove("http_status");
            MDC.remove("response_headers");
        }
    }

    private void logResponseBody(DataBuffer dataBuffer) {
        try {
            MDC.put("response_body", toPrettyJson(dataBuffer.toString(StandardCharsets.UTF_8)));
            log.info("logResponseBody");
        } finally {
            MDC.remove("response_body");
        }
    }

    private String toPrettyJson(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (java.lang.Exception e) {
            // JSON 문자열이 아님
        }
        return jsonString;
    }

    private ExchangeStrategies getCodecMemorySizeExchangeStrategy() {
        return ExchangeStrategies.builder()
            .codecs(configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB
            )
            .build();
    }
}
