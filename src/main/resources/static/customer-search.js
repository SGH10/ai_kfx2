(() => {
  const searchStorageKey = "leadflow-search-response";
  const selectedCustomersKey = "leadflow-selected-customers";
  const outreachImportFlagKey = "leadflow-outreach-import-pending";
  const searchHistoryKey = "leadflow-search-history";
  const maxHistoryItems = 20;

  const searchForm = document.querySelector("#search-form");
  const targetDescription = document.querySelector("#target-description");
  const industryPreset = document.querySelector("#industry-preset");
  const industryCustomGroup = document.querySelector("#industry-custom-group");
  const industryCustom = document.querySelector("#industry-custom");
  const marketPreset = document.querySelector("#market-preset");
  const keywordsPreset = document.querySelector("#keywords-preset");
  const keywordsCustomGroup = document.querySelector("#keywords-custom-group");
  const keywordsCustom = document.querySelector("#keywords-custom");
  const searchDepth = document.querySelector("#search-depth");
  const requestedLimitInput = document.querySelector("#requested-limit");

  const searchLogs = document.querySelector("#search-logs");
  const searchSummary = document.querySelector("#search-summary");
  const searchTime = document.querySelector("#search-time");
  const searchStatusChip = document.querySelector("#search-status-chip");
  const resultsBody = document.querySelector("#results-body");
  const exportButton = document.querySelector("#export-results");
  const pushButton = document.querySelector("#push-to-outreach");
  const selectedCount = document.querySelector("#selected-count");
  const selectAll = document.querySelector("#select-all");
  const searchResultCount = document.querySelector("#search-result-count");

  const historyToggle = document.querySelector("#history-toggle");
  const historyPanel = document.querySelector("#history-panel");
  const historyList = document.querySelector("#history-list");
  const historyEmpty = document.querySelector("#history-empty");
  const historyClear = document.querySelector("#history-clear");

  const statTotal = document.querySelector("#stat-total");
  const statEmail = document.querySelector("#stat-email");
  const statMatch = document.querySelector("#stat-match");
  const statMarket = document.querySelector("#stat-market");

  const searchState = {
    customers: [],
    selectedIds: new Set(),
    activeController: null,
    configuredLimit: 12,
    requestedLimit: 12,
    maxSearchDurationMs: 300000,
    startTime: 0,
    timerInterval: null,
    lastResponse: null,
    statusKey: "search.status.pending",
    statusType: "pending"
  };

  const progressTrack = document.querySelector(".progress-track");

  function currentLocale() {
    return typeof window.leadflowLocale?.locale === "string" ? window.leadflowLocale.locale : "zh-CN";
  }

  function t(key, params = {}) {
    const locale = currentLocale();
    const messages = window.pageTranslations || {};
    const dict = messages[locale] || messages["zh-CN"] || {};
    const fallback = messages["zh-CN"] || {};
    const value = dict[key] || fallback[key] || key;
    return String(value).replace(/\{(\w+)\}/g, (_, name) => params[name] ?? "");
  }

  function localizeSearchText(text, data = null) {
    const raw = String(text || "").trim();
    const locale = currentLocale();
    if (!raw) {
      return "";
    }
    if (locale !== "en") {
      return raw;
    }

    const collectedMatch = raw.match(/已从公开搜索结果中收集到\s*(\d+)\s*条企业线索/);
    if (collectedMatch) {
      return t("search.summary.collected", { count: collectedMatch[1] });
    }
    if (raw === "已完成搜索。") {
      return buildCompletedSummary(data);
    }
    if (raw === "正在抓取客户线索，请稍候...") {
      return t("search.summary.running");
    }
    if (raw === "搜索耗时较长，请稍后刷新页面查看结果。") {
      return t("search.error.timeout");
    }
    if (raw === "客户搜索失败，请检查服务状态后重试。") {
      return t("search.error.failed");
    }
    if (raw.startsWith("从历史记录恢复：")) {
      return raw.replace("从历史记录恢复：", "Restored from history: ");
    }
    return raw;
  }

  function localizeLogTime(time) {
    const raw = String(time || "--:--:--");
    if (currentLocale() !== "en") {
      return raw;
    }
    if (raw === "进行中") {
      return t("search.log.runningTime");
    }
    if (raw === "失败") {
      return t("search.log.failedTime");
    }
    return raw;
  }

  function localizeChannel(channel) {
    const raw = String(channel || "").trim();
    if (!raw) {
      return t("search.table.officialWebsite");
    }

    if (raw === "官网" || raw === "Official website") {
      return t("search.table.officialWebsite");
    }
    if (raw === "搜索引擎 + 官网" || raw === "Search engine + website") {
      return t("search.table.searchEngineWebsite");
    }
    if (raw === "公开搜索" || raw === "Public search") {
      return t("search.table.publicSearch");
    }
    return raw;
  }

  function buildCompletedSummary(data = null) {
    const count = Array.isArray(data?.customers) ? data.customers.length : searchState.customers.length;
    return count > 0 ? t("search.summary.collected", { count }) : t("search.summary.complete");
  }

  function statusKeyForLabel(label) {
    const map = {
      "等待开始": "search.status.pending",
      "搜索中...": "search.status.running",
      "已完成": "search.status.complete",
      "无结果": "search.status.noResults",
      "搜索失败": "search.status.failed",
      "已恢复": "search.status.restored",
      Waiting: "search.status.pending",
      "Searching...": "search.status.running",
      Complete: "search.status.complete",
      "No Results": "search.status.noResults",
      "Search Failed": "search.status.failed",
      Restored: "search.status.restored"
    };
    return map[label] || (String(label || "").startsWith("search.") ? label : "");
  }

  function renderSearchStatus() {
    if (!searchStatusChip) {
      return;
    }
    searchStatusChip.textContent = searchState.statusKey ? t(searchState.statusKey) : "";
    searchStatusChip.className = `status-chip ${searchState.statusType}`;
  }

  function renderSearchTime(timestamp) {
    if (!searchTime) {
      return;
    }
    if (!timestamp) {
      searchTime.textContent = "";
      return;
    }
    const label = currentLocale() === "en" ? "Search time: " : "搜索时间：";
    searchTime.textContent = label + formatFullTime(new Date(timestamp));
  }

  function refreshLocalizedDynamicContent() {
    renderSearchStatus();

    if (searchState.lastResponse) {
      const data = searchState.lastResponse;
      const summary = data.summary || buildCompletedSummary(data);
      searchSummary.textContent = localizeSearchText(summary, data);
      renderSearchTime(data.timestamp);
      renderLogs(compactLogs(data.logs || [], summary));
      renderStats(data.stats || {});
    } else if (searchState.statusKey === "search.status.running") {
      searchSummary.textContent = t("search.summary.running");
      renderSearchTime(null);
    } else if (searchSummary?.dataset.i18n) {
      searchSummary.textContent = t(searchSummary.dataset.i18n);
    }

    renderResults(searchState.customers);
    refreshSelectionState();
    renderSearchHistory();

    if (typeof currentModalLeadId !== "undefined" && currentModalLeadId) {
      const lead = searchState.customers.find((customer) => customer.id === currentModalLeadId);
      if (lead) {
        openLeadModal(lead);
      }
    }
  }

  init().catch((error) => {
    console.error("Customer search bootstrap failed:", error);
  });

  async function init() {
    await hydrateSettings();
    hydrateSearchResult();
    bindFormBehavior();

    searchForm?.addEventListener("submit", handleSearchSubmit);
    exportButton?.addEventListener("click", exportSelectedCustomers);
    pushButton?.addEventListener("click", pushSelectedCustomers);
    resultsBody?.addEventListener("change", handleResultSelection);
    selectAll?.addEventListener("change", handleSelectAll);

    bindHistoryPanel();
    renderSearchHistory();
    window.addEventListener("leadflow:locale-changed", refreshLocalizedDynamicContent);
  }

  async function hydrateSettings() {
    try {
      const response = await fetch("/api/settings", { cache: "no-store" });
      if (!response.ok) {
        return;
      }
      const settings = await response.json();
      const configuredLimit = normalizeRequestedLimit(settings.search?.resultsPerPage, 12);
      searchState.configuredLimit = configuredLimit;
      searchState.requestedLimit = configuredLimit;
      searchState.maxSearchDurationMs = Number(settings.crawler?.maxSearchDurationMs || 300000);
      if (requestedLimitInput) {
        requestedLimitInput.value = String(configuredLimit);
      }
    } catch (error) {
      console.error("Failed to load search settings:", error);
    }
  }

  function bindFormBehavior() {
    toggleCustomField(industryPreset, industryCustomGroup, industryCustom);
    if (keywordsPreset || keywordsCustomGroup || keywordsCustom) {
      toggleCustomField(keywordsPreset, keywordsCustomGroup, keywordsCustom);
    }

    industryPreset?.addEventListener("change", () => {
      toggleCustomField(industryPreset, industryCustomGroup, industryCustom);
    });

    keywordsPreset?.addEventListener("change", () => {
      toggleCustomField(keywordsPreset, keywordsCustomGroup, keywordsCustom);
    });
  }

  function toggleCustomField(select, group, input) {
    if (!select && !group && !input) {
      return;
    }

    const isCustom = select?.value === "custom";
    group?.classList.toggle("is-hidden", !isCustom);
    if (input) {
      input.required = Boolean(isCustom);
      if (!isCustom) {
        input.value = "";
      }
    }
  }

  async function handleSearchSubmit(event) {
    event.preventDefault();

    if (!searchForm) {
      return;
    }

    const submitButton = document.querySelector("#search-submit");
    if (!submitButton) {
      return;
    }

    if (searchState.activeController) {
      searchState.activeController.abort();
    }

    const controller = new AbortController();
    searchState.activeController = controller;
    const timeoutId = window.setTimeout(() => controller.abort("timeout"), searchState.maxSearchDurationMs || 300000);

    // Build payload first so searchState.requestedLimit is updated before rendering stats
    const payload = buildSearchPayload();

    resetSearchViewForPending();
    setSearchStatus("search.status.running", "running");
    submitButton.disabled = true;
    submitButton.textContent = t("search.status.running");

    // Start real timer and indeterminate progress bar
    searchState.startTime = Date.now();
    clearSearchTimer();
    searchState.timerInterval = window.setInterval(updateRunningTimer, 100);
    setProgressBar(null, true);

    renderLogs([
      { time: t("search.log.runningTime"), message: t("search.log.runningRequest") },
      { time: t("search.log.runningTime"), message: t("search.log.runningExtract") }
    ]);

    try {
      const response = await fetch("/api/customers/search", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      });

      if (!response.ok) {
        const errorText = await response.text().catch(() => "");
        throw new Error(errorText || "Search API returned a non-OK response.");
      }

      const data = await response.json();
      data.timestamp = Date.now();
      data.requestedLimit = payload.requestedLimit;
      applySearchResponse(data);
      cacheSearchResponse(data);
      saveSearchHistory(data, payload);
      setSearchStatus(data.customers?.length ? "search.status.complete" : "search.status.noResults", data.customers?.length ? "complete" : "error");
    } catch (error) {
      console.error("Customer search failed:", error);
      const message = error?.name === "AbortError"
        ? t("search.error.timeout")
        : t("search.error.failed");
      clearSearchResults();
      renderLogs([{ time: t("search.log.failedTime"), message }]);
      searchSummary.textContent = message;
      if (searchTime) searchTime.textContent = "";
      setSearchStatus("search.status.failed", "error");
    } finally {
      window.clearTimeout(timeoutId);
      clearSearchTimer();
      if (searchState.activeController === controller) {
        searchState.activeController = null;
      }
      submitButton.disabled = false;
      submitButton.textContent = t("search.startButton");
    }
  }

  function buildSearchPayload() {
    const companySize = String(new FormData(searchForm).get("companySize") || "").trim();
    const industry = industryPreset?.value === "custom"
      ? String(industryCustom?.value || "").trim()
      : String(industryPreset?.value || "").trim();
    const keywords = keywordsPreset
      ? (keywordsPreset?.value === "custom"
        ? String(keywordsCustom?.value || "").trim()
        : String(keywordsPreset?.value || "").trim())
      : "";
    const market = String(marketPreset?.value || "").trim();
    const description = String(targetDescription?.value || "").trim();
    const requestedLimit = normalizeRequestedLimit(requestedLimitInput?.value, searchState.configuredLimit, 12);
    const depth = String(searchDepth?.value || "standard");

    let resolvedIndustry = industry;
    let resolvedKeywords = keywords;
    let resolvedMarket = market;

    if (description) {
      resolvedKeywords = resolvedKeywords || description;
      if (description.includes("中国")) {
        resolvedMarket = "中国";
      } else if (description.includes("美国")) {
        resolvedMarket = "美国";
      } else if (description.includes("德国")) {
        resolvedMarket = "德国";
      }
    }

    searchState.requestedLimit = requestedLimit;

    return {
      industry: resolvedIndustry,
      market: resolvedMarket,
      keywords: resolvedKeywords,
      companySize,
      requestedLimit
    };
  }

  function cacheSearchResponse(data) {
    try {
      const cachedData = {
        ...data,
        requestedLimit: normalizeRequestedLimit(
          data?.requestedLimit,
          searchState.configuredLimit,
          data?.stats?.totalCustomers,
          data?.customers?.length
        )
      };
      localStorage.setItem(searchStorageKey, JSON.stringify(cachedData));
    } catch (error) {
      console.error("Failed to cache search response:", error);
    }
  }

  function hydrateSearchResult() {
    const cached = localStorage.getItem(searchStorageKey);
    if (!cached) {
      setSearchStatus("search.status.pending", "pending");
      if (searchTime) searchTime.textContent = "";
      if (searchResultCount) {
        searchResultCount.textContent = "0";
      }
      return;
    }

    try {
      const data = JSON.parse(cached);
      applySearchResponse(data, { preserveFormLimit: true });
      setSearchStatus(data.customers?.length ? "search.status.complete" : "search.status.noResults", data.customers?.length ? "complete" : "error");
    } catch (error) {
      localStorage.removeItem(searchStorageKey);
      renderLogs([]);
      setSearchStatus("search.status.pending", "pending");
      if (searchTime) searchTime.textContent = "";
      if (searchResultCount) {
        searchResultCount.textContent = "0";
      }
    }
  }

  function applySearchResponse(data, options = {}) {
    searchState.lastResponse = data;
    searchState.customers = Array.isArray(data.customers) ? data.customers : [];
    searchState.selectedIds = new Set(searchState.customers.map((customer) => customer.id));
    searchState.requestedLimit = resolveResponseRequestedLimit(data);
    if (!options.preserveFormLimit && requestedLimitInput) {
      requestedLimitInput.value = String(searchState.requestedLimit);
    }

    const summary = data.summary || buildCompletedSummary(data);
    searchSummary.textContent = localizeSearchText(summary, data);
    renderSearchTime(data.timestamp);
    renderLogs(compactLogs(data.logs || [], summary));
    renderStats(data.stats || {});
    renderResults(searchState.customers);
    refreshSelectionState();
    if (searchResultCount) {
      searchResultCount.textContent = String(searchState.customers.length);
    }
  }

  function resetSearchViewForPending() {
    searchState.lastResponse = null;
    searchState.customers = [];
    searchState.selectedIds = new Set();
    searchState.startTime = Date.now();
    clearSearchTimer();
    setProgressBar(null, true);
    renderStats({
      totalCustomers: 0,
      emailCount: 0,
      highMatchCount: 0,
      marketCoverage: 0
    });
    renderResults([]);
    refreshSelectionState();
    searchSummary.textContent = t("search.summary.running");
    if (searchTime) searchTime.textContent = "";
    if (searchResultCount) {
      searchResultCount.textContent = "0";
    }
  }

  function clearSearchResults() {
    searchState.lastResponse = null;
    searchState.customers = [];
    searchState.selectedIds = new Set();
    localStorage.removeItem(searchStorageKey);
    clearSearchTimer();
    searchState.startTime = 0;
    setProgressBar(0, false);
    renderStats({
      totalCustomers: 0,
      emailCount: 0,
      highMatchCount: 0,
      marketCoverage: 0
    });
    renderResults([]);
    refreshSelectionState();
    if (searchResultCount) {
      searchResultCount.textContent = "0";
    }
  }

  function renderLogs(logs) {
    if (!Array.isArray(logs) || logs.length === 0) {
      searchLogs.innerHTML = `
        <li class="log-item">
          <span class="log-time">--:--:--</span>
          <span class="log-text">${escapeHtml(t("search.log.noNew"))}</span>
        </li>
      `;
      return;
    }

    searchLogs.innerHTML = logs
      .map(
        (log) => `
          <li class="log-item">
            <span class="log-time">${escapeHtml(localizeLogTime(log.time || "--:--:--"))}</span>
            <span class="log-text">${escapeHtml(localizeSearchText(log.message || ""))}</span>
          </li>
        `
      )
      .join("");
  }

  function compactLogs(logs, summaryText) {
    if (!Array.isArray(logs) || logs.length === 0) {
      return [];
    }

    const strategyLog = logs.find((log) => String(log.message || "").includes("Search strategy"));
    const finalLog = logs[logs.length - 1];
    const compacted = [];

    if (strategyLog) {
      compacted.push(strategyLog);
    }
    if (finalLog && finalLog !== strategyLog) {
      compacted.push({
        time: finalLog.time || "--:--:--",
        message: summaryText
      });
    }

    return compacted.length > 0 ? compacted.slice(0, 3) : [logs[0]];
  }

  function renderStats(stats, targetOverride = null) {
    const target = normalizeRequestedLimit(targetOverride, searchState.requestedLimit, searchState.configuredLimit, 1);
    const found = stats.totalCustomers || 0;

    statTotal.textContent = target;
    statEmail.textContent = found;
    statMatch.textContent = stats.highMatchCount || 0;

    // Calculate actual elapsed time
    let elapsedMs = 0;
    if (searchState.startTime > 0) {
      elapsedMs = Date.now() - searchState.startTime;
    }
    statMarket.textContent = formatElapsedMs(elapsedMs);

    // Update progress bar with real percentage
    const progressPercent = Math.min(100, Math.round((found / target) * 100));
    setProgressBar(progressPercent, false);
  }

  function renderResults(customers) {
    if (!Array.isArray(customers) || customers.length === 0) {
      resultsBody.innerHTML = `
        <tr>
          <td colspan="6">
            <div class="empty-state">
              <span class="empty-icon" aria-hidden="true">
                <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="6"></circle>
                  <path d="m20 20-4.2-4.2"></path>
                </svg>
              </span>
              <h2>${escapeHtml(t("search.emptyTitle"))}</h2>
              <p class="empty-copy">${escapeHtml(t("search.emptyCopy"))}</p>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    resultsBody.innerHTML = customers
      .map(
        (customer) => `
          <tr>
            <td>
              <input type="checkbox" data-lead-id="${escapeHtml(customer.id)}" ${searchState.selectedIds.has(customer.id) ? "checked" : ""} />
            </td>
            <td>
              <div class="company-cell">
                <strong>${escapeHtml(customer.companyName)}</strong>
                <a class="company-link" href="${escapeHtml(customer.website)}" target="_blank" rel="noreferrer">${escapeHtml(customer.website)}</a>
              </div>
            </td>
            <td>
              ${customer.email
                ? `<span class="table-email">${escapeHtml(customer.email)}</span>`
                : `<span class="table-note">${escapeHtml(t("search.table.noEmail"))}</span>`}
            </td>
            <td>${escapeHtml(localizeChannel(customer.channel))}</td>
            <td>${renderFitBadge(customer)}</td>
            <td>
              <div class="ops-cell">
                <button type="button" class="btn-view" data-lead-id="${escapeHtml(customer.id)}">${escapeHtml(t("search.table.view"))}</button>
                <button type="button" class="btn-outreach-single" data-lead-id="${escapeHtml(customer.id)}">${escapeHtml(t("search.table.createOutreach"))}</button>
              </div>
            </td>
          </tr>
        `
      )
      .join("");
  }

  function refreshSelectionState() {
    const selectedCustomers = getSelectedCustomers();
    if (selectedCount) {
      selectedCount.textContent = selectedCustomers.length;
    }
    if (exportButton) {
      exportButton.disabled = selectedCustomers.length === 0;
    }
  }

  function exportSelectedCustomers() {
    const selectedCustomers = getSelectedCustomers();
    if (selectedCustomers.length === 0) {
      return;
    }

    const lines = [[
      t("search.csv.company"),
      t("search.csv.contact"),
      t("search.csv.channel"),
      t("search.csv.fit"),
      t("search.csv.website")
    ].join(",")];
    selectedCustomers.forEach((customer) => {
      lines.push([
        csvEscape(customer.companyName),
        csvEscape(`${displayValue(customer.contactName, "Business Contact")} / ${displayValue(customer.email, t("search.table.noEmail"))}`),
        csvEscape(localizeChannel(customer.channel)),
        csvEscape(customer.fitNote || "candidate website"),
        csvEscape(customer.website)
      ].join(","));
    });

    const blob = new Blob(["\uFEFF" + lines.join("\n")], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "customer-search-results.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  function pushSelectedCustomers() {
    const selectedCustomers = getSelectedCustomers();
    localStorage.setItem(selectedCustomersKey, JSON.stringify(selectedCustomers));
    localStorage.setItem(outreachImportFlagKey, "1");
    window.location.href = "/ai-outreach?t=" + Date.now();
  }

  function handleResultSelection(event) {
    const target = event.target;
    if (!(target instanceof HTMLInputElement) || !target.matches("[data-lead-id]")) {
      return;
    }

    if (target.checked) {
      searchState.selectedIds.add(target.dataset.leadId);
    } else {
      searchState.selectedIds.delete(target.dataset.leadId);
    }
    refreshSelectionState();
  }

  function handleSelectAll() {
    if (selectAll.checked) {
      searchState.selectedIds = new Set(searchState.customers.map((customer) => customer.id));
    } else {
      searchState.selectedIds = new Set();
    }
    renderResults(searchState.customers);
    refreshSelectionState();
  }

  function getSelectedCustomers() {
    return searchState.customers.filter((customer) => searchState.selectedIds.has(customer.id));
  }

  function setSearchStatus(label, type) {
    searchState.statusKey = statusKeyForLabel(label) || label;
    searchState.statusType = type;
    renderSearchStatus();
  }

  function csvEscape(value) {
    const content = String(value ?? "");
    return `"${content.replaceAll("\"", "\"\"")}"`;
  }

  function renderFitBadge(customer) {
    const hasContact = !!(customer.contactName && customer.contactName.trim());
    const hasEmail = !!(customer.email && customer.email.trim());
    const count = (hasContact ? 1 : 0) + (hasEmail ? 1 : 0);
    if (count === 2) return `<span class="fit-badge fit-high">${escapeHtml(t("search.table.fitHigh"))}</span>`;
    if (count === 1) return `<span class="fit-badge fit-mid">${escapeHtml(t("search.table.fitMedium"))}</span>`;
    return `<span class="fit-badge fit-low">${escapeHtml(t("search.table.fitLow"))}</span>`;
  }

  function displayValue(value, fallbackText) {
    const text = String(value ?? "").trim();
    return text || fallbackText;
  }

  function normalizeRequestedLimit(...values) {
    for (const value of values) {
      const number = Number(value);
      if (Number.isFinite(number) && number > 0) {
        return Math.round(number);
      }
    }
    return 12;
  }

  function resolveResponseRequestedLimit(data) {
    return normalizeRequestedLimit(
      data?.requestedLimit,
      data?.config?.requestedLimit,
      data?.searchConfig?.requestedLimit,
      searchState.configuredLimit,
      12
    );
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#39;");
  }

  function updateRunningTimer() {
    const elapsed = Date.now() - searchState.startTime;
    if (statMarket) {
      statMarket.textContent = formatElapsedMs(elapsed);
    }
  }

  function clearSearchTimer() {
    if (searchState.timerInterval) {
      window.clearInterval(searchState.timerInterval);
      searchState.timerInterval = null;
    }
  }

  function setProgressBar(percent, indeterminate) {
    if (!progressTrack) {
      return;
    }
    if (indeterminate) {
      progressTrack.classList.add("is-indeterminate");
      progressTrack.style.setProperty("--progress-width", "30%");
    } else {
      progressTrack.classList.remove("is-indeterminate");
      if (percent !== null) {
        progressTrack.style.setProperty("--progress-width", percent + "%");
      }
    }
  }

  function formatElapsedMs(ms) {
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
  }

  /* ── Search History ── */

  function bindHistoryPanel() {
    if (!historyToggle || !historyPanel) return;

    historyToggle.addEventListener("click", (event) => {
      event.stopPropagation();
      const isHidden = historyPanel.classList.contains("is-hidden");
      if (isHidden) {
        renderSearchHistory();
        historyPanel.classList.remove("is-hidden");
        historyToggle.setAttribute("aria-expanded", "true");
      } else {
        historyPanel.classList.add("is-hidden");
        historyToggle.setAttribute("aria-expanded", "false");
      }
    });

    historyClear?.addEventListener("click", (event) => {
      event.stopPropagation();
      localStorage.removeItem(searchHistoryKey);
      renderSearchHistory();
    });

    historyPanel.addEventListener("click", (event) => {
      event.stopPropagation();
    });

    document.addEventListener("click", () => {
      if (!historyPanel.classList.contains("is-hidden")) {
        historyPanel.classList.add("is-hidden");
        historyToggle?.setAttribute("aria-expanded", "false");
      }
    });
  }

  function saveSearchHistory(data, payload) {
    try {
      const raw = localStorage.getItem(searchHistoryKey);
      let list = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(list)) list = [];

      const item = {
        id: String(Date.now()) + "-" + Math.random().toString(36).slice(2, 7),
        timestamp: Date.now(),
        summary: buildHistorySummary(payload),
        config: {
          industry: payload.industry,
          market: payload.market,
          keywords: payload.keywords,
          companySize: payload.companySize,
          requestedLimit: payload.requestedLimit
        },
        requestedLimit: payload.requestedLimit,
        resultCount: data.customers?.length || 0,
        customers: data.customers || []
      };

      list.unshift(item);
      if (list.length > maxHistoryItems) {
        list = list.slice(0, maxHistoryItems);
      }

      localStorage.setItem(searchHistoryKey, JSON.stringify(list));
    } catch (error) {
      console.error("Failed to save search history:", error);
    }
  }

  function buildHistorySummary(payload) {
    const parts = [];
    if (payload.industry) parts.push(payload.industry);
    if (payload.market && payload.market !== "全部") parts.push(payload.market);
    if (parts.length === 0) return payload.keywords || "AI搜索";
    return parts.join(" · ");
  }

  function renderSearchHistory() {
    if (!historyList || !historyEmpty) return;

    let list = [];
    try {
      const raw = localStorage.getItem(searchHistoryKey);
      if (raw) list = JSON.parse(raw);
    } catch {
      list = [];
    }
    if (!Array.isArray(list)) list = [];

    const locale = typeof window.leadflowLocale?.locale === "string"
      ? window.leadflowLocale.locale
      : "zh-CN";
    const countLabel = locale === "en"
      ? (count) => `${count} leads`
      : (count) => `${count} 位客户`;

    if (list.length === 0) {
      historyList.innerHTML = "";
      historyList.classList.add("is-hidden");
      historyEmpty.classList.remove("is-hidden");
      return;
    }

    historyEmpty.classList.add("is-hidden");
    historyList.classList.remove("is-hidden");

    historyList.innerHTML = list
      .map((item) => {
        const date = new Date(item.timestamp);
        const timeText = formatFullTime(date);
        const countText = countLabel(item.resultCount || 0);
        const tagsHtml = buildHistoryTagsHtml(item.config);
        return `
          <li class="history-item" data-history-id="${escapeHtml(item.id)}">
            <div class="history-item-row">
              <span class="history-time">${escapeHtml(timeText)}</span>
              <button type="button" class="history-restore" data-history-id="${escapeHtml(item.id)}" data-i18n="search.history.restore">恢复</button>
            </div>
            <div class="history-item-row">
              <div class="history-item-tags">${tagsHtml}</div>
              <span class="history-tag orange">${escapeHtml(countText)}</span>
            </div>
          </li>
        `;
      })
      .join("");

    historyList.querySelectorAll(".history-item").forEach((el) => {
      el.addEventListener("click", (event) => {
        const button = event.target.closest(".history-restore");
        if (!button) {
          const id = el.dataset.historyId;
          restoreSearchHistory(id);
        }
      });
    });

    historyList.querySelectorAll(".history-restore").forEach((btn) => {
      btn.addEventListener("click", (event) => {
        event.stopPropagation();
        restoreSearchHistory(btn.dataset.historyId);
      });
    });

    if (typeof window.applyPageTranslations === "function") {
      window.applyPageTranslations(locale);
    }
  }

  function buildHistoryTagsHtml(config) {
    const tags = [];
    if (config.industry) tags.push(config.industry);
    if (config.market && config.market !== "全部") tags.push(config.market);
    if (config.companySize) tags.push(config.companySize);
    if (!tags.length && config.keywords) tags.push(config.keywords.slice(0, 20));
    return tags
      .map((t) => `<span class="history-tag">${escapeHtml(t)}</span>`)
      .join("");
  }

  function formatHistoryTime(date) {
    const now = new Date();
    const isToday =
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate();

    const pad = (n) => String(n).padStart(2, "0");
    const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;

    if (isToday) return time;

    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    return `${month}-${day} ${time}`;
  }

  function formatFullTime(date) {
    const pad = (n) => String(n).padStart(2, "0");
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
    return `${year}-${month}-${day} ${time}`;
  }

  function restoreSearchHistory(id) {
    let list = [];
    try {
      const raw = localStorage.getItem(searchHistoryKey);
      if (raw) list = JSON.parse(raw);
    } catch {
      list = [];
    }
    if (!Array.isArray(list)) return;

    const item = list.find((h) => h.id === id);
    if (!item) return;

    const requestedLimit = normalizeRequestedLimit(
      item.requestedLimit,
      item.config?.requestedLimit,
      searchState.configuredLimit,
      12
    );

    const data = {
      summary: item.summary,
      timestamp: item.timestamp,
      requestedLimit,
      stats: {
        totalCustomers: item.resultCount,
        emailCount: item.resultCount,
        highMatchCount: item.resultCount,
        marketCoverage: 0
      },
      logs: [
        {
          time: formatHistoryTime(new Date(item.timestamp)),
          message: `从历史记录恢复：${item.summary}`
        }
      ],
      customers: item.customers || []
    };

    localStorage.setItem(searchStorageKey, JSON.stringify(data));
    applySearchResponse(data, { preserveFormLimit: true });
    const restoredSummary = currentLocale() === "en"
      ? `Restored from history: ${item.summary} (${item.resultCount} leads)`
      : `从历史记录恢复：${item.summary}（${item.resultCount} 位客户）`;
    setSearchStatus("search.status.restored", "complete");
    searchSummary.textContent = restoredSummary;

    if (historyPanel && !historyPanel.classList.contains("is-hidden")) {
      historyPanel.classList.add("is-hidden");
      historyToggle?.setAttribute("aria-expanded", "false");
    }
  }

  document.querySelectorAll(".tab-item[data-scroll-to]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const target = document.getElementById(btn.dataset.scrollTo);
      if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
      document.querySelectorAll(".tab-item").forEach((b) => b.classList.remove("is-active"));
      btn.classList.add("is-active");
    });
  });

  // ── 每行"生成开发信"按钮 ──────────────────────────────────────
  resultsBody?.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-outreach-single");
    if (!btn) return;
    const leadId = btn.dataset.leadId;
    const lead = searchState.customers.find((c) => c.id === leadId);
    if (!lead) return;
    localStorage.setItem(selectedCustomersKey, JSON.stringify([lead]));
    localStorage.setItem(outreachImportFlagKey, "1");
    window.location.href = "/ai-outreach?t=" + Date.now();
  });

  // ── 客户详情弹窗 ──────────────────────────────────────────────
  const leadModal = document.getElementById("lead-modal");
  const leadModalClose = document.getElementById("lead-modal-close");
  const modalAvatar = document.getElementById("modal-avatar");
  const modalTitle = document.getElementById("lead-modal-title");
  const modalWebsite = document.getElementById("modal-website");
  const modalOpenWebsite = document.getElementById("modal-open-website");
  const modalCountry = document.getElementById("modal-country");
  const modalContact = document.getElementById("modal-contact");
  const modalEmail = document.getElementById("modal-email");
  const modalChannel = document.getElementById("modal-channel");
  const modalFit = document.getElementById("modal-fit");
  const modalAddOutreach = document.getElementById("modal-add-outreach");
  const modalRowCountry = document.getElementById("modal-row-country");
  const modalRowContact = document.getElementById("modal-row-contact");
  const modalRowEmail = document.getElementById("modal-row-email");
  const modalRowChannel = document.getElementById("modal-row-channel");
  const modalRowFit = document.getElementById("modal-row-fit");

  let currentModalLeadId = null;

  function setModalRow(row, valueEl, value) {
    if (value) {
      row.style.display = "";
      valueEl.textContent = value;
    } else {
      row.style.display = "none";
    }
  }

  function openLeadModal(lead) {
    currentModalLeadId = lead.id;
    const name = lead.companyName || t("search.modal.unknownCompany");
    modalTitle.textContent = name;
    modalAvatar.textContent = name.charAt(0).toUpperCase();

    const site = lead.website || "";
    modalWebsite.href = site;
    modalWebsite.textContent = site;
    if (modalOpenWebsite) modalOpenWebsite.href = site;

    setModalRow(modalRowCountry, modalCountry, lead.country);
    setModalRow(modalRowContact, modalContact, lead.contactName);

    const email = lead.email || "";
    if (email) {
      modalRowEmail.style.display = "";
      modalEmail.href = `mailto:${email}`;
      modalEmail.textContent = email;
    } else {
      modalRowEmail.style.display = "none";
    }

    setModalRow(modalRowChannel, modalChannel, localizeChannel(lead.channel));
    setModalRow(modalRowFit, modalFit, lead.fitNote);

    leadModal.classList.remove("is-hidden");
    document.body.style.overflow = "hidden";
  }

  function closeLeadModal() {
    leadModal.classList.add("is-hidden");
    document.body.style.overflow = "";
    currentModalLeadId = null;
  }

  leadModalClose?.addEventListener("click", closeLeadModal);

  leadModal?.addEventListener("click", (e) => {
    if (e.target === leadModal) closeLeadModal();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && leadModal && !leadModal.classList.contains("is-hidden")) closeLeadModal();
  });

  resultsBody?.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-view");
    if (!btn) return;
    const lead = searchState.customers.find((c) => c.id === btn.dataset.leadId);
    if (lead) openLeadModal(lead);
  });

  modalAddOutreach?.addEventListener("click", () => {
    if (!currentModalLeadId) return;
    const lead = searchState.customers.find((c) => c.id === currentModalLeadId);
    if (!lead) return;
    closeLeadModal();
    localStorage.setItem(selectedCustomersKey, JSON.stringify([lead]));
    localStorage.setItem(outreachImportFlagKey, "1");
    window.location.href = "/ai-outreach?t=" + Date.now();
  });
})();
