package com.zhihuishu.autowatcher.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final String CONFIG_FILE = "config.properties";
    private final Properties properties = new Properties();

    public Config() {
        loadConfig();
    }

    private void loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        
        // If config doesn't exist, create default
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        
        // Load config
        try (InputStream input = Files.newInputStream(Paths.get(CONFIG_FILE))) {
            properties.load(input);
            logger.info("Configuration loaded successfully");
        } catch (IOException ex) {
            logger.error("Error loading configuration", ex);
        }
    }

    private void createDefaultConfig(Path configPath) {
        Properties defaultProps = new Properties();
        defaultProps.setProperty("username", "yourUserName");
        defaultProps.setProperty("password", "yourPassword");
        defaultProps.setProperty("browser", "edge");
        defaultProps.setProperty("headless", "false");
        defaultProps.setProperty("courseId", "yourCourseId");
        try {
            defaultProps.store(Files.newOutputStream(configPath), "Zhihuishu Auto Watcher Configuration");
            logger.info("Default configuration created at: {}", configPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create default configuration", e);
        }
    }

    public String getUsername() {
        return properties.getProperty("username");
    }

    public String getPassword() {
        return properties.getProperty("password");
    }

    public String getBrowser() {
        return properties.getProperty("browser", "edge");
    }

    public String getCourseId() {
        return properties.getProperty("courseId", "11138959");
    }

    public boolean isHeadless() {
        return Boolean.parseBoolean(properties.getProperty("headless", "false"));
    }

}
