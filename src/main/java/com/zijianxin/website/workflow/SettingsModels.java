package com.zijianxin.website.workflow;

import java.util.List;
import java.util.UUID;

public final class SettingsModels {

    private SettingsModels() {
    }

    public record AppSettings(
            AiSettings ai,
            SearchSettings search,
            CrawlerSettings crawler,
            MailSettings mail,
            GeneralSettings general,
            TemplateSettings templates
    ) {
        public AppSettings {
            ai = ai == null ? AiSettings.defaults() : ai;
            search = search == null ? SearchSettings.defaults() : search;
            crawler = crawler == null ? CrawlerSettings.defaults() : crawler;
            mail = mail == null ? MailSettings.defaults() : mail;
            general = general == null ? GeneralSettings.defaults() : general;
            templates = templates == null ? TemplateSettings.defaults() : templates;
        }

        public static AppSettings defaults() {
            return new AppSettings(
                    AiSettings.defaults(),
                    SearchSettings.defaults(),
                    CrawlerSettings.defaults(),
                    MailSettings.defaults(),
                    GeneralSettings.defaults(),
                    TemplateSettings.defaults()
            );
        }

        public AppSettings withAi(AiSettings value) {
            return new AppSettings(value, search, crawler, mail, general, templates);
        }

        public AppSettings withSearch(SearchSettings value) {
            return new AppSettings(ai, value, crawler, mail, general, templates);
        }

        public AppSettings withCrawler(CrawlerSettings value) {
            return new AppSettings(ai, search, value, mail, general, templates);
        }

        public AppSettings withMail(MailSettings value) {
            return new AppSettings(ai, search, crawler, value, general, templates);
        }

        public AppSettings withGeneral(GeneralSettings value) {
            return new AppSettings(ai, search, crawler, mail, value, templates);
        }

        public AppSettings withTemplates(TemplateSettings value) {
            return new AppSettings(ai, search, crawler, mail, general, value);
        }
    }

    public record AiSettings(
            String provider,
            String apiKey,
            String model,
            String baseUrl,
            String defaultCompanyName,
            String defaultProductName,
            String defaultValueProposition,
            String defaultLanguage,
            String defaultTone,
            String defaultCallToAction,
            String defaultOptimizationLogic
    ) {
        public AiSettings {
            provider = provider == null || provider.isBlank() ? "Qwen" : provider;
            apiKey = apiKey == null ? "" : apiKey;
            model = model == null || model.isBlank() ? "qwen-max" : model;
            baseUrl = baseUrl == null ? "" : baseUrl;
            defaultCompanyName = defaultCompanyName == null || defaultCompanyName.isBlank() ? "One Minute Tech" : defaultCompanyName;
            defaultProductName = defaultProductName == null || defaultProductName.isBlank() ? "AI Lead Generation Workflow" : defaultProductName;
            defaultValueProposition = defaultValueProposition == null ? "" : defaultValueProposition;
            defaultLanguage = defaultLanguage == null || defaultLanguage.isBlank() ? "zh-CN" : defaultLanguage;
            defaultTone = defaultTone == null || defaultTone.isBlank() ? "professional" : defaultTone;
            defaultCallToAction = defaultCallToAction == null ? "" : defaultCallToAction;
            defaultOptimizationLogic = defaultOptimizationLogic == null ? "" : defaultOptimizationLogic;
        }

        public static AiSettings defaults() {
            return new AiSettings(
                    "Qwen",
                    "",
                    "qwen-plus",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    "One Minute Tech",
                    "AI Lead Generation Workflow",
                    "We help trade teams combine customer search, outreach draft generation, and follow-up management in one workflow.",
                    "zh-CN",
                    "professional",
                    "If it is convenient, we can arrange a short 15-minute call to see whether this workflow fits your team.",
                    ""
            );
        }
    }

    public record AiConnectionTestResult(
            boolean success,
            String requestUrl,
            String responsePreview
    ) {
    }

    public record SearchConnectionTestResult(
            boolean success,
            String requestUrl,
            String responsePreview
    ) {
    }

