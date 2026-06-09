package com.zijianxin.website.workflow;

import org.springframework.stereotype.Service;

@Service
public class WorkflowService {

    private final WorkflowSearchService workflowSearchService;
    private final WorkflowDraftService workflowDraftService;
    private final WorkflowEmailService workflowEmailService;
    private final SettingsService settingsService;
    private final AiCompletionService aiCompletionService;

    public WorkflowService(
            WorkflowSearchService workflowSearchService,
            WorkflowDraftService workflowDraftService,
            WorkflowEmailService workflowEmailService,
            SettingsService settingsService,
            AiCompletionService aiCompletionService
    ) {
        this.workflowSearchService = workflowSearchService;
        this.workflowDraftService = workflowDraftService;
        this.workflowEmailService = workflowEmailService;
        this.settingsService = settingsService;
        this.aiCompletionService = aiCompletionService;
    }

    public synchronized WorkflowModels.CustomerSearchResponse searchCustomers(WorkflowModels.CustomerSearchRequest request) {
        return workflowSearchService.searchCustomers(request);
    }

    public WorkflowModels.CustomerSearchResponse getLastSearchResponse() {
        return workflowSearchService.getLastSearchResponse();
    }

    public WorkflowModels.CustomerLead inspectCustomerUrlForDebug(WorkflowModels.DebugInspectRequest request) {
        return workflowSearchService.inspectUrlForDebug(
                request == null ? "" : request.url(),
                request == null ? "China" : request.market(),
                request == null ? "" : request.industry(),
                request == null ? "" : request.keywords()
        );
    }

    public WorkflowModels.DraftResponse generateDraft(WorkflowModels.DraftRequest request) {
        return workflowDraftService.generateDraft(request);
    }

    public WorkflowModels.DraftResponse optimizeDraft(WorkflowModels.DraftOptimizationRequest request) {
        return workflowDraftService.optimizeDraft(request);
    }

    public WorkflowModels.TranslateEmailResponse translateEmail(WorkflowModels.TranslateEmailRequest request) {
        return workflowDraftService.translateEmail(request);
    }

    public WorkflowModels.SendEmailResponse sendEmail(WorkflowModels.SendEmailRequest request) {
        return workflowEmailService.sendEmail(request);
    }

    public SettingsModels.AppSettings getSettings() {
        return settingsService.getSettings();
    }

    public SettingsModels.AiSettings saveAiSettings(SettingsModels.AiSettings request) {
        return settingsService.saveAiSettings(request);
    }

    public SettingsModels.AiConnectionTestResult testAiSettings(SettingsModels.AiSettings request) {
        return aiCompletionService.testConnection(request);
    }

    public SettingsModels.SearchSettings saveSearchSettings(SettingsModels.SearchSettings request) {
        return settingsService.saveSearchSettings(request);
    }

    public SettingsModels.SearchConnectionTestResult testSearchSettings(SettingsModels.SearchSettings request) {
        return workflowSearchService.testSerpApiConnection(request);
    }

    public SettingsModels.CrawlerSettings saveCrawlerSettings(SettingsModels.CrawlerSettings request) {
        return settingsService.saveCrawlerSettings(request);
    }

    public SettingsModels.MailSettings saveMailSettings(SettingsModels.MailSettings request) {
        return settingsService.saveMailSettings(request);
    }

    public SettingsModels.GeneralSettings saveGeneralSettings(SettingsModels.GeneralSettings request) {
        return settingsService.saveGeneralSettings(request);
    }
}
