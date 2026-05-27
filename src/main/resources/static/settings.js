(() => {
  const path = window.location.pathname;

  init().catch((error) => {
    console.error("Settings bootstrap failed:", error);
  });

  async function init() {
    const settings = await fetchSettings();
    if (!settings) {
      return;
    }

    if (path === "/ai-settings") {
      bindAiSettings(settings);
      return;
    }

    if (path === "/crawler-settings") {
      bindSearchSettings(settings);
      return;
    }

    if (path === "/crawler-rules") {
      bindCrawlerSettings(settings);
      return;
    }

    if (path === "/mail-settings") {
      bindMailSettings(settings);
      return;
    }

    if (path === "/general-settings") {
      bindGeneralSettings(settings);
    }
  }

  async function fetchSettings() {
    const response = await fetch("/api/settings", { cache: "no-store" });
    if (!response.ok) {
      return null;
    }
    return response.json();
  }

  function bindAiSettings(settings) {
    setValue("#ai-provider", settings.ai.provider);
    setValue("#ai-api-key", settings.ai.apiKey);
    setValue("#ai-model", settings.ai.model);
    setValue("#ai-base-url", settings.ai.baseUrl);
    setValue("#ai-default-company-name", settings.ai.defaultCompanyName);
    setValue("#ai-default-product-name", settings.ai.defaultProductName);
    setValue("#ai-default-language", settings.ai.defaultLanguage);
    setValue("#ai-default-tone", settings.ai.defaultTone);
    setValue("#ai-default-value-proposition", settings.ai.defaultValueProposition);
    setValue("#ai-default-call-to-action", settings.ai.defaultCallToAction);
    setValue("#ai-default-optimization-logic", settings.ai.defaultOptimizationLogic);

    document.querySelector("#test-ai-connection")?.addEventListener("click", async () => {
      const button = document.querySelector("#test-ai-connection");
      if (button) {
        button.disabled = true;
      }

      setResult(
        "#ai-test-result",
        t("正在测试连接，请稍候...", "Testing connection, please wait..."),
        true
      );

      try {
        const response = await fetch("/api/settings/ai/test", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(buildAiPayload())
        });

        const rawText = await response.text();
        if (!response.ok) {
          setResult(
            "#ai-test-result",
            rawText || t("连接测试失败，请检查配置后重试。", "Connection test failed. Please review the configuration and try again."),
            false
          );
          return;
        }

        const result = rawText ? JSON.parse(rawText) : null;
        const successMessage = formatAiTestSuccess(result);
        setResult("#ai-test-result", successMessage, true);
      } catch (error) {
        console.error("AI connection test failed:", error);
        setResult(
          "#ai-test-result",
          t("连接测试失败，请检查网络或稍后重试。", "Connection test failed. Please check the network or try again later."),
          false
        );
      } finally {
        if (button) {
          button.disabled = false;
        }
      }
    });

    document.querySelector("#ai-settings-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      await saveSettings(
        "/api/settings/ai",
        buildAiPayload(),
        "#ai-settings-result",
        t("AI 配置已保存，开发信页默认值会同步更新。", "AI settings saved. Outreach page defaults have been updated.")
      );
    });
  }

  function bindSearchSettings(settings) {
    setValue("#search-serp-api-key", settings.search.serpApiKey);
    setValue("#search-default-engine", settings.search.defaultEngine);
    setValue("#search-results-per-page", settings.search.resultsPerPage);
    setValue("#search-linkedin-api-key", settings.search.linkedinApiKey);

    document.querySelector("#search-settings-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      await saveSettings(
        "/api/settings/search",
        {
          serpApiKey: valueOf("#search-serp-api-key"),
          defaultEngine: valueOf("#search-default-engine"),
          resultsPerPage: numberOf("#search-results-per-page", 12),
          linkedinApiKey: valueOf("#search-linkedin-api-key")
        },
        "#search-settings-result",
        t("搜索配置已保存，客户搜索页会使用新的默认搜索参数。", "Search settings saved. Customer search page will use the new parameters.")
      );
    });
  }

  function bindCrawlerSettings(settings) {
    document.querySelector("#crawler-log-mode")?.closest(".input-group")?.remove();
    setValue("#crawler-request-timeout-ms", settings.crawler.requestTimeoutMs);
    setValue("#crawler-max-search-duration-ms", settings.crawler.maxSearchDurationMs);
    setValue("#crawler-candidate-limit", settings.crawler.candidateLimit);
    setValue("#crawler-search-engine-page-limit", settings.crawler.searchEnginePageLimit);
    setValue("#crawler-max-parallel-inspections", settings.crawler.maxParallelInspections);
    setValue("#crawler-email-extraction-depth", settings.crawler.emailExtractionDepth);
    setValue("#crawler-google-fallback-enabled", String(settings.crawler.googleFallbackEnabled));
    setValue("#crawler-google-fallback-query-limit", settings.crawler.googleFallbackQueryLimit);
    setValue("#crawler-google-fallback-page-limit", settings.crawler.googleFallbackPageLimit);
    setValue("#crawler-google-fallback-timeout-ms", settings.crawler.googleFallbackTimeoutMs);
    setValue("#crawler-same-domain-weight", settings.crawler.sameDomainWeight);
    setValue("#crawler-market-weight", settings.crawler.marketWeight);
    setValue("#crawler-keyword-weight", settings.crawler.keywordWeight);
    setValue("#crawler-company-signal-weight", settings.crawler.companySignalWeight);

    document.querySelector("#crawler-settings-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      await saveSettings(
        "/api/settings/crawler",
        {
          requestTimeoutMs: numberOf("#crawler-request-timeout-ms", 8000),
          maxSearchDurationMs: numberOf("#crawler-max-search-duration-ms", 300000),
          candidateLimit: numberOf("#crawler-candidate-limit", 36),
          searchEnginePageLimit: numberOf("#crawler-search-engine-page-limit", 2),
          maxParallelInspections: numberOf("#crawler-max-parallel-inspections", 8),
          emailExtractionDepth: valueOf("#crawler-email-extraction-depth"),
          logMode: settings.crawler.logMode,
          googleFallbackEnabled: settings.crawler.googleFallbackEnabled,
          googleFallbackQueryLimit: settings.crawler.googleFallbackQueryLimit,
          googleFallbackPageLimit: settings.crawler.googleFallbackPageLimit,
          googleFallbackTimeoutMs: settings.crawler.googleFallbackTimeoutMs,
          sameDomainWeight: numberOf("#crawler-same-domain-weight", 10),
          marketWeight: numberOf("#crawler-market-weight", 8),
          keywordWeight: numberOf("#crawler-keyword-weight", 6),
          companySignalWeight: numberOf("#crawler-company-signal-weight", 8)
        },
        "#crawler-settings-result",
        t("爬虫配置已保存，搜索超时、候选池和评分逻辑会同步生效。", "Crawler settings saved. Timeout, candidate pool, and scoring will take effect immediately.")
      );
    });
  }

  function bindMailSettings(settings) {
    setValue("#mail-sender-name", settings.mail.senderName);
    setValue("#mail-sender-email", settings.mail.senderEmail);
    setValue("#mail-reply-to-email", settings.mail.replyToEmail);
    setValue("#mail-signature", settings.mail.signature);
    setValue("#mail-smtp-provider", settings.mail.smtpProvider);
    setValue("#mail-smtp-host", settings.mail.smtpHost);
    setValue("#mail-smtp-port", settings.mail.smtpPort);
    setValue("#mail-smtp-security", settings.mail.smtpSecurity);
    setValue("#mail-smtp-username", settings.mail.smtpUsername);
    setValue("#mail-smtp-password", settings.mail.smtpPassword);
    setValue("#mail-batch-limit", settings.mail.batchLimit);
    setValue("#mail-hourly-limit", settings.mail.hourlyLimit);
    setValue("#mail-retry-count", settings.mail.retryCount);
    setValue("#mail-send-mode", settings.mail.sendMode);

    document.querySelector("#mail-settings-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      await saveSettings(
        "/api/settings/mail",
        {
          senderName: valueOf("#mail-sender-name"),
          senderEmail: valueOf("#mail-sender-email"),
          replyToEmail: valueOf("#mail-reply-to-email"),
          signature: valueOf("#mail-signature"),
          smtpProvider: valueOf("#mail-smtp-provider"),
          smtpHost: valueOf("#mail-smtp-host"),
          smtpPort: numberOf("#mail-smtp-port", 587),
          smtpSecurity: valueOf("#mail-smtp-security"),
          smtpUsername: valueOf("#mail-smtp-username"),
          smtpPassword: valueOf("#mail-smtp-password"),
          batchLimit: numberOf("#mail-batch-limit", 50),
          hourlyLimit: numberOf("#mail-hourly-limit", 120),
          retryCount: numberOf("#mail-retry-count", 2),
          sendMode: valueOf("#mail-send-mode")
        },
        "#mail-settings-result",
        t("邮箱配置已保存，开发信页和发送接口会使用新的默认发件身份。", "Mail settings saved. The outreach page and send API will use the new sender identity.")
      );
    });
  }

  function bindGeneralSettings(settings) {
    setValue("#general-language", settings.general.language);
    setValue("#general-timezone", settings.general.timezone);
    setValue("#general-date-format", settings.general.dateFormat);
    setValue("#general-number-format", settings.general.numberFormat);
    setValue("#general-default-landing-page", settings.general.defaultLandingPage);
    setValue("#general-log-retention-days", settings.general.logRetentionDays);
    setValue("#general-local-cache-enabled", String(settings.general.localCacheEnabled));
    setValue("#general-auto-select-leads", String(settings.general.autoSelectLeads));
    setValue("#general-require-review-before-send", String(settings.general.requireReviewBeforeSend));
    setValue("#general-debug-mode", String(settings.general.debugMode));

    document.querySelector("#general-settings-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      await saveSettings(
        "/api/settings/general",
        {
          language: valueOf("#general-language"),
          timezone: valueOf("#general-timezone"),
          dateFormat: valueOf("#general-date-format"),
          numberFormat: valueOf("#general-number-format"),
          defaultLandingPage: valueOf("#general-default-landing-page"),
          localCacheEnabled: booleanOf("#general-local-cache-enabled", true),
          autoSelectLeads: booleanOf("#general-auto-select-leads", true),
          requireReviewBeforeSend: booleanOf("#general-require-review-before-send", true),
          logRetentionDays: numberOf("#general-log-retention-days", 30),
          debugMode: booleanOf("#general-debug-mode", false)
        },
        "#general-settings-result",
        t("通用配置已保存，默认入口和本地缓存行为会同步更新。", "General settings saved. Default landing page and cache behavior have been updated.")
      );
    });
  }

  async function saveSettings(url, payload, resultSelector, successText) {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      setResult(resultSelector, t("保存失败，请稍后重试。", "Save failed. Please try again."), false);
      return;
    }

    setResult(resultSelector, successText, true);
  }

  function setResult(selector, message, success) {
    const element = document.querySelector(selector);
    if (!element) {
      return;
    }
    element.textContent = message;
    element.classList.remove("is-success", "is-warning");
    element.classList.add(success ? "is-success" : "is-warning");
  }

  function buildAiPayload() {
    return {
      provider: valueOf("#ai-provider"),
      apiKey: valueOf("#ai-api-key"),
      model: valueOf("#ai-model"),
      baseUrl: valueOf("#ai-base-url"),
      defaultCompanyName: valueOf("#ai-default-company-name"),
      defaultProductName: valueOf("#ai-default-product-name"),
      defaultValueProposition: valueOf("#ai-default-value-proposition"),
      defaultLanguage: valueOf("#ai-default-language"),
      defaultTone: valueOf("#ai-default-tone"),
      defaultCallToAction: valueOf("#ai-default-call-to-action"),
      defaultOptimizationLogic: valueOf("#ai-default-optimization-logic")
    };
  }

  function formatAiTestSuccess(result) {
    const requestUrl = result?.requestUrl ? `\nURL: ${result.requestUrl}` : "";
    const previewLabel = t("返回预览", "Response preview");
    const preview = result?.responsePreview ? `\n${previewLabel}: ${result.responsePreview}` : "";
    return t("连接测试成功。", "Connection test succeeded.") + requestUrl + preview;
  }

  function setValue(selector, value) {
    const element = document.querySelector(selector);
    if (element) {
      element.value = value ?? "";
    }
  }

  function valueOf(selector) {
    return String(document.querySelector(selector)?.value ?? "").trim();
  }

  function numberOf(selector, fallbackValue) {
    const parsed = Number(valueOf(selector));
    return Number.isFinite(parsed) ? parsed : fallbackValue;
  }

  function booleanOf(selector, fallbackValue) {
    const raw = valueOf(selector);
    if (raw === "true") {
      return true;
    }
    if (raw === "false") {
      return false;
    }
    return fallbackValue;
  }

  function t(zh, en) {
    const locale = localStorage.getItem("leadflow-locale");
    return locale === "en" ? en : zh;
  }
})();
