package com.uv.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para que el frontend pueda usar fetch, y registro del interceptor del token.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final boolean requerirToken;
    private final String[] origenes;

    public WebConfig(AuthInterceptor authInterceptor,
            @Value("${app.auth.requerir-token:true}") boolean requerirToken,
            @Value("${app.cors.origenes}") String[] origenes) {
        this.authInterceptor = authInterceptor;
        this.requerirToken = requerirToken;
        this.origenes = origenes;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origenes)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .exposedHeaders("Location");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (requerirToken) {
            registry.addInterceptor(authInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns("/api/auth/login", "/api/auth/registro");
        }
    }
}
