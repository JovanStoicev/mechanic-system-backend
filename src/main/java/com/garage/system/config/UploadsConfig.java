package com.garage.system.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class UploadsConfig {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @PostConstruct
    public void ensureUploadsDir() throws Exception {
        Files.createDirectories(Path.of(uploadsDir));
    }
}
