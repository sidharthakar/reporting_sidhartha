package io.allure.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Expose /storage/** URLs to the file system path "storage/"
        registry.addResourceHandler("/storage/**")
                .addResourceLocations("file:storage/")
                .setCachePeriod(0);
    }
}
