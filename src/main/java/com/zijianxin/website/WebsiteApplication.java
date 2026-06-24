package com.zijianxin.website;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

@SpringBootApplication
public class WebsiteApplication {

    private static final Logger log = LoggerFactory.getLogger(WebsiteApplication.class);
    private static final String HOME_URL = "http://127.0.0.1:8808";

    public static void main(String[] args) {
        SpringApplication.run(WebsiteApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printStartupGuide() {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("网址入口：");
        System.out.println("首页总览： " + HOME_URL);
        System.out.println("客户搜索： " + HOME_URL + "/customer-search");
        System.out.println("AI开发信： " + HOME_URL + "/ai-outreach");
        System.out.println("爬虫配置： " + HOME_URL + "/crawler-settings");
        System.out.println("AI 配置： " + HOME_URL + "/ai-settings");
        System.out.println("说明：首页看流程总览，客户搜索页执行抓取，AI开发信页生成并发送开发信，两个配置页分别管理爬虫与 AI 参数。");
        System.out.println("============================================================");
        System.out.println();
        openHomePage();
    }

    private void openHomePage() {
        if (openWithWindowsShell()) {
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.info("Desktop browse action is not supported. Please open {}", HOME_URL);
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create(HOME_URL));
        } catch (Exception exception) {
            log.warn("Failed to open browser automatically. Please open {}", HOME_URL, exception);
        }
    }

    private boolean openWithWindowsShell() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win")) {
            return false;
        }

        try {
            new ProcessBuilder("cmd", "/c", "start", "", HOME_URL).start();
            return true;
        } catch (IOException exception) {
            log.warn("Failed to open browser with Windows shell. Falling back to Desktop browse.", exception);
            return false;
        }
    }
}
