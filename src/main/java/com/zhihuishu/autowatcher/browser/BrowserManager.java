package com.zhihuishu.autowatcher.browser;

import com.zhihuishu.autowatcher.config.Config;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(BrowserManager.class);
    private WebDriver driver;
    private final Config config;

    public BrowserManager(Config config) {
        this.config = config;
    }

    public WebDriver initializeDriver() {
        String browser = config.getBrowser().toLowerCase();
        boolean headless = config.isHeadless();
        
        switch (browser) {
            case "chrome":
                return initializeChromeDriver(headless);
            case "firefox":
                return initializeFirefoxDriver(headless);
            case "edge":
                return initializeEdgeDriver(headless);
            default:
                logger.warn("Unsupported browser: {}. Defaulting to Edge.", browser);
                return initializeEdgeDriver(headless);
        }
    }

    private WebDriver initializeChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");
        
        driver = new ChromeDriver(options);
        logger.info("Chrome WebDriver initialized successfully");
        return driver;
    }

    private WebDriver initializeFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        driver = new FirefoxDriver(options);
        logger.info("Firefox WebDriver initialized successfully");
        return driver;
    }
    
    private WebDriver initializeEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");
        
        driver = new EdgeDriver(options);
        logger.info("Edge WebDriver initialized successfully");
        return driver;
    }

    public void closeDriver() {
        if (driver != null) {
            driver.quit();
            logger.info("WebDriver closed successfully");
        }
    }
}
