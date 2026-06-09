package com.zijianxin.website.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowSearchServiceTest {

    private final WorkflowSearchService service = new WorkflowSearchService(null, new ObjectMapper());
    private final SettingsModels.CrawlerSettings crawlerSettings = SettingsModels.CrawlerSettings.defaults();

    @Test
    void prioritySearchSourcesStayAboveHalfForChinaMarket() {
        assertPriorityShareAboveHalf("Google", "China");
        assertPriorityShareAboveHalf("Baidu", "China");
        assertPriorityShareAboveHalf("Bing", "China");
        assertPriorityShareAboveHalf("DuckDuckGo", "China");
        assertPriorityShareAboveHalf("Brave", "China");
        assertPriorityShareAboveHalf("360", "China");
    }

    @Test
    void prioritySearchSourcesStayAboveHalfForNonChinaMarket() {
        assertPriorityShareAboveHalf("Google", "USA");
        assertPriorityShareAboveHalf("Baidu", "USA");
        assertPriorityShareAboveHalf("Bing", "USA");
        assertPriorityShareAboveHalf("DuckDuckGo", "USA");
        assertPriorityShareAboveHalf("Brave", "USA");
        assertPriorityShareAboveHalf("360", "USA");
    }

    @Test
    void autoModeKeepsTheOriginalAutomaticChain() {
        WorkflowSearchService.SearchSourceMix mix = service.planDirectSearchSourceMix("Auto", "China", 12, crawlerSettings);

        assertThat(mix.engine()).isEqualTo("AUTO");
        assertThat(mix.primaryFetches()).isZero();
        assertThat(mix.autoFallbackFetches()).isPositive();
        assertThat(mix.autoFallbackFetches()).isEqualTo(72);
        assertThat(mix.autoFallbackQueryLimit()).isEqualTo(12);
        assertThat(mix.primaryShare()).isZero();
    }

    @Test
    void autoModeIncludesBraveForNonChinaMarkets() {
        WorkflowSearchService.SearchSourceMix mix = service.planDirectSearchSourceMix("Auto", "USA", 3, crawlerSettings);

        assertThat(mix.autoFallbackFetches()).isEqualTo(15);
    }

    @Test
    void countrySelectorValuesAreNormalizedFromUiLabels() {
        assertThat(service.normalizeMarket("全部")).isEqualTo("ALL");
        assertThat(service.normalizeMarket("中国")).isEqualTo("China");
        assertThat(service.normalizeMarket("美国")).isEqualTo("USA");
        assertThat(service.normalizeMarket("德国")).isEqualTo("Germany");
        assertThat(service.normalizeMarket("United States")).isEqualTo("USA");
    }

    @Test
    void usaMarketKeepsDotComCompanySites() {
        assertThat(service.clearlyConflictsWithMarket("example-industrial.com", "", "USA")).isFalse();
        assertThat(service.clearlyConflictsWithMarket("example-industrial.us", "", "USA")).isFalse();
        assertThat(service.clearlyConflictsWithMarket("example-industrial.cn", "", "USA")).isTrue();
        assertThat(service.clearlyConflictsWithMarket("example-industrial.de", "", "USA")).isTrue();
    }

    @Test
    void chinaMarketRejectsObviousForeignCompanySignals() {
        assertThat(service.clearlyConflictsWithMarket(
                "synchroelectronics.com",
                "Synchro Electronics Global Pvt. Ltd. India electronics manufacturing services",
                "China"
        )).isTrue();
        assertThat(service.clearlyConflictsWithMarket(
                "example.co.in",
                "electronics manufacturing company",
                "China"
        )).isTrue();
        assertThat(service.clearlyConflictsWithMarket(
                "enbrightech.com",
                "\u82cf\u5dde\u73af\u660e\u65b0\u6750\u6599\u79d1\u6280\u6709\u9650\u516c\u53f8 electronics materials",
                "China"
        )).isFalse();
        assertThat(service.clearlyConflictsWithMarket(
                "epoch-int.com.cn",
                "Epoch International electronics manufacturer",
                "China"
        )).isFalse();
    }

    @Test
    void chinaMarketRequiresSpecificChinaWebsiteSignals() {
        assertThat(service.matchesMarketSignal(
                "foreign-electronics.com",
                "electronics manufacturer serving China market",
                "China"
        )).isFalse();
        assertThat(service.matchesMarketSignal(
                "foreign-electronics.com",
                "Shenzhen electronics manufacturer contact us",
                "China"
        )).isTrue();
        assertThat(service.matchesMarketSignal(
                "example.cn",
                "electronics manufacturer",
                "China"
        )).isTrue();
    }

    @Test
    void customPhoneIndustryUsesBrandOfficialWebsiteQueries() {
        var queries = service.buildSearchQueries("手机", "China", "", "ALL");

        assertThat(queries)
                .contains("手机品牌 官网")
                .contains("国产手机品牌 官网")
                .contains("手机厂商 官网")
                .contains("华为 手机 官网")
                .contains("小米 手机 官网")
                .contains("OPPO 手机 官网");
    }

    @Test
    void customIndustryQueriesIncludeGenericDiscoveryRoutes() {
        var queries = service.buildSearchQueries("新能源", "China", "", "ALL");

        assertThat(queries)
                .contains("新能源 官网")
                .contains("新能源 官方网站")
                .contains("新能源 公司官网")
                .contains("新能源 供应商 联系方式")
                .contains("site:.cn 新能源 官网")
                .contains("site:.com.cn 新能源 有限公司");
    }

    @Test
    void phoneBrandFallbackAllowsOfficialBrandSites() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "小米手机官网",
                "https://mi.cn/",
                "小米手机官方商城 智能手机 官网",
                "手机",
                "China",
                ""
        )).isTrue();
    }

    @Test
    void fallbackKeepsChineseDotComCompanyCandidates() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "Shenzhen Aster Electronics Co., Ltd - Phone Accessories Manufacturer",
                "https://aster-electronics.com/",
                "Shenzhen electronics manufacturer phone accessories factory contact email",
                "electronics manufacturing",
                "China",
                "phone accessories"
        )).isTrue();
    }

    @Test
    void fallbackRejectsObviousForeignCompanyCandidatesForChina() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "Synchro Electronics Global Pvt. Ltd.",
                "https://synchroelectronics.com/",
                "India electronics manufacturing services company contact",
                "electronics manufacturing",
                "China",
                "electronics manufacturing"
        )).isFalse();
    }

    @Test
    void foreignSearchRejectsLeadDatabaseSites() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "InfoGlobalData",
                "https://www.infoglobaldata.com/medical-device-companies-list/",
                "Medical device companies list with verified contacts and sales leads",
                "medical device",
                "USA",
                "medical device"
        )).isFalse();
        assertThat(service.fallbackCandidateAllowedForTest(
                "CompanyData by BoldData",
                "https://companydata.com/medical-device-company-database/",
                "Company database and email list for marketing",
                "medical device",
                "USA",
                "medical device"
        )).isFalse();
    }

    @Test
    void foreignSearchKeepsRealCompanySites() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "Argon Medical Devices",
                "https://www.argonmedical.com/",
                "Argon Medical Devices USA manufacturer products contact customer service company profile",
                "medical device",
                "USA",
                "medical device"
        )).isTrue();
    }

    @Test
    void foreignSearchKeepsDotComCompanySitesWithoutExplicitCountryText() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "Argon Medical Devices",
                "https://www.argonmedical.com/",
                "Medical device manufacturer products contact customer service company profile",
                "medical device",
                "USA",
                "medical device"
        )).isTrue();
    }

    @Test
    void foreignSearchRejectsObviousChinaSupplierSites() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "JLCPCB PCB manufacturer",
                "https://www.jlc.com/",
                "Shenzhen China PCB prototype manufacturer products contact",
                "electronics manufacturing",
                "USA",
                "pcb"
        )).isFalse();
        assertThat(service.fallbackCandidateAllowedForTest(
                "\u4eac\u4e1c\u5de5\u4e1a",
                "https://www.jingdongindustrials.com/",
                "\u4e2d\u56fd\u5de5\u4e1a\u54c1\u4f9b\u5e94\u94fe industrial products supplier contact",
                "\u5de5\u4e1a\u96f6\u90e8\u4ef6",
                "USA",
                ""
        )).isFalse();
    }

    @Test
    void chinaIndustrialPartsQueriesUseHighYieldNativeTerms() {
        var queries = service.buildSearchQueries("工业零部件", "China", "", "ALL");

        assertThat(queries)
                .contains("工业零部件 厂家 联系方式")
                .contains("机械零部件 厂家 联系方式")
                .contains("精密零部件 加工 有限公司")
                .contains("五金零件 生产厂家 官网")
                .contains("冲压件 加工 有限公司")
                .doesNotContain("汽车零部件 厂家 联系方式");
    }

    @Test
    void chinaFallbackKeepsIndustrialPartsCompaniesFromSearchSnippets() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "昆山精密机械零部件加工有限公司",
                "https://www.example-parts.cn/",
                "机械零部件 精密加工 五金零件 生产厂家 联系方式",
                "工业零部件",
                "China",
                ""
        )).isTrue();
    }

    @Test
    void chinaFallbackRejectsGenericIndustrialPortals() {
        assertThat(service.fallbackCandidateAllowedForTest(
                "全球五金网",
                "https://www.wjw.cn/",
                "五金零件 生产厂家 供应商 行业门户",
                "工业零部件",
                "China",
                ""
        )).isFalse();
        assertThat(service.fallbackCandidateAllowedForTest(
                "汽车之家",
                "https://www.autohome.com.cn/",
                "汽车配件 报价 资讯 门户",
                "工业零部件",
                "China",
                ""
        )).isFalse();
        assertThat(service.fallbackCandidateAllowedForTest(
                "什么是钣金？关于用途、材料、种类和工艺的基本知识",
                "https://info-meviy.misumi.com.cn/article/sheet-metal/",
                "钣金 工艺流程 基本知识",
                "工业零部件",
                "China",
                ""
        )).isFalse();
    }

    @Test
    void publicInstitutionDomainsAreNotCompanyCandidates() {
        assertThat(service.looksLikeReferenceHost("fda.gov")).isTrue();
        assertThat(service.looksLikeReferenceHost("nih.gov")).isTrue();
        assertThat(service.looksLikeReferenceHost("example.edu")).isTrue();
        assertThat(service.looksLikeReferenceHost("chinesewords.org")).isTrue();
        assertThat(service.looksLikeReferenceHost("thomasnet.com")).isTrue();
        assertThat(service.looksLikeReferenceHost("qmed.com")).isTrue();
        assertThat(service.looksLikeReferenceHost("eet-china.com")).isTrue();
        assertThat(service.looksLikeReferenceHost("eeworld.com.cn")).isTrue();
        assertThat(service.looksLikeReferenceHost("www.samsung.com.cn")).isFalse();
        assertThat(service.looksLikeReferenceHost("njruiyi.gys.cn")).isTrue();
        assertThat(service.looksLikeReferenceHost("doc88.com")).isTrue();
        assertThat(service.looksLikeReferenceHost("cklido.diytrade.com")).isTrue();
        assertThat(service.looksLikeReferenceHost("wenda.so.com")).isTrue();
        assertThat(service.looksLikeReferenceHost("tradeindia.com")).isTrue();
        assertThat(service.looksLikeReferenceHost("njruiyi.cn.china.cn")).isTrue();
        assertThat(service.looksLikeReferenceHost("example-medical.com")).isFalse();
    }

    @Test
    void nonChinaMarketQueriesDoNotDependOnlyOnCountryTld() {
        var queries = service.buildSearchQueries("电子制造", "USA", "smartphone accessories", "ALL");

        assertThat(queries)
                .anyMatch(query -> query.startsWith("United States "))
                .anyMatch(query -> query.startsWith("American "))
                .anyMatch(query -> query.startsWith("site:.us "));
        assertThat(queries.get(0)).doesNotStartWith("site:.us ");
    }

    @Test
    void customChineseIndustryBuildsConcreteForeignMarketQueries() {
        var queries = service.buildSearchQueries("\u5de5\u4e1a\u96f6\u90e8\u4ef6", "USA", "", "ALL");

        assertThat(queries)
                .anyMatch(query -> query.contains("United States industrial components"))
                .anyMatch(query -> query.contains("American industrial components"))
                .noneMatch(query -> query.equals("United States manufacturer official website"));
    }

    @Test
    void foreignMarketsInspectLargerCandidatePool() {
        assertThat(service.inspectionLimitForTest(300, 18, "USA")).isEqualTo(216);
        assertThat(service.inspectionLimitForTest(300, 18, "Germany")).isEqualTo(216);
        assertThat(service.inspectionLimitForTest(300, 18, "China")).isEqualTo(144);
    }

    @Test
    void nonChinaMedicalExpansionUsesBusinessSearchTermsFirst() {
        var queries = service.buildIntentExpansionQueriesForTest("美国医疗器械制造商 官网 联系方式", "医疗器械", "USA", "");

        assertThat(queries)
                .isNotEmpty()
                .first()
                .asString()
                .contains("contract manufacturing");
    }

    @Test
    void germanyMedicalExpansionUsesSelectedMarketInsteadOfUsa() {
        var queries = service.buildIntentExpansionQueriesForTest("Germany medical device manufacturer contact", "medical device", "Germany", "");

        assertThat(queries)
                .anyMatch(query -> query.contains("Germany"))
                .noneMatch(query -> query.contains("USA"));
    }

    @Test
    void medicalIndustryHintsIncludeMedicalDeviceSynonyms() {
        assertThat(service.buildSearchHintsForTest("医疗器械"))
                .contains("medical device", "medical equipment", "medical device contract manufacturing");
    }

    @Test
    void chinaMedicalDescriptionUsesBroadMedicalSearchQueries() {
        var queries = service.buildIntentExpansionQueriesForTest("可生产出口海外的高端器械", "医疗器械", "China", "");

        assertThat(queries)
                .contains("site:.cn medical device manufacturer contact")
                .contains("医疗器械 厂家 联系方式");
    }

    @Test
    void phoneAccessoriesDescriptionUsesFactorySearchQueries() {
        var queries = service.buildIntentExpansionQueriesForTest("主要生产手机配件", "电子制造", "China", "");

        assertThat(queries)
                .contains("site:.cn 手机配件 厂家 联系方式")
                .contains("手机配件 生产厂家 官网")
                .contains("phone accessories OEM factory China contact");
    }

    @Test
    void phoneAccessoriesHintsIncludeSpecificProductTerms() {
        assertThat(service.buildSearchHintsForTest("主要生产手机配件"))
                .contains("mobile phone accessories", "phone accessories", "手机配件", "充电器", "数据线");
    }

    @Test
    void searchCandidateSourcesAreShownAsSearchEngines() {
        assertThat(service.searchEngineChannel("Google")).isEqualTo("Google");
        assertThat(service.searchEngineChannel("Bing RSS")).isEqualTo("Bing");
        assertThat(service.searchEngineChannel("Bing HTML")).isEqualTo("Bing");
        assertThat(service.searchEngineChannel("SerpAPI/google")).isEqualTo("Google");
        assertThat(service.searchEngineChannel("360 Search")).isEqualTo("360 Search");
    }

    @Test
    void chineseContactLinksAreDiscoveredForEmailExtraction() {
        var document = Jsoup.parse("""
                <html><body>
                  <a href="/about.html">关于我们</a>
                  <a href="/lxwm.html">联系我们</a>
                </body></html>
                """, "https://example.cn/");

        assertThat(service.findContactPages("https://example.cn/", document, 5))
                .contains("https://example.cn/about.html", "https://example.cn/lxwm.html");
    }

    @Test
    void emailExtractionHandlesCommonObfuscation() {
        assertThat(service.extractEmails("sales(at)example(dot)com info@example.cn"))
                .contains("sales@example.com", "info@example.cn");
        assertThat(service.extractEmails("located at Beijing dot com road")).isEmpty();
    }

    private void assertPriorityShareAboveHalf(String engine, String market) {
        WorkflowSearchService.SearchSourceMix mix = service.planDirectSearchSourceMix(engine, market, 19, crawlerSettings);

        String expectedEngine = "360".equals(engine) ? "SO360" : engine.toUpperCase();
        assertThat(mix.engine()).isEqualTo(expectedEngine);
        assertThat(mix.primaryFetches()).isGreaterThan(0);
        assertThat(mix.autoFallbackFetches()).isGreaterThanOrEqualTo(0);
        assertThat(mix.primaryFetches()).isGreaterThan(mix.autoFallbackFetches());
        assertThat(mix.primaryShare()).isGreaterThan(0.5);
    }
}
