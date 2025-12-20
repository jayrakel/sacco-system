package com.sacco.sacco_system.modules.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files (logos, favicons, profile pictures, etc.)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}

