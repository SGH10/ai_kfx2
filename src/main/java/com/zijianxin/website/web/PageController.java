package com.zijianxin.website.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/customer-search")
    public String customerSearchPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/customer-search.html";
    }

    @GetMapping("/favicon.ico")
    public String favicon() {
        return "forward:/favicon.svg";
    }

    @GetMapping("/ai-outreach")
    public String aiOutreachPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/ai-outreach.html";
    }

    @GetMapping("/crawler-settings")
    public String crawlerSettingsPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/crawler-settings.html";
    }

    @GetMapping("/ai-settings")
    public String aiSettingsPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/ai-settings.html";
    }

    @GetMapping("/business-profile")
    public String businessProfilePage(HttpServletResponse response) {
        noCache(response);
        return "forward:/business-profile.html";
    }

    @GetMapping("/mail-settings")
    public String mailSettingsPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/mail-settings.html";
    }

    @GetMapping("/email-templates")
    public String emailTemplatesPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/email-templates.html";
    }

    @GetMapping("/general-settings")
    public String generalSettingsPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/general-settings.html";
    }

    @GetMapping("/crawler-rules")
    public String crawlerRulesPage(HttpServletResponse response) {
        noCache(response);
        return "forward:/crawler-rules.html";
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }
}
