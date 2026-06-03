package com.zijianxin.website.workflow;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WorkflowSearchService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSearchService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "baidu.com",
            "bing.com",
            "duckduckgo.com",
            "google.com",
            "steampowered.com",
            "mdpi.com",
            "ithome.com",
            "eduease.com",
            "thepaper.cn",
            "dictionary.cambridge.org",
            "iciba.com",
            "chazidian.com",
            "dict.cn",
            "dict.eudic.net",
            "engoo.cn.com",
            "hopenglish.com",
            "zhihu.com",
            "wikipedia.org",
            "baike.sogou.com",
            "sogou.com",
            "jc35.com",
            "jdzj.com",
            "made-in-china.com",
            "alibaba.com",
            "1688.com",
            "qcc.com",
            "qixin.com",
            "cufe.edu.cn",
            "cass.cn",
            "wistrategy.com",
            "pcsoft.com.cn",
            "onlinedown.net",
            "zol.com.cn"
    );

    private static final List<String> CONTACT_PATH_HINTS = List.of(
            "/contact", "/contact-us", "/about", "/about-us", "/company", "/company-profile", "/support",
            "/sales", "/team", "/impressum", "/imprint", "/legal", "/enquiry", "/inquiry", "/kontakt"
    );

    private static final List<String> CONTACT_PAGE_FALLBACKS = List.of(
            "/contact",
            "/contact-us",
            "/about",
            "/about-us",
            "/company",
            "/company-profile",
            "/support"
    );

    private static final List<String> CONTACT_TEXT_HINTS = List.of(
            "contact", "contact us", "about", "about us", "support", "sales", "team", "impressum", "imprint", "kontakt"
    );

    private static final List<String> NON_COMPANY_TEXT_HINTS = List.of(
            "dictionary", "translation", "translate", "encyclopedia", "news", "article", "blog", "forum", "wiki",
            "journal", "open access", "kids", "game", "gaming", "magazine", "paper", "research", "download",
            "meaning", "what is", "是什么意思", "翻译", "download", "破解版", "安装包", "教程", "软件"
    );

    private static final List<String> EDITORIAL_URL_PATTERNS = List.of(
            "/news/", "newsdetail", "/article/", "/articles/", "/blog/", "/forum/", "/wiki/",
            "/baike/", "/zhidao/", "/question/", "/answers/", "/post/"
    );

    private static final List<String> FREE_MAIL_DOMAINS = List.of(
            "qq.com", "163.com", "126.com", "gmail.com", "hotmail.com", "outlook.com", "yahoo.com"
    );

    private static final List<String> COMPANY_TEXT_HINTS = List.of(
            "company", "manufacturer", "factory", "supplier", "products", "solutions", "contact us", "about us",
            "co., ltd", "limited", "corporation", "inc", "llc", "gmbh",
            "\u6709\u9650\u516c\u53f8", "\u96c6\u56e2", "\u5de5\u5382", "\u5236\u9020", "\u4ea7\u54c1",
            "\u8054\u7cfb\u6211\u4eec", "\u5173\u4e8e\u6211\u4eec", "\u5b98\u7f51", "\u5b98\u65b9\u7f51\u7ad9"
    );

    private static final List<String> STRONG_COMPANY_HINTS = List.of(
            "co., ltd", "co ltd", "limited", "corporation", "inc", "llc", "gmbh", "official website", "official site",
            "\u6709\u9650\u516c\u53f8", "\u80a1\u4efd\u6709\u9650\u516c\u53f8", "\u96c6\u56e2", "\u5b98\u7f51", "\u5b98\u65b9\u7f51\u7ad9",
            "\u8054\u7cfb\u6211\u4eec", "\u5173\u4e8e\u6211\u4eec"
    );

    private static final List<String> REFERENCE_HOST_HINTS = List.of(
            "dict", "dictionary", "translate", "translation", "wiki", "baike", "news", "blog", "forum",
            "zhidao", "csdn", "bilibili", "hujiang", "zhuaniao", "yingyuqiao", "cqvip", "sciencedirect", "ieee",
            "sohu", "sina", "xueqiu", "ncss", "wenku", "36kr", "toutiao", "baijiahao", "ifeng", "eastmoney",
            "cailian", "yicai", "jiemian"
    );

    private static final List<String> PERSON_TITLE_HINTS = List.of(
            "manager", "director", "sales", "contact"
    );

    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "the", "and", "for", "with", "that", "from", "this", "into", "your", "their", "mainly",
            "主要", "需要", "寻找", "查找", "目标", "客户", "公司", "企业", "行业", "市场", "地区"
    );

    private static final Set<String> JUNK_PAGE_TITLES = Set.of(
            "联系我们", "关于我们", "首页", "产品中心", "新闻资讯", "新闻中心", "联系方式",
            "公司简介", "解决方案", "服务支持", "下载中心", "网站地图",
            "contact us", "about us", "home", "products", "news", "contact",
            "about", "services", "support", "downloads", "sitemap", "search",
            "index", "welcome", "main", "page not found", "404", "error"
    );

    private static final java.util.regex.Pattern CHINESE_COMPANY_SUFFIX_PATTERN =
            java.util.regex.Pattern.compile("^(.+?(?:有限公司|股份有限公司|集团公司|集团有限公司|控股有限公司|科技有限公司))");

    private static final String SERPAPI_BASE_URL = "https://serpapi.com/search";

    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile WorkflowModels.CustomerSearchResponse lastSearchResponse;

    public WorkflowSearchService(SettingsService settingsService, ObjectMapper objectMapper) {
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public WorkflowModels.CustomerSearchResponse getLastSearchResponse() {
        return lastSearchResponse;
    }

    public SettingsModels.SearchConnectionTestResult testSerpApiConnection(SettingsModels.SearchSettings request) {
        SettingsModels.SearchSettings settings = request == null ? SettingsModels.SearchSettings.defaults() : request;
        String serpApiKey = settings.serpApiKey();
        String serpEngine = mapEngineName(settings.defaultEngine());
        String apiUrl = SERPAPI_BASE_URL
                + "?q=" + URLEncoder.encode("test", StandardCharsets.UTF_8)
                + "&api_key=" + URLEncoder.encode(serpApiKey == null ? "" : serpApiKey.trim(), StandardCharsets.UTF_8)
                + "&engine=" + serpEngine
                + "&num=1";
        String displayUrl = apiUrl.replaceFirst("api_key=[^&]*", "api_key=***");

        if (serpApiKey == null || serpApiKey.isBlank()) {
            return new SettingsModels.SearchConnectionTestResult(false, displayUrl, "SerpAPI key is empty.");
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .timeout(java.time.Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            String preview = body.length() > 400 ? body.substring(0, 400) : body;
            if (response.statusCode() != 200) {
                return new SettingsModels.SearchConnectionTestResult(false, displayUrl, "HTTP " + response.statusCode() + ": " + preview);
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.get("error");
            if (error != null && !error.isNull() && !error.asText("").isBlank()) {
                return new SettingsModels.SearchConnectionTestResult(false, displayUrl, error.asText());
            }

            JsonNode organicResults = root.get("organic_results");
            boolean valid = organicResults != null && organicResults.isArray();
            return new SettingsModels.SearchConnectionTestResult(valid, displayUrl, preview);
        } catch (Exception e) {
            return new SettingsModels.SearchConnectionTestResult(false, displayUrl, e.getMessage());
        }
    }

    public WorkflowModels.CustomerSearchResponse searchCustomers(WorkflowModels.CustomerSearchRequest request) {
        SearchSession session = new SearchSession();
        SettingsModels.AppSettings settings = settingsService.getSettings();
        SettingsModels.SearchSettings searchSettings = settings.search();
        SettingsModels.CrawlerSettings crawlerSettings = settings.crawler();

        String industry = normalizeInput(fallback(request.industry(), ""));
        // "全部行业" means no industry filter — treat as generic/broad search
        if ("全部行业".equals(industry) || "all industries".equalsIgnoreCase(industry)) {
            industry = "";
        }
        String market = normalizeMarket(fallback(request.market(), "China"));
        String keywords = normalizeInput(fallback(request.keywords(), ""));
        String rawCompanySize = normalizeInput(fallback(request.companySize(), ""));
        // Normalize "全部规模" / empty to ALL so it doesn't get injected into queries
        String companySize = (rawCompanySize.isBlank()
                || "全部规模".equals(rawCompanySize)
                || "all sizes".equalsIgnoreCase(rawCompanySize)
                || "all".equalsIgnoreCase(rawCompanySize))
                ? "ALL" : rawCompanySize;

        int leadLimit = normalizePositive(request.requestedLimit(), searchSettings.resultsPerPage(), 10);
        int candidatePoolLimit = Math.max(
                Math.max(leadLimit * 8, 80),
                normalizePositive(crawlerSettings.candidateLimit(), 50, 50)
        );
        int timeoutMs = normalizePositive(crawlerSettings.requestTimeoutMs(), 8000, 8000);

        List<String> queries = buildSearchQueries(industry, market, keywords, companySize);
        session.log("Received search task.");
        session.log("Search strategy: " + String.join(" | ", queries));
        session.log("Runtime config: limit=" + leadLimit + ", pool=" + candidatePoolLimit + ", timeout=" + timeoutMs + "ms");

        List<SearchCandidate> candidates = fetchCandidates(queries, market, session, leadLimit, candidatePoolLimit, timeoutMs, searchSettings, crawlerSettings);
        List<WorkflowModels.CustomerLead> leads = inspectCandidates(
                candidates,
                industry,
                market,
                keywords,
                leadLimit,
                timeoutMs,
                crawlerSettings,
                session
        );

        WorkflowModels.SearchStats stats = new WorkflowModels.SearchStats(
                leads.size(),
                (int) leads.stream().filter(lead -> !lead.email().isBlank()).count(),
                (int) leads.stream().filter(lead -> lead.fitNote().contains("email") || lead.fitNote().contains("contact page")).count(),
                (int) Math.max(0, leads.stream().map(WorkflowModels.CustomerLead::country).distinct().count())
        );

        String summary = leads.isEmpty()
                ? "未从公开搜索结果中找到含有效联系信息的匹配公司。"
                : "已从公开搜索结果中收集到 " + leads.size() + " 条企业线索。";

        session.log("Search finished with " + leads.size() + " leads.");

        WorkflowModels.CustomerSearchResponse response = new WorkflowModels.CustomerSearchResponse(
                summary,
                stats,
                List.copyOf(session.logs()),
                leads
        );
        lastSearchResponse = response;
        return response;
    }

    private List<String> buildSearchQueries(String industry, String market, String keywords, String companySize) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        // "全部行业" is normalized to empty — use generic terms so queries stay useful
        boolean broadIndustry = industry.isBlank();
        String effectiveIndustry = broadIndustry ? "manufacturer" : industry;

        List<String> industryHints = buildSearchHints(effectiveIndustry);
        List<String> keywordHints = buildSearchHints(keywords);
        boolean hasKeywordHints = !keywordHints.isEmpty();
        String normalizedIndustry = normalizeSearchPhrase(industry);
        boolean componentsIndustry = normalizedIndustry.contains("industrial components")
                || normalizedIndustry.contains("零部件")
                || normalizedIndustry.contains("配件")
                || normalizedIndustry.contains("部件");
        boolean machineryIndustry = normalizedIndustry.contains("industrial machinery")
                || normalizedIndustry.contains("general industrial machinery")
                || normalizedIndustry.contains("通用机械设备")
                || normalizedIndustry.contains("通用机械")
                || normalizedIndustry.contains("机械设备");
        boolean automationIndustry = normalizedIndustry.contains("industrial automation")
                || normalizedIndustry.contains("工业自动化")
                || normalizedIndustry.contains("自动化设备")
                || normalizedIndustry.contains("工控")
                || normalizedIndustry.contains("控制系统");
        boolean cncIndustry = normalizedIndustry.contains("cnc")
                || normalizedIndustry.contains("金属加工")
                || normalizedIndustry.contains("数控")
                || normalizedIndustry.contains("机床");
        String primaryIndustryHint = firstQueryHint(industryHints, toEnglishHint(industry));
        String secondaryIndustryHint = nextDistinctQueryHint(industryHints, primaryIndustryHint);
        String primaryKeywordHint = hasKeywordHints
                ? firstQueryHint(keywordHints, toEnglishHint(keywords))
                : "";
        String nativeIndustryHint = firstNativeHint(industryHints, industry);
        String secondaryNativeIndustryHint = nextDistinctNativeHint(industryHints, nativeIndustryHint);
        String nativeKeywordHint = hasKeywordHints
                ? firstNativeHint(keywordHints, keywords)
                : "";

        if ("China".equalsIgnoreCase(market)) {
            if (broadIndustry) {
                // No industry filter: use generic company queries
                queries.add(joinQuery("site:.cn", primaryKeywordHint, "manufacturer", "official website"));
                queries.add(joinQuery("site:.com.cn", primaryKeywordHint, "company"));
                queries.add(joinQuery(primaryKeywordHint, "manufacturer", "contact email"));
                queries.add(joinQuery(primaryKeywordHint, "factory", "contact us"));
                queries.add(joinQuery(primaryKeywordHint, "supplier", "sales email"));
                queries.add(joinQuery(primaryKeywordHint, "company profile"));
                queries.add(joinQuery(nativeKeywordHint, "官网"));
                queries.add(joinQuery(nativeKeywordHint, "厂家", "联系方式"));
                queries.add(joinQuery(nativeKeywordHint, "公司", "邮箱"));
                queries.add(joinQuery(nativeKeywordHint, "有限公司"));
            } else {
            queries.add(joinQuery("site:.cn", primaryKeywordHint, primaryIndustryHint, "manufacturer", "official website"));
            queries.add(joinQuery("site:.com.cn", primaryKeywordHint, primaryIndustryHint, "company"));
            queries.add(joinQuery(primaryKeywordHint, primaryIndustryHint, "manufacturer", "contact email"));
            queries.add(joinQuery(primaryKeywordHint, primaryIndustryHint, "factory", "contact us"));
            queries.add(joinQuery(primaryKeywordHint, primaryIndustryHint, "supplier", "sales email"));
            queries.add(joinQuery(primaryKeywordHint, primaryIndustryHint, "company profile"));
            queries.add(joinQuery(nativeKeywordHint, nativeIndustryHint, "官网"));
            queries.add(joinQuery(nativeKeywordHint, nativeIndustryHint, "厂家", "联系方式"));
            queries.add(joinQuery(nativeKeywordHint, nativeIndustryHint, "公司", "邮箱"));
            queries.add(joinQuery(nativeKeywordHint, nativeIndustryHint, "有限公司"));
            if (!secondaryIndustryHint.isBlank()) {
                queries.add(joinQuery("site:.cn", secondaryIndustryHint, "manufacturer", "official website"));
                queries.add(joinQuery(secondaryIndustryHint, "supplier", "contact"));
            }
            if (!secondaryNativeIndustryHint.isBlank()) {
                queries.add(joinQuery(secondaryNativeIndustryHint, "官网"));
                queries.add(joinQuery(secondaryNativeIndustryHint, "有限公司"));
            }
            if (componentsIndustry) {
                queries.add(joinQuery("site:.cn", "parts", "manufacturer"));
                queries.add(joinQuery("site:.cn", "components", "supplier"));
                queries.add(joinQuery("零部件", "有限公司"));
                queries.add(joinQuery("配件", "厂家"));
                queries.add(joinQuery("site:.cn", "零部件", "有限公司"));
                queries.add(joinQuery("site:.cn", "配件", "有限公司"));
                queries.add(joinQuery("site:.cn", "部件", "厂家"));
            }
            if (machineryIndustry) {
                queries.add(joinQuery("site:.cn", "machinery equipment", "manufacturer"));
                queries.add(joinQuery("通用机械", "有限公司"));
                queries.add(joinQuery("机械设备", "厂家"));
                queries.add(joinQuery("site:.cn", "通用机械", "有限公司"));
                queries.add(joinQuery("site:.cn", "机械设备", "有限公司"));
                queries.add(joinQuery("site:.cn", "机械设备", "厂家"));
            }
            if (automationIndustry) {
                queries.add(joinQuery("site:.cn", "工业自动化", "有限公司"));
                queries.add(joinQuery("site:.cn", "自动化设备", "有限公司"));
                queries.add(joinQuery("site:.cn", "工控", "有限公司"));
                queries.add(joinQuery("site:.cn", "plc", "company"));
                queries.add(joinQuery("控制系统", "厂家"));
                queries.add(joinQuery("伺服", "厂家"));
            }
            if (cncIndustry) {
                queries.add(joinQuery("site:.cn", "数控", "有限公司"));
                queries.add(joinQuery("site:.cn", "机床", "有限公司"));
                queries.add(joinQuery("site:.cn", "金属加工", "厂家"));
                queries.add(joinQuery("site:.cn", "CNC", "机床"));
                queries.add(joinQuery("数控", "机床", "厂家"));
                queries.add(joinQuery("加工中心", "厂家"));
            }
            } // end else (specific industry)
            if (!"ALL".equalsIgnoreCase(companySize)) {
                queries.add(joinQuery(primaryKeywordHint, primaryIndustryHint, companySize, "company"));
            }
            return new ArrayList<>(queries);
        }

        if ("ALL".equalsIgnoreCase(market)) {
            String ih = broadIndustry ? "" : primaryIndustryHint;
            queries.add(joinQuery(primaryKeywordHint, ih, "manufacturer", "official website"));
            queries.add(joinQuery(primaryKeywordHint, ih, "supplier", "contact"));
            queries.add(joinQuery(primaryKeywordHint, ih, "factory", "email"));
            queries.add(joinQuery(primaryKeywordHint, ih, "company profile"));
            queries.add(joinQuery(primaryKeywordHint, ih, "sales email"));
            if (!"ALL".equalsIgnoreCase(companySize)) {
                queries.add(joinQuery(primaryKeywordHint, ih, companySize, "company"));
            }
            return new ArrayList<>(queries);
        }

        String marketAlias = marketAlias(market);
        String marketSite = marketSite(market);
        String ih = broadIndustry ? "" : primaryIndustryHint;
        queries.add(joinQuery(marketSite, primaryKeywordHint, ih, "manufacturer", "official website"));
        queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, "supplier", "contact"));
        queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, "factory", "email"));
        queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, "company profile"));
        queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, "sales email"));
        if (!"ALL".equalsIgnoreCase(companySize)) {
            queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, companySize, "company"));
        }
        return new ArrayList<>(queries);
    }

    private List<SearchCandidate> fetchCandidates(
            List<String> queries,
            String market,
            SearchSession session,
            int leadLimit,
            int candidatePoolLimit,
            int timeoutMs,
            SettingsModels.SearchSettings searchSettings,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        String serpApiKey = searchSettings.serpApiKey();
        if (serpApiKey != null && !serpApiKey.isBlank()) {
            session.log("Mode: SerpAPI (engine=" + searchSettings.defaultEngine() + ")");
            return fetchCandidatesFromSerpApi(queries, session, candidatePoolLimit, serpApiKey, searchSettings.defaultEngine());
        }

        session.log("Mode: Direct web scraping (Bing / DuckDuckGo / Baidu)");
        List<SearchCandidate> candidates = new ArrayList<>();
        Set<String> seenHosts = new LinkedHashSet<>();

        for (String query : queries) {
            if (candidates.size() >= candidatePoolLimit) {
                break;
            }
            session.log("Trying query: " + query);
            collectFromBingRss(query, candidates, seenHosts, candidatePoolLimit, timeoutMs);
            collectFromBingHtml(query, candidates, seenHosts, candidatePoolLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
            if (!"China".equalsIgnoreCase(market)) {
                collectFromDuckDuckGo(query, candidates, seenHosts, candidatePoolLimit, timeoutMs);
            }
            if ("China".equalsIgnoreCase(market)) {
                collectFromBaidu(query, candidates, seenHosts, candidatePoolLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
            }
        }

        if (crawlerSettings.googleFallbackEnabled() && !"China".equalsIgnoreCase(market) && candidates.size() < leadLimit) {
            session.log("Google fallback enabled because primary sources returned only " + candidates.size() + " candidates.");
            collectGoogleFallback(queries, candidates, seenHosts, candidatePoolLimit, timeoutMs, crawlerSettings);
        }

        session.log("Collected " + candidates.size() + " candidate websites.");
        return candidates;
    }

    private void collectGoogleFallback(
            List<String> queries,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        int queryLimit = Math.min(queries.size(), normalizePositive(crawlerSettings.googleFallbackQueryLimit(), 2, 1));
        int googleTimeoutMs = Math.min(timeoutMs, normalizePositive(crawlerSettings.googleFallbackTimeoutMs(), 5000, 1000));
        for (int index = 0; index < queryLimit; index++) {
            if (candidates.size() >= candidatePoolLimit) {
                return;
            }
            collectFromGoogle(
                    queries.get(index),
                    candidates,
                    seenHosts,
                    candidatePoolLimit,
                    googleTimeoutMs,
                    normalizePositive(crawlerSettings.googleFallbackPageLimit(), 1, 1)
            );
        }
    }

    private List<SearchCandidate> fetchCandidatesFromSerpApi(
            List<String> queries,
            SearchSession session,
            int candidatePoolLimit,
            String serpApiKey,
            String engineName
    ) {
        List<SearchCandidate> candidates = new ArrayList<>();
        Set<String> seenHosts = new LinkedHashSet<>();
        String serpEngine = mapEngineName(engineName);
        int resultsPerQuery = Math.min(candidatePoolLimit, 10);

        for (String query : queries) {
            if (candidates.size() >= candidatePoolLimit) {
                break;
            }
            session.log("SerpAPI query: " + query);
            try {
                String apiUrl = SERPAPI_BASE_URL
                        + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                        + "&api_key=" + serpApiKey
                        + "&engine=" + serpEngine
                        + "&num=" + resultsPerQuery;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Accept", "application/json")
                        .timeout(java.time.Duration.ofSeconds(30))
                        .GET()
                        .build();

                log.info("Crawler fetch: {}", apiUrl);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    session.log("SerpAPI returned HTTP " + response.statusCode() + " for query: " + query);
                    continue;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode organicResults = root.get("organic_results");
                if (organicResults == null || !organicResults.isArray()) {
                    session.log("SerpAPI response has no organic_results for query: " + query);
                    continue;
                }

                for (JsonNode result : organicResults) {
                    String title = safeText(result, "title");
                    String link = safeText(result, "link");
                    String snippet = safeText(result, "snippet");
                    addCandidate(candidates, seenHosts, title, link, snippet, "SerpAPI/" + serpEngine);
                    if (candidates.size() >= candidatePoolLimit) {
                        break;
                    }
                }
                session.log("SerpAPI returned " + organicResults.size() + " results for: " + query);

            } catch (Exception e) {
                session.log("SerpAPI call failed for query '" + query + "': " + e.getMessage());
                // Continue to next query instead of failing entirely
            }
        }

        session.log("SerpAPI collected " + candidates.size() + " candidates.");
        return candidates;
    }

    private String mapEngineName(String name) {
        if (name == null) {
            return "google";
        }
        return switch (name.trim().toLowerCase()) {
            case "bing" -> "bing";
            case "duckduckgo" -> "duckduckgo";
            case "baidu" -> "baidu";
            default -> "google"; // SerpAPI defaults to Google
        };
    }

    private String safeText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child == null || child.isNull()) ? "" : cleanText(child.asText());
    }

    private void collectFromBingRss(
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs
    ) {
        if (candidates.size() >= candidatePoolLimit) {
            return;
        }

        String searchUrl = "https://www.bing.com/search?format=rss&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            log.info("Crawler fetch: {}", searchUrl);
            Document document = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .referrer("https://www.bing.com/")
                    .timeout(timeoutMs)
                    .parser(Parser.xmlParser())
                    .get();

            for (Element item : document.select("item")) {
                Element titleElement = item.selectFirst("title");
                Element linkElement = item.selectFirst("link");
                Element descriptionElement = item.selectFirst("description");
                if (linkElement == null) {
                    continue;
                }
                addCandidate(
                        candidates,
                        seenHosts,
                        cleanText(titleElement == null ? "" : titleElement.text()),
                        cleanText(linkElement.text()),
                        cleanText(descriptionElement == null ? "" : descriptionElement.text()),
                        "Bing RSS"
                );
                if (candidates.size() >= candidatePoolLimit) {
                    break;
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void collectFromGoogle(
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs,
            int pageLimit
    ) {
        for (int pageIndex = 0; pageIndex < pageLimit; pageIndex++) {
            if (candidates.size() >= candidatePoolLimit) {
                return;
            }

            int offset = pageIndex * 10;
            String searchUrl = "https://www.google.com/search?num=10&hl=en&filter=0&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&start=" + offset;
            try {
                log.info("Crawler fetch: {}", searchUrl);
                Document document = Jsoup.connect(searchUrl)
                        .userAgent(USER_AGENT)
                        .referrer("https://www.google.com/")
                        .timeout(timeoutMs)
                        .get();

                for (Element link : document.select("a[href*='/url?']:has(h3), a[href^='https://www.google.com/url?']:has(h3)")) {
                    Element titleElement = link.selectFirst("h3");
                    if (titleElement == null) {
                        continue;
                    }
                    String resolved = resolveGoogleUrl(link.absUrl("href"), link.attr("href"));
                    String snippet = cleanText(link.parent() == null ? "" : link.parent().text());
                    addCandidate(
                            candidates,
                            seenHosts,
                            cleanText(titleElement.text()),
                            resolved,
                            snippet,
                            "Google"
                    );
                    if (candidates.size() >= candidatePoolLimit) {
                        break;
                    }
                }
            } catch (IOException ignored) {
            } catch (Exception exception) {
                log.info("Google query failed for: {} ({})", query, exception.getMessage());
            }
        }
    }

    private void collectFromBingHtml(
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs,
            int pageLimit
    ) {
        for (int pageIndex = 0; pageIndex < pageLimit; pageIndex++) {
            if (candidates.size() >= candidatePoolLimit) {
                return;
            }

            int offset = pageIndex * 10;
            String searchUrl = "https://www.bing.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&first=" + (offset + 1);
            try {
                log.info("Crawler fetch: {}", searchUrl);
                Document document = Jsoup.connect(searchUrl)
                        .userAgent(USER_AGENT)
                        .referrer("https://www.bing.com/")
                        .timeout(timeoutMs)
                        .get();

                for (Element result : document.select("li.b_algo")) {
                    Element link = result.selectFirst("h2 a");
                    if (link == null) {
                        continue;
                    }
                    addCandidate(
                            candidates,
                            seenHosts,
                            cleanText(link.text()),
                            cleanText(link.attr("abs:href")),
                            cleanText(result.select(".b_caption").text()),
                            "Bing HTML"
                    );
                    if (candidates.size() >= candidatePoolLimit) {
                        break;
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void collectFromDuckDuckGo(
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs
    ) {
        if (candidates.size() >= candidatePoolLimit) {
            return;
        }

        String searchUrl = "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            log.info("Crawler fetch: {}", searchUrl);
            Document document = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .referrer("https://duckduckgo.com/")
                    .timeout(timeoutMs)
                    .get();

            for (Element link : document.select("a.result__a, a.result-link")) {
                String resolved = resolveDuckDuckGoUrl(link.attr("abs:href"), link.attr("href"));
                String snippet = cleanText(link.closest(".result") == null ? "" : link.closest(".result").select(".result__snippet").text());
                addCandidate(candidates, seenHosts, cleanText(link.text()), resolved, snippet, "DuckDuckGo HTML");
                if (candidates.size() >= candidatePoolLimit) {
                    break;
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void collectFromBaidu(
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs,
            int pageLimit
    ) {
        for (int pageIndex = 0; pageIndex < pageLimit; pageIndex++) {
            if (candidates.size() >= candidatePoolLimit) {
                return;
            }

            int offset = pageIndex * 10;
            String searchUrl = "https://www.baidu.com/s?wd=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&pn=" + offset;
            try {
                log.info("Crawler fetch: {}", searchUrl);
                Document document = Jsoup.connect(searchUrl)
                        .userAgent(USER_AGENT)
                        .referrer("https://www.baidu.com/")
                        .timeout(timeoutMs)
                        .get();

                for (Element link : document.select("h3 a")) {
                    String resolved = followRedirectUrl(link.absUrl("href"), timeoutMs);
                    String snippet = cleanText(link.closest("div") == null ? "" : link.closest("div").text());
                    addCandidate(candidates, seenHosts, cleanText(link.text()), resolved, snippet, "Baidu");
                    if (candidates.size() >= candidatePoolLimit) {
                        break;
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void addCandidate(List<SearchCandidate> candidates, Set<String> seenHosts, String title, String url, String snippet, String source) {
        String normalizedUrl = cleanText(url);
        if (normalizedUrl.isBlank() || !normalizedUrl.startsWith("http")) {
            return;
        }

        String host = normalizeHost(hostOf(normalizedUrl));
        String combined = (cleanText(title) + " " + cleanText(snippet)).toLowerCase(Locale.ROOT);
        if (host.isBlank() || !seenHosts.add(host) || isBlockedHost(host) || looksLikeReferenceHost(host) || looksLikeBlockedContent(normalizedUrl, combined)) {
            return;
        }

        candidates.add(new SearchCandidate(title, normalizedUrl, snippet, source));
    }

    private List<WorkflowModels.CustomerLead> inspectCandidates(
            List<SearchCandidate> candidates,
            String industry,
            String market,
            String keywords,
            int searchLimit,
            int timeoutMs,
            SettingsModels.CrawlerSettings crawlerSettings,
            SearchSession session
    ) {
        List<WorkflowModels.CustomerLead> leads = new ArrayList<>();
        Set<String> acceptedHosts = new LinkedHashSet<>();

        List<SearchCandidate> sortedCandidates = candidates.stream()
                .sorted(Comparator.comparingInt((SearchCandidate candidate) -> scoreCandidate(candidate, market, industry, keywords, crawlerSettings)).reversed())
                .toList();

        int inspectionLimit = Math.min(sortedCandidates.size(), Math.max(searchLimit * 6, 30));
        int maxParallelInspections = normalizePositive(crawlerSettings.maxParallelInspections(), 8, 1);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(maxParallelInspections, Math.max(2, inspectionLimit)));
        CompletionService<InspectedCandidate> completionService = new ExecutorCompletionService<>(executor);
        int submitted = 0;

        try {
            for (int index = 0; index < inspectionLimit; index++) {
                SearchCandidate candidate = sortedCandidates.get(index);
                completionService.submit(() -> new InspectedCandidate(
                        candidate,
                        inspectWebsite(candidate, industry, market, keywords, timeoutMs, crawlerSettings)
                ));
                submitted++;
            }

            for (int index = 0; index < submitted; index++) {
                if (leads.size() >= searchLimit) {
                    break;
                }

                InspectedCandidate inspectedCandidate = completionService.take().get();
                if (inspectedCandidate == null || inspectedCandidate.scanResult() == null) {
                    continue;
                }

                PageScanResult scanResult = inspectedCandidate.scanResult();
                String host = normalizeHost(hostOf(scanResult.website()));
                if (host.isBlank() || !acceptedHosts.add(host)) {
                    continue;
                }

                leads.add(toLead(scanResult));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
            // Keep best-effort search behavior even if some pages fail.
        } finally {
            executor.shutdownNow();
        }

        if (leads.size() < searchLimit) {
            fillWithFallbackCandidates(
                    sortedCandidates,
                    acceptedHosts,
                    leads,
                    industry,
                    market,
                    keywords,
                    searchLimit
            );
        }

        leads.sort(Comparator.comparingInt((WorkflowModels.CustomerLead lead) -> lead.email().isBlank() ? 0 : 1).reversed());
        session.log("Filtered down to " + leads.size() + " company leads after website inspection.");
        return leads;
    }

    private int scoreCandidate(SearchCandidate candidate, String market, String industry, String keywords, SettingsModels.CrawlerSettings crawlerSettings) {
        String host = normalizeHost(hostOf(candidate.url()));
        String combined = (cleanText(candidate.title()) + " " + cleanText(candidate.snippet())).toLowerCase(Locale.ROOT);
        int score = 0;

        if (looksLikeCompanyCandidate(host, combined)) {
            score += normalizePositive(crawlerSettings.companySignalWeight(), 8, 1);
        }
        if (matchesMarketSignal(host, combined, market)) {
            score += normalizePositive(crawlerSettings.marketWeight(), 8, 1);
        }
        if (matchesKeywords(industry, keywords, combined)) {
            score += normalizePositive(crawlerSettings.keywordWeight(), 6, 1);
        }
        if (combined.contains("contact") || combined.contains("email")) {
            score += 6;
        }
        if (combined.contains("manufacturer") || combined.contains("factory") || combined.contains("company")) {
            score += 6;
        }
        return score;
    }

    private PageScanResult inspectWebsite(
            SearchCandidate candidate,
            String industry,
            String market,
            String keywords,
            int timeoutMs,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        try {
            boolean searchResultMatched = passesSearchResultFilters(candidate, industry, market, keywords);
            String landingPageUrl = candidate.url();
            Document document = fetchDocument(landingPageUrl, timeoutMs);
            if (document == null) {
                return null;
            }

            String finalUrl = document.location().isBlank() ? landingPageUrl : document.location();
            String host = normalizeHost(hostOf(finalUrl));
            String title = cleanText(document.title());
            String text = cleanText(document.text());
            String combined = String.join(" ", title, text, candidate.title(), candidate.snippet()).toLowerCase(Locale.ROOT);

            boolean landingPageMatched = passesWebsiteFilters(host, finalUrl, combined, industry, market, keywords);
            if (!landingPageMatched) {
                String homepageCandidate = rootUrlOf(finalUrl);
                if (!homepageCandidate.equals(landingPageUrl)) {
                    Document homepageDocument = fetchDocument(homepageCandidate, timeoutMs);
                    if (homepageDocument != null) {
                        document = homepageDocument;
                        finalUrl = document.location().isBlank() ? homepageCandidate : document.location();
                        host = normalizeHost(hostOf(finalUrl));
                        title = cleanText(document.title());
                        text = cleanText(document.text());
                        combined = String.join(" ", title, text, candidate.title(), candidate.snippet()).toLowerCase(Locale.ROOT);
                    }
                }
            }

            boolean websiteMatched = passesWebsiteFilters(host, finalUrl, combined, industry, market, keywords);
            if (!searchResultMatched && !websiteMatched) {
                return null;
            }

            String homepageUrl = rootUrlOf(finalUrl);
            Set<String> emails = new LinkedHashSet<>(extractEmails(document.html()));
            String contactPageUrl = null;
            Document contactDocument = null;

            if (!"HOME_ONLY".equalsIgnoreCase(crawlerSettings.emailExtractionDepth())) {
                contactPageUrl = findContactPage(homepageUrl, document);
                if (contactPageUrl == null) {
                    contactPageUrl = probeContactPage(homepageUrl, timeoutMs);
                }
                if (contactPageUrl != null) {
                    contactDocument = fetchDocument(contactPageUrl, timeoutMs);
                    if (contactDocument != null) {
                        emails.addAll(extractEmails(contactDocument.html()));
                    }
                }
            }

            String email = chooseBestEmail(emails, host);
            String companyName = extractCompanyName(document, candidate, host);
            String contactName = extractContactName(document, contactDocument, email);

            List<String> notes = new ArrayList<>();
            if (contactPageUrl != null) {
                notes.add("contact page");
            }
            if (!email.isBlank()) {
                notes.add("email found");
            }

            return new PageScanResult(
                    companyName,
                    homepageUrl,
                    inferCountry(host, market),
                    contactName.isBlank() ? "Business Contact" : contactName,
                    email,
                    "Search engine + website",
                    notes.isEmpty() ? "company website" : String.join("; ", notes)
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean passesSearchResultFilters(SearchCandidate candidate, String industry, String market, String keywords) {
        String candidateUrl = cleanText(candidate.url());
        String host = normalizeHost(hostOf(candidateUrl));
        String combined = (cleanText(candidate.title()) + " " + cleanText(candidate.snippet())).toLowerCase(Locale.ROOT);
        if (host.isBlank() || isBlockedHost(host)) {
            return false;
        }
        if (looksLikeBlockedContent(candidateUrl, combined)) {
            return false;
        }
        if (!looksLikeCompanyCandidate(host, combined)) {
            return false;
        }
        if (!matchesMarketSignal(host, combined, market)) {
            return false;
        }
        return matchesKeywords(industry, keywords, combined);
    }

    private boolean passesWebsiteFilters(String host, String finalUrl, String combined, String industry, String market, String keywords) {
        if (host.isBlank() || isBlockedHost(host)) {
            return false;
        }
        if (looksLikeBlockedContent(finalUrl, combined)) {
            return false;
        }
        if (!looksLikeCompanyCandidate(host, combined)) {
            return false;
        }
        if (!matchesMarketSignal(host, combined, market)) {
            return false;
        }
        return matchesKeywords(industry, keywords, combined);
    }

    private WorkflowModels.CustomerLead toLead(PageScanResult scanResult) {
        return new WorkflowModels.CustomerLead(
                "lead-" + UUID.randomUUID(),
                scanResult.companyName(),
                scanResult.website(),
                scanResult.country(),
                scanResult.contactName(),
                scanResult.email(),
                scanResult.channel(),
                scanResult.fitNote()
        );
    }

    private void fillWithFallbackCandidates(
            List<SearchCandidate> sortedCandidates,
            Set<String> acceptedHosts,
            List<WorkflowModels.CustomerLead> leads,
            String industry,
            String market,
            String keywords,
            int searchLimit
    ) {
        for (SearchCandidate candidate : sortedCandidates) {
            if (leads.size() >= searchLimit) {
                break;
            }

            String host = normalizeHost(hostOf(candidate.url()));
            String combined = (cleanText(candidate.title()) + " " + cleanText(candidate.snippet())).toLowerCase(Locale.ROOT);
            if (host.isBlank() || acceptedHosts.contains(host) || isBlockedHost(host)) {
                continue;
            }
            if (!looksLikeCompanyCandidate(host, combined)) {
                continue;
            }
            if (!matchesMarketSignal(host, combined, market)) {
                continue;
            }
            if (!matchesKeywords(industry, keywords, combined)) {
                continue;
            }
            if (!looksLikeCompanyCandidate(host, combined)) {
                continue;
            }
            if (looksLikeBlockedContent(candidate.url(), combined)) {
                continue;
            }

            String companyName = simplifyTitle(candidate.title()).isBlank() ? host : simplifyTitle(candidate.title());
            if (companyName.length() <= 2 || companyName.toLowerCase(Locale.ROOT).contains("数据库") || companyName.toLowerCase(Locale.ROOT).contains("字典")) {
                continue;
            }
            acceptedHosts.add(host);
            leads.add(new WorkflowModels.CustomerLead(
                    "lead-" + UUID.randomUUID(),
                    companyName,
                    rootUrlOf(candidate.url()),
                    inferCountry(host, market),
                    "Business Contact",
                    "",
                    candidate.source(),
                    "candidate website; manual review suggested"
            ));
        }
    }

    private Document fetchDocument(String url, int timeoutMs) throws IOException {
        log.info("Crawler fetch: {}", url);
        Connection.Response response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutMs)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .execute();
        if (response.statusCode() >= 400) {
            return null;
        }
        return response.parse();
    }

    private List<String> extractEmails(String html) {
        Set<String> emails = new LinkedHashSet<>();
        Matcher matcher = EMAIL_PATTERN.matcher(html == null ? "" : html);
        while (matcher.find()) {
            String email = cleanText(matcher.group()).toLowerCase(Locale.ROOT);
            if (email.contains("@")) {
                emails.add(email);
            }
        }

        Matcher mailtoMatcher = Pattern.compile("mailto:([^\"'?#\\s>]+)", Pattern.CASE_INSENSITIVE).matcher(html == null ? "" : html);
        while (mailtoMatcher.find()) {
            String email = cleanText(mailtoMatcher.group(1)).toLowerCase(Locale.ROOT);
            if (email.contains("@")) {
                emails.add(email);
            }
        }
        return new ArrayList<>(emails);
    }

    private String chooseBestEmail(Set<String> emails, String host) {
        String bestEmail = "";
        int bestScore = Integer.MIN_VALUE;
        for (String email : emails) {
            int score = scoreEmail(email, host, settingsService.getSettings().crawler());
            if (score > bestScore) {
                bestScore = score;
                bestEmail = email;
            }
        }
        return bestEmail;
    }

    private int scoreEmail(String email, String host, SettingsModels.CrawlerSettings crawlerSettings) {
        int score = 0;
        if (email.endsWith("@" + host)) {
            score += normalizePositive(crawlerSettings.sameDomainWeight(), 10, 1);
        }
        if (!isFreeMail(email)) {
            score += 3;
        }
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        if (!List.of("info", "contact", "sales", "support", "service").contains(local)) {
            score += 2;
        }
        return score;
    }

    private String findContactPage(String baseUrl, Document document) {
        for (Element link : document.select("a[href]")) {
            String href = cleanText(link.attr("abs:href"));
            String text = cleanText(link.text()).toLowerCase(Locale.ROOT);
            if (href.isBlank() || !sameHost(baseUrl, href)) {
                continue;
            }
            if (CONTACT_PATH_HINTS.stream().anyMatch(href.toLowerCase(Locale.ROOT)::contains)
                    || CONTACT_TEXT_HINTS.stream().anyMatch(text::contains)) {
                return href;
            }
        }
        return null;
    }

    private String probeContactPage(String baseUrl, int timeoutMs) {
        for (String path : CONTACT_PAGE_FALLBACKS) {
            String candidate = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
            try {
                Document document = fetchDocument(candidate, timeoutMs);
                if (document != null) {
                    return candidate;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private String extractCompanyName(Document document, SearchCandidate candidate, String host) {
        String ogName = metaContent(document, "meta[property=og:site_name]");
        if (!ogName.isBlank() && isLikelyCompanyName(ogName)) {
            return cleanCompanyName(ogName);
        }
        String appName = metaContent(document, "meta[name=application-name]");
        if (!appName.isBlank() && isLikelyCompanyName(appName)) {
            return cleanCompanyName(appName);
        }
        String title = simplifyTitle(document.title());
        if (!title.isBlank() && isLikelyCompanyName(title)) {
            return cleanCompanyName(title);
        }
        String candidateTitle = simplifyTitle(candidate.title());
        if (!candidateTitle.isBlank() && isLikelyCompanyName(candidateTitle)) {
            return cleanCompanyName(candidateTitle);
        }
        return host;
    }

    private boolean isLikelyCompanyName(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.toLowerCase(java.util.Locale.ROOT).trim();
        if (JUNK_PAGE_TITLES.contains(lower)) return false;
        if (name.trim().length() > 80) return false;
        return true;
    }

    private String cleanCompanyName(String name) {
        String trimmed = name.trim();
        // 对中文名尝试截取到公司后缀（有限公司、集团等）
        java.util.regex.Matcher m = CHINESE_COMPANY_SUFFIX_PATTERN.matcher(trimmed);
        if (m.find()) {
            return m.group(1);
        }
        // 超长则截断
        if (trimmed.length() > 60) {
            return trimmed.substring(0, 60).trim();
        }
        return trimmed;
    }

    private String extractContactName(Document document, Document contactDocument, String email) {
        String fromContactPage = extractVisibleContactName(contactDocument);
        if (!fromContactPage.isBlank()) {
            return fromContactPage;
        }
        String fromPage = extractVisibleContactName(document);
        if (!fromPage.isBlank()) {
            return fromPage;
        }
        return deriveContactName(email);
    }

    private String extractVisibleContactName(Document document) {
        if (document == null) {
            return "";
        }
        for (Element element : document.select("p, li, div, span, strong")) {
            String text = cleanText(element.text());
            if (looksLikePersonName(text)) {
                return text;
            }
        }
        return "";
    }

    private boolean looksLikePersonName(String text) {
        String normalized = cleanText(text);
        if (normalized.isBlank() || normalized.length() > 40 || normalized.contains("@")) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean titleLike = PERSON_TITLE_HINTS.stream().anyMatch(lower::contains);
        boolean englishName = normalized.matches("(?i)(mr\\.?|ms\\.?|mrs\\.?|dr\\.?)?\\s*[A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,2}");
        return titleLike || englishName;
    }

    private String deriveContactName(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "";
        }
        String local = email.substring(0, email.indexOf('@'));
        if (List.of("info", "contact", "sales", "support", "service").contains(local)) {
            return "";
        }
        return List.of(local.split("[._-]+")).stream()
                .filter(part -> !part.isBlank())
                .map(this::capitalize)
                .collect(Collectors.joining(" "));
    }

    private boolean matchesKeywords(String industry, String keywords, String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> keywordHints = buildSearchHints(keywords);
        List<String> industryHints = buildSearchHints(industry);
        boolean hasKeywordHints = !keywordHints.isEmpty();

        boolean industryMatched = industryHints.stream().anyMatch(hint -> containsMeaningfulPhrase(lower, hint));
        if (!hasKeywordHints) {
            return industryMatched
                    || industryHints.stream().anyMatch(hint -> containsLooseKeywordToken(lower, hint))
                    || isBroadIndustry(industry);
        }

        boolean keywordMatched = keywordHints.stream().anyMatch(hint -> containsMeaningfulPhrase(lower, hint));
        boolean looseKeywordMatched = keywordHints.stream().anyMatch(hint -> containsLooseKeywordToken(lower, hint));

        if (keywordMatched && (industryMatched || isBroadIndustry(industry))) {
            return true;
        }

        return (industryMatched || industryHints.stream().anyMatch(hint -> containsLooseKeywordToken(lower, hint)))
                && (keywordMatched || looseKeywordMatched);
    }

    private boolean matchesMarketSignal(String host, String text, String market) {
        if ("ALL".equalsIgnoreCase(market)) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if ("China".equalsIgnoreCase(market) && containsChineseScript(text)) {
            return true;
        }
        return host.endsWith(marketSite(market).replace("site:.", "."))
                || lower.contains(market.toLowerCase(Locale.ROOT))
                || lower.contains(marketAlias(market));
    }

    private boolean looksLikeBlockedContent(String url, String combined) {
        String lowerUrl = cleanText(url).toLowerCase(Locale.ROOT);
        return looksLikeReferenceHost(hostOf(url))
                || EDITORIAL_URL_PATTERNS.stream().anyMatch(lowerUrl::contains)
                || NON_COMPANY_TEXT_HINTS.stream().anyMatch(combined::contains);
    }

    private boolean looksLikeCompanyCandidate(String host, String combined) {
        if (host.isBlank()) {
            return false;
        }
        if (looksLikeReferenceHost(host)) {
            return false;
        }
        if (NON_COMPANY_TEXT_HINTS.stream().anyMatch(combined::contains)) {
            return false;
        }
        boolean companyHint = COMPANY_TEXT_HINTS.stream().anyMatch(combined::contains);
        boolean corporateDomain = host.endsWith(".cn") || host.endsWith(".com.cn") || host.endsWith(".de") || host.endsWith(".us");
        return companyHint || corporateDomain || hasStrongCompanySignal(host, combined);
    }

    private boolean hasStrongCompanySignal(String host, String combined) {
        if (host.isBlank()) {
            return false;
        }
        if (looksLikeReferenceHost(host)) {
            return false;
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        String lowerCombined = combined.toLowerCase(Locale.ROOT);
        return STRONG_COMPANY_HINTS.stream().anyMatch(lowerCombined::contains)
                || lowerHost.contains("-auto")
                || lowerHost.contains("automation")
                || lowerHost.contains("component")
                || lowerHost.contains("components")
                || lowerHost.contains("part")
                || lowerHost.contains("parts")
                || lowerHost.contains("medical")
                || lowerHost.contains("machinery")
                || lowerHost.contains("equipment")
                || lowerHost.contains("industrial")
                || lowerHost.contains("robot")
                || lowerHost.contains("tech");
    }

    private boolean looksLikeReferenceHost(String host) {
        String normalizedHost = normalizeHost(host);
        if (normalizedHost.isBlank()) {
            return false;
        }
        return REFERENCE_HOST_HINTS.stream().anyMatch(normalizedHost::contains)
                || normalizedHost.endsWith(".edu.cn")
                || normalizedHost.contains("university")
                || normalizedHost.contains("college")
                || normalizedHost.contains("school")
                || normalizedHost.contains("datacenter");
    }

    private boolean isBlockedHost(String host) {
        return BLOCKED_HOSTS.stream().anyMatch(blocked -> host.equals(blocked) || host.endsWith("." + blocked));
    }

    private boolean isFreeMail(String email) {
        if (!email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        return FREE_MAIL_DOMAINS.contains(domain);
    }

    private boolean isBroadIndustry(String industry) {
        if (industry == null || industry.isBlank()) {
            return true; // "\u5168\u90e8\u884c\u4e1a" was normalized to empty
        }
        String normalized = normalizeSearchPhrase(industry);
        return normalized.contains("industrial equipment")
                || normalized.contains("\u5de5\u4e1a\u8bbe\u5907")
                || normalized.contains("machinery")
                || normalized.contains("manufacturer");
    }

    private String inferCountry(String host, String fallbackMarket) {
        if (host.endsWith(".cn")) {
            return "China";
        }
        if (host.endsWith(".de")) {
            return "Germany";
        }
        if (host.endsWith(".us")) {
            return "USA";
        }
        return fallbackMarket;
    }

    private String normalizeMarket(String market) {
        String trimmed = market.trim();
        if (trimmed.equalsIgnoreCase("all") || "\u5168\u90e8".equals(trimmed)) {
            return "ALL";
        }
        if (trimmed.equalsIgnoreCase("china") || trimmed.equalsIgnoreCase("cn") || "\u4e2d\u56fd".equals(trimmed)) {
            return "China";
        }
        if (trimmed.equalsIgnoreCase("usa") || trimmed.equalsIgnoreCase("us") || trimmed.equalsIgnoreCase("united states") || "\u7f8e\u56fd".equals(trimmed)) {
            return "USA";
        }
        if (trimmed.equalsIgnoreCase("germany") || trimmed.equalsIgnoreCase("de") || "\u5fb7\u56fd".equals(trimmed)) {
            return "Germany";
        }
        return trimmed;
    }

    private String marketAlias(String market) {
        return switch (market) {
            case "China" -> "china";
            case "USA" -> "usa";
            case "Germany" -> "germany";
            default -> market.toLowerCase(Locale.ROOT);
        };
    }

    private String marketSite(String market) {
        return switch (market) {
            case "China" -> "site:.cn";
            case "USA" -> "site:.us";
            case "Germany" -> "site:.de";
            default -> "";
        };
    }

    private String toEnglishHint(String value) {
        String normalized = normalizeInput(value).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.contains("cnc") || normalized.contains("\u673a\u5e8a") || normalized.contains("\u91d1\u5c5e\u52a0\u5de5")) {
            return "cnc machining";
        }
        if (normalized.contains("\u5de5\u4e1a\u81ea\u52a8\u5316")) {
            return "industrial automation";
        }
        if (normalized.contains("\u5de5\u4e1a\u96f6\u90e8\u4ef6") || normalized.contains("\u6736\u4ef6")) {
            return "industrial components";
        }
        if (normalized.contains("\u901a\u7528\u673a\u68b0") || normalized.contains("\u8bbe\u5907")) {
            return "industrial machinery";
        }
        if (normalized.contains("\u6570\u63a7")) {
            return "cnc";
        }
        if (normalized.contains("\u7535\u5b50\u5236\u9020")) {
            return "electronics manufacturing";
        }
        if (normalized.contains("\u533b\u7597\u5668\u68b0")) {
            return "medical equipment";
        }
        if (normalized.contains("\u4f9b\u5e94\u5546")) {
            return "supplier";
        }
        if (normalized.contains("\u8fdb\u53e3\u5546")) {
            return "importer";
        }
        if (normalized.contains("\u7ecf\u9500\u5546")) {
            return "distributor";
        }
        if (normalized.contains("\u5236\u9020\u5546") || normalized.contains("\u5382")) {
            return "manufacturer";
        }
        return normalized;
    }

    private String rootUrlOf(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return url;
            }
            return scheme + "://" + host + "/";
        } catch (IllegalArgumentException exception) {
            return url;
        }
    }

    private String hostOf(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private boolean sameHost(String leftUrl, String rightUrl) {
        return normalizeHost(hostOf(leftUrl)).equals(normalizeHost(hostOf(rightUrl)));
    }

    private String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    private String resolveDuckDuckGoUrl(String absoluteHref, String rawHref) {
        String candidate = absoluteHref == null || absoluteHref.isBlank() ? rawHref : absoluteHref;
        if (candidate == null || candidate.isBlank()) {
            return "";
        }

        String query = "";
        try {
            URI uri = URI.create(candidate);
            query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
        } catch (IllegalArgumentException ignored) {
            int queryStart = candidate.indexOf('?');
            if (queryStart >= 0) {
                query = candidate.substring(queryStart + 1);
            }
        }

        for (String part : query.split("&")) {
            if (part.startsWith("uddg=")) {
                return URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
            }
        }
        return candidate;
    }

    private String resolveGoogleUrl(String absoluteHref, String rawHref) {
        String candidate = absoluteHref == null || absoluteHref.isBlank() ? rawHref : absoluteHref;
        if (candidate == null || candidate.isBlank()) {
            return "";
        }

        try {
            URI uri = URI.create(candidate);
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
            for (String part : query.split("&")) {
                int equalsIndex = part.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = part.substring(0, equalsIndex);
                String value = part.substring(equalsIndex + 1);
                if ("q".equalsIgnoreCase(key) || "url".equalsIgnoreCase(key)) {
                    String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
                    if (!decoded.isBlank()) {
                        return decoded;
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        return candidate;
    }

    private String followRedirectUrl(String url, int timeoutMs) {
        try {
            log.info("Crawler fetch: {}", url);
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(timeoutMs)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();
            return response.url().toString();
        } catch (IOException exception) {
            return url;
        }
    }

    private String metaContent(Document document, String selector) {
        Element element = document.selectFirst(selector);
        return element == null ? "" : cleanText(element.attr("content"));
    }

    private String simplifyTitle(String title) {
        String normalized = cleanText(title);
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split("\\s*[-/｜–—·_]\\s*");
        // 优先选取最像公司名的段落：长度适中（4-60字符）且是第一段
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() >= 4 && trimmed.length() <= 60) {
                return trimmed;
            }
        }
        return parts[0].trim();
    }

    private String normalizeInput(String value) {
        return fallback(value, "").replaceAll("\\s+", " ").trim();
    }

    private String normalizeSearchPhrase(String value) {
        return normalizeInput(value).toLowerCase(Locale.ROOT);
    }

    private boolean containsMeaningfulPhrase(String haystack, String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return false;
        }

        if (haystack.contains(phrase)) {
            return true;
        }

        String[] tokens = phrase.split("\\s+");
        if (tokens.length <= 1) {
            return haystack.contains(phrase);
        }

        int matchedTokens = 0;
        int expectedTokens = 0;
        for (String token : tokens) {
            if (token.length() < 3) {
                continue;
            }
            expectedTokens++;
            if (haystack.contains(token)) {
                matchedTokens++;
            }
        }

        if (expectedTokens == 0) {
            return false;
        }
        if (expectedTokens <= 2) {
            return matchedTokens >= 1;
        }
        if (expectedTokens <= 5) {
            return matchedTokens >= 2;
        }
        return matchedTokens >= Math.max(2, Math.min(4, (int) Math.ceil(expectedTokens * 0.5)));
    }

    private List<String> buildSearchHints(String value) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        String normalized = normalizeInput(value);
        extractKnownBusinessPhrases(normalized).forEach(hints::add);

        String alias = normalizeInput(toEnglishHint(value));
        if (!alias.isBlank()) {
            hints.add(alias);
        }

        if (!normalized.isBlank() && looksUsefulHint(normalized)) {
            hints.add(normalized);
        }

        for (String token : normalized.split("[\\s,，;；/|]+")) {
            String cleaned = cleanText(token);
            if (cleaned.length() < 2 || SEARCH_STOP_WORDS.contains(cleaned.toLowerCase(Locale.ROOT))) {
                continue;
            }
            hints.add(cleaned);
            if (hints.size() >= 8) {
                break;
            }
        }
        return new ArrayList<>(hints);
    }

    private boolean looksUsefulHint(String normalized) {
        if (normalized.isBlank()) {
            return false;
        }
        if (containsChineseScript(normalized)) {
            return normalized.length() <= 18;
        }
        return normalized.length() <= 40 && normalized.split("\\s+").length <= 4;
    }

    private List<String> extractKnownBusinessPhrases(String text) {
        String normalized = normalizeInput(text).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> phrases = new LinkedHashSet<>();

        if (normalized.contains("机床") || normalized.contains("machine tool")) {
            phrases.add("machine tool");
            phrases.add("机床");
            phrases.add("cnc machine");
        }
        if (normalized.contains("工业设备") || normalized.contains("industrial equipment")) {
            phrases.add("industrial equipment");
            phrases.add("工业设备");
            phrases.add("machinery");
        }
        if (normalized.contains("工业自动化") || normalized.contains("industrial automation")) {
            phrases.add("industrial automation");
            phrases.add("工业自动化");
        }
        if (normalized.contains("自动化设备") || normalized.contains("automation equipment")) {
            phrases.add("automation equipment");
            phrases.add("自动化设备");
        }
        if (normalized.contains("电子制造") || normalized.contains("electronics manufacturing")) {
            phrases.add("electronics manufacturing");
            phrases.add("电子制造");
        }
        if (normalized.contains("电子")) {
            phrases.add("electronics");
            phrases.add("电子");
        }
        if (normalized.contains("手机") || normalized.contains("smartphone")) {
            phrases.add("smartphone");
            phrases.add("手机");
        }
        if (normalized.contains("手表") || normalized.contains("watch")) {
            phrases.add("watch");
            phrases.add("手表");
        }
        if (normalized.contains("配件") || normalized.contains("accessories")) {
            phrases.add("accessories");
            phrases.add("配件");
        }
        if (normalized.contains("智能制造") || normalized.contains("smart manufacturing")) {
            phrases.add("smart manufacturing");
            phrases.add("智能制造");
        }
        if (normalized.contains("数控") || normalized.contains("cnc")) {
            phrases.add("cnc");
            phrases.add("数控");
        }
        if (normalized.contains("制造商") || normalized.contains("manufacturer")) {
            phrases.add("manufacturer");
            phrases.add("制造商");
        }
        if (normalized.contains("供应商") || normalized.contains("supplier")) {
            phrases.add("supplier");
            phrases.add("供应商");
        }

        if (normalized.contains("industrial components")) {
            phrases.add("industrial components");
            phrases.add("industrial parts");
            phrases.add("mechanical parts");
            phrases.add("components");
            phrases.add("parts");
        }
        if (normalized.contains("industrial automation")) {
            phrases.add("industrial automation");
            phrases.add("automation");
            phrases.add("automation equipment");
            phrases.add("control system");
            phrases.add("motion control");
            phrases.add("plc");
            phrases.add("servo");
            phrases.add("sensor");
        }
        if (normalized.contains("工业自动化")) {
            phrases.add("工业自动化");
            phrases.add("自动化设备");
            phrases.add("工控");
            phrases.add("控制系统");
            phrases.add("伺服");
            phrases.add("传感器");
            phrases.add("工业自动化");
            phrases.add("automation");
            phrases.add("plc");
        }
        if (normalized.contains("工业零部件")) {
            phrases.add("工业零部件");
            phrases.add("零部件");
            phrases.add("配件");
            phrases.add("部件");
            phrases.add("industrial components");
            phrases.add("industrial parts");
            phrases.add("mechanical parts");
        }
        if (normalized.contains("general industrial machinery")) {
            phrases.add("general industrial machinery");
            phrases.add("industrial machinery");
            phrases.add("general machinery");
            phrases.add("machinery equipment");
            phrases.add("equipment");
        }
        if (normalized.contains("industrial machinery")) {
            phrases.add("industrial machinery");
            phrases.add("machinery equipment");
            phrases.add("equipment");
        }
        if (normalized.contains("通用机械设备")) {
            phrases.add("通用机械设备");
            phrases.add("通用机械");
            phrases.add("机械设备");
            phrases.add("industrial machinery");
            phrases.add("machinery equipment");
            phrases.add("equipment");
        }
        if (!containsChineseScript(normalized) && phrases.isEmpty() && !normalized.isBlank()) {
            phrases.add(normalized);
            if (normalized.split("\\s+").length <= 4) {
                phrases.add(normalized + " manufacturer");
                phrases.add(normalized + " supplier");
                phrases.add(normalized + " company");
            }
        }

        return new ArrayList<>(phrases);
    }

    private String firstHint(List<String> hints, String fallbackValue) {
        return hints.stream().filter(hint -> !hint.isBlank()).findFirst().orElse(cleanText(fallbackValue));
    }

    private String firstQueryHint(List<String> hints, String fallbackValue) {
        return hints.stream()
                .filter(this::looksUsefulHint)
                .findFirst()
                .orElse(firstHint(hints, fallbackValue));
    }

    private String firstNativeHint(List<String> hints, String fallbackValue) {
        return hints.stream()
                .filter(hint -> !hint.isBlank() && containsChineseScript(hint))
                .findFirst()
                .orElse(cleanText(fallbackValue));
    }

    private String nextDistinctQueryHint(List<String> hints, String current) {
        return hints.stream()
                .filter(this::looksUsefulHint)
                .filter(hint -> !hint.equalsIgnoreCase(cleanText(current)))
                .findFirst()
                .orElse("");
    }

    private String nextDistinctNativeHint(List<String> hints, String current) {
        return hints.stream()
                .filter(hint -> !hint.isBlank() && containsChineseScript(hint))
                .filter(hint -> !hint.equals(cleanText(current)))
                .findFirst()
                .orElse("");
    }

    private boolean containsLooseKeywordToken(String haystack, String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return false;
        }
        for (String token : phrase.split("\\s+")) {
            String normalized = cleanText(token).toLowerCase(Locale.ROOT);
            if (normalized.length() < 3 || SEARCH_STOP_WORDS.contains(normalized)) {
                continue;
            }
            if (haystack.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChineseScript(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private record InspectedCandidate(SearchCandidate candidate, PageScanResult scanResult) {
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String capitalize(String part) {
        if (part.isBlank()) {
            return part;
        }
        return Character.toUpperCase(part.charAt(0)) + part.substring(1);
    }

    private String joinQuery(String... parts) {
        return List.of(parts).stream()
                .map(this::cleanText)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value.trim();
    }

    private int normalizePositive(Integer value, int fallbackValue, int minValue) {
        int resolved = value == null || value <= 0 ? fallbackValue : value;
        return Math.max(resolved, minValue);
    }

    private record SearchCandidate(String title, String url, String snippet, String source) {
    }

    private record PageScanResult(String companyName, String website, String country, String contactName, String email, String channel, String fitNote) {
    }

    private static final class SearchSession {
        private final List<WorkflowModels.SearchLogEntry> logs = new ArrayList<>();

        private void log(String message) {
            logs.add(new WorkflowModels.SearchLogEntry(LocalTime.now().format(TIME_FORMATTER), message));
        }

        private List<WorkflowModels.SearchLogEntry> logs() {
            return logs;
        }
    }
}
