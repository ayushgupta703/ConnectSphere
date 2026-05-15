package com.connectsphere.searchservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {

        return template -> {

            RequestAttributes attributes =
                    RequestContextHolder.getRequestAttributes();

            if (attributes instanceof ServletRequestAttributes servletAttributes) {

                HttpServletRequest request =
                        servletAttributes.getRequest();

                String authHeader =
                        request.getHeader("Authorization");

                if (authHeader != null &&
                        authHeader.startsWith("Bearer ")) {

                    template.header("Authorization", authHeader);
                }
            }
        };
    }
}