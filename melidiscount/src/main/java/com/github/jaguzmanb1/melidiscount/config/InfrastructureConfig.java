package com.github.jaguzmanb1.melidiscount.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class InfrastructureConfig {

    /**
     * Shared, thread‑safe HTTP/2 client.
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                         .version(HttpClient.Version.HTTP_2)
                         .connectTimeout(Duration.ofSeconds(5))
                         .build();
    }

    /**
     * Pre‑configured Jackson mapper reused across the app.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
