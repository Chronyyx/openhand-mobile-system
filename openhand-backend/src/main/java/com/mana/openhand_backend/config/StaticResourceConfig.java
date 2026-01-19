package com.mana.openhand_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private final String urlPath;
    private final Path storageDir;

    public StaticResourceConfig(
            @Value("${openhand.app.profile-images.url-path:/uploads/profile-pictures}") String urlPath,
            @Value("${openhand.app.profile-images.dir:./uploads/profile-pictures}") String storageDir) {
        this.urlPath = normalizeUrlPath(urlPath);
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlPath + "/**")
                .addResourceLocations(storageDir.toUri().toString());
    }

    private static String normalizeUrlPath(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return "/uploads/profile-pictures";
        }
        String trimmed = urlPath.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
