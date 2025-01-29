package org.microspring.webflux;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Represents a server-side HTTP request
 */
public class ReactiveServerRequest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpMethod method;
    private final URI uri;
    private final HttpHeaders headers;
    private final Mono<String> body;

    public ReactiveServerRequest(HttpMethod method, URI uri, HttpHeaders headers, Mono<String> body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public URI getUri() {
        return uri;
        }
        
    public HttpHeaders getHeaders() {
        return headers;
    }

    public Mono<String> getBody() {
        return body;
    }

    /**
     * Convert the request body to a specified class type
     * @param clazz The target class type
     * @return A Mono containing the converted object
     * @param <T> The type parameter
     */
    public <T> Mono<T> bodyToObject(Class<T> clazz) {
        return body.map(content -> {
            try {
                return objectMapper.readValue(content, clazz);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse JSON", e);
            }
        });
    }
} 