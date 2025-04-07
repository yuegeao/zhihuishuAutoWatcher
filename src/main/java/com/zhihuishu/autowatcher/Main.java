package com.zhihuishu.autowatcher;

import com.zhihuishu.autowatcher.browser.BrowserManager;
import com.zhihuishu.autowatcher.config.Config;
import com.zhihuishu.autowatcher.service.ZhihuishuService;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Zhihuishu Auto Watcher");
        
        Config config = new Config();
        BrowserManager browserManager = new BrowserManager(config);
        WebDriver driver = null;
        
        try {
            driver = browserManager.initializeDriver();
            ZhihuishuService service = new ZhihuishuService(driver, config);
            
            // Login to Zhihuishu
            boolean loginSuccess = service.login();
            
            if (loginSuccess) {
                // Start watching courses
                service.watchCourses();
                logger.info("Finished watching all courses");
            } else {
                logger.error("Failed to login. Please check your credentials in config.properties");
            }
        } catch (Exception e) {
            logger.error("An error occurred", e);
        } finally {
            if (driver != null) {
                browserManager.closeDriver();
            }
            logger.info("Zhihuishu Auto Watcher finished");
        }
    }
}
