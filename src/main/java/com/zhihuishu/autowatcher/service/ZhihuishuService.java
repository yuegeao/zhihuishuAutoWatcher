package com.zhihuishu.autowatcher.service;

import com.zhihuishu.autowatcher.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZhihuishuService {
    private static final Logger logger = LoggerFactory.getLogger(ZhihuishuService.class);
    private static final String LOGIN_URL = "https://onlineweb.zhihuishu.com/onlinestuh5";

    private final WebDriver driver;
    private final Config config;
    private final WebDriverWait wait;
    private final String courseResourceUrl;

    public ZhihuishuService(WebDriver driver, Config config) {
        this.driver = driver;
        this.config = config;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        this.courseResourceUrl = String.format(
                "https://wenda.zhihuishu.com/stu/courseInfo/studyResource?courseId=%s",
                config.getCourseId());
    }

    public boolean login() {
        try {
            logger.info("正在尝试登录智慧树");
            driver.get(LOGIN_URL);

            // 等待登录页面加载
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"lUsername\"]")));

            // 输入用户名和密码
            driver.findElement(By.xpath("//*[@id=\"lUsername\"]")).sendKeys(config.getUsername());
            driver.findElement(By.xpath("//*[@id=\"lPassword\"]")).sendKeys(config.getPassword());

            // 点击登录按钮
            driver.findElement(By.className("wall-sub-btn")).click();

            // 等待滑块验证出现
            logger.info("等待滑块验证出现...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[33]/div[2]/div/div/div" +
                    "[1]/span[2]")));
            logger.info("检测到滑块验证，请手动完成验证...");

            // 等待用户完成滑块验证并成功登录
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"dif_v1\"]/div/div/div[1]/div[2]/span")));

            logger.info("登录成功");
            return true;
        } catch (Exception e) {
            logger.error("登录失败", e);
            return false;
        }
    }

    public void watchCourses() {
        try {
            // 导航到指定的课程资源页面
            logger.info("正在跳转到课程资源页面");
            driver.get(this.getCourseResourceUrl());
            logger.info("已导航到课程资源页面");

            // 等待页面加载完成
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"__layout\"]/div/div/div[2]/div[3]/div/span[1]/a")));

            // 观看视频的具体逻辑
            processVideoResources();

            logger.info("完成观看所有课程资源");
        } catch (Exception e) {
            logger.error("观看课程时出错", e);
        }
    }

    private void processVideoResources() {
        try {
            logger.info("开始处理视频资源...");

            // 从本地文件读取HTML内容
            String htmlContent = readClassesHtmlFile();

            if (htmlContent == null || htmlContent.isEmpty()) {
                logger.error("无法读取classes.html文件或文件为空");
                return;
            }

            // 使用正则表达式提取所有的fileId
            List<String> fileIds = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            // 匹配格式: id="file_数字" onclick="changeFile(数字)" class="file-item">...</div>
            Pattern pattern = Pattern.compile("id=\"file_(\\d+)\"\\s+onclick=\"changeFile\\(\\d+\\)\"\\s+class=\"file-item\">.*?<span class=\"file-name\".*?>(.*?)</span>.*?<div class=\"status-box\">(.*?)</div>");
            Matcher matcher = pattern.matcher(htmlContent);

            // 找到所有匹配项
            while (matcher.find()) {
                String fileId = matcher.group(1);
                String fileName = matcher.group(2);
                String statusBox = matcher.group(3);

                // 检查是否已完成观看(通过status-box中是否有rate=100%来判断)
                boolean completed = statusBox.contains("rate\">100%");

                if (!completed) {
                    fileIds.add(fileId);
                    fileNames.add(fileName);
                    logger.info("找到未完成视频: {} (ID: {})", fileName, fileId);
                } else {
                    logger.info("跳过已完成视频: {} (ID: {})", fileName, fileId);
                }
            }

            logger.info("共找到 {} 个未完成的视频", fileIds.size());

            // 按顺序处理每个视频
            for (int i = 0; i < fileIds.size(); i++) {
                String fileId = fileIds.get(i);
                String fileName = fileNames.get(i);

                try {
                    logger.info("开始观看视频 [{}/{}]: {}", (i + 1), fileIds.size(), fileName);

                    // 使用config中的courseId
                    String videoUrl = String.format(
                            "https://hike.zhihuishu.com/aidedteaching/sourceLearning/sourceLearning?courseId=%s&fileId=%s",
                            config.getCourseId(), fileId);

                    logger.info("访问视频URL: {}", videoUrl);
                    driver.get(videoUrl);

                    // 等待视频加载并播放
                    handleVideoPlayback();

                    logger.info("完成观看视频: {}", fileName);


                } catch (Exception e) {
                    logger.error("处理视频时出错: {} (ID: {}), 错误信息: {}", fileName, fileId, e.getMessage());

                    // 出错后返回课程资源页，继续处理下一个视频
                    driver.get(this.getCourseResourceUrl());
                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//*[@id=\"__layout\"]/div/div/div[3]/div[1]/div[2]/div[2]/div[3]/div/div[2]/div[3]/div")));
                    Thread.sleep(2000);
                }
            }

            logger.info("完成处理所有视频资源");
        } catch (Exception e) {
            logger.error("处理视频资源总过程出错", e);
        }
    }

    /**
     * 从resources目录下读取classes.html文件内容
     */
    private String readClassesHtmlFile() {
        // 从类路径(resources目录)读取文件
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("classes.html")) {
            if (is == null) {
                logger.error("无法找到classes.html文件");
                return null;
            }

            // 读取文件内容
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                logger.info("成功读取classes.html文件，文件大小: {} 字节", content.length());
                return content.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleVideoPlayback() {
        try {
            // 等待视频播放器加载
            logger.info("等待视频播放器加载...");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".video-js")));
            logger.info("视频播放器已加载");

            // 等待几秒钟，确保视频完全加载
            Thread.sleep(3000);

            driver.findElement(By.className("definiBox")).click();
            logger.info("点击清晰度按钮");
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".line1gq"))).click();
            logger.info("选择高清");
            wait.until(ExpectedConditions.elementToBeClickable(By.className("volumeIcon"))).click();
            logger.info("静音");

            // 检查是否已经播放过该视频
            boolean shouldSkip = checkIfVideoAlreadyWatched();
            if (shouldSkip) {
                logger.info("该视频已经播放过，跳过到下一个视频");
                return;
            }

            // 尝试点击播放按钮（如果可见）
            try {
                WebElement playButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(
                        "#playButton > div")));
                playButton.click();
                logger.info("点击播放按钮");
            } catch (Exception e) {
                logger.info("播放按钮不可见或自动播放已开始");
            }

            // 等待几秒钟，确保视频开始播放
            Thread.sleep(3000);

            // 获取视频总时长
            String totalDuration = "";
            try {
                // 移动鼠标以显示控制栏
                moveMouseToRevealControls();

                WebElement durationElement = driver.findElement(By.className("duration"));
                totalDuration = durationElement.getText();
                logger.info("视频总时长: {}", totalDuration);
            } catch (Exception e) {
                logger.warn("无法获取视频总时长: {}", e.getMessage());
            }

            // 计算总时长（秒）
            int totalSeconds = parseDurationToSeconds(totalDuration);
            if (totalSeconds <= 0) {
                logger.warn("无法解析视频总时长，将使用默认等待时间");
                totalSeconds = 600; // 默认10分钟
            }

            // 等待视频播放完成
            boolean videoCompleted = false;
            int checkCount = 0;
            // 计算最大等待次数，为视频总时长的1.5倍（以秒为单位），每20秒检查一次
            int maxChecks = (int) (totalSeconds * 1.5 / 20);
            int maxWaitMinutes = maxChecks * 20 / 60; // 转换为分钟

            logger.info("开始等待视频播放完成... 预计需要 {} 分钟", totalSeconds / 60);

            // 记录上一次的播放时间，用于检测卡住的情况
            String lastTimeText = "";
            int stuckCounter = 0;

            while (!videoCompleted && checkCount < maxChecks) {
                try {
                    // 移动鼠标以显示控制栏，然后获取时间
                    moveMouseToRevealControls();

                    // 尝试查找播放进度元素
                    List<WebElement> progressElements = driver.findElements(By.className("currentTime"));

                    if (!progressElements.isEmpty()) {
                        String currentTimeText = progressElements.get(0).getText();
                        String durationText = "";
                        try {
                            durationText = driver.findElement(By.className("duration")).getText();
                        } catch (Exception e) {
                            // 有时候duration元素可能不可见
                            durationText = totalDuration; // 使用之前获取的总时长
                        }

                        // 检测是否卡住（连续多次相同时间）
                        if (currentTimeText.equals(lastTimeText)) {
                            stuckCounter++;
                            if (stuckCounter > 2) { // 如果连续3次(60秒)没有变化
                                logger.warn("检测到视频可能卡住，尝试刷新...");
                                driver.navigate().refresh();
                                Thread.sleep(5000);

                                stuckCounter = 0;
                            }
                        } else {
                            stuckCounter = 0;
                            lastTimeText = currentTimeText;
                        }

                        // 每次检查都记录进度
                        logger.info("视频播放进度: {} / {}", currentTimeText, durationText);

                        // 检查是否接近结束
                        if (!currentTimeText.isEmpty() && !durationText.isEmpty() && isTimeAlmostEqual(currentTimeText, durationText)) {
                            videoCompleted = true;
                            logger.info("视频播放完成: {} / {}", currentTimeText, durationText);
                        }

                        // 如果无法获取时间但已经播放了足够长的时间，也认为完成
                        int currentSeconds = parseDurationToSeconds(currentTimeText);
                        if (currentSeconds > 0 && currentSeconds >= totalSeconds * 0.95) {
                            videoCompleted = true;
                            logger.info("视频接近总时长，认为已完成: {} / {}", currentTimeText, totalDuration);
                        }
                    } else {
                        // 检查是否存在表示视频已完成的其他元素
                        List<WebElement> completeElements = driver.findElements(By.cssSelector(".video-complete-hint"));
                        if (!completeElements.isEmpty()) {
                            videoCompleted = true;
                            logger.info("检测到视频完成提示元素");
                        }
                    }

                    // 每20秒检查一次
                    Thread.sleep(20000);
                    checkCount++;

                } catch (Exception e) {
                    logger.warn("检查视频进度时出错: {}", e.getMessage());
                    Thread.sleep(20000);
                    checkCount++;
                }
            }

            if (!videoCompleted) {
                logger.warn("视频等待超时，已等待 {} 次检查 (约 {} 分钟)，强制进入下一个视频", checkCount, checkCount * 20 / 60);
            }

        } catch (Exception e) {
            logger.error("处理视频播放过程出错", e);
        }
    }

    /**
     * 模拟鼠标从底部到顶部移动，以显示视频控制栏
     */
    /**
     * 简化的鼠标移动方法 - 直接从屏幕底部到顶部移动，无需精确定位视频播放器
     */
    private void moveMouseToRevealControls() {
        try {
            // 创建Actions对象用于模拟鼠标操作
            Actions actions = new Actions(driver);

            // 获取浏览器窗口尺寸
            int windowHeight = driver.manage().window().getSize().getHeight();

            // 先将鼠标移动到中心点
            WebElement videoElement = driver.findElement(By.cssSelector(".video-js"));
            actions.moveToElement(videoElement).perform();
            Thread.sleep(200);

            // 从中心点向下移动（约窗口高度的30%）
            actions.moveByOffset(0, (int)(windowHeight * 0.3)).perform();
            Thread.sleep(200);

            // 再向上移动（约窗口高度的60%）
            actions.moveByOffset(0, (int)(-windowHeight * 0.6)).perform();
            Thread.sleep(200);

            // 最后回到中心点
            actions.moveToElement(videoElement).perform();
            Thread.sleep(500);
        } catch (Exception e) {
            logger.warn("模拟鼠标移动时出错: {}", e.getMessage());
        }
    }
    // 辅助方法：将时间格式解析为秒数
    private int parseDurationToSeconds(String duration) {
        try {
            if (duration == null || duration.isEmpty()) {
                return 0;
            }

            String[] parts = duration.split(":");
            int seconds = 0;

            if (parts.length == 2) { // MM:SS
                seconds = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) { // HH:MM:SS
                seconds = Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            }

            return seconds;
        } catch (Exception e) {
            logger.warn("解析时间格式出错: {}", e.getMessage());
            return 0;
        }
    }

    // 辅助方法：检查两个时间是否几乎相等
    private boolean isTimeAlmostEqual(String time1, String time2) {
        try {
            // 解析时间格式 "MM:SS" 或 "HH:MM:SS"
            String[] parts1 = time1.split(":");
            String[] parts2 = time2.split(":");

            // 转换为秒
            int seconds1 = 0;
            int seconds2 = 0;

            if (parts1.length == 2) { // MM:SS
                seconds1 = Integer.parseInt(parts1[0]) * 60 + Integer.parseInt(parts1[1]);
            } else if (parts1.length == 3) { // HH:MM:SS
                seconds1 = Integer.parseInt(parts1[0]) * 3600 + Integer.parseInt(parts1[1]) * 60 + Integer.parseInt(parts1[2]);
            }

            if (parts2.length == 2) { // MM:SS
                seconds2 = Integer.parseInt(parts2[0]) * 60 + Integer.parseInt(parts2[1]);
            } else if (parts2.length == 3) { // HH:MM:SS
                seconds2 = Integer.parseInt(parts2[0]) * 3600 + Integer.parseInt(parts2[1]) * 60 + Integer.parseInt(parts2[2]);
            }

            // 如果时间相差不到10秒，或者进度已经达到95%以上，认为基本完成
            return (seconds2 - seconds1 <= 10) || (seconds1 >= seconds2 * 0.95);
        } catch (Exception e) {
            logger.warn("比较时间时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查视频是否已经播放过（播放进度接近100%）
     * @return 如果视频已接近完成（进度超过95%）则返回true
     */
    private boolean checkIfVideoAlreadyWatched() {
        try {
            // 移动鼠标以显示控制栏
            moveMouseToRevealControls();

            // 等待一下让控制栏显示出来
            Thread.sleep(200);

            // 尝试获取当前播放时间和总时长
            WebElement currentTimeElement = null;
            WebElement durationElement = null;

            try {
                currentTimeElement = driver.findElement(By.className("currentTime"));
                durationElement = driver.findElement(By.className("duration"));
            } catch (Exception e) {
                logger.warn("无法获取视频进度元素，视频可能尚未开始播放");
                return false;
            }

            if (currentTimeElement != null && durationElement != null) {
                String currentTime = currentTimeElement.getText();
                String totalDuration = durationElement.getText();

                logger.info("检测到视频进度: {} / {}", currentTime, totalDuration);

                // 如果当前时间或总时长为空，则无法判断
                if (currentTime.isEmpty() || totalDuration.isEmpty()) {
                    return false;
                }

                // 解析时间并判断是否已完成95%以上
                int currentSeconds = parseDurationToSeconds(currentTime);
                int totalSeconds = parseDurationToSeconds(totalDuration);

                if (totalSeconds > 0) {
                    double progressPercentage = (double) currentSeconds / totalSeconds * 100;
                    logger.info("视频播放进度: {}%", String.format("%.2f", progressPercentage));
                    // 如果进度超过95%，认为视频已经看过
                    if (progressPercentage >= 95) {
                        logger.info("该视频已经播放超过95%，视为已完成");
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            logger.warn("检查视频播放进度时出错: {}", e.getMessage());
            return false;
        }
    }

    public String getCourseResourceUrl() {
        return courseResourceUrl;
    }
}
