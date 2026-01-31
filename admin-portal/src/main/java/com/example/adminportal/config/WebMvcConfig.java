package com.example.adminportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for serving static resources and handling route forwarding.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Forward root URL to index.html to support SPA routing.
     * Spring Boot automatically serves static resources from classpath:/static/
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
