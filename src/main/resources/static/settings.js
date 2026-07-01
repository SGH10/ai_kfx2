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
  const aiTestState = {
    kind: "",
    payload: null,
    success: false
  };
  const defaultEmailTemplates = [
    {
      id: "tpl-first-zh",
      name: "初次接触（中文）",
      language: "zh-CN",
      scenario: "first-contact",
      subject: "想和 {{recipientCompany}} 聊聊 {{productName}} 的合作机会",
      body: `{{contactName}}您好，

我是 {{senderName}}，来自 {{senderCompany}}，我们在做 {{productName}} 相关方案。留意到 {{recipientCompany}} 的业务方向后，感觉我们的产品可能能在 {{valueProposition}} 这类场景里提供一些实际帮助。

如果您现在也在关注类似需求，我可以先发一份简短资料，或者约 15 分钟沟通一下，看是否值得进一步对接。

祝好，
{{senderName}}`,
      instruction: "适合第一次联系目标客户。不要机械照抄模板，要根据客户公司和产品价值写出具体、自然的合作理由；如果没有真实联系人姓名，只用“您好”。",
      enabled: true
    },
    {
      id: "tpl-first-en",
      name: "First Contact (English)",
      language: "en",
      scenario: "first-contact",
      subject: "Potential fit around {{productName}}",
      body: `Hi {{contactName}},

I am {{senderName}} from {{senderCompany}}. We provide {{productName}} and help teams like {{recipientCompany}} {{valueProposition}}.

Would you be open to a short 15-minute call to see whether there is a fit?

Best regards,
{{senderName}}`,
      instruction: "Use for first-touch outbound emails. Keep it concise, relevant, and focused on one clear call to action.",
      enabled: true
    },
    {
      id: "tpl-follow-up-en",
      name: "Follow-up Email",
      language: "en",
      scenario: "follow-up",
      subject: "Quick follow-up on {{productName}}",
      body: `Hi {{contactName}},

I wanted to quickly follow up on my previous note about {{productName}}.

If this is relevant for {{recipientCompany}}, I would be happy to share a short overview or a few practical examples.

Best,
{{senderName}}`,
      instruction: "Use after no reply. Keep it lighter than the first email and avoid repeating the whole pitch.",
      enabled: true
    }
  ];
  const templateState = {
    defaultTemplateId: "",
    items: [],
    selectedId: ""
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

    if (path === "/email-templates") {
      bindTemplateSettings(settings);
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
    window.addEventListener("leadflow:locale-changed", handleAiLocaleChange);

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
      aiTestState.kind = "pending";
      aiTestState.payload = null;
      aiTestState.success = true;

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
          aiTestState.kind = "error";
          aiTestState.payload = rawText;
          aiTestState.success = false;
          setResult(
            "#ai-test-result",
            formatAiTestError(rawText),
            false
          );
          return;
        }

        const result = rawText ? JSON.parse(rawText) : null;
        aiTestState.kind = "success";
        aiTestState.payload = result;
        aiTestState.success = true;
        const successMessage = formatAiTestSuccess(result);
        setResult("#ai-test-result", successMessage, true);
      } catch (error) {
        console.error("AI connection test failed:", error);
        aiTestState.kind = "network-error";
        aiTestState.payload = null;
        aiTestState.success = false;
        setResult(
          "#ai-test-result",
          formatAiTestNetworkError(),
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

  function bindTemplateSettings(settings) {
    hydrateTemplateState(settings.templates);
    renderTemplateList();
    renderSelectedTemplate();

    document.querySelector("#template-list")?.addEventListener("click", (event) => {
      const button = event.target.closest("[data-template-id]");
      if (!button) {
        return;
      }
      persistSelectedTemplateForm();
      templateState.selectedId = button.getAttribute("data-template-id");
      renderTemplateList();
      renderSelectedTemplate();
    });

    document.querySelector("#template-new")?.addEventListener("click", () => {
      persistSelectedTemplateForm();
      const template = createBlankTemplate();
      templateState.items.push(template);
      templateState.selectedId = template.id;
      if (!templateState.defaultTemplateId) {
        templateState.defaultTemplateId = template.id;
      }
      renderTemplateList();
      renderSelectedTemplate();
      setResult("#template-settings-result", t("已新建模板，保存配置后生效。", "Template created. Save settings to apply."), true);
    });

    document.querySelector("#template-duplicate")?.addEventListener("click", () => {
      persistSelectedTemplateForm();
      const current = getSelectedTemplate();
      if (!current) {
        return;
      }
      const copy = {
        ...current,
        id: createTemplateId(),
        name: `${current.name || t("未命名模板", "Untitled Template")} ${t("副本", "Copy")}`
      };
      templateState.items.push(copy);
      templateState.selectedId = copy.id;
      renderTemplateList();
      renderSelectedTemplate();
      setResult("#template-settings-result", t("已复制模板，保存配置后生效。", "Template duplicated. Save settings to apply."), true);
    });

    document.querySelector("#template-delete")?.addEventListener("click", () => {
      persistSelectedTemplateForm();
      if (templateState.items.length <= 1) {
        setResult("#template-settings-result", t("至少需要保留一个模板。", "Keep at least one template."), false);
        return;
      }
      const current = getSelectedTemplate();
      if (!current) {
        return;
      }
      templateState.items = templateState.items.filter((item) => item.id !== current.id);
      if (templateState.defaultTemplateId === current.id) {
        templateState.defaultTemplateId = templateState.items[0].id;
      }
      templateState.selectedId = templateState.items[0].id;
      renderTemplateList();
      renderSelectedTemplate();
      setResult("#template-settings-result", t("已删除模板，保存配置后生效。", "Template deleted. Save settings to apply."), true);
    });

    document.querySelector("#template-reset")?.addEventListener("click", () => {
      const defaults = normalizeTemplateSettings(null);
      templateState.items = defaults.items;
      templateState.defaultTemplateId = defaults.defaultTemplateId;
      templateState.selectedId = defaults.defaultTemplateId;
      renderTemplateList();
      renderSelectedTemplate();
      setResult("#template-settings-result", t("已恢复默认模板，保存配置后生效。", "Default templates restored. Save settings to apply."), true);
    });

    document.querySelector("#template-default")?.addEventListener("change", (event) => {
      if (event.target.checked && templateState.selectedId) {
        templateState.defaultTemplateId = templateState.selectedId;
        renderTemplateList();
      } else if (!event.target.checked) {
        event.target.checked = true;
      }
    });

    document.querySelector("#template-enabled")?.addEventListener("change", persistSelectedTemplateForm);
    document.querySelector("#template-language")?.addEventListener("change", persistSelectedTemplateForm);

    document.querySelector("#template-settings-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      persistSelectedTemplateForm();
      if (!validateTemplateState()) {
        return;
      }

      const response = await fetch("/api/settings/templates", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(buildTemplateSettingsPayload())
      });

      if (!response.ok) {
        setResult("#template-settings-result", t("模板保存失败，请稍后重试。", "Template save failed. Please try again."), false);
        return;
      }

      hydrateTemplateState(await response.json());
      renderTemplateList();
      renderSelectedTemplate();
      setResult("#template-settings-result", t("邮件模板已保存，开发信页面会使用最新模板。", "Email templates saved. The outreach page will use the latest templates."), true);
    });

    window.addEventListener("leadflow:locale-changed", () => {
      persistSelectedTemplateForm();
      renderTemplateList();
      renderSelectedTemplate();
    });
  }

  function hydrateTemplateState(templateSettings) {
    const normalized = normalizeTemplateSettings(templateSettings);
    templateState.defaultTemplateId = normalized.defaultTemplateId;
    templateState.items = normalized.items;
    templateState.selectedId = normalized.defaultTemplateId;
  }

  function normalizeTemplateSettings(templateSettings) {
    const sourceItems = Array.isArray(templateSettings?.items) && templateSettings.items.length > 0
      ? templateSettings.items
      : defaultEmailTemplates;
    const items = sourceItems.map(normalizeTemplate).filter((item) => item.id);
    const normalizedItems = items.length > 0 ? items : defaultEmailTemplates.map(normalizeTemplate);
    const requestedDefaultId = String(templateSettings?.defaultTemplateId || "").trim();
    const defaultTemplateId = normalizedItems.some((item) => item.id === requestedDefaultId)
      ? requestedDefaultId
      : normalizedItems[0].id;
    return {
      defaultTemplateId,
      items: normalizedItems
    };
  }

  function normalizeTemplate(template) {
    return {
      id: String(template?.id || createTemplateId()).trim(),
      name: String(template?.name || t("未命名模板", "Untitled Template")).trim(),
      language: String(template?.language || "zh-CN").trim(),
      scenario: String(template?.scenario || "first-contact").trim(),
      subject: String(template?.subject || ""),
      body: String(template?.body || ""),
      instruction: String(template?.instruction || ""),
      enabled: template?.enabled !== false
    };
  }

  function renderTemplateList() {
    const list = document.querySelector("#template-list");
    const status = document.querySelector("#template-count-status");
    if (!list) {
      return;
    }

    if (status) {
      status.textContent = t(`${templateState.items.length} 个模板`, `${templateState.items.length} templates`);
    }

    list.innerHTML = templateState.items
      .map((template) => {
        const isSelected = template.id === templateState.selectedId;
        const isDefault = template.id === templateState.defaultTemplateId;
        const statusLabel = template.enabled ? t("启用", "Enabled") : t("停用", "Disabled");
        return `
          <button class="template-list-item ${isSelected ? "is-active" : ""}" type="button" data-template-id="${escapeHtml(template.id)}" role="option" aria-selected="${isSelected}">
            <span>
              <strong>${escapeHtml(template.name)}</strong>
              <small>${escapeHtml(languageLabel(template.language))} · ${escapeHtml(template.scenario || t("未设置场景", "No scenario"))}</small>
            </span>
            <span class="template-list-badges">
              ${isDefault ? `<em>${escapeHtml(t("默认", "Default"))}</em>` : ""}
              <em class="${template.enabled ? "green" : "gray"}">${escapeHtml(statusLabel)}</em>
            </span>
          </button>
        `;
      })
      .join("");
  }

  function renderSelectedTemplate() {
    const current = getSelectedTemplate();
    const form = document.querySelector("#template-settings-form");
    if (!current) {
      form?.classList.add("is-empty");
      return;
    }

    form?.classList.remove("is-empty");
    setValue("#template-name", current.name);
    setValue("#template-language", current.language);
    setValue("#template-scenario", current.scenario);
    setValue("#template-subject", current.subject);
    setValue("#template-body", current.body);
    setValue("#template-instruction", current.instruction);
    setChecked("#template-enabled", current.enabled);
    setChecked("#template-default", current.id === templateState.defaultTemplateId);
  }

  function persistSelectedTemplateForm() {
    const current = getSelectedTemplate();
    if (!current) {
      return;
    }

    current.name = valueOf("#template-name") || current.name || t("未命名模板", "Untitled Template");
    current.language = valueOf("#template-language") || "zh-CN";
    current.scenario = valueOf("#template-scenario") || "first-contact";
    current.subject = valueOf("#template-subject");
    current.body = valueOf("#template-body");
    current.instruction = valueOf("#template-instruction");
    current.enabled = document.querySelector("#template-enabled")?.checked !== false;

    if (document.querySelector("#template-default")?.checked) {
      templateState.defaultTemplateId = current.id;
    }
    renderTemplateList();
  }

  function validateTemplateState() {
    if (templateState.items.length === 0) {
      setResult("#template-settings-result", t("至少需要保留一个模板。", "Keep at least one template."), false);
      return false;
    }

    const invalid = templateState.items.find((item) => !item.name.trim());
    if (invalid) {
      templateState.selectedId = invalid.id;
      renderTemplateList();
      renderSelectedTemplate();
      setResult("#template-settings-result", t("请填写模板名称。", "Enter a template name."), false);
      document.querySelector("#template-name")?.focus();
      return false;
    }

    const hasEnabled = templateState.items.some((item) => item.enabled);
    if (!hasEnabled) {
      setResult("#template-settings-result", t("至少需要启用一个模板，开发信页面才有可选模板。", "Enable at least one template so the outreach page has a selectable template."), false);
      return false;
    }

    if (!templateState.items.some((item) => item.id === templateState.defaultTemplateId)) {
      templateState.defaultTemplateId = templateState.items[0].id;
    }
    return true;
  }

  function buildTemplateSettingsPayload() {
    return {
      defaultTemplateId: templateState.defaultTemplateId,
      items: templateState.items.map((item) => ({
        id: item.id,
        name: item.name,
        language: item.language,
        scenario: item.scenario,
        subject: item.subject,
        body: item.body,
        instruction: item.instruction,
        enabled: item.enabled
      }))
    };
  }

  function getSelectedTemplate() {
    return templateState.items.find((item) => item.id === templateState.selectedId) || templateState.items[0] || null;
  }

  function createBlankTemplate() {
    return {
      id: createTemplateId(),
      name: t("新建模板", "New Template"),
      language: localStorage.getItem("leadflow-locale") === "en" ? "en" : "zh-CN",
      scenario: "first-contact",
      subject: "",
      body: "",
      instruction: "",
      enabled: true
    };
  }

  function createTemplateId() {
    return `tpl-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
  }

  function languageLabel(language) {
    const labels = {
      "zh-CN": t("中文", "Chinese"),
      en: t("英语", "English"),
      de: t("德语", "German"),
      fr: t("法语", "French"),
      es: t("西班牙语", "Spanish"),
      ru: t("俄语", "Russian"),
      ar: t("阿拉伯语", "Arabic"),
      pt: t("葡萄牙语", "Portuguese")
    };
    return labels[language] || language || t("未知语言", "Unknown Language");
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

  function handleAiLocaleChange() {
    refreshAiModelOptionsForLocale();
    renderStoredAiTestResult();
  }

  function renderStoredAiTestResult() {
    if (aiTestState.kind === "pending") {
      setResult(
        "#ai-test-result",
        t("正在测试连接，请稍候...", "Testing connection, please wait..."),
        true
      );
      return;
    }

    if (aiTestState.kind === "success") {
      setResult("#ai-test-result", formatAiTestSuccess(aiTestState.payload), true);
      return;
    }

    if (aiTestState.kind === "error") {
      setResult("#ai-test-result", formatAiTestError(aiTestState.payload), false);
      return;
    }

    if (aiTestState.kind === "network-error") {
      setResult("#ai-test-result", formatAiTestNetworkError(), false);
      return;
    }

    if (aiTestState.kind === "validation") {
      setResult("#ai-test-result", formatAiValidationMessage(aiTestState.payload), false);
    }
  }

  function validateAiManualFields(resultSelector) {
    if (valueOf("#ai-provider") === customProviderValue && !valueOf("#ai-custom-provider")) {
      aiTestState.kind = "validation";
      aiTestState.payload = "provider";
      aiTestState.success = false;
      setResult(
        resultSelector,
        formatAiValidationMessage("provider"),
        false
      );
      document.querySelector("#ai-custom-provider")?.focus();
      return false;
    }

    if (valueOf("#ai-model") === customModelValue && !valueOf("#ai-custom-model")) {
      aiTestState.kind = "validation";
      aiTestState.payload = "model";
      aiTestState.success = false;
      setResult(
        resultSelector,
        formatAiValidationMessage("model"),
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

  function formatAiValidationMessage(type) {
    if (type === "provider") {
      return t("请输入厂商名称。", "Enter the provider name.");
    }
    return t("请输入模型名称。", "Enter the model name.");
  }

  function formatAiTestNetworkError() {
    return t(
      "连接测试失败，请检查网络或稍后重试。",
      "Connection test failed. Please check the network or try again later."
    );
  }

  function formatAiTestFallbackError() {
    return t(
      "连接测试失败，请检查配置后重试。",
      "Connection test failed. Please review the configuration and try again."
    );
  }

  function formatAiTestError(rawText) {
    const raw = String(rawText || "").trim();
    if (!raw) {
      return formatAiTestFallbackError();
    }

    const lower = raw.toLowerCase();
    const codeMatch = raw.match(/\((\d{3})\)/);
    const statusCode = codeMatch ? codeMatch[1] : "";
    let summary = "";

    if (lower.includes("model") && (lower.includes("does not exist") || lower.includes("not found") || lower.includes("access"))) {
      summary = t(
        "模型不存在，或当前 API Key 没有权限使用该模型。",
        "The model does not exist, or the current API key does not have access to it."
      );
    } else if (statusCode === "401" || lower.includes("unauthorized") || lower.includes("invalid api key") || lower.includes("invalid_api_key")) {
      summary = t(
        "API Key 无效、已过期，或没有正确传递。",
        "The API key is invalid, expired, or was not sent correctly."
      );
    } else if (statusCode === "403" || lower.includes("forbidden") || lower.includes("permission")) {
      summary = t(
        "当前账号或 API Key 没有权限访问这个模型或接口。",
        "The current account or API key does not have permission to access this model or endpoint."
      );
    } else if (statusCode === "404") {
      summary = t(
        "接口地址或模型不存在，请检查 Base URL 和模型名称是否匹配当前厂商。",
        "The endpoint or model was not found. Check that the Base URL and model name match the selected provider."
      );
    } else if (statusCode === "429" || lower.includes("rate limit") || lower.includes("quota") || lower.includes("insufficient")) {
      summary = t(
        "请求过快、额度不足，或当前账号没有可用配额。",
        "The request was rate-limited, quota is insufficient, or the account has no available capacity."
      );
    } else if (lower.includes("timeout") || lower.includes("timed out")) {
      summary = t(
        "连接超时，请检查网络、代理或 Base URL。",
        "The request timed out. Check the network, proxy, or Base URL."
      );
    } else if (lower.includes("failed to connect") || lower.includes("connect")) {
      summary = t(
        "无法连接到 AI 网关，请检查 Base URL、网络或代理设置。",
        "Could not connect to the AI gateway. Check the Base URL, network, or proxy settings."
      );
    } else {
      summary = formatAiTestFallbackError();
    }

    const rawLabel = t("原始返回", "Raw response");
    return `${summary}\n\n${rawLabel}:\n${raw}`;
  }

  function setValue(selector, value) {
    const element = document.querySelector(selector);
    if (element) {
      element.value = value ?? "";
    }
  }

  function setChecked(selector, value) {
    const element = document.querySelector(selector);
    if (element) {
      element.checked = Boolean(value);
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

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#39;");
  }

  function t(zh, en) {
    const locale = localStorage.getItem("leadflow-locale");
    return locale === "en" ? en : zh;
  }
})();