    public record SearchSettings(
            String serpApiKey,
            String defaultEngine,
            int resultsPerPage,
            String linkedinApiKey
    ) {
        public SearchSettings {
            serpApiKey = serpApiKey == null ? "" : serpApiKey;
            defaultEngine = defaultEngine == null || defaultEngine.isBlank() ? "Auto" : defaultEngine;
            resultsPerPage = resultsPerPage <= 0 ? 12 : resultsPerPage;
            linkedinApiKey = linkedinApiKey == null ? "" : linkedinApiKey;
        }

        public static SearchSettings defaults() {
            return new SearchSettings("", "Auto", 12, "");
        }
    }

    public record CrawlerSettings(
            int requestTimeoutMs,
            int maxSearchDurationMs,
            int candidateLimit,
            int searchEnginePageLimit,
            int maxParallelInspections,
            boolean googleFallbackEnabled,
            int googleFallbackQueryLimit,
            int googleFallbackPageLimit,
            int googleFallbackTimeoutMs,
            String emailExtractionDepth,
            String logMode,
            int sameDomainWeight,
            int marketWeight,
            int keywordWeight,
            int companySignalWeight
    ) {
        public CrawlerSettings {
            requestTimeoutMs = requestTimeoutMs <= 0 ? 8000 : requestTimeoutMs;
            maxSearchDurationMs = maxSearchDurationMs <= 0 ? 300000 : maxSearchDurationMs;
            candidateLimit = candidateLimit <= 0 ? 36 : candidateLimit;
            searchEnginePageLimit = searchEnginePageLimit <= 0 ? 2 : searchEnginePageLimit;
            maxParallelInspections = maxParallelInspections <= 0 ? 8 : maxParallelInspections;
            googleFallbackQueryLimit = googleFallbackQueryLimit <= 0 ? 2 : googleFallbackQueryLimit;
            googleFallbackPageLimit = googleFallbackPageLimit <= 0 ? 1 : googleFallbackPageLimit;
            googleFallbackTimeoutMs = googleFallbackTimeoutMs <= 0 ? 5000 : googleFallbackTimeoutMs;
            emailExtractionDepth = emailExtractionDepth == null || emailExtractionDepth.isBlank() ? "HOME_AND_CONTACT" : emailExtractionDepth;
            logMode = logMode == null || logMode.isBlank() ? "summary" : logMode;
            sameDomainWeight = sameDomainWeight <= 0 ? 10 : sameDomainWeight;
            marketWeight = marketWeight <= 0 ? 8 : marketWeight;
            keywordWeight = keywordWeight <= 0 ? 6 : keywordWeight;
            companySignalWeight = companySignalWeight <= 0 ? 8 : companySignalWeight;
        }

        public static CrawlerSettings defaults() {
            return new CrawlerSettings(8000, 300000, 36, 2, 8, true, 2, 1, 5000, "HOME_AND_CONTACT", "summary", 10, 8, 6, 8);
        }
    }

    public record MailSettings(
            String senderName,
            String senderEmail,
            String replyToEmail,
            String signature,
            String smtpProvider,
            String smtpHost,
            int smtpPort,
            String smtpSecurity,
            String smtpUsername,
            String smtpPassword,
            int batchLimit,
            int hourlyLimit,
            int retryCount,
            String sendMode
    ) {
        public static MailSettings defaults() {
            return new MailSettings(
                    "Zijian",
                    "zijian@example.com",
                    "reply@example.com",
                    "Zijian | One Minute Tech",
                    "Custom SMTP",
                    "smtp.example.com",
                    587,
                    "STARTTLS",
                    "zijian@example.com",
                    "",
                    50,
                    120,
                    2,
                    "smtp"
            );
        }
    }

    public record GeneralSettings(
            String language,
            String timezone,
            String dateFormat,
            String numberFormat,
            String defaultLandingPage,
            boolean localCacheEnabled,
            boolean autoSelectLeads,
            boolean requireReviewBeforeSend,
            int logRetentionDays,
            boolean debugMode
    ) {
        public static GeneralSettings defaults() {
            return new GeneralSettings(
                    "zh-CN",
                    "Asia/Shanghai",
                    "YYYY-MM-DD",
                    "1,234.56",
                    "/customer-search",
                    true,
                    true,
                    true,
                    30,
                    false
            );
        }
    }

