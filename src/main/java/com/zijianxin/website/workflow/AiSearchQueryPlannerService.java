package com.zijianxin.website.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AiSearchQueryPlannerService {

    private static final Logger log = LoggerFactory.getLogger(AiSearchQueryPlannerService.class);
    private static final int MAX_AI_QUERIES = 16;

    private final SettingsService settingsService;
    private final AiCompletionService aiCompletionService;
    private final ObjectMapper objectMapper;

    public AiSearchQueryPlannerService(
            SettingsService settingsService,
            AiCompletionService aiCompletionService,
            ObjectMapper objectMapper
    ) {
        this.settingsService = settingsService;
        this.aiCompletionService = aiCompletionService;
        this.objectMapper = objectMapper;
    }

    public SearchQueryPlan plan(SearchQueryPlanRequest request) {
        SettingsModels.AiSettings ai = settingsService.getSettings().ai();
        if (ai.apiKey() == null || ai.apiKey().isBlank()) {
            return SearchQueryPlan.empty("AI search planner skipped: API key is not configured.");
        }

        try {
            String raw = aiCompletionService.complete(ai, buildSystemPrompt(), buildUserPrompt(request));
            SearchQueryPlan parsed = parsePlan(raw);
            if (parsed.queries().isEmpty()) {
                return SearchQueryPlan.empty("AI search planner returned no usable queries.");
            }
            return parsed;
        } catch (Exception exception) {
            log.warn("AI search planner failed. Falling back to rule-based search queries. Cause: {}", exception.getMessage());
            return SearchQueryPlan.empty("AI search planner failed: " + exception.getMessage());
        }
    }

    private String buildSystemPrompt() {
        return """
                You are a B2B lead-generation search strategist.
                Convert the user's target customer description and filters into high-signal web search queries.
                Return ONLY valid JSON. Do not use markdown.

                JSON schema:
                {
                  "normalizedIntent": "short summary of the target customer",
                  "targetCompanyTypes": ["manufacturer", "factory", "OEM supplier"],
                  "productTerms": ["term 1", "term 2"],
                  "queries": ["query 1", "query 2"],
                  "excludeTerms": ["news", "blog", "directory"]
                }

                Query rules:
                - Generate queries for real company official websites, not marketplaces, news, dictionaries, forums, or articles.
                - Avoid consumer brand official sites unless the selected industry or target description is explicitly brand/company/official-site oriented.
                - For accessories/components, prefer OEM/ODM factory, manufacturer, supplier, sales email, and contact queries.
                - Mix native-language and English product terms when useful.
                - Use contact-intent words such as contact, email, sales, official website, 官网, 厂家, 联系方式.
                - Avoid boolean operators like OR and avoid overly complex search syntax.
                - Do not put every important term in quotes; keep several broader queries.
                - For China, include site:.cn and site:.com.cn queries and Chinese company-intent terms.
                - For USA, include site:.us only when useful; also include broader USA manufacturer/supplier queries.
                - For Germany, include site:.de and German/English company-intent terms when useful.
                - Prefer concrete product/category terms over generic industry names.
                - Generate 8 to 14 distinct queries.
                - Keep each query under 12 words.
                """;
    }

    private String buildUserPrompt(SearchQueryPlanRequest request) {
        return """
                Target customer description:
                %s

                Selected industry:
                %s

                Target market:
                %s

                Company size:
                %s

                Search depth:
                %s

                Requested lead count:
                %s
                """.formatted(
                fallback(request.targetDescription(), "N/A"),
                fallback(request.industry(), "N/A"),
                fallback(request.market(), "N/A"),
                fallback(request.companySize(), "ALL"),
                fallback(request.searchDepth(), "standard"),
                request.requestedLimit()
        );
    }

    private SearchQueryPlan parsePlan(String raw) throws Exception {
        String json = extractJson(raw);
        JsonNode root = objectMapper.readTree(json);
        List<String> queries = readStringArray(root.path("queries"), MAX_AI_QUERIES);
        List<String> excludeTerms = readStringArray(root.path("excludeTerms"), 20);
        List<String> productTerms = readStringArray(root.path("productTerms"), 20);
        List<String> targetCompanyTypes = readStringArray(root.path("targetCompanyTypes"), 12);
        String normalizedIntent = root.path("normalizedIntent").asText("");

        return new SearchQueryPlan(
                cleanText(normalizedIntent),
                queries,
                excludeTerms,
                productTerms,
                targetCompanyTypes,
                "AI generated " + queries.size() + " search queries."
        );
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private List<String> readStringArray(JsonNode node, int limit) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (values.size() >= limit) {
                    return;
                }
                String value = cleanText(item.asText(""));
                if (isUsableQuery(value)) {
                    values.add(value);
                }
            });
        }
        return new ArrayList<>(values);
    }

    private boolean isUsableQuery(String value) {
        if (value.isBlank() || value.length() > 160) {
            return false;
        }
        String lower = value.toLowerCase();
        return !lower.contains("http://") && !lower.contains("https://");
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value.trim();
    }

    public record SearchQueryPlanRequest(
            String targetDescription,
            String industry,
            String market,
            String companySize,
            String searchDepth,
            int requestedLimit
    ) {
    }

    public record SearchQueryPlan(
            String normalizedIntent,
            List<String> queries,
            List<String> excludeTerms,
            List<String> productTerms,
            List<String> targetCompanyTypes,
            String note
    ) {
        public SearchQueryPlan {
            normalizedIntent = normalizedIntent == null ? "" : normalizedIntent;
            queries = queries == null ? List.of() : List.copyOf(queries);
            excludeTerms = excludeTerms == null ? List.of() : List.copyOf(excludeTerms);
            productTerms = productTerms == null ? List.of() : List.copyOf(productTerms);
            targetCompanyTypes = targetCompanyTypes == null ? List.of() : List.copyOf(targetCompanyTypes);
            note = note == null ? "" : note;
        }

        public static SearchQueryPlan empty(String note) {
            return new SearchQueryPlan("", List.of(), List.of(), List.of(), List.of(), note);
        }
    }
}
