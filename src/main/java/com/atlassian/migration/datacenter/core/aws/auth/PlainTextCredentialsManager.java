package com.atlassian.migration.datacenter.core.aws.auth;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlainTextCredentialsManager implements CredentialsFetcher, CredentialsStorer {

    private static final String AWS_CREDS_PLUGIN_STORAGE_KEY = "com.atlassian.migration.datacenter.core.aws.auth";
    private static final String ACCESS_KEY_ID_PLUGIN_STORAGE_SUFFIX = ".accessKeyId";
    private static final String SECRET_ACCESS_KEY_PLUGIN_STORAGE_SUFFIX = ".secretAccessKey";


    public PluginSettingsFactory pluginSettingsFactory;

    @Autowired
    public PlainTextCredentialsManager(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public String getAccessKeyId() {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        return (String) pluginSettings.get(AWS_CREDS_PLUGIN_STORAGE_KEY + ACCESS_KEY_ID_PLUGIN_STORAGE_SUFFIX);
    }

    @Override
    public String getSecretAccessKey() {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        return (String) pluginSettings.get(AWS_CREDS_PLUGIN_STORAGE_KEY + SECRET_ACCESS_KEY_PLUGIN_STORAGE_SUFFIX);
    }

    @Override
    public void storeAccessKeyId(String accessKeyId) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        pluginSettings.put(AWS_CREDS_PLUGIN_STORAGE_KEY + ACCESS_KEY_ID_PLUGIN_STORAGE_SUFFIX, accessKeyId);
    }

    @Override
    public void storeSecretAccessKey(String secretAccessKey) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        pluginSettings.put(AWS_CREDS_PLUGIN_STORAGE_KEY + SECRET_ACCESS_KEY_PLUGIN_STORAGE_SUFFIX, secretAccessKey);
    }
}