    public record TemplateSettings(
            String defaultTemplateId,
            List<EmailTemplate> items
    ) {
        public TemplateSettings {
            List<EmailTemplate> normalizedItems = items == null ? List.of() : items.stream()
                    .map(item -> item == null ? EmailTemplate.defaults().get(0) : item)
                    .toList();
            if (normalizedItems.isEmpty()) {
                normalizedItems = EmailTemplate.defaults();
            }
            items = List.copyOf(normalizedItems);
            defaultTemplateId = defaultTemplateId == null || defaultTemplateId.isBlank()
                    ? items.get(0).id()
                    : defaultTemplateId;
            String resolvedDefaultTemplateId = defaultTemplateId;
            boolean exists = items.stream().anyMatch(item -> item.id().equals(resolvedDefaultTemplateId));
            if (!exists) {
                defaultTemplateId = items.get(0).id();
            }
        }

        public static TemplateSettings defaults() {
            List<EmailTemplate> defaults = EmailTemplate.defaults();
            return new TemplateSettings(defaults.get(0).id(), defaults);
        }
    }

    public record EmailTemplate(
            String id,
            String name,
            String language,
            String scenario,
            String subject,
            String body,
            String instruction,
            boolean enabled
    ) {
        public EmailTemplate {
            id = id == null || id.isBlank() ? "tpl-" + UUID.randomUUID() : id;
            name = name == null || name.isBlank() ? "Default Template" : name;
            language = language == null || language.isBlank() ? "zh-CN" : language;
            scenario = scenario == null || scenario.isBlank() ? "first-contact" : scenario;
            subject = subject == null ? "" : subject;
            body = body == null ? "" : body;
            instruction = instruction == null ? "" : instruction;
        }

        public static List<EmailTemplate> defaults() {
            return List.of(
                    new EmailTemplate(
                            "tpl-first-zh",
                            "初次接触（中文）",
                            "zh-CN",
                            "first-contact",
                            "想和 {{recipientCompany}} 聊聊 {{productName}} 的合作机会",
                            """
                                    {{contactName}}您好，

                                    我是 {{senderCompany}} 的 {{senderName}}，我们在做 {{productName}} 相关方案。留意到 {{recipientCompany}} 的业务方向后，感觉我们的产品可能能在 {{valueProposition}} 这类场景里提供一些实际帮助。

                                    如果您现在也在关注类似需求，我可以先发一份简短资料，或者约 15 分钟沟通一下，看是否值得进一步对接。

                                    祝好，
                                    {{senderName}}
                                    """,
                            "适合第一次联系目标客户。不要机械照抄模板，要根据客户公司和产品价值写出具体、自然的合作理由；如果没有真实联系人姓名，只用“您好”。",
                            true
                    ),
                    new EmailTemplate(
                            "tpl-first-en",
                            "First Contact (English)",
                            "en",
                            "first-contact",
                            "Potential fit around {{productName}}",
                            """
                                    Hi {{contactName}},

                                    I am {{senderName}} from {{senderCompany}}. We provide {{productName}} and help teams like {{recipientCompany}} {{valueProposition}}.

                                    Would you be open to a short 15-minute call to see whether there is a fit?

                                    Best regards,
                                    {{senderName}}
                                    """,
                            "Use for first-touch outbound emails. Keep it concise, relevant, and focused on one clear call to action.",
                            true
                    ),
                    new EmailTemplate(
                            "tpl-follow-up-en",
                            "Follow-up Email",
                            "en",
                            "follow-up",
                            "Quick follow-up on {{productName}}",
                            """
                                    Hi {{contactName}},

                                    I wanted to quickly follow up on my previous note about {{productName}}.

                                    If this is relevant for {{recipientCompany}}, I would be happy to share a short overview or a few practical examples.

                                    Best,
                                    {{senderName}}
                                    """,
                            "Use after no reply. Keep it lighter than the first email and avoid repeating the whole pitch.",
                            true
                    )
            );
        }
    }
}
