package com.ginkgooai.core.identity.config.mvc;

import com.ginkgooai.core.common.interceptor.ContextsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcInterceptor implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ContextsInterceptor())
            .excludePathPatterns(
                "/api/project/v3/api-docs/**",
                "/api/project/swagger-ui/**",
                "/webjars/**"
            );
    }

}