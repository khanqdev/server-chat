package com.example.chat_server.config;

import com.example.chat_server.security.ratelimit.RateLimitFilter;
import com.example.chat_server.security.ratelimit.RateLimitProperties;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    // Registered ahead of the Spring Security chain so flooded requests are dropped before any auth work
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitProperties properties,
                                                                   ObjectMapper objectMapper) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(properties, objectMapper));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
