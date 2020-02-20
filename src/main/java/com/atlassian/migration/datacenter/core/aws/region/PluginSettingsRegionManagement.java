package com.atlassian.migration.datacenter.core.aws.region;

import com.amazonaws.regions.Regions;
import com.atlassian.migration.datacenter.core.aws.GlobalInfrastructure;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages the persistence and retrieval of the region used to make AWS SDK API calls.
 * The region is stored in the plugin settings of this app.
 */
@Component
public class PluginSettingsRegionManagement implements RegionManagement {

    private final GlobalInfrastructure globalInfrastructure;
    private final PluginSettings pluginSettings;

    @Autowired
    public PluginSettingsRegionManagement(@ComponentImport PluginSettingsFactory pluginSettingsFactory, GlobalInfrastructure globalInfrastructure) {
        this.globalInfrastructure = globalInfrastructure;
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    /**
     * @return The id of the region that has been stored most recently (e.g. us-east-2, ap-southeast-1). If no region
     * has been configured, it will return the id of the default region.
     */
    @Override
    public String getRegion() {
        String pluginSettingsRegion = (String) this.pluginSettings.get(AWS_REGION_PLUGIN_STORAGE_KEY + REGION_PLUGIN_STORAGE_SUFFIX);
        if (pluginSettingsRegion == null || pluginSettingsRegion.equals("")) {
            return Regions.DEFAULT_REGION.getName();
        }
        return pluginSettingsRegion;
    }

    /**
     * Sets the region to be used for AWS API calls
     * @param region the id of the region to use e.g. us-east-1, eu-central-1
     * @throws InvalidAWSRegionException if the region id provided is not a supported AWS region.
     * @see GlobalInfrastructure
     */
    @Override
    public void storeRegion(String region) throws InvalidAWSRegionException {
        if(!isValidRegion(region)) {
            throw new InvalidAWSRegionException();
        }
        this.pluginSettings.put(AWS_REGION_PLUGIN_STORAGE_KEY + REGION_PLUGIN_STORAGE_SUFFIX, region);
    }

    private boolean isValidRegion(String testRegion) {
        return globalInfrastructure.
                getRegions()
                .parallelStream()
                .anyMatch(region -> region.equals(testRegion));
    }
}
