package com.neogaming.config;

import com.neogaming.ai.client.AIServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class AIServiceConfig {

    private final AIServiceProperties aiServiceProperties;

    @Bean("aiServiceRestClient")
    public RestClient aiServiceRestClient() {
        return RestClient.builder()
                .baseUrl(aiServiceProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
