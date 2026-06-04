package com.zijianxin.website.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    }

    @Test
    void prioritySearchSourcesStayAboveHalfForNonChinaMarket() {
        assertPriorityShareAboveHalf("Google", "USA");
        assertPriorityShareAboveHalf("Baidu", "USA");
        assertPriorityShareAboveHalf("Bing", "USA");
        assertPriorityShareAboveHalf("DuckDuckGo", "USA");
    }

    @Test
    void autoModeKeepsTheOriginalAutomaticChain() {
        WorkflowSearchService.SearchSourceMix mix = service.planDirectSearchSourceMix("Auto", "China", 12, crawlerSettings);

        assertThat(mix.engine()).isEqualTo("AUTO");
        assertThat(mix.primaryFetches()).isZero();
        assertThat(mix.autoFallbackFetches()).isPositive();
        assertThat(mix.autoFallbackQueryLimit()).isEqualTo(12);
        assertThat(mix.primaryShare()).isZero();
    }

    private void assertPriorityShareAboveHalf(String engine, String market) {
        WorkflowSearchService.SearchSourceMix mix = service.planDirectSearchSourceMix(engine, market, 19, crawlerSettings);

        assertThat(mix.engine()).isEqualTo(engine.toUpperCase());
        assertThat(mix.primaryFetches()).isGreaterThan(0);
        assertThat(mix.autoFallbackFetches()).isGreaterThanOrEqualTo(0);
        assertThat(mix.primaryFetches()).isGreaterThan(mix.autoFallbackFetches());
        assertThat(mix.primaryShare()).isGreaterThan(0.5);
    }
}
