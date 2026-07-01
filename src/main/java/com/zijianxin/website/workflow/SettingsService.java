package com.zijianxin.website.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SettingsService {

    private static final String SETTINGS_PATH_PROPERTY = "zijianxin.settings.path";
    private static final String SETTINGS_PATH_ENV = "ZIJIANXIN_SETTINGS_PATH";
    private static final String APP_DIRECTORY_NAME = "开发信";
    private static final String LEGACY_APP_DIRECTORY_NAME = "ZijianxinWebsite";
    private static final Path PROJECT_SETTINGS_PATH = Path.of("data", "app-settings.json");
    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final ObjectMapper objectMapper;
    private final Path settingsPath;
    private SettingsModels.AppSettings cachedSettings;

    public SettingsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.settingsPath = resolveSettingsPath();
        this.cachedSettings = loadSettings();
    }

    public synchronized SettingsModels.AppSettings getSettings() {
        return cachedSettings;
    }

    public synchronized SettingsModels.AiSettings saveAiSettings(SettingsModels.AiSettings settings) {
        cachedSettings = cachedSettings.withAi(settings);
        persist();
        return cachedSettings.ai();
    }

    public synchronized SettingsModels.SearchSettings saveSearchSettings(SettingsModels.SearchSettings settings) {
        cachedSettings = cachedSettings.withSearch(settings);
        persist();
        return cachedSettings.search();
    }

    public synchronized SettingsModels.CrawlerSettings saveCrawlerSettings(SettingsModels.CrawlerSettings settings) {
        cachedSettings = cachedSettings.withCrawler(settings);
        persist();
        return cachedSettings.crawler();
    }

    public synchronized SettingsModels.MailSettings saveMailSettings(SettingsModels.MailSettings settings) {
        cachedSettings = cachedSettings.withMail(settings);
        persist();
        return cachedSettings.mail();
    }

    public synchronized SettingsModels.GeneralSettings saveGeneralSettings(SettingsModels.GeneralSettings settings) {
        cachedSettings = cachedSettings.withGeneral(settings);
        persist();
        return cachedSettings.general();
    }

    public synchronized SettingsModels.TemplateSettings saveTemplateSettings(SettingsModels.TemplateSettings settings) {
        cachedSettings = cachedSettings.withTemplates(settings);
        persist();
        return cachedSettings.templates();
    }

    private SettingsModels.AppSettings loadSettings() {
        if (!Files.exists(settingsPath)) {
            SettingsModels.AppSettings defaults = SettingsModels.AppSettings.defaults();
            writeSettings(defaults);
            log.info("Settings file not found. Created default settings at {}", settingsPath.toAbsolutePath());
            return defaults;
        }

        try (InputStream inputStream = Files.newInputStream(settingsPath)) {
            SettingsModels.AppSettings loaded = objectMapper.readValue(inputStream, SettingsModels.AppSettings.class);
            return loaded == null ? SettingsModels.AppSettings.defaults() : loaded;
        } catch (IOException exception) {
            SettingsModels.AppSettings defaults = SettingsModels.AppSettings.defaults();
            log.warn("Failed to parse settings file {}. Rewriting with defaults. Cause: {}", settingsPath.toAbsolutePath(), exception.getMessage());
            writeSettings(defaults);
            return defaults;
        }
    }

    private void persist() {
        writeSettings(cachedSettings);
    }

    private void writeSettings(SettingsModels.AppSettings settings) {
        try {
            Path parent = settingsPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream outputStream = Files.newOutputStream(settingsPath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, settings);
            }
        } catch (IOException exception) {
            log.error("Failed to persist application settings to {}", settingsPath.toAbsolutePath(), exception);
            throw new IllegalStateException("Failed to persist application settings", exception);
        }
    }

    private static Path resolveSettingsPath() {
        String explicitPath = System.getProperty(SETTINGS_PATH_PROPERTY);
        if (explicitPath == null || explicitPath.isBlank()) {
            explicitPath = System.getenv(SETTINGS_PATH_ENV);
        }
        if (explicitPath != null && !explicitPath.isBlank()) {
            return Path.of(explicitPath.trim()).toAbsolutePath();
        }

        if (Files.isRegularFile(PROJECT_SETTINGS_PATH) && Files.isWritable(PROJECT_SETTINGS_PATH)) {
            return PROJECT_SETTINGS_PATH.toAbsolutePath();
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return resolveAppDataSettingsPath(appData);
        }

        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, ".zijianxin-website", "app-settings.json").toAbsolutePath();
    }

    private static Path resolveAppDataSettingsPath(String appData) {
        Path settingsPath = Path.of(appData, APP_DIRECTORY_NAME, "app-settings.json").toAbsolutePath();
        Path legacySettingsPath = Path.of(appData, LEGACY_APP_DIRECTORY_NAME, "app-settings.json").toAbsolutePath();
        if (Files.notExists(settingsPath) && Files.isRegularFile(legacySettingsPath)) {
            try {
                Files.createDirectories(settingsPath.getParent());
                Files.copy(legacySettingsPath, settingsPath);
            } catch (IOException exception) {
                return legacySettingsPath;
            }
        }
        return settingsPath;
    }
}
