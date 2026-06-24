(() => {
  const path = window.location.pathname;
  const customProviderValue = "__custom_provider__";
  const customModelValue = "__custom_model__";
  const builtInAiProviders = new Set([
    "Qwen",
    "OpenAI",
    "Anthropic",
    "DeepSeek",
    "GLM"
  ]);
  const aiModelOptionsByProvider = {
    Qwen: [
      "qwen3.7-max",
      "qwen3-max",
      "qwen3.7-plus",
      "qwen3.6-plus",
      "qwen3.5-plus",
      "qwen-plus",
      "qwen-plus-latest",
      "qwen3.5-flash",
      "qwen-flash",
      "qwen-turbo",
      "qwen-long",
      "qwen-max"
    ],
    OpenAI: [
      "gpt-5.5",
      "gpt-5.4",
      "gpt-5.4-mini",
      "gpt-5.4-nano"
    ],
    Anthropic: [
      "claude-fable-5",
      "claude-opus-4-8",
      "claude-sonnet-4-6",
      "claude-haiku-4-5"
    ],
    DeepSeek: [
      "deepseek-v4-flash",
      "deepseek-v4-pro",
      "deepseek-chat",
      "deepseek-reasoner"
    ],
    GLM: [
      "glm-5.2",
      "glm-5.1",
      "glm-5-turbo",
      "glm-5",
      "glm-4.7",
      "glm-4.7-flash",
      "glm-4.7-flashx",
      "glm-4.6",
      "glm-4.5-air",
      "glm-4.5-flash"
    ]
  };

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

    if (path === "/business-profile") {
      bindBusinessProfileSettings(settings);
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
    hydrateAiProvider(settings.ai.provider);
    setValue("#ai-api-key", settings.ai.apiKey);
    hydrateAiModel(settings.ai.model);
    setValue("#ai-base-url", settings.ai.baseUrl);
    document.querySelector("#ai-provider")?.addEventListener("change", handleAiProviderChange);
    document.querySelector("#ai-model")?.addEventListener("change", syncCustomModelField);
    window.addEventListener("leadflow:locale-changed", refreshAiModelOptionsForLocale);

    document.querySelector("#test-ai-connection")?.addEventListener("click", async () => {
      const button = document.querySelector("#test-ai-connection");
      if (!validateAiManualFields("#ai-test-result")) {
        return;
      }
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
          body: JSON.stringify(buildAiPayload(settings.ai))
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
      if (!validateAiManualFields("#ai-settings-result")) {
        return;
      }
      await saveSettings(
        "/api/settings/ai",
        buildAiPayload(settings.ai),
        "#ai-settings-result",
        t("AI 模型配置已保存。", "AI model settings saved.")
      );
    });
  }

  function bindBusinessProfileSettings(settings) {
    setValue("#ai-default-company-name", settings.ai.defaultCompanyName);
    setValue("#ai-default-product-name", settings.ai.defaultProductName);
    setValue("#ai-default-language", settings.ai.defaultLanguage);
    setValue("#ai-default-tone", settings.ai.defaultTone);
    setValue("#ai-default-value-proposition", settings.ai.defaultValueProposition);
    setValue("#ai-default-call-to-action", settings.ai.defaultCallToAction);
    setValue("#ai-default-optimization-logic", settings.ai.defaultOptimizationLogic);

    document.querySelector("#business-profile-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      await saveSettings(
        "/api/settings/ai",
        buildAiPayload(settings.ai),
        "#business-profile-result",
        t("业务资料已保存，开发信页默认值会同步更新。", "Business profile saved. Outreach defaults have been updated.")
      );
    });
  }

  function bindSearchSettings(settings) {
    setValue("#search-serp-api-key", settings.search.serpApiKey);
    setValue("#search-default-engine", settings.search.defaultEngine);
    setValue("#search-results-per-page", settings.search.resultsPerPage);
    setValue("#search-linkedin-api-key", settings.search.linkedinApiKey);

    const serpApiKeyInput = document.querySelector("#search-serp-api-key");
    const testSerpApiKeyButton = document.querySelector("#test-serp-api-key");
    const resetSearchSettingsButton = document.querySelector("#search-settings-reset");
    let serpApiStatusState = valueOf("#search-serp-api-key") ? "pending" : "empty";
    let isTestingSerpApiKey = false;
    const syncSerpApiStatus = () => {
      updateSerpApiStatus(isTestingSerpApiKey ? "checking" : serpApiStatusState);
    };

    syncSerpApiStatus();
    serpApiKeyInput?.addEventListener("input", () => {
      serpApiStatusState = valueOf("#search-serp-api-key") ? "pending" : "empty";
      syncSerpApiStatus();
    });
    window.addEventListener("leadflow:locale-changed", syncSerpApiStatus);

    testSerpApiKeyButton?.addEventListener("click", async () => {
      if (isTestingSerpApiKey) {
        return;
      }

      const testedApiKey = valueOf("#search-serp-api-key");
      if (!testedApiKey) {
        serpApiStatusState = "empty";
        syncSerpApiStatus();
        setResult("#search-settings-result", t("请先输入 SerpAPI Key。", "Enter a SerpAPI Key first."), false);
        return;
      }

      serpApiStatusState = "checking";
      isTestingSerpApiKey = true;
      syncSerpApiStatus();
      setResult("#search-settings-result", t("正在检测 SerpAPI Key...", "Checking SerpAPI Key..."), true);

      try {
        const response = await fetch("/api/settings/search/test", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(buildSearchPayload())
        });
        const result = response.ok ? await response.json() : null;
        const currentApiKey = valueOf("#search-serp-api-key");
        if (currentApiKey !== testedApiKey) {
          serpApiStatusState = currentApiKey ? "pending" : "empty";
        } else {
          serpApiStatusState = result?.success ? "valid" : "invalid";
        }
        syncSerpApiStatus();
        setResult(
          "#search-settings-result",
          result?.success
            ? t("SerpAPI Key 检测通过。", "SerpAPI Key is valid.")
            : t("SerpAPI Key 无效，请检查后重试。", "SerpAPI Key is invalid. Check it and try again."),
          Boolean(result?.success)
        );
      } catch (error) {
        console.error("SerpAPI key test failed:", error);
        const currentApiKey = valueOf("#search-serp-api-key");
        serpApiStatusState = currentApiKey === testedApiKey
          ? "invalid"
          : currentApiKey ? "pending" : "empty";
        syncSerpApiStatus();
        setResult("#search-settings-result", t("检测失败，请检查网络或稍后重试。", "Test failed. Check the network or try again later."), false);
      } finally {
        isTestingSerpApiKey = false;
        syncSerpApiStatus();
      }
    });

    resetSearchSettingsButton?.addEventListener("click", () => {
      setValue("#search-serp-api-key", "");
      setValue("#search-default-engine", "Auto");
      setValue("#search-results-per-page", 12);
      setValue("#search-linkedin-api-key", "");
      serpApiStatusState = "empty";
      syncSerpApiStatus();
      setResult(
        "#search-settings-result",
        t("已恢复默认，点击保存配置后生效。", "Defaults restored. Click Save Settings to apply."),
        true
      );
    });

    document.querySelector("#search-settings-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const saved = await saveSettings(
        "/api/settings/search",
        buildSearchPayload(),
        "#search-settings-result",
        t("搜索配置已保存，客户搜索页会使用新的默认搜索参数。", "Search settings saved. Customer search page will use the new parameters.")
      );
      if (saved) {
        syncSerpApiStatus();
      }
    });
  }

  function buildSearchPayload() {
    return {
      serpApiKey: valueOf("#search-serp-api-key"),
      defaultEngine: valueOf("#search-default-engine"),
      resultsPerPage: numberOf("#search-results-per-page", 12),
      linkedinApiKey: valueOf("#search-linkedin-api-key")
    };
  }

  function updateSerpApiStatus(state) {
    const status = document.querySelector("#search-serp-status");
    const button = document.querySelector("#test-serp-api-key");
    if (!status) {
      return;
    }

    const labels = {
      empty: t("未配置", "Not Configured"),
      pending: t("待检测", "Needs Test"),
      checking: t("检测中", "Checking"),
      invalid: t("Key 无效", "Invalid Key"),
      valid: t("已配置", "Configured")
    };
    const buttonLabels = {
      empty: t("检测 Key", "Test Key"),
      pending: t("检测 Key", "Test Key"),
      checking: t("检测中", "Checking"),
      invalid: t("重新检测", "Retest"),
      valid: t("重新检测", "Retest")
    };
    const normalizedState = valueOf("#search-serp-api-key") ? state : "empty";
    status.textContent = labels[normalizedState] || labels.empty;
    status.classList.toggle("green", normalizedState === "valid");
    status.classList.toggle("yellow", normalizedState === "checking" || normalizedState === "pending");
    status.classList.toggle("red", normalizedState === "invalid");
    status.classList.toggle("gray", normalizedState === "empty");

    if (button) {
      button.textContent = buttonLabels[normalizedState] || buttonLabels.empty;
      button.disabled = normalizedState === "empty" || normalizedState === "checking";
      button.classList.toggle("is-checking", normalizedState === "checking");
    }
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
      return false;
    }

    setResult(resultSelector, successText, true);
    return true;
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

  function buildAiPayload(baseAi = {}) {
    return {
      provider: resolveAiProvider(baseAi.provider),
      apiKey: valueOrFallback("#ai-api-key", baseAi.apiKey),
      model: resolveAiModel(baseAi.model),
      baseUrl: valueOrFallback("#ai-base-url", baseAi.baseUrl),
      defaultCompanyName: valueOrFallback("#ai-default-company-name", baseAi.defaultCompanyName),
      defaultProductName: valueOrFallback("#ai-default-product-name", baseAi.defaultProductName),
      defaultValueProposition: valueOrFallback("#ai-default-value-proposition", baseAi.defaultValueProposition),
      defaultLanguage: valueOrFallback("#ai-default-language", baseAi.defaultLanguage),
      defaultTone: valueOrFallback("#ai-default-tone", baseAi.defaultTone),
      defaultCallToAction: valueOrFallback("#ai-default-call-to-action", baseAi.defaultCallToAction),
      defaultOptimizationLogic: valueOrFallback("#ai-default-optimization-logic", baseAi.defaultOptimizationLogic)
    };
  }

  function hydrateAiProvider(provider) {
    const resolvedProvider = String(provider || "").trim();
    if (!resolvedProvider || builtInAiProviders.has(resolvedProvider)) {
      setValue("#ai-provider", resolvedProvider || "Qwen");
      setValue("#ai-custom-provider", "");
    } else if (resolvedProvider === "Custom API") {
      setValue("#ai-provider", customProviderValue);
      setValue("#ai-custom-provider", "");
    } else {
      setValue("#ai-provider", customProviderValue);
      setValue("#ai-custom-provider", resolvedProvider);
    }
    syncCustomProviderField();
  }

  function handleAiProviderChange() {
    syncCustomProviderField();
    hydrateAiModel("");
  }

  function syncCustomProviderField() {
    const isCustom = valueOf("#ai-provider") === customProviderValue;
    const group = document.querySelector("#ai-custom-provider-group");
    const input = document.querySelector("#ai-custom-provider");
    group?.classList.toggle("is-hidden", !isCustom);
    if (input) {
      input.required = isCustom;
    }
  }

  function resolveAiProvider(fallbackProvider) {
    if (valueOf("#ai-provider") !== customProviderValue) {
      return valueOrFallback("#ai-provider", fallbackProvider);
    }

    const customProvider = valueOf("#ai-custom-provider");
    const fallback = String(fallbackProvider || "").trim();
    return customProvider || (fallback === "Custom API" ? "" : fallback) || "Qwen";
  }

  function hydrateAiModel(model) {
    const selectedModel = String(model || "").trim();
    const modelSelect = document.querySelector("#ai-model");
    if (!modelSelect) {
      return;
    }

    renderAiModelOptions(selectedModel);
    syncCustomModelField();
  }

  function renderAiModelOptions(selectedModel) {
    const modelSelect = document.querySelector("#ai-model");
    if (!modelSelect) {
      return;
    }

    const provider = valueOf("#ai-provider");
    const isCustomProvider = provider === customProviderValue;
    const options = isCustomProvider ? [] : aiModelOptionsByProvider[provider] || [];
    modelSelect.innerHTML = "";

    options.forEach((model) => {
      modelSelect.add(new Option(model, model));
    });
    modelSelect.add(new Option(t("手动输入模型", "Manual Model"), customModelValue));

    if (isCustomProvider) {
      modelSelect.value = customModelValue;
      setValue("#ai-custom-model", selectedModel);
      return;
    }

    if (selectedModel && options.includes(selectedModel)) {
      modelSelect.value = selectedModel;
      setValue("#ai-custom-model", "");
      return;
    }

    if (selectedModel) {
      modelSelect.value = customModelValue;
      setValue("#ai-custom-model", selectedModel);
      return;
    }

    modelSelect.value = options[0] || customModelValue;
    setValue("#ai-custom-model", "");
  }

  function syncCustomModelField() {
    const isCustom = valueOf("#ai-model") === customModelValue;
    const group = document.querySelector("#ai-custom-model-group");
    const input = document.querySelector("#ai-custom-model");
    group?.classList.toggle("is-hidden", !isCustom);
    if (input) {
      input.required = isCustom;
      if (!isCustom) {
        input.value = "";
      }
    }
  }

  function resolveAiModel(fallbackModel) {
    if (valueOf("#ai-model") !== customModelValue) {
      return valueOrFallback("#ai-model", fallbackModel);
    }

    const customModel = valueOf("#ai-custom-model");
    const fallback = String(fallbackModel || "").trim();
    return customModel || (fallback === customModelValue ? "" : fallback);
  }

  function refreshAiModelOptionsForLocale() {
    hydrateAiModel(resolveAiModel(""));
  }

  function validateAiManualFields(resultSelector) {
    if (valueOf("#ai-provider") === customProviderValue && !valueOf("#ai-custom-provider")) {
      setResult(
        resultSelector,
        t("请输入厂商名称。", "Enter the provider name."),
        false
      );
      document.querySelector("#ai-custom-provider")?.focus();
      return false;
    }

    if (valueOf("#ai-model") === customModelValue && !valueOf("#ai-custom-model")) {
      setResult(
        resultSelector,
        t("请输入模型名称。", "Enter the model name."),
        false
      );
      document.querySelector("#ai-custom-model")?.focus();
      return false;
    }

    return true;
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

  function valueOrFallback(selector, fallbackValue) {
    const element = document.querySelector(selector);
    return element ? String(element.value ?? "").trim() : String(fallbackValue ?? "").trim();
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
