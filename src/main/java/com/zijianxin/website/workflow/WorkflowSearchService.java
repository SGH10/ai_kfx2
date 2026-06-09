package com.zijianxin.website.workflow;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WorkflowSearchService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSearchService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHINA_PHONE_PATTERN = Pattern.compile("(?i)(?:\\+|00)86[\\s\\-.]?\\d{2,}");
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "baidu.com",
            "bing.com",
            "duckduckgo.com",
            "google.com",
            "steampowered.com",
            "mdpi.com",
            "webmd.com",
            "mayoclinic.org",
            "medlineplus.gov",
            "merriam-webster.com",
            "britannica.com",
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
            "china.cn",
            "alibaba.com",
            "1688.com",
            "qcc.com",
            "qixin.com",
            "cufe.edu.cn",
            "cass.cn",
            "wistrategy.com",
            "pcsoft.com.cn",
            "onlinedown.net",
            "zol.com.cn",
            "pconline.com.cn",
            "mobile.pconline.com.cn",
            "amazon.cn",
            "jd.com",
            "taobao.com",
            "tmall.com"
            , "cqc.com.cn",
            "cnal.com",
            "hc360.com",
            "huangye88.com",
            "gongchang.com",
            "b2b168.com",
            "chem17.com",
            "foodjx.com",
            "zyzhan.com",
            "cnca.cn",
            "samr.gov.cn",
            "ndrc.gov.cn",
            "gov.cn",
            "mee.gov.cn",
            "cac.gov.cn",
            "people.com.cn",
            "qstheory.cn",
            "msn.cn",
            "ad.siemens.com.cn",
            "siemens.com.cn",
            "aa.com",
            "united.com",
            "americanairlines.cn",
            "trip.com",
            "ctrip.com",
            "kayak.com",
            "expedia.com",
            "eet-china.com",
            "eeworld.com.cn",
            "elecfans.com",
            "21ic.com",
            "cntronics.com",
            "mydrivers.com",
            "cnmo.com",
            "pchome.net",
            "microsoft.com",
            "bluetooth.com",
            "chinatelecom.com.cn",
            "mobile.de",
            "dodge.com",
            "cell.com",
            "cell.com.cn",
            "macrodatas.cn",
            "iikx.com",
            "xueshu.com",
            "goldsupplier.com",
            "tradekey.com",
            "shuidi.cn",
            "hisupplier.com",
            "52wmb.com",
            "51sole.com",
            "gys.cn",
            "image.so.com",
            "map.360.cn",
            "wenda.so.com",
            "doc88.com",
            "diytrade.com",
            "52ol.cn",
            "tradeindia.com",
            "mailchimp.com",
            "leadiq.com",
            "infoglobaldata.com",
            "easyleadz.com",
            "companydata.com",
            "bolddata.com",
            "bolddata.nl",
            "globaldatabase.com",
            "globaldatabase.co.uk",
            "zoominfo.com",
            "apollo.io",
            "lusha.com",
            "rocketreach.co",
            "adapt.io",
            "seamless.ai",
            "lead411.com",
            "dnb.com",
            "dunandbradstreet.com",
            "data-axle.com",
            "snov.io",
            "hunter.io",
            "hardware.cn",
            "wjw.cn",
            "autohome.com.cn",
            "m.autohome.com.cn",
            "yiparts.com",
            "ejxcn.com",
            "sizhengwang.cn",
            "misumi.com.cn",
            "info-meviy.misumi.com.cn",
            "tirapid.com",
            "proleantech.com",
            "mechrevo.com",
            "leapmotor.com",
            "larksuite.com",
            "community.ifs.com",
            "support.sap.com",
            "help.sap.com",
            "docs.ezyvet.com",
            "knowledgebase.counta.com",
            "help.commonsku.com",
            "supplier.io",
            "youtube.com",
            "linkedin.com",
            "indeed.com",
            "oracle.com",
            "adobe.com",
            "canva.com",
            "pinterest.com",
            "behance.net"
    );

    private static final Set<String> CONSUMER_PHONE_BRAND_HOSTS = Set.of(
            "apple.com",
            "apple.com.cn",
            "samsung.com",
            "samsung.com.cn",
            "huawei.com",
            "huawei.com.cn",
            "mi.com",
            "mi.cn",
            "xiaomi.com",
            "xiaomi.cn",
            "redmi.com",
            "oppo.com",
            "oppo.com.cn",
            "vivo.com",
            "vivo.com.cn",
            "honor.com",
            "honor.cn",
            "oneplus.com",
            "oneplus.com.cn",
            "realme.com",
            "realme.com.cn",
            "meizu.com",
            "nubia.com",
            "zte.com.cn",
            "lenovo.com.cn",
            "coolpad.com",
            "gionee.com",
            "transsion.com",
            "tecno-mobile.com",
            "infinixmobility.com",
            "itel-mobile.com"
    );

    private static final List<String> CONSUMER_PHONE_BRAND_TEXT_HINTS = List.of(
            "huawei", "\u534e\u4e3a", "xiaomi", "\u5c0f\u7c73", "redmi", "\u7ea2\u7c73",
            "oppo", "vivo", "honor", "\u8363\u8000", "oneplus", "\u4e00\u52a0",
            "realme", "\u771f\u6211", "meizu", "\u9b45\u65cf", "nubia", "\u52aa\u6bd4\u4e9a",
            "zte", "\u4e2d\u5174", "lenovo", "\u8054\u60f3", "coolpad", "\u9177\u6d3e",
            "gionee", "\u91d1\u7acb", "transsion", "\u4f20\u97f3", "tecno", "infinix", "itel",
            "apple", "iphone", "\u82f9\u679c", "samsung", "\u4e09\u661f"
    );

    private static final List<String> CONTACT_PATH_HINTS = List.of(
            "/contact", "/contact-us", "/about", "/about-us", "/company", "/company-profile", "/support",
            "/sales", "/team", "/impressum", "/imprint", "/legal", "/enquiry", "/inquiry", "/kontakt",
            "/contactus", "/contacts", "/lianxi", "/lxwm", "/contact-us.html", "/contact.html",
            "/about/contact", "/index.php/contact", "/index.php?m=contact"
    );

    private static final List<String> CONTACT_PAGE_FALLBACKS = List.of(
            "/contact",
            "/contact-us",
            "/contactus",
            "/contacts",
            "/contact.html",
            "/contactus.html",
            "/lxwm",
            "/lxwm.html",
            "/lianxi",
            "/lianxiwomen",
            "/about/contact",
            "/about",
            "/about-us",
            "/company",
            "/company-profile",
            "/support"
    );

    private static final List<String> CONTACT_TEXT_HINTS = List.of(
            "contact", "contact us", "contacts", "about", "about us", "support", "sales", "team", "impressum", "imprint", "kontakt",
            "\u8054\u7cfb", "\u8054\u7cfb\u6211\u4eec", "\u8054\u7cfb\u65b9\u5f0f", "\u8054\u7edc\u6211\u4eec", "\u5173\u4e8e\u6211\u4eec",
            "\u516c\u53f8\u7b80\u4ecb", "\u5728\u7ebf\u7559\u8a00", "\u9500\u552e", "\u5ba2\u670d"
    );

    private static final int MAX_CONTACT_PAGES_PER_SITE = 5;
    private static final int MAX_DEEP_EMAIL_PAGES_PER_SITE = 8;

    private static final List<String> NON_COMPANY_TEXT_HINTS = List.of(
            "dictionary", "translation", "translate", "encyclopedia", "news", "article", "blog", "forum", "wiki",
            "journal", "open access", "kids", "game", "gaming", "magazine", "paper", "research", "download",
            "meaning", "what is", "airline", "flight", "travel", "ticket", "hotel", "law firm", "attorney", "legal",
            "protocol", "specification", "datasheet", "tutorial", "guide", "explained", "association", "institute",
            "b2b marketplace", "marketplace", "verified china suppliers", "trade companies", "post offer free",
            "是什么意思", "翻译", "download", "破解版", "安装包", "教程", "软件",
                "评测", "测评", "报价", "排行榜", "品牌榜", "参数", "论坛", "商城", "电商", "购买", "零售", "律师事务所",
                "协议", "通信协议", "新手", "看懂", "指南", "协会", "行业协会", "信息网", "研究院", "检定", "标准",
                "什么是", "工艺流程", "基本知识", "工艺和应用",
                "黄页", "商铺", "信用查询", "工商信息", "水滴信用", "地图", "图片"
    );

    private static final List<String> LEAD_DATABASE_TEXT_HINTS = List.of(
            "company database", "business database", "b2b database", "contact database", "email database",
            "lead database", "sales leads", "lead generation", "email list", "mailing list", "prospect list",
            "list of companies", "companies list", "company list", "business directory", "supplier directory",
            "vendor directory", "executive contacts", "verified contacts", "database provider",
            "download leads", "buy leads", "buy email list", "targeted leads", "global database", "marketing database",
            "b2b contacts", "contact lists", "industry list", "industry database"
    );

    private static final List<String> LEAD_DATABASE_URL_HINTS = List.of(
            "company-database", "business-database", "b2b-database", "contact-database", "email-database",
            "lead-database", "sales-leads", "lead-generation", "email-list", "mailing-list",
            "prospect-list", "companies-list", "company-list", "business-directory", "supplier-directory",
            "vendor-directory", "executive-contacts", "verified-contacts", "buy-leads", "buy-email-list",
            "b2b-contacts", "contact-lists", "industry-list", "industry-database"
    );

    private static final List<String> EDITORIAL_URL_PATTERNS = List.of(
            "/news/", "newsdetail", "/article/", "/articles/", "/blog/", "/forum/", "/wiki/",
            "/baike/", "/zhidao/", "/question/", "/answers/", "/post/", "/archives/"
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

    private static final List<String> CHINA_LEGAL_HINTS = List.of(
            "\u6709\u9650\u516c\u53f8", "\u80a1\u4efd\u6709\u9650\u516c\u53f8", "\u79d1\u6280\u6709\u9650\u516c\u53f8", "\u96c6\u56e2",
            "\u4e2d\u56fd\u5de5\u5382", "\u4e2d\u56fd\u5236\u9020", "\u4e2d\u56fd\u4f9b\u5e94\u5546",
            "china factory", "china manufacturer", "chinese manufacturer", "made in china"
    );

    private static final List<String> CHINA_LOCATION_HINTS = List.of(
            "\u5e7f\u4e1c", "\u6df1\u5733", "\u4e1c\u839e", "\u5e7f\u5dde", "\u4f5b\u5c71", "\u4e2d\u5c71", "\u73e0\u6d77", "\u60e0\u5dde",
            "\u4e0a\u6d77", "\u82cf\u5dde", "\u6606\u5c71", "\u65e0\u9521", "\u5e38\u5dde", "\u5357\u4eac", "\u676d\u5dde", "\u5b81\u6ce2", "\u6e29\u5dde",
            "\u53a6\u95e8", "\u798f\u5dde", "\u9752\u5c9b", "\u70df\u53f0", "\u6d4e\u5357", "\u5317\u4eac", "\u5929\u6d25", "\u91cd\u5e86",
            "\u6210\u90fd", "\u6b66\u6c49", "\u897f\u5b89", "\u957f\u6c99", "\u90d1\u5dde", "\u5408\u80a5",
            "p.r. china", "pr china", "prc", "shenzhen", "dongguan", "guangzhou", "foshan", "zhongshan",
            "zhuhai", "huizhou", "guangdong", "shanghai", "suzhou", "kunshan", "wuxi", "changzhou", "nanjing",
            "hangzhou", "ningbo", "wenzhou", "xiamen", "fuzhou", "qingdao", "jinan", "beijing", "tianjin",
            "chongqing", "chengdu", "wuhan", "xi'an", "xian", "changsha", "zhengzhou", "hefei"
    );

    private static final List<String> FOREIGN_ENTITY_HINTS_FOR_CHINA = List.of(
            "pvt. ltd", "pvt ltd", "private limited", "llp", "sdn bhd", "pte ltd", "pty ltd", "co., inc",
            "inc.", "s.a.", "s.r.l.", "sas", "bv", "oy", "kk"
    );

    private static final List<String> FOREIGN_LOCATION_HINTS_FOR_CHINA = List.of(
            "india", "indian", "mumbai", "delhi", "bangalore", "bengaluru", "chennai", "pune", "kolkata",
            "gujarat", "maharashtra", "tamil nadu", "hyderabad", "new delhi", "taiwan", "hong kong",
            "singapore", "malaysia", "vietnam", "thailand", "indonesia", "philippines", "japan", "korea",
            "germany", "united states", "usa", "u.s.a", "america", "canada", "mexico", "brazil",
            "united kingdom", "uk", "italy", "france", "spain", "netherlands", "poland", "turkey",
            "\u5370\u5ea6", "\u53f0\u6e7e", "\u9999\u6e2f", "\u65b0\u52a0\u5761", "\u9a6c\u6765\u897f\u4e9a", "\u8d8a\u5357",
            "\u6cf0\u56fd", "\u5370\u5c3c", "\u83f2\u5f8b\u5bbe", "\u65e5\u672c", "\u97e9\u56fd", "\u5fb7\u56fd",
            "\u7f8e\u56fd", "\u82f1\u56fd", "\u52a0\u62ff\u5927"
    );

    private static final List<String> REFERENCE_HOST_HINTS = List.of(
            "dict", "dictionary", "translate", "translation", "wiki", "baike", "news", "blog", "forum",
            "zhidao", "csdn", "bilibili", "hujiang", "zhuaniao", "yingyuqiao", "cqvip", "sciencedirect", "ieee",
            "sohu", "sina", "xueqiu", "ncss", "wenku", "36kr", "toutiao", "baijiahao", "ifeng", "eastmoney",
            "cailian", "yicai", "jiemian", "pconline", "zol", "cnpp", "maigoo", "9game", "php.cn",
            "amazon", "jd", "taobao", "tmall", "thomasnet", "qmed", "ensun", "globalspec", "sourceforge",
            "china.cn",
            "webmd", "mayoclinic", "medlineplus", "merriam-webster", "britiannica", "britannica",
            "airline", "flight", "travel", "trip", "kayak", "expedia",
            "eet-china", "eeworld", "elecfans", "21ic", "cntronics",
            "mydrivers", "cnmo", "pchome", "microsoft", "bluetooth", "chinatelecom", "mobile.de", "dodge",
            "cell.com", "macrodatas", "iikx", "xueshu", "goldsupplier", "tradekey", "shuidi", "hisupplier",
            "52wmb", "51sole", "gys.cn", "image.so.com", "map.360.cn", "wenda.so.com", "doc88", "diytrade",
            "52ol", "tradeindia"
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
    private final AiSearchQueryPlannerService aiSearchQueryPlannerService;
    private final ProxyConfig crawlerProxyConfig;
    private final java.net.Proxy crawlerProxy;
    private final HttpClient httpClient;
    private volatile WorkflowModels.CustomerSearchResponse lastSearchResponse;

    public WorkflowSearchService(SettingsService settingsService, ObjectMapper objectMapper) {
        this(settingsService, objectMapper, null);
    }

    @Autowired
    public WorkflowSearchService(
            SettingsService settingsService,
            ObjectMapper objectMapper,
            AiSearchQueryPlannerService aiSearchQueryPlannerService
    ) {
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
        this.aiSearchQueryPlannerService = aiSearchQueryPlannerService;
        this.crawlerProxyConfig = detectCrawlerProxyConfig();
        this.crawlerProxy = createCrawlerProxy(crawlerProxyConfig);
        this.httpClient = createHttpClient(crawlerProxyConfig);
    }

    private HttpClient createHttpClient(ProxyConfig proxyConfig) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (proxyConfig.enabled() && proxyConfig.type() == ProxyType.HTTP) {
            builder.proxy(ProxySelector.of(InetSocketAddress.createUnresolved(proxyConfig.host(), proxyConfig.port())));
            log.info("Crawler HTTP client proxy enabled: {}", proxyConfig.displayName());
        }
        return builder.build();
    }

    private java.net.Proxy createCrawlerProxy(ProxyConfig proxyConfig) {
        if (!proxyConfig.enabled()) {
            return null;
        }
        java.net.Proxy.Type proxyType = proxyConfig.type() == ProxyType.SOCKS
                ? java.net.Proxy.Type.SOCKS
                : java.net.Proxy.Type.HTTP;
        return new java.net.Proxy(proxyType, InetSocketAddress.createUnresolved(proxyConfig.host(), proxyConfig.port()));
    }

    private Connection crawlerConnection(String url) {
        return crawlerConnection(url, true);
    }

    private Connection crawlerConnection(String url, boolean useProxy) {
        Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);
        if (useProxy && crawlerProxy != null) {
            connection.proxy(crawlerProxy);
        }
        return connection;
    }

    private ProxyConfig detectCrawlerProxyConfig() {
        ProxyConfig fromProperties = proxyConfigFromSystemProperties();
        if (fromProperties.enabled()) {
            return fromProperties;
        }

        for (String envName : List.of("HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy", "ALL_PROXY", "all_proxy")) {
            ProxyConfig fromEnv = parseProxyConfig(System.getenv(envName), "env " + envName);
            if (fromEnv.enabled()) {
                return fromEnv;
            }
        }

        return proxyConfigFromWindowsInternetSettings();
    }

    private ProxyConfig proxyConfigFromSystemProperties() {
        ProxyConfig https = proxyConfigFromHostPort(
                System.getProperty("https.proxyHost"),
                System.getProperty("https.proxyPort"),
                ProxyType.HTTP,
                "JVM https.proxyHost"
        );
        if (https.enabled()) {
            return https;
        }

        ProxyConfig http = proxyConfigFromHostPort(
                System.getProperty("http.proxyHost"),
                System.getProperty("http.proxyPort"),
                ProxyType.HTTP,
                "JVM http.proxyHost"
        );
        if (http.enabled()) {
            return http;
        }

        return proxyConfigFromHostPort(
                System.getProperty("socksProxyHost"),
                System.getProperty("socksProxyPort"),
                ProxyType.SOCKS,
                "JVM socksProxyHost"
        );
    }

    private ProxyConfig proxyConfigFromHostPort(String host, String portValue, ProxyType type, String source) {
        String normalizedHost = cleanText(host);
        int defaultPort = type == ProxyType.SOCKS ? 1080 : 80;
        int port = parseProxyPort(portValue, defaultPort);
        if (normalizedHost.isBlank() || port <= 0) {
            return noProxyConfig();
        }
        return new ProxyConfig(normalizedHost, port, type, source);
    }

    private ProxyConfig proxyConfigFromWindowsInternetSettings() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!osName.contains("win")) {
            return noProxyConfig();
        }

        String output = runShortCommand(List.of(
                "reg",
                "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                "/v",
                "ProxyEnable"
        ));
        if (output.isBlank() || !windowsProxyEnabled(output)) {
            return noProxyConfig();
        }

        String serverOutput = runShortCommand(List.of(
                "reg",
                "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                "/v",
                "ProxyServer"
        ));
        return parseProxyConfig(extractRegistryValue(serverOutput, "ProxyServer"), "Windows user proxy");
    }

    private boolean windowsProxyEnabled(String registryOutput) {
        String value = extractRegistryValue(registryOutput, "ProxyEnable").toLowerCase(Locale.ROOT);
        return value.equals("1") || value.equals("0x1") || value.endsWith(" 0x1");
    }

    private String extractRegistryValue(String registryOutput, String valueName) {
        if (registryOutput == null || registryOutput.isBlank()) {
            return "";
        }
        for (String line : registryOutput.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.toLowerCase(Locale.ROOT).startsWith(valueName.toLowerCase(Locale.ROOT) + " ")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+", 3);
            if (parts.length == 3) {
                return cleanText(parts[2]);
            }
        }
        return "";
    }

    private String runShortCommand(List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(1200, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return "";
            }
            if (process.exitValue() != 0) {
                return "";
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private ProxyConfig parseProxyConfig(String rawValue, String source) {
        String value = cleanText(rawValue);
        if (value.isBlank()) {
            return noProxyConfig();
        }

        String selected = selectProxyServerEntry(value);
        if (selected.isBlank()) {
            return noProxyConfig();
        }

        ProxyType type = selected.toLowerCase(Locale.ROOT).startsWith("socks") ? ProxyType.SOCKS : ProxyType.HTTP;
        String normalized = selected;
        if (!normalized.contains("://")) {
            normalized = (type == ProxyType.SOCKS ? "socks://" : "http://") + normalized;
        }

        try {
            URI uri = URI.create(normalized);
            String host = cleanText(uri.getHost());
            int port = uri.getPort();
            if (port <= 0) {
                port = defaultProxyPort(uri.getScheme(), type);
            }
            if (!host.isBlank() && port > 0) {
                return new ProxyConfig(host, port, type, source);
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to host:port parsing.
        }

        String withoutCredentials = selected.contains("@")
                ? selected.substring(selected.lastIndexOf('@') + 1)
                : selected;
        int slashIndex = withoutCredentials.indexOf('/');
        if (slashIndex >= 0) {
            withoutCredentials = withoutCredentials.substring(0, slashIndex);
        }
        int colonIndex = withoutCredentials.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex >= withoutCredentials.length() - 1) {
            return noProxyConfig();
        }
        String host = cleanText(withoutCredentials.substring(0, colonIndex));
        int port = parseProxyPort(withoutCredentials.substring(colonIndex + 1), -1);
        return host.isBlank() || port <= 0 ? noProxyConfig() : new ProxyConfig(host, port, type, source);
    }

    private String selectProxyServerEntry(String value) {
        if (!value.contains("=")) {
            return value;
        }

        String https = "";
        String http = "";
        String socks = "";
        for (String entry : value.split(";")) {
            int equalsIndex = entry.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex >= entry.length() - 1) {
                continue;
            }
            String key = entry.substring(0, equalsIndex).trim().toLowerCase(Locale.ROOT);
            String server = entry.substring(equalsIndex + 1).trim();
            if ("https".equals(key)) {
                https = server;
            } else if ("http".equals(key)) {
                http = server;
            } else if ("socks".equals(key) || "socks5".equals(key)) {
                socks = "socks://" + server;
            }
        }
        if (!https.isBlank()) {
            return https;
        }
        if (!http.isBlank()) {
            return http;
        }
        return socks;
    }

    private int defaultProxyPort(String scheme, ProxyType type) {
        String normalizedScheme = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
        if (normalizedScheme.startsWith("socks") || type == ProxyType.SOCKS) {
            return 1080;
        }
        return "https".equals(normalizedScheme) ? 443 : 80;
    }

    private int parseProxyPort(String value, int fallbackValue) {
        String normalized = cleanText(value);
        if (normalized.isBlank()) {
            return fallbackValue;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return fallbackValue;
        }
    }

    private ProxyConfig noProxyConfig() {
        return new ProxyConfig("", 0, ProxyType.HTTP, "");
    }

    public WorkflowModels.CustomerSearchResponse getLastSearchResponse() {
        return lastSearchResponse;
    }

    public WorkflowModels.CustomerLead inspectUrlForDebug(String url, String market, String industry, String keywords) {
        SearchCandidate candidate = new SearchCandidate(
                cleanText(url),
                cleanText(url),
                "",
                "Debug"
        );
        PageScanResult scanResult = inspectWebsite(
                candidate,
                fallback(industry, ""),
                normalizeMarket(fallback(market, "China")),
                fallback(keywords, ""),
                buildSearchExclusionTerms(List.of()),
                settingsService.getSettings().crawler().requestTimeoutMs(),
                settingsService.getSettings().crawler()
        );
        return scanResult == null ? null : toLead(scanResult);
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
        String targetDescription = normalizeInput(fallback(request.targetDescription(), ""));
        if (!targetDescription.isBlank() && normalizeSearchPhrase(keywords).equals(normalizeSearchPhrase(targetDescription))) {
            keywords = "";
        }
        String searchDepth = normalizeInput(fallback(request.searchDepth(), "standard"));
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

        AiSearchQueryPlannerService.SearchQueryPlan aiPlan = planAiSearchQueries(
                targetDescription,
                industry,
                market,
                keywords,
                companySize,
                searchDepth,
                leadLimit
        );
        List<String> ruleQueries = buildSearchQueries(industry, market, keywords, companySize);
        List<String> expansionQueries = buildIntentExpansionQueries(targetDescription, industry, market, keywords);
        List<String> queries = mergeSearchQueries(
                expansionQueries,
                aiPlan.queries(),
                limitRuleQueries(ruleQueries, aiPlan, targetDescription)
        );
        List<String> exclusionTerms = buildSearchExclusionTerms(aiPlan.excludeTerms());
        String effectiveKeywords = buildEffectiveKeywords(keywords, targetDescription, aiPlan);
        session.log("Received search task.");
        if (!targetDescription.isBlank()) {
            session.log("Target description: " + targetDescription);
        }
        if (!aiPlan.queries().isEmpty()) {
            session.log("AI search planner: " + fallback(aiPlan.normalizedIntent(), aiPlan.note()));
            session.log("AI query plan: " + String.join(" | ", aiPlan.queries()));
        } else if (!aiPlan.note().isBlank()) {
            session.log(aiPlan.note());
        }
        session.log("Search strategy: " + String.join(" | ", queries));
        session.log("Runtime config: limit=" + leadLimit + ", pool=" + candidatePoolLimit + ", timeout=" + timeoutMs + "ms");
        if (crawlerProxyConfig.enabled()) {
            session.log("Network proxy: " + crawlerProxyConfig.displayName());
        }

        List<SearchCandidate> candidates = fetchCandidates(queries, market, industry, effectiveKeywords, session, leadLimit, candidatePoolLimit, timeoutMs, searchSettings, crawlerSettings);
        List<WorkflowModels.CustomerLead> leads = inspectCandidates(
                candidates,
                industry,
                market,
                effectiveKeywords,
                exclusionTerms,
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

    private AiSearchQueryPlannerService.SearchQueryPlan planAiSearchQueries(
            String targetDescription,
            String industry,
            String market,
            String keywords,
            String companySize,
            String searchDepth,
            int leadLimit
    ) {
        if (aiSearchQueryPlannerService == null) {
            return AiSearchQueryPlannerService.SearchQueryPlan.empty("");
        }
        if (targetDescription.isBlank() && keywords.isBlank() && industry.isBlank()) {
            return AiSearchQueryPlannerService.SearchQueryPlan.empty("AI search planner skipped: no target description, industry, or keywords.");
        }

        String descriptionForAi = !targetDescription.isBlank() ? targetDescription : (!keywords.isBlank() ? keywords : industry);
        return aiSearchQueryPlannerService.plan(new AiSearchQueryPlannerService.SearchQueryPlanRequest(
                descriptionForAi,
                industry,
                market,
                companySize,
                searchDepth,
                leadLimit
        ));
    }

    private List<String> mergeSearchQueries(List<String> aiQueries, List<String> expansionQueries, List<String> ruleQueries) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (aiQueries != null) {
            aiQueries.stream()
                    .map(this::cleanText)
                    .filter(query -> !query.isBlank())
                    .forEach(merged::add);
        }
        if (expansionQueries != null) {
            expansionQueries.stream()
                    .map(this::cleanText)
                    .filter(query -> !query.isBlank())
                    .forEach(merged::add);
        }
        if (ruleQueries != null) {
            ruleQueries.stream()
                    .map(this::cleanText)
                    .filter(query -> !query.isBlank())
                    .forEach(merged::add);
        }
        return new ArrayList<>(merged);
    }

    private List<String> buildIntentExpansionQueries(String targetDescription, String industry, String market, String keywords) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        String combined = normalizeSearchPhrase(String.join(" ", targetDescription, industry, keywords));
        boolean china = "China".equalsIgnoreCase(market);
        String foreignMarketName = "ALL".equalsIgnoreCase(market) ? "" : marketSearchName(market);
        String foreignMarketAdjective = "ALL".equalsIgnoreCase(market) ? "" : marketAdjective(market);
        boolean phoneManufacturingMachine = (combined.contains("手机") || combined.contains("3c") || combined.contains("smartphone") || combined.contains("mobile phone"))
                && (combined.contains("机床") || combined.contains("cnc") || combined.contains("加工") || combined.contains("machining") || combined.contains("设备"));
        boolean phoneAccessories = combined.contains("手机配件")
                || combined.contains("手机壳")
                || combined.contains("数据线")
                || combined.contains("充电器")
                || combined.contains("耳机")
                || combined.contains("蓝牙耳机")
                || combined.contains("mobile phone accessories")
                || combined.contains("smartphone accessories")
                || combined.contains("phone accessories")
                || combined.contains("charger")
                || combined.contains("earphone")
                || combined.contains("earbuds")
                || combined.contains("data cable")
                || combined.contains("usb-c cable");
        boolean medicalEquipment = combined.contains("medical device")
                || combined.contains("medical equipment")
                || combined.contains("medtech")
                || combined.contains("医疗器械")
                || combined.contains("医疗设备")
                || combined.contains("器械");
        boolean medicalIndustry = normalizeSearchPhrase(industry).contains("medical device")
                || normalizeSearchPhrase(industry).contains("medical equipment")
                || normalizeSearchPhrase(industry).contains("medtech")
                || normalizeSearchPhrase(industry).contains("医疗器械")
                || normalizeSearchPhrase(industry).contains("医疗设备")
                || normalizeSearchPhrase(industry).contains("器械");
        boolean phoneBrandIndustry = isConsumerPhoneBrandIndustry(targetDescription, industry, keywords);
        boolean electronicsManufacturing = combined.contains("electronics manufacturing")
                || combined.contains("electronics manufacturer")
                || combined.contains("electronic manufacturing")
                || combined.contains("电子制造")
                || combined.contains("pcb")
                || combined.contains("pcba");

        if (phoneBrandIndustry) {
            addConsumerPhoneBrandQueries(queries, market);
        }

        if (phoneManufacturingMachine) {
            if (china) {
                queries.add("site:.cn 3C 数控机床 厂家 联系方式");
                queries.add("site:.cn 3C CNC 加工设备 厂家 官网");
                queries.add("site:.cn 手机结构件 加工设备 厂家");
                queries.add("site:.cn 手机中框 CNC 设备 厂家");
                queries.add("site:.cn 精密CNC 加工中心 厂家 联系我们");
                queries.add("site:.cn 数控机床 3C 行业 联系方式");
                queries.add("site:.com.cn CNC 加工中心 3C 厂家");
                queries.add("site:.com.cn 数控机床 手机零部件 厂家");
                queries.add("site:.cn 3C 钻攻中心 厂家 联系方式");
                queries.add("site:.cn 高速钻攻中心 厂家 官网");
                queries.add("site:.cn 玻璃精雕机 厂家 联系我们");
                queries.add("site:.cn 手机玻璃 精雕机 厂家");
                queries.add("site:.cn 手机外壳 精雕机 厂家");
                queries.add("site:.cn 3C 精雕机 厂家 官网");
                queries.add("site:.cn 高速加工中心 3C 厂家");
                queries.add("site:.cn CNC钻攻中心 生产厂家");
                queries.add("site:.cn 3C自动化设备 手机 厂家");
                queries.add("site:.cn 手机组装 自动化设备 厂家");
            } else {
                queries.add("3C CNC machining equipment manufacturer contact");
                queries.add("smartphone parts CNC machine manufacturer");
                queries.add("precision CNC machining center manufacturer contact");
            }
        }

        if (phoneAccessories) {
            if (china) {
                queries.add("手机配件 厂家 联系方式");
                queries.add("手机配件 生产厂家 官网");
                queries.add("深圳 手机配件 有限公司 联系方式");
                queries.add("东莞 手机配件 工厂 联系我们");
                queries.add("手机壳 生产厂家 联系我们");
                queries.add("手机数据线 工厂 联系方式");
                queries.add("手机充电器 厂家 邮箱");
                queries.add("蓝牙耳机 工厂 联系方式");
                queries.add("手机配件 OEM ODM 工厂");
                queries.add("site:.cn 手机配件 厂家 联系方式");
                queries.add("site:.com.cn 手机配件 工厂 官网");
                queries.add("phone accessories OEM factory China contact");
                queries.add("smartphone accessories manufacturer China email");
                queries.add("mobile phone accessories supplier China contact");
                queries.add("phone case manufacturer China contact");
                queries.add("phone data cable manufacturer China contact");
                queries.add("USB-C phone cable manufacturer China contact");
                queries.add("mobile phone charger factory China sales email");
                queries.add("wireless earbuds factory China contact");
            } else {
                queries.add(joinQuery(foreignMarketName, "phone accessories OEM factory contact"));
                queries.add(joinQuery(foreignMarketName, "smartphone accessories manufacturer sales email"));
                queries.add(joinQuery(foreignMarketName, "mobile phone accessories supplier contact"));
                queries.add(joinQuery(foreignMarketName, "mobile phone charger factory sales email"));
                queries.add(joinQuery(foreignMarketName, "wireless earbuds factory contact"));
                queries.add(joinQuery(foreignMarketAdjective, "phone accessories supplier official website"));
            }
        }

        if (china && (medicalEquipment || medicalIndustry)) {
            queries.add("site:.cn medical device manufacturer contact");
            queries.add("site:.com.cn medical equipment manufacturer");
            queries.add("医疗器械 厂家 联系方式");
            queries.add("医疗设备 生产厂家 官网");
            queries.add("医用设备 有限公司 邮箱");
            queries.add("medical device supplier China email");
        }

        if (!china && (medicalEquipment || medicalIndustry)) {
            queries.add(joinQuery("medical device contract manufacturing", foreignMarketName, "contact"));
            queries.add(joinQuery("medical device manufacturing services", foreignMarketName, "contact"));
            queries.add(joinQuery("medical device OEM manufacturer", foreignMarketName, "contact"));
            queries.add(joinQuery("ISO 13485 medical device manufacturer", foreignMarketName, "contact"));
            queries.add(joinQuery("medical device supplier sales contact", foreignMarketName));
            queries.add(joinQuery(foreignMarketAdjective, "medical equipment manufacturer products contact"));
        }

        if (!china && electronicsManufacturing) {
            queries.add(joinQuery("electronics contract manufacturing", foreignMarketName, "contact"));
            queries.add(joinQuery("electronics manufacturing services", foreignMarketName, "contact"));
            queries.add(joinQuery("PCB assembly manufacturer", foreignMarketName, "contact"));
            queries.add(joinQuery("PCBA manufacturer sales contact", foreignMarketName));
            queries.add(joinQuery("electronic manufacturing services company", foreignMarketName, "contact"));
            queries.add(joinQuery(foreignMarketAdjective, "OEM electronics manufacturer contact"));
        }

        return new ArrayList<>(queries);
    }

    private void addConsumerPhoneBrandQueries(LinkedHashSet<String> queries, String market) {
        if ("China".equalsIgnoreCase(market)) {
            queries.add("手机品牌 官网");
            queries.add("智能手机品牌 官网");
            queries.add("国产手机品牌 官网");
            queries.add("中国手机品牌 官网");
            queries.add("手机厂商 官网");
            queries.add("智能手机厂商 官网");
            queries.add("手机公司 官网");
            queries.add("手机品牌 官方网站");
            queries.add("site:.cn 手机品牌 官网");
            queries.add("site:.com.cn 手机品牌 官网");
            queries.add("site:.cn 智能手机 官网");
            queries.add("site:.com.cn 智能手机 官网");
            queries.add("华为 手机 官网");
            queries.add("小米 手机 官网");
            queries.add("OPPO 手机 官网");
            queries.add("vivo 手机 官网");
            queries.add("荣耀 手机 官网");
            queries.add("魅族 手机 官网");
            queries.add("一加 手机 官网");
            queries.add("realme 手机 官网");
            queries.add("努比亚 手机 官网");
            queries.add("中兴 手机 官网");
            queries.add("传音 手机 官网");
            return;
        }

        String marketName = "ALL".equalsIgnoreCase(market) ? "" : marketSearchName(market);
        queries.add(joinQuery(marketName, "smartphone brand official website"));
        queries.add(joinQuery(marketName, "mobile phone brand official website"));
        queries.add(joinQuery(marketName, "smartphone company official website"));
        queries.add(joinQuery(marketName, "mobile phone manufacturer official website"));
        queries.add(joinQuery(marketName, "phone brand company contact"));
    }

    private boolean isConsumerPhoneBrandIndustry(String targetDescription, String industry, String keywords) {
        String combined = normalizeSearchPhrase(String.join(" ", targetDescription, industry, keywords));
        if (combined.isBlank()) {
            return false;
        }
        boolean phoneIntent = combined.contains("手机")
                || combined.contains("智能手机")
                || combined.contains("smartphone")
                || combined.contains("mobile phone")
                || combined.contains("cell phone");
        if (!phoneIntent) {
            return false;
        }
        boolean accessoryOrComponentIntent = combined.contains("手机配件")
                || combined.contains("手机壳")
                || combined.contains("手机零部件")
                || combined.contains("手机结构件")
                || combined.contains("数据线")
                || combined.contains("充电器")
                || combined.contains("耳机")
                || combined.contains("蓝牙耳机")
                || combined.contains("accessories")
                || combined.contains("component")
                || combined.contains("parts")
                || combined.contains("charger")
                || combined.contains("case")
                || combined.contains("earphone")
                || combined.contains("earbuds")
                || combined.contains("data cable");
        boolean equipmentIntent = combined.contains("机床")
                || combined.contains("cnc")
                || combined.contains("加工设备")
                || combined.contains("自动化设备")
                || combined.contains("machining equipment");
        if (accessoryOrComponentIntent || equipmentIntent) {
            return false;
        }

        String normalizedIndustry = normalizeSearchPhrase(industry);
        boolean bareIndustry = Set.of(
                "手机", "智能手机", "手机行业", "智能手机行业",
                "smartphone", "smartphones", "mobile phone", "mobile phones", "cell phone", "cell phones"
        ).contains(normalizedIndustry);
        boolean brandIntent = combined.contains("品牌")
                || combined.contains("厂商")
                || combined.contains("手机官网")
                || combined.contains("手机公司")
                || combined.contains("官网")
                || combined.contains("官方")
                || combined.contains("brand")
                || combined.contains("official website")
                || combined.contains("manufacturer");
        return bareIndustry || brandIntent;
    }

    private boolean looksLikeConsumerPhoneBrandCandidate(String host, String combined) {
        String normalizedHost = normalizeHost(host);
        String lowerCombined = cleanText(combined).toLowerCase(Locale.ROOT);
        if (CONSUMER_PHONE_BRAND_HOSTS.stream().anyMatch(brandHost -> normalizedHost.equals(brandHost) || normalizedHost.endsWith("." + brandHost))) {
            return true;
        }
        boolean brandSignal = CONSUMER_PHONE_BRAND_TEXT_HINTS.stream().anyMatch(lowerCombined::contains);
        boolean phoneSignal = lowerCombined.contains("手机")
                || lowerCombined.contains("智能手机")
                || lowerCombined.contains("smartphone")
                || lowerCombined.contains("mobile phone")
                || lowerCombined.contains("iphone")
                || lowerCombined.contains("android");
        boolean officialOrCompanySignal = lowerCombined.contains("官网")
                || lowerCombined.contains("官方网站")
                || lowerCombined.contains("official website")
                || lowerCombined.contains("official site")
                || lowerCombined.contains("公司")
                || lowerCombined.contains("company");
        return brandSignal && phoneSignal && officialOrCompanySignal;
    }

    private void addGenericChinaIndustryDiscoveryQueries(
            LinkedHashSet<String> queries,
            List<String> industryHints,
            List<String> keywordHints,
            String industry,
            String keywords
    ) {
        List<String> nativeTerms = queryTerms(industryHints, industry, true);
        List<String> englishTerms = queryTerms(industryHints, toEnglishHint(industry), false);
        List<String> nativeKeywordTerms = queryTerms(keywordHints, keywords, true);
        List<String> englishKeywordTerms = queryTerms(keywordHints, toEnglishHint(keywords), false);

        for (String term : nativeTerms) {
            addGenericChinaNativeQueries(queries, term, nativeKeywordTerms);
        }
        for (String term : englishTerms) {
            addGenericChinaEnglishQueries(queries, term, englishKeywordTerms);
        }
    }

    private void addGenericChinaNativeQueries(LinkedHashSet<String> queries, String term, List<String> keywordTerms) {
        if (term.isBlank()) {
            return;
        }
        queries.add(joinQuery(term, "官网"));
        queries.add(joinQuery(term, "官方网站"));
        queries.add(joinQuery(term, "公司官网"));
        queries.add(joinQuery(term, "企业官网"));
        queries.add(joinQuery(term, "厂家", "联系方式"));
        queries.add(joinQuery(term, "供应商", "联系方式"));
        queries.add(joinQuery(term, "有限公司"));
        queries.add(joinQuery("site:.cn", term, "官网"));
        queries.add(joinQuery("site:.cn", term, "有限公司"));
        queries.add(joinQuery("site:.com.cn", term, "官网"));
        queries.add(joinQuery("site:.com.cn", term, "有限公司"));
        for (String keywordTerm : keywordTerms.stream().limit(2).toList()) {
            queries.add(joinQuery(keywordTerm, term, "官网"));
            queries.add(joinQuery(keywordTerm, term, "联系方式"));
        }
    }

    private void addGenericChinaEnglishQueries(LinkedHashSet<String> queries, String term, List<String> keywordTerms) {
        if (term.isBlank()) {
            return;
        }
        queries.add(joinQuery("site:.cn", term, "official website"));
        queries.add(joinQuery("site:.com.cn", term, "company"));
        queries.add(joinQuery(term, "China", "official website"));
        queries.add(joinQuery(term, "China", "company"));
        queries.add(joinQuery(term, "China", "manufacturer", "contact"));
        queries.add(joinQuery(term, "China", "supplier", "contact"));
        for (String keywordTerm : keywordTerms.stream().limit(2).toList()) {
            queries.add(joinQuery(keywordTerm, term, "China", "official website"));
            queries.add(joinQuery(keywordTerm, term, "supplier", "contact"));
        }
    }

    private void addForeignMarketDiscoveryQueries(
            LinkedHashSet<String> queries,
            String market,
            List<String> industryHints,
            List<String> keywordHints,
            String industry,
            String keywords
    ) {
        if ("China".equalsIgnoreCase(market) || "ALL".equalsIgnoreCase(market)) {
            return;
        }

        List<String> industryTerms = queryTerms(industryHints, toEnglishHint(industry), false);
        List<String> keywordTerms = queryTerms(keywordHints, toEnglishHint(keywords), false);
        if (industryTerms.isEmpty()) {
            industryTerms = List.of("manufacturer");
        }

        String marketName = marketSearchName(market);
        String marketAdjective = marketAdjective(market);
        String marketSite = marketSite(market);
        for (String industryTerm : industryTerms.stream().limit(3).toList()) {
            queries.add(joinQuery(marketName, industryTerm, "company official website"));
            queries.add(joinQuery(marketName, industryTerm, "manufacturer contact"));
            queries.add(joinQuery(marketAdjective, industryTerm, "supplier contact"));
            queries.add(joinQuery(industryTerm, marketName, "sales email"));
            if (!marketSite.isBlank()) {
                queries.add(joinQuery(marketSite, industryTerm, "company contact"));
            }
            for (String keywordTerm : keywordTerms.stream().limit(2).toList()) {
                queries.add(joinQuery(marketName, keywordTerm, industryTerm, "manufacturer contact"));
                queries.add(joinQuery(keywordTerm, industryTerm, marketName, "official website"));
            }
        }
    }

    private List<String> queryTerms(List<String> hints, String fallbackValue, boolean nativeOnly) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (hints != null) {
            hints.stream()
                    .map(this::cleanText)
                    .filter(term -> !term.isBlank())
                    .filter(this::looksUsefulQueryTerm)
                    .filter(term -> nativeOnly == containsChineseScript(term))
                    .limit(4)
                    .forEach(terms::add);
        }
        String fallbackTerm = cleanText(fallbackValue);
        if (!fallbackTerm.isBlank() && looksUsefulQueryTerm(fallbackTerm) && nativeOnly == containsChineseScript(fallbackTerm)) {
            terms.add(fallbackTerm);
        }
        return new ArrayList<>(terms);
    }

    private boolean looksUsefulQueryTerm(String term) {
        String cleaned = cleanText(term);
        if (cleaned.length() < 2) {
            return false;
        }
        if (SEARCH_STOP_WORDS.contains(cleaned.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return looksUsefulHint(cleaned);
    }

    List<String> buildIntentExpansionQueriesForTest(String targetDescription, String industry, String market, String keywords) {
        return buildIntentExpansionQueries(targetDescription, industry, market, keywords);
    }

    private List<String> limitRuleQueries(
            List<String> ruleQueries,
            AiSearchQueryPlannerService.SearchQueryPlan aiPlan,
            String targetDescription
    ) {
        if (aiPlan == null || aiPlan.queries().isEmpty() || ruleQueries == null) {
            return ruleQueries == null ? List.of() : ruleQueries;
        }
        if (!cleanText(targetDescription).isBlank()) {
            return List.of();
        }
        return ruleQueries.stream()
                .limit(6)
                .toList();
    }

    private List<String> buildSearchExclusionTerms(List<String> aiExcludeTerms) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        List.of(
                "news", "article", "blog", "forum", "wiki", "dictionary", "directory", "marketplace",
                "review", "ranking", "price", "shopping", "retail", "download",
                "新闻", "资讯", "文章", "博客", "论坛", "百科", "词典", "黄页", "目录",
                "评测", "测评", "排行", "排行榜", "报价", "商城", "购买", "零售", "下载"
        ).forEach(terms::add);
        if (aiExcludeTerms != null) {
            aiExcludeTerms.stream()
                    .map(this::cleanText)
                    .filter(term -> !term.isBlank())
                    .forEach(terms::add);
        }
        return new ArrayList<>(terms);
    }

    private String buildEffectiveKeywords(String keywords, String targetDescription, AiSearchQueryPlannerService.SearchQueryPlan aiPlan) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        buildSearchHints(keywords).forEach(terms::add);
        buildSearchHints(targetDescription).forEach(terms::add);
        if (aiPlan != null) {
            aiPlan.productTerms().stream()
                    .map(this::cleanText)
                    .filter(term -> !term.isBlank())
                    .forEach(term -> buildSearchHints(term).forEach(terms::add));
        }
        return terms.stream()
                .filter(term -> !term.isBlank())
                .limit(10)
                .collect(Collectors.joining(" "));
    }

    List<String> buildSearchQueries(String industry, String market, String keywords, String companySize) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        // "全部行业" is normalized to empty — use generic terms so queries stay useful
        boolean broadIndustry = industry.isBlank();
        String effectiveIndustry = broadIndustry ? "manufacturer" : industry;

        List<String> industryHints = buildSearchHints(effectiveIndustry);
        List<String> keywordHints = buildSearchHints(keywords);
        boolean hasKeywordHints = !keywordHints.isEmpty();
        String normalizedIndustry = normalizeSearchPhrase(industry);
        boolean componentsIndustry = normalizedIndustry.contains("industrial components")
                || normalizedIndustry.contains("industrial parts")
                || normalizedIndustry.contains("mechanical parts")
                || normalizedIndustry.contains("工业零部件")
                || normalizedIndustry.contains("零部件")
                || normalizedIndustry.contains("零件")
                || normalizedIndustry.contains("配件")
                || normalizedIndustry.contains("部件");
        boolean machineryIndustry = normalizedIndustry.contains("industrial machinery")
                || normalizedIndustry.contains("general industrial machinery")
                || normalizedIndustry.contains("通用机械设备")
                || normalizedIndustry.contains("通用机械")
                || normalizedIndustry.contains("机械设备");
        boolean packagingMachineryIndustry = normalizedIndustry.contains("packaging machinery")
                || normalizedIndustry.contains("packaging equipment")
                || normalizedIndustry.contains("packing machine")
                || normalizedIndustry.contains("包装机械设备")
                || normalizedIndustry.contains("包装机械")
                || normalizedIndustry.contains("包装设备")
                || normalizedIndustry.contains("包装机");
        if (packagingMachineryIndustry) {
            machineryIndustry = false;
        }
        boolean automationIndustry = normalizedIndustry.contains("industrial automation")
                || normalizedIndustry.contains("工业自动化")
                || normalizedIndustry.contains("自动化设备")
                || normalizedIndustry.contains("工控")
                || normalizedIndustry.contains("控制系统");
        boolean cncIndustry = normalizedIndustry.contains("cnc")
                || normalizedIndustry.contains("金属加工")
                || normalizedIndustry.contains("数控")
                || normalizedIndustry.contains("机床");
        boolean medicalIndustry = normalizedIndustry.contains("medical equipment")
                || normalizedIndustry.contains("medical device")
                || normalizedIndustry.contains("medtech")
                || normalizedIndustry.contains("医疗器械")
                || normalizedIndustry.contains("医疗设备")
                || normalizedIndustry.contains("医用设备")
                || normalizedIndustry.contains("器械");
        boolean phoneBrandIndustry = isConsumerPhoneBrandIndustry("", industry, keywords);
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
            if (phoneBrandIndustry) {
                addConsumerPhoneBrandQueries(queries, market);
            }
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
            addGenericChinaIndustryDiscoveryQueries(queries, industryHints, keywordHints, industry, keywords);
            if (componentsIndustry) {
                queries.add(joinQuery("site:.cn", "parts", "manufacturer"));
                queries.add(joinQuery("site:.cn", "components", "supplier"));
                queries.add(joinQuery("site:.cn", "industrial parts", "manufacturer"));
                queries.add(joinQuery("site:.cn", "mechanical parts", "manufacturer"));
                queries.add(joinQuery("industrial components", "China", "manufacturer", "contact"));
                queries.add(joinQuery("mechanical parts", "China", "factory", "contact"));
                queries.add(joinQuery("工业零部件", "厂家", "联系方式"));
                queries.add(joinQuery("工业零部件", "生产厂家", "官网"));
                queries.add(joinQuery("机械零部件", "厂家", "联系方式"));
                queries.add(joinQuery("精密机械零部件", "有限公司"));
                queries.add(joinQuery("精密零部件", "加工", "有限公司"));
                queries.add(joinQuery("金属零件加工", "有限公司"));
                queries.add(joinQuery("非标零件加工", "厂家", "联系方式"));
                queries.add(joinQuery("冲压件", "加工", "有限公司"));
                queries.add(joinQuery("钣金件", "加工", "有限公司"));
                queries.add(joinQuery("紧固件", "厂家", "联系方式"));
                queries.add(joinQuery("传动配件", "有限公司"));
                queries.add(joinQuery("工业配件", "有限公司"));
                queries.add(joinQuery("五金零件", "生产厂家", "官网"));
                queries.add(joinQuery("机加工件", "加工厂", "联系方式"));
                queries.add(joinQuery("设备零部件", "有限公司"));
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
            if (packagingMachineryIndustry) {
                queries.add(joinQuery("site:.cn", "packaging machinery", "manufacturer"));
                queries.add(joinQuery("site:.cn", "packaging equipment", "manufacturer"));
                queries.add(joinQuery("packaging machinery", "supplier", "contact"));
                queries.add(joinQuery("包装机械", "有限公司"));
                queries.add(joinQuery("包装设备", "厂家"));
                queries.add(joinQuery("包装机", "厂家", "联系方式"));
                queries.add(joinQuery("site:.cn", "包装机械", "有限公司"));
                queries.add(joinQuery("site:.cn", "包装设备", "有限公司"));
                queries.add(joinQuery("site:.cn", "包装机", "厂家"));
                queries.add(joinQuery("灌装机", "厂家"));
                queries.add(joinQuery("封口机", "厂家"));
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
            if (medicalIndustry) {
                queries.add(joinQuery("site:.cn", "medical device", "manufacturer", "official website"));
                queries.add(joinQuery("site:.cn", "medical equipment", "manufacturer"));
                queries.add(joinQuery("medical device", "supplier", "contact"));
                queries.add(joinQuery("site:.cn", "医疗器械", "有限公司"));
                queries.add(joinQuery("site:.cn", "医疗设备", "有限公司"));
                queries.add(joinQuery("医疗器械", "厂家", "联系方式"));
                queries.add(joinQuery("医疗设备", "厂家", "联系方式"));
                queries.add(joinQuery("医用设备", "厂家"));
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
        String marketSearchName = marketSearchName(market);
        String marketAdjective = marketAdjective(market);
        String marketSite = marketSite(market);
        String ih = broadIndustry ? "" : primaryIndustryHint;
        queries.add(joinQuery(marketSearchName, primaryKeywordHint, ih, "manufacturer", "official website"));
        queries.add(joinQuery(marketAdjective, primaryKeywordHint, ih, "supplier", "contact"));
        queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, "factory", "email"));
        if (!marketSite.isBlank()) {
            queries.add(joinQuery(marketSite, primaryKeywordHint, ih, "manufacturer", "contact"));
        }
        addForeignMarketDiscoveryQueries(queries, market, industryHints, keywordHints, industry, keywords);
        queries.add(joinQuery(marketSearchName, primaryKeywordHint, ih, "company profile"));
        queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, "sales email"));
        if (!"ALL".equalsIgnoreCase(companySize)) {
            queries.add(joinQuery(marketAlias, primaryKeywordHint, ih, companySize, "company"));
        }
        return new ArrayList<>(queries);
    }

    private List<SearchCandidate> fetchCandidates(
            List<String> queries,
            String market,
            String industry,
            String keywords,
            SearchSession session,
            int leadLimit,
            int candidatePoolLimit,
            int timeoutMs,
        SettingsModels.SearchSettings searchSettings,
        SettingsModels.CrawlerSettings crawlerSettings
    ) {
        String serpApiKey = searchSettings.serpApiKey();
        DirectSearchEngine directEngine = directSearchEngine(searchSettings.defaultEngine());
        if (serpApiKey != null && !serpApiKey.isBlank() && !isDirectOnlyEngine(directEngine)) {
            session.log("Mode: SerpAPI (engine=" + mapEngineName(searchSettings.defaultEngine()) + ")");
            return fetchCandidatesFromSerpApi(queries, session, candidatePoolLimit, serpApiKey, searchSettings.defaultEngine());
        }

        SearchSourceMix sourceMix = planDirectSearchSourceMix(directEngine, market, queries.size(), crawlerSettings);
        int collectionLimit = directSearchCollectionLimit(directEngine, candidatePoolLimit, leadLimit);
        session.log("Mode: Direct web scraping (" + directSearchModeLabel(directEngine, market) + ")");
        if (directEngine != DirectSearchEngine.AUTO) {
            session.log("Source mix: " + sourceMix.engine() + " primary fetches=" + sourceMix.primaryFetches()
                    + ", auto fallback fetches=" + sourceMix.autoFallbackFetches()
                    + ", fallback queries=" + sourceMix.autoFallbackQueryLimit() + "/" + sourceMix.queryCount());
        }
        List<SearchCandidate> candidates = new ArrayList<>();
        Set<String> seenHosts = new LinkedHashSet<>();
        int primaryQueryLimit = primaryDirectQueryLimit(directEngine, queries.size(), crawlerSettings);
        int fallbackExpansionCandidateThreshold = fallbackExpansionCandidateThreshold(directEngine, leadLimit, candidatePoolLimit);
        if (directEngine == DirectSearchEngine.GOOGLE && primaryQueryLimit < queries.size()) {
            session.log("Google direct scraping is limited to " + primaryQueryLimit + " quick probe queries; automatic sources will fill the rest.");
        }

        for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++) {
            if (candidates.size() >= collectionLimit) {
                break;
            }
            String query = queries.get(queryIndex);
            session.log("Trying query: " + query);
            if (directEngine != DirectSearchEngine.AUTO && queryIndex < primaryQueryLimit) {
                collectFromDirectEngine(directEngine, query, candidates, seenHosts, collectionLimit, timeoutMs, crawlerSettings);
            }
            if (directEngine == DirectSearchEngine.AUTO || queryIndex < sourceMix.autoFallbackQueryLimit()) {
                collectFromAutoDirectSources(query, market, directEngine, candidates, seenHosts, collectionLimit, timeoutMs, crawlerSettings, session);
            }
        }

        if (
                directEngine != DirectSearchEngine.AUTO
                        && candidates.size() < fallbackExpansionCandidateThreshold
                        && sourceMix.autoFallbackQueryLimit() < queries.size()
        ) {
            session.log("Auto fallback expanded because primary priority sources returned only " + candidates.size() + " candidates.");
            for (int queryIndex = sourceMix.autoFallbackQueryLimit(); queryIndex < queries.size(); queryIndex++) {
                if (
                        candidates.size() >= collectionLimit
                                || (directEngine == DirectSearchEngine.GOOGLE && candidates.size() >= fallbackExpansionCandidateThreshold)
                ) {
                    break;
                }
                collectFromAutoDirectSources(queries.get(queryIndex), market, directEngine, candidates, seenHosts, collectionLimit, timeoutMs, crawlerSettings, session);
            }
        }

        if (
                directEngine != DirectSearchEngine.GOOGLE
                        && crawlerSettings.googleFallbackEnabled()
                        && !"China".equalsIgnoreCase(market)
                        && candidates.size() < leadLimit
        ) {
            session.log("Google fallback enabled because primary sources returned only " + candidates.size() + " candidates.");
            collectGoogleFallback(queries, candidates, seenHosts, collectionLimit, timeoutMs, crawlerSettings);
        }

        if ("China".equalsIgnoreCase(market) && candidates.size() < Math.min(collectionLimit, Math.max(leadLimit * 2, 36))) {
            int before = candidates.size();
            List<String> supplementalQueries = buildSupplementalChinaQueries(queries, industry, keywords);
            for (String supplementalQuery : supplementalQueries) {
                if (candidates.size() >= collectionLimit || candidates.size() >= Math.max(leadLimit * 3, 54)) {
                    break;
                }
                collectFromBingRss(supplementalQuery, candidates, seenHosts, collectionLimit, timeoutMs);
                collectFromBingHtml(supplementalQuery, candidates, seenHosts, collectionLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
                if (candidates.size() < Math.max(leadLimit * 2, 36)) {
                    collectFromSo360(supplementalQuery, candidates, seenHosts, collectionLimit, timeoutMs);
                    collectFromBaidu(supplementalQuery, candidates, seenHosts, collectionLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
                }
            }
            if (candidates.size() > before) {
                session.log("Supplemental China queries added " + (candidates.size() - before) + " candidates.");
            }
        }

        session.log("Collected " + candidates.size() + " candidate websites.");
        return candidates;
    }

    private List<String> buildSupplementalChinaQueries(List<String> queries, String industry, String keywords) {
        String combined = queries == null ? "" : String.join(" ", queries).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> supplemental = new LinkedHashSet<>();
        if (isConsumerPhoneBrandIndustry("", industry, keywords)) {
            addConsumerPhoneBrandQueries(supplemental, "China");
        }
        if (combined.contains("工业零部件") || combined.contains("零部件") || combined.contains("配件") || combined.contains("industrial parts")) {
            supplemental.add("工业零部件 厂家 联系方式");
            supplemental.add("机械零部件 厂家 联系方式");
            supplemental.add("精密机械零部件 有限公司");
            supplemental.add("精密零部件 加工 有限公司");
            supplemental.add("金属零件加工 有限公司");
            supplemental.add("非标零件加工 厂家 联系方式");
            supplemental.add("冲压件 加工 有限公司");
            supplemental.add("钣金件 加工 有限公司");
            supplemental.add("紧固件 厂家 联系方式");
            supplemental.add("传动配件 有限公司");
            supplemental.add("工业配件 有限公司");
            supplemental.add("五金零件 生产厂家 官网");
            supplemental.add("机加工件 加工厂 联系方式");
            supplemental.add("设备零部件 有限公司");
            supplemental.add("零配件 厂家 联系方式");
        }
        addGenericChinaIndustryDiscoveryQueries(
                supplemental,
                buildSearchHints(industry),
                buildSearchHints(keywords),
                industry,
                keywords
        );
        if (supplemental.isEmpty()) {
            supplemental.add("厂家 联系方式 有限公司");
            supplemental.add("生产厂家 官网 有限公司");
        }
        return new ArrayList<>(supplemental);
    }

    private void collectFromDirectEngine(
            DirectSearchEngine engine,
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        switch (engine) {
            case GOOGLE -> collectFromGoogle(
                    query,
                    candidates,
                    seenHosts,
                    candidatePoolLimit,
                    Math.min(timeoutMs, normalizePositive(crawlerSettings.googleFallbackTimeoutMs(), 5000, 1000)),
                    normalizePositive(crawlerSettings.googleFallbackPageLimit(), 1, 1)
            );
            case BAIDU -> collectFromBaidu(query, candidates, seenHosts, candidatePoolLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
            case BING -> {
                collectFromBingRss(query, candidates, seenHosts, candidatePoolLimit, timeoutMs);
                collectFromBingHtml(query, candidates, seenHosts, candidatePoolLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
            }
            case DUCKDUCKGO -> collectFromDuckDuckGo(query, candidates, seenHosts, candidatePoolLimit, timeoutMs);
            case BRAVE -> collectFromBrave(query, candidates, seenHosts, candidatePoolLimit, Math.min(timeoutMs, 5000));
            case SO360 -> collectFromSo360(query, candidates, seenHosts, candidatePoolLimit, timeoutMs);
            case AUTO -> {
                // AUTO is handled by collectFromAutoDirectSources.
            }
        }
    }

    private int primaryDirectQueryLimit(
            DirectSearchEngine engine,
            int queryCount,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        if (engine == DirectSearchEngine.GOOGLE) {
            return Math.min(queryCount, normalizePositive(crawlerSettings.googleFallbackQueryLimit(), 2, 1));
        }
        return queryCount;
    }

    private int fallbackExpansionCandidateThreshold(DirectSearchEngine engine, int leadLimit, int candidatePoolLimit) {
        if (engine == DirectSearchEngine.GOOGLE) {
            return Math.min(candidatePoolLimit, Math.max(leadLimit * 4, 50));
        }
        return leadLimit;
    }

    private void collectFromAutoDirectSources(
            String query,
            String market,
            DirectSearchEngine primaryEngine,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs,
            SettingsModels.CrawlerSettings crawlerSettings,
            SearchSession session
    ) {
        if (!"China".equalsIgnoreCase(market)
                && primaryEngine != DirectSearchEngine.DUCKDUCKGO
                && primaryEngine != DirectSearchEngine.BRAVE) {
            int before = candidates.size();
            collectFromBrave(query, candidates, seenHosts, candidatePoolLimit, Math.min(timeoutMs, 5000));
            logCollectedCandidates(session, "Brave", before, candidates.size());
            if (candidates.size() >= candidatePoolLimit) {
                return;
            }
        }
        if (primaryEngine != DirectSearchEngine.BING) {
            int before = candidates.size();
            collectFromBingRss(query, candidates, seenHosts, candidatePoolLimit, timeoutMs);
            logCollectedCandidates(session, "Bing RSS", before, candidates.size());
            if (candidates.size() >= candidatePoolLimit) {
                return;
            }
            before = candidates.size();
            collectFromBingHtml(query, candidates, seenHosts, candidatePoolLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
            logCollectedCandidates(session, "Bing HTML", before, candidates.size());
            if (candidates.size() >= candidatePoolLimit) {
                return;
            }
        }
        if (!"China".equalsIgnoreCase(market) && primaryEngine != DirectSearchEngine.DUCKDUCKGO) {
            int before = candidates.size();
            collectFromDuckDuckGo(query, candidates, seenHosts, candidatePoolLimit, Math.min(timeoutMs, 3000));
            logCollectedCandidates(session, "DuckDuckGo", before, candidates.size());
        }
        if ("China".equalsIgnoreCase(market) && primaryEngine != DirectSearchEngine.BAIDU) {
            int before = candidates.size();
            if (primaryEngine != DirectSearchEngine.SO360) {
                collectFromSo360(query, candidates, seenHosts, candidatePoolLimit, timeoutMs);
                logCollectedCandidates(session, "360 Search", before, candidates.size());
                if (candidates.size() >= candidatePoolLimit) {
                    return;
                }
                before = candidates.size();
            }
            collectFromBaidu(query, candidates, seenHosts, candidatePoolLimit, timeoutMs, crawlerSettings.searchEnginePageLimit());
            logCollectedCandidates(session, "Baidu", before, candidates.size());
        }
    }

    private void logCollectedCandidates(SearchSession session, String source, int before, int after) {
        if (session != null && after > before) {
            session.log(source + " added " + (after - before) + " candidates.");
        }
    }

    private int directSearchCollectionLimit(DirectSearchEngine engine, int candidatePoolLimit, int leadLimit) {
        if (engine == DirectSearchEngine.AUTO) {
            return Math.min(candidatePoolLimit, Math.max(leadLimit * 6, 18));
        }
        return candidatePoolLimit + Math.max(leadLimit * 4, 40);
    }

    private String directSearchModeLabel(DirectSearchEngine engine, String market) {
        String fallbackSources = "China".equalsIgnoreCase(market)
                ? "Auto fallback: Bing RSS / Bing HTML / 360 Search / Baidu"
                : "Auto fallback: Bing RSS / Bing HTML / Brave / DuckDuckGo";
        return switch (engine) {
            case AUTO -> "Auto: " + fallbackSources.replace("Auto fallback: ", "");
            case GOOGLE -> "Google priority + " + fallbackSources;
            case BAIDU -> "Baidu priority + " + fallbackSources;
            case BING -> "Bing priority + " + fallbackSources;
            case DUCKDUCKGO -> "DuckDuckGo priority + " + fallbackSources;
            case BRAVE -> "Brave priority + " + fallbackSources;
            case SO360 -> "360 Search priority + " + fallbackSources;
        };
    }

    SearchSourceMix planDirectSearchSourceMix(
            String engineName,
            String market,
            int queryCount,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        return planDirectSearchSourceMix(directSearchEngine(engineName), market, queryCount, crawlerSettings);
    }

    SearchSourceMix planDirectSearchSourceMix(
            DirectSearchEngine engine,
            String market,
            int queryCount,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        int normalizedQueryCount = Math.max(queryCount, 0);
        if (engine == DirectSearchEngine.AUTO || normalizedQueryCount == 0) {
            int autoFetches = autoFallbackFetchCost(engine, market, crawlerSettings) * normalizedQueryCount;
            return new SearchSourceMix(engine.name(), normalizedQueryCount, 0, autoFetches, normalizedQueryCount);
        }

        int primaryFetches = primaryFetchCost(engine, crawlerSettings) * normalizedQueryCount;
        int fallbackFetchCost = autoFallbackFetchCost(engine, market, crawlerSettings);
        int fallbackQueryLimit = 0;
        if (fallbackFetchCost > 0 && primaryFetches > 1) {
            fallbackQueryLimit = Math.min(normalizedQueryCount, Math.max(0, (primaryFetches - 1) / fallbackFetchCost));
        }
        int fallbackFetches = fallbackFetchCost * fallbackQueryLimit;
        return new SearchSourceMix(engine.name(), normalizedQueryCount, primaryFetches, fallbackFetches, fallbackQueryLimit);
    }

    private int primaryFetchCost(DirectSearchEngine engine, SettingsModels.CrawlerSettings crawlerSettings) {
        int pageLimit = normalizePositive(crawlerSettings.searchEnginePageLimit(), 2, 1);
        return switch (engine) {
            case GOOGLE -> normalizePositive(crawlerSettings.googleFallbackPageLimit(), 1, 1);
            case BAIDU -> pageLimit;
            case BING -> 1 + pageLimit;
            case DUCKDUCKGO -> 1;
            case BRAVE -> 1;
            case SO360 -> 1;
            case AUTO -> 0;
        };
    }

    private int autoFallbackFetchCost(
            DirectSearchEngine primaryEngine,
            String market,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        int pageLimit = normalizePositive(crawlerSettings.searchEnginePageLimit(), 2, 1);
        int fetches = 0;
        if (primaryEngine != DirectSearchEngine.BING) {
            fetches += 1 + pageLimit;
        }
        if (!"China".equalsIgnoreCase(market)) {
            if (primaryEngine != DirectSearchEngine.DUCKDUCKGO && primaryEngine != DirectSearchEngine.BRAVE) {
                fetches += 1;
            }
            if (primaryEngine != DirectSearchEngine.DUCKDUCKGO) {
                fetches += 1;
            }
        }
        if ("China".equalsIgnoreCase(market) && primaryEngine != DirectSearchEngine.BAIDU) {
            fetches += pageLimit;
            if (primaryEngine != DirectSearchEngine.SO360) {
                fetches += 1;
            }
        }
        return fetches;
    }

    private DirectSearchEngine directSearchEngine(String name) {
        if (name == null || name.isBlank()) {
            return DirectSearchEngine.AUTO;
        }
        return switch (name.trim().toLowerCase(Locale.ROOT).replaceAll("[_\\s-]+", "")) {
            case "google" -> DirectSearchEngine.GOOGLE;
            case "baidu" -> DirectSearchEngine.BAIDU;
            case "bing" -> DirectSearchEngine.BING;
            case "duckduckgo", "duck" -> DirectSearchEngine.DUCKDUCKGO;
            case "brave", "bravesearch" -> DirectSearchEngine.BRAVE;
            case "360", "360search", "so360", "s360", "so" -> DirectSearchEngine.SO360;
            default -> DirectSearchEngine.AUTO;
        };
    }

    private boolean isDirectOnlyEngine(DirectSearchEngine engine) {
        return engine == DirectSearchEngine.BRAVE || engine == DirectSearchEngine.SO360;
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
            Document document = crawlerConnection(searchUrl)
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
                Document document = crawlerConnection(searchUrl)
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
                Document document = crawlerConnection(searchUrl)
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
            Document document = crawlerConnection(searchUrl)
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

    private void collectFromBrave(
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs
    ) {
        if (candidates.size() >= candidatePoolLimit) {
            return;
        }

        String searchUrl = "https://search.brave.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            log.info("Crawler fetch: {}", searchUrl);
            Document document = crawlerConnection(searchUrl)
                    .referrer("https://search.brave.com/")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(timeoutMs)
                    .get();

            for (Element link : document.select("a[href]:has(.search-snippet-title), a[href]:has(.title), a[href][href^=http]")) {
                String href = cleanText(link.attr("abs:href"));
                if (!href.startsWith("http") || href.contains("search.brave.com/search")) {
                    continue;
                }
                String title = cleanText(link.select(".search-snippet-title, .title").text());
                if (title.isBlank()) {
                    title = cleanText(link.text());
                }
                Element result = link.closest(".snippet, .snippet-content, div");
                String snippet = cleanText(result == null ? "" : result.text());
                addCandidate(candidates, seenHosts, title, href, snippet, "Brave HTML");
                if (candidates.size() >= candidatePoolLimit) {
                    break;
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void collectFromSo360(
            String query,
            List<SearchCandidate> candidates,
            Set<String> seenHosts,
            int candidatePoolLimit,
            int timeoutMs
    ) {
        if (candidates.size() >= candidatePoolLimit) {
            return;
        }

        String searchUrl = "https://www.so.com/s?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            log.info("Crawler fetch: {}", searchUrl);
            Document document = crawlerConnection(searchUrl)
                    .referrer("https://www.so.com/")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(timeoutMs)
                    .get();

            for (Element result : document.select("li.res-list, li.result, .res-list")) {
                Element link = result.selectFirst("h3.res-title a[href], h3 a[href], a[href][data-mdurl]");
                if (link == null) {
                    continue;
                }
                String resolved = cleanText(link.attr("data-mdurl"));
                if (resolved.isBlank()) {
                    resolved = resolveSo360Url(link.attr("abs:href"), link.attr("href"));
                }
                if (resolved.contains("so.com/link")) {
                    continue;
                }
                String snippet = cleanText(result.select(".res-desc, .g-linkinfo, p").text());
                addCandidate(candidates, seenHosts, cleanText(link.text()), resolved, snippet, "360 Search");
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
                Document document = crawlerConnection(searchUrl)
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
        boolean phoneBrandCandidate = looksLikeConsumerPhoneBrandCandidate(host, combined);
        if (host.isBlank() || !seenHosts.add(host) || isBlockedHost(host) || looksLikeReferenceHost(host)
                || (!phoneBrandCandidate && looksLikeBlockedContent(normalizedUrl, combined, buildSearchExclusionTerms(List.of())))) {
            return;
        }

        candidates.add(new SearchCandidate(title, normalizedUrl, snippet, source));
    }

    private List<WorkflowModels.CustomerLead> inspectCandidates(
            List<SearchCandidate> candidates,
            String industry,
            String market,
            String keywords,
            List<String> exclusionTerms,
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

        int inspectionLimit = Math.min(sortedCandidates.size(), Math.max(searchLimit * 8, 50));
        int maxParallelInspections = normalizePositive(crawlerSettings.maxParallelInspections(), 8, 1);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(maxParallelInspections, Math.max(2, inspectionLimit)));
        CompletionService<InspectedCandidate> completionService = new ExecutorCompletionService<>(executor);
        int submitted = 0;

        try {
            for (int index = 0; index < inspectionLimit; index++) {
                SearchCandidate candidate = sortedCandidates.get(index);
                completionService.submit(() -> new InspectedCandidate(
                        candidate,
                        inspectWebsite(candidate, industry, market, keywords, exclusionTerms, timeoutMs, crawlerSettings)
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
            int beforeFallback = leads.size();
            fillWithFallbackCandidates(
                    sortedCandidates,
                    acceptedHosts,
                    leads,
                    industry,
                    market,
                    keywords,
                    exclusionTerms,
                    searchLimit
            );
            int fallbackAdded = leads.size() - beforeFallback;
            if (fallbackAdded > 0) {
                session.log("Added " + fallbackAdded + " candidate leads from search results for manual review.");
            }
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
            List<String> exclusionTerms,
            int timeoutMs,
            SettingsModels.CrawlerSettings crawlerSettings
    ) {
        try {
            boolean searchResultMatched = passesSearchResultFilters(candidate, industry, market, keywords, exclusionTerms);
            String landingPageUrl = candidate.url();
            Document document = fetchDocument(landingPageUrl, timeoutMs);
            if (document == null) {
                return null;
            }

            String finalUrl = document.location().isBlank() ? landingPageUrl : document.location();
            String host = normalizeHost(hostOf(finalUrl));
            String title = cleanText(document.title());
            String text = cleanText(document.text());
            String websiteCombined = String.join(" ", title, text).toLowerCase(Locale.ROOT);
            String combined = String.join(" ", websiteCombined, candidate.title(), candidate.snippet()).toLowerCase(Locale.ROOT);
            if (host.isBlank() || isBlockedHost(host) || looksLikeReferenceHost(host)) {
                return null;
            }

            boolean landingPageMatched = passesWebsiteFilters(host, finalUrl, websiteCombined, combined, industry, market, keywords, exclusionTerms);
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
                        websiteCombined = String.join(" ", title, text).toLowerCase(Locale.ROOT);
                        combined = String.join(" ", websiteCombined, candidate.title(), candidate.snippet()).toLowerCase(Locale.ROOT);
                        if (host.isBlank() || isBlockedHost(host) || looksLikeReferenceHost(host)) {
                            return null;
                        }
                    }
                }
            }

            boolean websiteMatched = passesWebsiteFilters(host, finalUrl, websiteCombined, combined, industry, market, keywords, exclusionTerms);
            if (!searchResultMatched && !websiteMatched) {
                return null;
            }

            String homepageUrl = rootUrlOf(finalUrl);
            Set<String> emails = new LinkedHashSet<>(extractEmails(document.html()));
            List<String> visitedEmailPages = new ArrayList<>();
            visitedEmailPages.add(finalUrl);
            List<String> contactPageUrls = new ArrayList<>();
            Document contactDocument = null;

            if (!"HOME_ONLY".equalsIgnoreCase(crawlerSettings.emailExtractionDepth())) {
                contactPageUrls.addAll(findContactPages(homepageUrl, document, MAX_CONTACT_PAGES_PER_SITE));
                contactPageUrls.addAll(probeContactPages(homepageUrl, timeoutMs, MAX_CONTACT_PAGES_PER_SITE - contactPageUrls.size()));
                for (String contactPageUrl : contactPageUrls) {
                    if (contactPageUrl.isBlank() || visitedEmailPages.stream().anyMatch(visited -> sameHostAndPath(visited, contactPageUrl))) {
                        continue;
                    }
                    visitedEmailPages.add(contactPageUrl);
                    Document fetchedContactDocument = fetchDocument(contactPageUrl, timeoutMs);
                    if (fetchedContactDocument != null && contactDocument == null) {
                        contactDocument = fetchedContactDocument;
                    }
                    if (fetchedContactDocument != null) {
                        emails.addAll(extractEmails(fetchedContactDocument.html()));
                    }
                }

                if (emails.isEmpty() && "DEEP".equalsIgnoreCase(crawlerSettings.emailExtractionDepth())) {
                    for (String pageUrl : findDeepEmailPages(homepageUrl, document, visitedEmailPages, MAX_DEEP_EMAIL_PAGES_PER_SITE)) {
                        if (visitedEmailPages.stream().anyMatch(visited -> sameHostAndPath(visited, pageUrl))) {
                            continue;
                        }
                        visitedEmailPages.add(pageUrl);
                        Document extraDocument = fetchDocument(pageUrl, timeoutMs);
                        if (extraDocument != null) {
                            emails.addAll(extractEmails(extraDocument.html()));
                        }
                        if (!emails.isEmpty()) {
                            break;
                        }
                    }
                }

                if (emails.isEmpty()) {
                    String siteTextEmail = extractObfuscatedEmail(document.text());
                    if (!siteTextEmail.isBlank()) {
                        emails.add(siteTextEmail);
                    }
                    if (contactDocument != null) {
                        String contactTextEmail = extractObfuscatedEmail(contactDocument.text());
                        if (!contactTextEmail.isBlank()) {
                            emails.add(contactTextEmail);
                        }
                    }
                }
            }

            String email = chooseBestEmail(emails, host);
            String companyName = extractCompanyName(document, candidate, host);
            String contactName = extractContactName(document, contactDocument, email);

            List<String> notes = new ArrayList<>();
            if (!contactPageUrls.isEmpty()) {
                notes.add("contact pages: " + Math.min(contactPageUrls.size(), visitedEmailPages.size()));
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
                    searchEngineChannel(candidate.source()),
                    notes.isEmpty() ? "company website" : String.join("; ", notes)
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean passesSearchResultFilters(SearchCandidate candidate, String industry, String market, String keywords, List<String> exclusionTerms) {
        String candidateUrl = cleanText(candidate.url());
        String host = normalizeHost(hostOf(candidateUrl));
        String combined = (cleanText(candidate.title()) + " " + cleanText(candidate.snippet())).toLowerCase(Locale.ROOT);
        if (host.isBlank() || isBlockedHost(host)) {
            return false;
        }
        if (isConsumerPhoneBrandIndustry("", industry, keywords) && looksLikeConsumerPhoneBrandCandidate(host, combined)) {
            return true;
        }
        if (looksLikeBlockedContent(candidateUrl, combined, exclusionTerms)) {
            return false;
        }
        if (!looksLikeCompanyCandidate(host, combined)) {
            return false;
        }
        if (clearlyConflictsWithMarket(host, combined, market)) {
            return false;
        }
        if (requiresSearchResultMarketMatch(market) && !matchesMarketSignal(host, combined, market)) {
            return false;
        }
        return matchesKeywords(industry, keywords, combined);
    }

    private boolean hasChinaBusinessIdentity(String host, String websiteText) {
        String normalizedHost = normalizeHost(host);
        String lower = cleanText(websiteText).toLowerCase(Locale.ROOT);
        if (hasMarketTopLevelDomain(normalizedHost, "China") && (
                CHINA_LEGAL_HINTS.stream().anyMatch(lower::contains)
                        || CHINA_LOCATION_HINTS.stream().anyMatch(lower::contains)
                        || CHINA_PHONE_PATTERN.matcher(lower).find()
                        || lower.contains("\u7ca4icp")
                        || lower.contains("\u4eacicp")
                        || lower.contains("\u6caaicp")
                        || lower.contains("icp\u5907")
        )) {
            return true;
        }
        return CHINA_LEGAL_HINTS.stream().anyMatch(lower::contains)
                || CHINA_PHONE_PATTERN.matcher(lower).find();
    }

    private boolean passesWebsiteFilters(String host, String finalUrl, String websiteCombined, String combined, String industry, String market, String keywords, List<String> exclusionTerms) {
        if (host.isBlank() || isBlockedHost(host)) {
            return false;
        }
        if (isConsumerPhoneBrandIndustry("", industry, keywords) && looksLikeConsumerPhoneBrandCandidate(host, combined)) {
            return true;
        }
        if (looksLikeBlockedWebsiteContent(finalUrl, combined, exclusionTerms)) {
            return false;
        }
        if (!looksLikeWebsiteCompanyCandidate(host, combined)) {
            return false;
        }
        if (clearlyConflictsWithMarket(host, combined, market)) {
            return false;
        }
        if (!matchesKeywords(industry, keywords, combined)) {
            return false;
        }
        if (requiresWebsiteMarketMatch(market)) {
            if (!matchesMarketSignal(host, websiteCombined, market)) {
                return false;
            }
            if (!hasChinaBusinessIdentity(host, websiteCombined)) {
                return false;
            }
        }
        return true;
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
            List<String> exclusionTerms,
            int searchLimit
    ) {
        for (SearchCandidate candidate : sortedCandidates) {
            if (leads.size() >= searchLimit) {
                break;
            }

            String host = normalizeHost(hostOf(candidate.url()));
            String combined = (cleanText(candidate.title()) + " " + cleanText(candidate.snippet())).toLowerCase(Locale.ROOT);
            if (host.isBlank() || acceptedHosts.contains(host) || !passesFallbackCandidateFilters(candidate, industry, market, keywords, exclusionTerms)) {
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
                    searchEngineChannel(candidate.source()),
                    "candidate website; manual review suggested"
            ));
        }
    }

    private boolean passesFallbackCandidateFilters(
            SearchCandidate candidate,
            String industry,
            String market,
            String keywords,
            List<String> exclusionTerms
    ) {
        String candidateUrl = cleanText(candidate.url());
        String host = normalizeHost(hostOf(candidateUrl));
        String combined = (cleanText(candidate.title()) + " " + cleanText(candidate.snippet())).toLowerCase(Locale.ROOT);
        if (host.isBlank() || isBlockedHost(host)) {
            return false;
        }
        if (isConsumerPhoneBrandIndustry("", industry, keywords) && looksLikeConsumerPhoneBrandCandidate(host, combined)) {
            return true;
        }
        if (looksLikeBlockedContent(candidateUrl, combined, exclusionTerms)) {
            return false;
        }
        if (!looksLikeCompanyCandidate(host, combined)) {
            return looksLikeRelaxedChinaFallbackCandidate(host, combined, industry, market);
        }
        if (clearlyConflictsWithMarket(host, combined, market)) {
            return false;
        }
        if (matchesKeywords(industry, keywords, combined)
                && (matchesMarketSignal(host, combined, market) || allowsForeignMarketWithoutExplicitSignal(host, combined, market))) {
            return true;
        }
        return looksLikeRelaxedChinaFallbackCandidate(host, combined, industry, market);
    }

    private boolean requiresSearchResultMarketMatch(String market) {
        return "China".equalsIgnoreCase(market);
    }

    private boolean allowsForeignMarketWithoutExplicitSignal(String host, String combined, String market) {
        return market != null
                && !market.isBlank()
                && !"ALL".equalsIgnoreCase(market)
                && !"China".equalsIgnoreCase(market)
                && !clearlyConflictsWithMarket(host, combined, market);
    }

    private boolean looksLikeRelaxedChinaFallbackCandidate(String host, String combined, String industry, String market) {
        if (!"China".equalsIgnoreCase(market) || clearlyConflictsWithMarket(host, combined, market)) {
            return false;
        }
        boolean chinaSignal = matchesMarketSignal(host, combined, market) || containsChineseScript(combined);
        boolean entitySignal = combined.contains("\u516c\u53f8")
                || combined.contains("\u6709\u9650")
                || combined.contains("\u5de5\u5382")
                || combined.contains("\u751f\u4ea7\u5382")
                || combined.contains("co., ltd")
                || combined.contains("co ltd")
                || combined.contains("limited");
        boolean companySignal = entitySignal
                || (looksLikeCompanyCandidate(host, combined) && !looksLikeReferenceHost(host));
        boolean notGenericPortal = !List.of(
                "\u4e94\u91d1\u7f51", "\u673a\u68b0\u7f51", "\u6c7d\u8f66\u4e4b\u5bb6", "\u95e8\u6237", "\u884c\u4e1a\u7f51",
                "portal", "directory", "marketplace"
        ).stream().anyMatch(combined::contains);
        boolean industrySignal = matchesKeywords(industry, "", combined) || containsRelaxedIndustrySignal(industry, combined);
        return chinaSignal && companySignal && industrySignal && notGenericPortal;
    }

    private boolean containsRelaxedIndustrySignal(String industry, String combined) {
        String normalizedIndustry = normalizeSearchPhrase(industry);
        if (normalizedIndustry.isBlank()) {
            return true;
        }
        if (normalizedIndustry.contains("industrial components")
                || normalizedIndustry.contains("industrial parts")
                || normalizedIndustry.contains("mechanical parts")
                || normalizedIndustry.contains("\u5de5\u4e1a\u96f6\u90e8\u4ef6")
                || normalizedIndustry.contains("\u96f6\u90e8\u4ef6")
                || normalizedIndustry.contains("\u96f6\u4ef6")
                || normalizedIndustry.contains("\u914d\u4ef6")
                || normalizedIndustry.contains("\u90e8\u4ef6")) {
            return List.of(
                    "\u5de5\u4e1a\u96f6\u90e8\u4ef6", "\u96f6\u90e8\u4ef6", "\u96f6\u4ef6", "\u914d\u4ef6",
                    "\u90e8\u4ef6", "\u7cbe\u5bc6", "\u52a0\u5de5", "\u4e94\u91d1", "\u6c7d\u8f66",
                    "\u673a\u68b0", "\u673a\u52a0\u5de5", "industrial parts", "industrial components",
                    "mechanical parts", "components", "parts", "machining"
            ).stream().anyMatch(combined::contains);
        }
        return false;
    }

    boolean fallbackCandidateAllowedForTest(String title, String url, String snippet, String industry, String market, String keywords) {
        return passesFallbackCandidateFilters(
                new SearchCandidate(title, url, snippet, "Test"),
                industry,
                market,
                keywords,
                buildSearchExclusionTerms(List.of())
        );
    }

    String searchEngineChannel(String source) {
        String raw = cleanText(source);
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("google")) {
            return "Google";
        }
        if (lower.contains("bing")) {
            return "Bing";
        }
        if (lower.contains("baidu")) {
            return "Baidu";
        }
        if (lower.contains("duckduckgo")) {
            return "DuckDuckGo";
        }
        if (lower.contains("brave")) {
            return "Brave";
        }
        if (lower.contains("360") || lower.contains("so.com")) {
            return "360 Search";
        }
        return raw.isBlank() ? "Search engine" : raw;
    }

    private Document fetchDocument(String url, int timeoutMs) throws IOException {
        log.info("Crawler fetch: {}", url);
        IOException lastException = null;
        for (String candidateUrl : websiteUrlVariants(url)) {
            try {
                Connection.Response response = fetchWebsiteResponse(candidateUrl, timeoutMs, crawlerProxy != null);
                if (response.statusCode() < 400) {
                    return response.parse();
                }
                if (response.statusCode() == 403 || response.statusCode() >= 500) {
                    continue;
                }
                return null;
            } catch (IOException exception) {
                lastException = exception;
            }
        }
        if (lastException != null) {
            log.info("Crawler fetch failed after URL variants: {} ({})", url, lastException.getMessage());
        }
        return null;
    }

    private Connection.Response fetchWebsiteResponse(String url, int timeoutMs, boolean allowDirectFallback) throws IOException {
        try {
            Connection.Response response = crawlerConnection(url)
                    .timeout(timeoutMs)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();
            if (allowDirectFallback && (response.statusCode() == 403 || response.statusCode() >= 500)) {
                log.info("Crawler direct retry after {} from proxy: {}", response.statusCode(), url);
                return crawlerConnection(url, false)
                        .timeout(timeoutMs)
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .execute();
            }
            return response;
        } catch (IOException exception) {
            if (!allowDirectFallback) {
                throw exception;
            }
            log.info("Crawler direct retry after proxy error: {} ({})", url, exception.getMessage());
            return crawlerConnection(url, false)
                    .timeout(timeoutMs)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();
        }
    }

    private List<String> websiteUrlVariants(String url) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        String cleaned = cleanText(url);
        if (cleaned.isBlank()) {
            return List.of();
        }
        variants.add(cleaned);
        try {
            URI uri = URI.create(cleaned);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || host.isBlank()) {
                return new ArrayList<>(variants);
            }
            addUrlVariant(variants, uri, alternateScheme(scheme), host);
            if (host.startsWith("www.")) {
                String bareHost = host.substring(4);
                addUrlVariant(variants, uri, scheme, bareHost);
                addUrlVariant(variants, uri, alternateScheme(scheme), bareHost);
            } else {
                String wwwHost = "www." + host;
                addUrlVariant(variants, uri, scheme, wwwHost);
                addUrlVariant(variants, uri, alternateScheme(scheme), wwwHost);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return new ArrayList<>(variants);
    }

    private void addUrlVariant(Set<String> variants, URI original, String scheme, String host) {
        if (scheme == null || scheme.isBlank() || host == null || host.isBlank()) {
            return;
        }
        String path = original.getRawPath() == null || original.getRawPath().isBlank() ? "/" : original.getRawPath();
        String query = original.getRawQuery() == null ? "" : "?" + original.getRawQuery();
        variants.add(scheme + "://" + host + path + query);
    }

    private String alternateScheme(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return "http";
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return "https";
        }
        return scheme;
    }

    List<String> extractEmails(String html) {
        Set<String> emails = new LinkedHashSet<>();
        String rawHtml = html == null ? "" : html;
        String normalizedHtml = normalizeEmailText(rawHtml);
        Matcher matcher = EMAIL_PATTERN.matcher(normalizedHtml);
        while (matcher.find()) {
            String email = cleanText(matcher.group()).toLowerCase(Locale.ROOT);
            if (isUsefulEmail(email)) {
                emails.add(email);
            }
        }

        Matcher mailtoMatcher = Pattern.compile("mailto:([^\"'?#\\s>]+)", Pattern.CASE_INSENSITIVE).matcher(normalizedHtml);
        while (mailtoMatcher.find()) {
            String email = cleanText(mailtoMatcher.group(1)).toLowerCase(Locale.ROOT);
            if (isUsefulEmail(email)) {
                emails.add(email);
            }
        }
        return new ArrayList<>(emails);
    }

    private boolean isUsefulEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String lower = email.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\.(png|jpg|jpeg|gif|webp|svg|css|js|ico)$")) {
            return false;
        }
        String local = lower.substring(0, lower.indexOf('@'));
        if (local.matches("\\d+")) {
            return false;
        }
        return true;
    }

    private String extractObfuscatedEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(normalizeEmailText(text));
        while (matcher.find()) {
            String email = cleanText(matcher.group()).toLowerCase(Locale.ROOT);
            if (isUsefulEmail(email)) {
                return email;
            }
        }
        return "";
    }

    private String normalizeEmailText(String value) {
        String normalized = Parser.unescapeEntities(value == null ? "" : value, true)
                .replace('\uff20', '@')
                .replace('\uff0e', '.')
                .replace("(at)", "@")
                .replace("[at]", "@")
                .replace("{at}", "@")
                .replace("(dot)", ".")
                .replace("[dot]", ".")
                .replace("{dot}", ".")
                .replace("\u3010at\u3011", "@")
                .replace("\u3010dot\u3011", ".");
        normalized = normalized.replaceAll("(?i)([A-Z0-9._%+-]+)\\s*(?:\\[|\\(|\\{)\\s*at\\s*(?:\\]|\\)|\\})\\s*([A-Z0-9.-]+)", "$1@$2");
        normalized = normalized.replaceAll("(?i)([A-Z0-9._%+-]+)\\s*(?:\\[|\\(|\\{)\\s*dot\\s*(?:\\]|\\)|\\})\\s*([A-Z0-9.-]+)", "$1.$2");
        return normalized;
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

    List<String> findContactPages(String baseUrl, Document document, int limit) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (document == null || limit <= 0) {
            return List.of();
        }
        for (Element link : document.select("a[href]")) {
            String href = cleanText(link.attr("abs:href"));
            String text = cleanText(link.text()).toLowerCase(Locale.ROOT);
            if (href.isBlank() || !sameHost(baseUrl, href)) {
                continue;
            }
            if (CONTACT_PATH_HINTS.stream().anyMatch(href.toLowerCase(Locale.ROOT)::contains)
                    || CONTACT_TEXT_HINTS.stream().anyMatch(text::contains)) {
                urls.add(href);
                if (urls.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<>(urls);
    }

    private List<String> probeContactPages(String baseUrl, int timeoutMs, int limit) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (limit <= 0) {
            return List.of();
        }
        for (String path : CONTACT_PAGE_FALLBACKS) {
            String candidate = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
            try {
                Document document = fetchDocument(candidate, timeoutMs);
                if (document != null) {
                    urls.add(candidate);
                    if (urls.size() >= limit) {
                        break;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return new ArrayList<>(urls);
    }

    private List<String> findDeepEmailPages(String baseUrl, Document document, List<String> alreadyVisited, int limit) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (document == null || limit <= 0) {
            return List.of();
        }
        for (Element link : document.select("a[href]")) {
            String href = cleanText(link.attr("abs:href"));
            if (href.isBlank() || !sameHost(baseUrl, href) || alreadyVisited.stream().anyMatch(visited -> sameHostAndPath(visited, href))) {
                continue;
            }
            String lowerHref = href.toLowerCase(Locale.ROOT);
            String text = cleanText(link.text()).toLowerCase(Locale.ROOT);
            boolean useful = CONTACT_PATH_HINTS.stream().anyMatch(lowerHref::contains)
                    || CONTACT_TEXT_HINTS.stream().anyMatch(text::contains)
                    || lowerHref.contains("/about")
                    || lowerHref.contains("/company")
                    || lowerHref.contains("/service")
                    || lowerHref.contains("/product")
                    || text.contains("\u4ea7\u54c1")
                    || text.contains("\u670d\u52a1")
                    || text.contains("\u516c\u53f8");
            if (!useful) {
                continue;
            }
            urls.add(href);
            if (urls.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(urls);
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
        String companyFromMetadata = extractCompanyNameFromText(String.join(
                " ",
                metaContent(document, "meta[name=description]"),
                document.select("h1, .logo, .company, .company-name").text()
        ));
        if (!companyFromMetadata.isBlank()) {
            return companyFromMetadata;
        }
        String companyFromTitle = extractCompanyNameFromText(document.title());
        if (!companyFromTitle.isBlank()) {
            return companyFromTitle;
        }
        String title = simplifyTitle(document.title());
        if (!title.isBlank() && isLikelyCompanyName(title)) {
            return cleanCompanyName(title);
        }
        String companyFromCandidate = extractCompanyNameFromText(candidate.title());
        if (!companyFromCandidate.isBlank()) {
            return companyFromCandidate;
        }
        String candidateTitle = simplifyTitle(candidate.title());
        if (!candidateTitle.isBlank() && isLikelyCompanyName(candidateTitle)) {
            return cleanCompanyName(candidateTitle);
        }
        return host;
    }

    private String extractCompanyNameFromText(String text) {
        String cleaned = cleanText(text);
        if (cleaned.isBlank()) {
            return "";
        }
        Matcher exactMatcher = CHINESE_COMPANY_SUFFIX_PATTERN.matcher(cleaned);
        if (exactMatcher.find()) {
            return cleanCompanyName(exactMatcher.group(1));
        }
        Matcher inlineMatcher = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9（）()]{2,50}(?:有限公司|股份有限公司|集团公司|集团有限公司|控股有限公司|科技有限公司))").matcher(cleaned);
        if (inlineMatcher.find()) {
            return cleanCompanyName(inlineMatcher.group(1));
        }
        return "";
    }

    private boolean isLikelyCompanyName(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.toLowerCase(java.util.Locale.ROOT).trim();
        if (JUNK_PAGE_TITLES.contains(lower)) return false;
        if (lower.contains(",") || lower.contains("，")) return false;
        if (List.of("机", "设备", "配件", "零件", "产品", "价格", "厂家", "供应商", "评测", "手机").stream().anyMatch(lower::contains)
                && STRONG_COMPANY_HINTS.stream().noneMatch(lower::contains)) {
            return false;
        }
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
                    || matchesIndustryFamily(industry, lower)
                    || industryHints.stream().anyMatch(hint -> containsLooseKeywordToken(lower, hint))
                    || isBroadIndustry(industry);
        }

        boolean keywordMatched = keywordHints.stream().anyMatch(hint -> containsMeaningfulPhrase(lower, hint));
        boolean looseKeywordMatched = keywordHints.stream().anyMatch(hint -> containsLooseKeywordToken(lower, hint));
        boolean strongKeywordMatched = keywordHints.stream().anyMatch(hint -> isStrongBusinessHint(hint) && containsMeaningfulPhrase(lower, hint));
        boolean productIntentMatched = keywordHints.stream().anyMatch(hint -> isProductIntentHint(hint) && containsMeaningfulPhrase(lower, hint));

        if (strongKeywordMatched || productIntentMatched) {
            return true;
        }

        if (keywordMatched && (industryMatched || isBroadIndustry(industry))) {
            return true;
        }

        return (industryMatched || industryHints.stream().anyMatch(hint -> containsLooseKeywordToken(lower, hint)))
                && (keywordMatched || looseKeywordMatched);
    }

    private boolean matchesIndustryFamily(String industry, String lowerText) {
        String normalized = normalizeSearchPhrase(industry);
        if (normalized.isBlank()) {
            return true;
        }
        if (normalized.contains("industrial automation") || normalized.contains("\u5de5\u4e1a\u81ea\u52a8\u5316")) {
            return List.of(
                    "automation", "automated", "plc", "servo", "motion control", "control system",
                    "robot", "robotics", "industrial control", "factory automation", "machine vision",
                    "\u81ea\u52a8\u5316", "\u5de5\u63a7", "\u63a7\u5236\u7cfb\u7edf", "\u4f3a\u670d", "\u9a71\u52a8\u5668",
                    "\u673a\u5668\u4eba", "\u673a\u5668\u89c6\u89c9", "\u667a\u80fd\u5236\u9020", "\u5de5\u4e1a\u63a7\u5236"
            ).stream().anyMatch(lowerText::contains);
        }
        if (normalized.contains("medical device") || normalized.contains("\u533b\u7597\u5668\u68b0") || normalized.contains("\u533b\u7597\u8bbe\u5907")) {
            return List.of("medical", "device", "healthcare", "\u533b\u7597", "\u533b\u7528", "\u5668\u68b0", "\u8bbe\u5907").stream().anyMatch(lowerText::contains);
        }
        if (normalized.contains("electronics manufacturing") || normalized.contains("\u7535\u5b50\u5236\u9020")) {
            return List.of("electronics", "electronic", "pcb", "pcba", "assembly", "\u7535\u5b50", "\u7ebf\u8def\u677f", "\u7535\u8def\u677f", "\u7ec4\u88c5").stream().anyMatch(lowerText::contains);
        }
        return false;
    }

    private boolean isStrongBusinessHint(String hint) {
        String normalized = normalizeSearchPhrase(hint);
        if (normalized.length() < 2) {
            return false;
        }
        return normalized.contains("machine tool")
                || normalized.contains("cnc")
                || normalized.contains("machining")
                || normalized.contains("automation equipment")
                || normalized.contains("industrial automation")
                || normalized.contains("electronics")
                || normalized.contains("electronic")
                || normalized.contains("medical device")
                || normalized.contains("medical equipment")
                || normalized.contains("medtech")
                || normalized.contains("pcb")
                || normalized.contains("pcba")
                || normalized.contains("circuit board")
                || normalized.contains("contract manufacturing")
                || normalized.contains("oem")
                || normalized.contains("manufacturer")
                || normalized.contains("factory")
                || normalized.contains("机床")
                || normalized.contains("数控")
                || normalized.contains("加工")
                || normalized.contains("加工中心")
                || normalized.contains("钻攻中心")
                || normalized.contains("精雕机")
                || normalized.contains("3c")
                || normalized.contains("自动化设备")
                || normalized.contains("自动化")
                || normalized.contains("设备")
                || normalized.contains("厂家")
                || normalized.contains("工厂");
    }

    private boolean isProductIntentHint(String hint) {
        String normalized = normalizeSearchPhrase(hint);
        if (normalized.length() < 2) {
            return false;
        }
        return normalized.contains("mobile phone accessories")
                || normalized.contains("smartphone accessories")
                || normalized.contains("phone accessories")
                || normalized.contains("cell phone accessories")
                || normalized.contains("phone case")
                || normalized.contains("data cable")
                || normalized.contains("usb-c cable")
                || normalized.contains("phone charger")
                || normalized.contains("mobile phone charger")
                || normalized.contains("wireless earbuds")
                || normalized.contains("earphone")
                || normalized.contains("earbuds")
                || normalized.contains("手机配件")
                || normalized.contains("手机壳")
                || normalized.contains("手机数据线")
                || normalized.contains("数据线")
                || normalized.contains("手机充电器")
                || normalized.contains("充电器")
                || normalized.contains("蓝牙耳机")
                || normalized.contains("耳机");
    }

    boolean matchesMarketSignal(String host, String text, String market) {
        if ("ALL".equalsIgnoreCase(market)) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if ("China".equalsIgnoreCase(market)) {
            return hasChinaMarketSignal(host, lower);
        }
        return host.endsWith(marketSite(market).replace("site:.", "."))
                || lower.contains(market.toLowerCase(Locale.ROOT))
                || lower.contains(marketAlias(market));
    }

    private boolean requiresWebsiteMarketMatch(String market) {
        return "China".equalsIgnoreCase(market);
    }

    private boolean hasChinaMarketSignal(String host, String lowerText) {
        String normalizedHost = normalizeHost(host);
        if (hasMarketTopLevelDomain(normalizedHost, "China")) {
            return true;
        }
        if (CHINA_PHONE_PATTERN.matcher(lowerText).find()) {
            return true;
        }
        if (CHINA_LEGAL_HINTS.stream().anyMatch(lowerText::contains)) {
            return true;
        }
        return CHINA_LOCATION_HINTS.stream().anyMatch(lowerText::contains);
    }

    boolean clearlyConflictsWithMarket(String host, String text, String market) {
        if (market == null || market.isBlank() || "ALL".equalsIgnoreCase(market)) {
            return false;
        }
        String normalizedHost = normalizeHost(host);
        if (normalizedHost.isBlank()) {
            return false;
        }

        if (hasMarketTopLevelDomain(normalizedHost, market)) {
            return false;
        }
        for (String conflictingTld : conflictingMarketTopLevelDomains(market)) {
            if (normalizedHost.endsWith(conflictingTld)) {
                return true;
            }
        }
        if ("China".equalsIgnoreCase(market) && clearlyForeignForChina(normalizedHost, text)) {
            return true;
        }
        return false;
    }

    private boolean clearlyForeignForChina(String host, String text) {
        String lower = cleanText(text).toLowerCase(Locale.ROOT);
        if (FOREIGN_ENTITY_HINTS_FOR_CHINA.stream().anyMatch(lower::contains)) {
            return true;
        }
        if (FOREIGN_LOCATION_HINTS_FOR_CHINA.stream().anyMatch(lower::contains) && !hasChinaMarketSignal(host, lower)) {
            return true;
        }
        return false;
    }

    private boolean hasMarketTopLevelDomain(String host, String market) {
        return marketTopLevelDomains(market).stream().anyMatch(host::endsWith);
    }

    private List<String> marketTopLevelDomains(String market) {
        return switch (market) {
            case "China" -> List.of(".com.cn", ".cn");
            case "USA" -> List.of(".us");
            case "Germany" -> List.of(".de");
            default -> List.of();
        };
    }

    private List<String> conflictingMarketTopLevelDomains(String market) {
        return switch (market) {
            case "China" -> List.of(
                    ".de", ".us", ".in", ".co.in", ".org.in", ".net.in", ".jp", ".co.jp", ".kr", ".co.kr",
                    ".sg", ".com.sg", ".my", ".com.my", ".vn", ".co.th", ".id", ".co.id", ".ph",
                    ".tw", ".com.tw", ".hk", ".com.hk", ".uk", ".co.uk", ".fr", ".it", ".es", ".nl",
                    ".pl", ".tr", ".br", ".com.br", ".mx", ".ca", ".com.au", ".au"
            );
            case "USA" -> List.of(".com.cn", ".cn", ".de");
            case "Germany" -> List.of(".com.cn", ".cn", ".us");
            default -> List.of();
        };
    }

    private boolean looksLikeBlockedContent(String url, String combined, List<String> exclusionTerms) {
        String lowerUrl = cleanText(url).toLowerCase(Locale.ROOT);
        String lowerCombined = cleanText(combined).toLowerCase(Locale.ROOT);
        return looksLikeReferenceHost(hostOf(url))
                || EDITORIAL_URL_PATTERNS.stream().anyMatch(lowerUrl::contains)
                || looksLikeLeadDatabaseContent(lowerUrl, lowerCombined)
                || NON_COMPANY_TEXT_HINTS.stream().anyMatch(lowerCombined::contains)
                || (exclusionTerms != null && exclusionTerms.stream()
                .map(term -> term == null ? "" : term.toLowerCase(Locale.ROOT).trim())
                .filter(term -> term.length() >= 2)
                .anyMatch(term -> lowerUrl.contains(term) || lowerCombined.contains(term)));
    }

    boolean looksLikeBlockedWebsiteContent(String url, String combined, List<String> exclusionTerms) {
        String lowerUrl = cleanText(url).toLowerCase(Locale.ROOT);
        String lowerCombined = cleanText(combined).toLowerCase(Locale.ROOT);
        return looksLikeReferenceHost(hostOf(url))
                || EDITORIAL_URL_PATTERNS.stream().anyMatch(lowerUrl::contains)
                || looksLikeLeadDatabaseContent(lowerUrl, lowerCombined)
                || (exclusionTerms != null && exclusionTerms.stream()
                .map(term -> term == null ? "" : term.toLowerCase(Locale.ROOT).trim())
                .filter(term -> term.length() >= 2)
                .anyMatch(term -> lowerUrl.contains(term) || lowerCombined.contains(term)));
    }

    private boolean looksLikeLeadDatabaseContent(String lowerUrl, String lowerCombined) {
        if (LEAD_DATABASE_URL_HINTS.stream().anyMatch(lowerUrl::contains)
                || LEAD_DATABASE_TEXT_HINTS.stream().anyMatch(lowerCombined::contains)) {
            return true;
        }
        boolean databasePage = lowerCombined.contains("database")
                && List.of("companies", "contacts", "businesses", "suppliers", "email").stream().anyMatch(lowerCombined::contains);
        boolean listPage = lowerCombined.contains("list")
                && lowerCombined.contains("companies")
                && List.of("email", "contact", "sales", "database", "directory").stream().anyMatch(lowerCombined::contains);
        return databasePage || listPage;
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

    private boolean looksLikeWebsiteCompanyCandidate(String host, String combined) {
        if (host.isBlank() || looksLikeReferenceHost(host)) {
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
        if (lowerHost.endsWith(".com") && (
                lowerCombined.contains("contact")
                        || lowerCombined.contains("about us")
                        || lowerCombined.contains("products")
                        || lowerCombined.contains("solutions")
                        || lowerCombined.contains("manufacturing")
                        || lowerCombined.contains("manufacturer")
                        || lowerCombined.contains("supplier")
                        || lowerCombined.contains("oem")
                        || lowerCombined.contains("contract manufacturer")
        )) {
            return true;
        }
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

    boolean looksLikeReferenceHost(String host) {
        String normalizedHost = normalizeHost(host);
        if (normalizedHost.isBlank()) {
            return false;
        }
        return REFERENCE_HOST_HINTS.stream().anyMatch(normalizedHost::contains)
                || normalizedHost.endsWith(".gov")
                || normalizedHost.contains(".gov.")
                || normalizedHost.endsWith(".edu")
                || normalizedHost.contains(".edu.")
                || normalizedHost.endsWith(".org")
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

    String normalizeMarket(String market) {
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

    private String marketSearchName(String market) {
        return switch (market) {
            case "China" -> "China";
            case "USA" -> "United States";
            case "Germany" -> "Germany";
            default -> market;
        };
    }

    private String marketAdjective(String market) {
        return switch (market) {
            case "China" -> "Chinese";
            case "USA" -> "American";
            case "Germany" -> "German";
            default -> marketAlias(market);
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
        if (normalized.contains("\u5305\u88c5\u673a\u68b0") || normalized.contains("\u5305\u88c5\u8bbe\u5907") || normalized.contains("\u5305\u88c5\u673a")) {
            return "packaging machinery";
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
            return "medical device";
        }
        if (normalized.contains("\u533b\u7597\u8bbe\u5907")) {
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

    private boolean sameHostAndPath(String leftUrl, String rightUrl) {
        try {
            URI left = URI.create(leftUrl);
            URI right = URI.create(rightUrl);
            String leftHost = normalizeHost(left.getHost());
            String rightHost = normalizeHost(right.getHost());
            String leftPath = cleanPath(left.getPath());
            String rightPath = cleanPath(right.getPath());
            return !leftHost.isBlank() && leftHost.equals(rightHost) && leftPath.equals(rightPath);
        } catch (IllegalArgumentException ignored) {
            return sameHost(leftUrl, rightUrl) && cleanText(leftUrl).equalsIgnoreCase(cleanText(rightUrl));
        }
    }

    private String cleanPath(String path) {
        String cleaned = cleanText(path);
        if (cleaned.isBlank()) {
            return "/";
        }
        while (cleaned.endsWith("/") && cleaned.length() > 1) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
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

    private String resolveSo360Url(String absoluteHref, String rawHref) {
        String candidate = absoluteHref == null || absoluteHref.isBlank() ? rawHref : absoluteHref;
        if (candidate == null || candidate.isBlank()) {
            return "";
        }
        if (!candidate.contains("so.com/link")) {
            return candidate;
        }

        try {
            Document document = crawlerConnection(candidate)
                    .timeout(3000)
                    .followRedirects(true)
                    .get();
            String location = cleanText(document.location());
            if (!location.isBlank() && !location.contains("so.com/link")) {
                return location;
            }
        } catch (IOException ignored) {
            // Keep the wrapped URL if the redirect cannot be resolved quickly.
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
            Connection.Response response = crawlerConnection(url)
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

    List<String> buildSearchHintsForTest(String value) {
        return buildSearchHints(value);
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
        if (normalized.contains("electronics manufacturer") || normalized.contains("electronic manufacturer")) {
            phrases.add("electronics manufacturing");
            phrases.add("electronics manufacturer");
            phrases.add("electronic manufacturing");
            phrases.add("contract manufacturing");
            phrases.add("pcb assembly");
            phrases.add("pcba");
            phrases.add("circuit board");
            phrases.add("oem electronics");
        }
        if (normalized.contains("电子") || normalized.contains("electronics")) {
            phrases.add("electronics");
            phrases.add("电子");
        }
        if (normalized.contains("医疗器械")
                || normalized.contains("医疗设备")
                || normalized.contains("medical device")
                || normalized.contains("medical equipment")
                || normalized.contains("medtech")) {
            phrases.add("medical device");
            phrases.add("medical equipment");
            phrases.add("medtech");
            phrases.add("medical device contract manufacturing");
            phrases.add("医疗器械");
            phrases.add("医疗设备");
        }
        if (normalized.contains("手机") || normalized.contains("smartphone")) {
            phrases.add("smartphone");
            phrases.add("手机");
        }
        if (normalized.contains("手机配件")
                || normalized.contains("mobile phone accessories")
                || normalized.contains("smartphone accessories")
                || normalized.contains("phone accessories")) {
            phrases.add("mobile phone accessories");
            phrases.add("smartphone accessories");
            phrases.add("phone accessories");
            phrases.add("手机配件");
            phrases.add("手机壳");
            phrases.add("数据线");
            phrases.add("充电器");
            phrases.add("耳机");
        }
        if (normalized.contains("手机壳") || normalized.contains("phone case")) {
            phrases.add("phone case");
            phrases.add("手机壳");
        }
        if (normalized.contains("数据线") || normalized.contains("data cable") || normalized.contains("usb-c cable")) {
            phrases.add("data cable");
            phrases.add("usb-c cable");
            phrases.add("数据线");
        }
        if (normalized.contains("手机充电器")
                || normalized.contains("充电器")
                || normalized.contains("phone charger")
                || normalized.contains("mobile phone charger")
                || normalized.contains("usb charger")) {
            phrases.add("phone charger");
            phrases.add("mobile phone charger");
            phrases.add("usb charger");
            phrases.add("手机充电器");
            phrases.add("充电器");
        }
        if (normalized.contains("耳机") || normalized.contains("earphone") || normalized.contains("earbuds")) {
            phrases.add("earphone");
            phrases.add("earbuds");
            phrases.add("耳机");
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
        if (normalized.contains("packaging machinery") || normalized.contains("packaging equipment")) {
            phrases.add("packaging machinery");
            phrases.add("packaging equipment");
            phrases.add("packing machine");
            phrases.add("packaging machine");
            phrases.add("packaging line");
        }
        if (normalized.contains("包装机械设备") || normalized.contains("包装机械") || normalized.contains("包装设备") || normalized.contains("包装机")) {
            phrases.add("包装机械设备");
            phrases.add("包装机械");
            phrases.add("包装设备");
            phrases.add("包装机");
            phrases.add("灌装机");
            phrases.add("封口机");
            phrases.add("packaging machinery");
            phrases.add("packaging equipment");
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

    record SearchSourceMix(
            String engine,
            int queryCount,
            int primaryFetches,
            int autoFallbackFetches,
            int autoFallbackQueryLimit
    ) {
        double primaryShare() {
            int totalFetches = primaryFetches + autoFallbackFetches;
            return totalFetches == 0 ? 0 : (double) primaryFetches / totalFetches;
        }
    }

    private enum DirectSearchEngine {
        AUTO,
        GOOGLE,
        BAIDU,
        BING,
        DUCKDUCKGO,
        BRAVE,
        SO360
    }

    private enum ProxyType {
        HTTP,
        SOCKS
    }

    record ProxyConfig(String host, int port, ProxyType type, String source) {
        boolean enabled() {
            return host != null && !host.isBlank() && port > 0;
        }

        String displayName() {
            if (!enabled()) {
                return "direct";
            }
            return type.name().toLowerCase(Locale.ROOT) + "://" + host + ":" + port + " (" + source + ")";
        }
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
