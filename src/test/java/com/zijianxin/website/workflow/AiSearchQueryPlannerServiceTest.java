package com.zijianxin.website.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSearchQueryPlannerServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void plannerParsesAiJsonQueries() {
        SettingsService settingsService = mock(SettingsService.class);
        AiCompletionService aiCompletionService = mock(AiCompletionService.class);
        when(settingsService.getSettings()).thenReturn(appSettingsWithAiKey());
        when(aiCompletionService.complete(any(), anyString(), anyString())).thenReturn("""
                {
                  "normalizedIntent": "手机配件制造商",
                  "productTerms": ["手机配件", "mobile phone accessories"],
                  "targetCompanyTypes": ["manufacturer", "factory"],
                  "queries": [
                    "site:.cn 手机配件 厂家 联系方式",
                    "mobile phone accessories manufacturer site:.cn contact",
                    "https://example.com/not-a-search-query"
                  ],
                  "excludeTerms": ["news", "blog"]
                }
                """);

        AiSearchQueryPlannerService service = new AiSearchQueryPlannerService(
                settingsService,
                aiCompletionService,
                objectMapper
        );

        AiSearchQueryPlannerService.SearchQueryPlan plan = service.plan(new AiSearchQueryPlannerService.SearchQueryPlanRequest(
                "生产手机配件的厂商",
                "电子制造",
                "China",
                "ALL",
                "standard",
                10
        ));

        assertThat(plan.normalizedIntent()).isEqualTo("手机配件制造商");
        assertThat(plan.queries()).containsExactly(
                "site:.cn 手机配件 厂家 联系方式",
                "mobile phone accessories manufacturer site:.cn contact"
        );
        assertThat(plan.excludeTerms()).containsExactly("news", "blog");
        assertThat(plan.productTerms()).containsExactly("手机配件", "mobile phone accessories");
        assertThat(plan.targetCompanyTypes()).containsExactly("manufacturer", "factory");
        verify(aiCompletionService).complete(any(), anyString(), anyString());
    }

    @Test
    void plannerFallsBackToEmptyPlanWhenAiFails() {
        SettingsService settingsService = mock(SettingsService.class);
        AiCompletionService aiCompletionService = mock(AiCompletionService.class);
        when(settingsService.getSettings()).thenReturn(appSettingsWithAiKey());
        when(aiCompletionService.complete(any(), anyString(), anyString())).thenThrow(new IllegalStateException("boom"));

        AiSearchQueryPlannerService service = new AiSearchQueryPlannerService(
                settingsService,
                aiCompletionService,
                objectMapper
        );

        AiSearchQueryPlannerService.SearchQueryPlan plan = service.plan(new AiSearchQueryPlannerService.SearchQueryPlanRequest(
                "生产手机配件的厂商",
                "电子制造",
                "China",
                "ALL",
                "standard",
                10
        ));

        assertThat(plan.queries()).isEmpty();
        assertThat(plan.note()).contains("AI search planner failed");
    }

    private SettingsModels.AppSettings appSettingsWithAiKey() {
        return new SettingsModels.AppSettings(
                new SettingsModels.AiSettings(
                        "Qwen",
                        "test-key",
                        "qwen-plus",
                        "https://example.test/v1",
                        "One Minute Tech",
                        "AI Lead Generation Workflow",
                        "",
                        "zh-CN",
                        "professional",
                        "",
                        ""
                ),
                SettingsModels.SearchSettings.defaults(),
                SettingsModels.CrawlerSettings.defaults(),
                SettingsModels.MailSettings.defaults(),
                SettingsModels.GeneralSettings.defaults(),
                SettingsModels.TemplateSettings.defaults()
        );
    }
}
