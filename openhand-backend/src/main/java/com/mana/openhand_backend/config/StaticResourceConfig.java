package com.mana.openhand_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${openhand.app.profilePicturesDir:uploads/profile-pictures}")
    private String profilePicturesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(profilePicturesDir).toAbsolutePath().normalize();
        String location = "file:" + uploadPath.toString() + "/";
        registry.addResourceHandler("/uploads/profile-pictures/**")
                .addResourceLocations(location);
    }
}
