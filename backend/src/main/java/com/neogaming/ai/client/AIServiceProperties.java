package com.neogaming.ai.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai-service")
@Getter
@Setter
public class AIServiceProperties {
    private String baseUrl;
    private String internalToken;
    private int timeoutSeconds = 45;
}
