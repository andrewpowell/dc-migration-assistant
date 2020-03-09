package com.atlassian.migration.datacenter.core.aws.cloud;

import com.atlassian.migration.datacenter.core.ModalMigrationStageWorker;
import com.atlassian.migration.datacenter.core.aws.auth.WriteCredentialsService;
import com.atlassian.migration.datacenter.core.aws.region.InvalidAWSRegionException;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.MigrationServiceV2;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSConfigurationServiceTest {

    @Mock
    WriteCredentialsService mockCredentialsWriter;

    @Mock
    RegionService mockRegionService;

    @Mock
    MigrationServiceV2 mockMigrationService;

    @Mock
    PluginSettingsFactory pluginSettingsFactory;

    @Mock
    PluginSettings pluginSettings;

    private AWSConfigurationService sut;

    @BeforeEach
    void setUp() {
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ModalMigrationStageWorker.MIGRATION_MODE_PLUGIN_SETTINGS_KEY)).thenReturn(ModalMigrationStageWorker.DCMigrationAssistantMode.DEFAULT);

        ModalMigrationStageWorker worker = new ModalMigrationStageWorker(pluginSettingsFactory, mockMigrationService);

        sut = new AWSConfigurationService(mockCredentialsWriter, mockRegionService, mockMigrationService, worker);
    }

    @Test
    void shouldStoreCredentials() throws InvalidMigrationStageError {
        mockValidMigration();

        final String username = "username";
        final String password = "password";
        sut.configureCloudProvider(username, password, "garbage");

        verify(mockCredentialsWriter).storeAccessKeyId(username);
        verify(mockCredentialsWriter).storeSecretAccessKey(password);
    }

    @Test
    void shouldStoreRegion() throws InvalidAWSRegionException, InvalidMigrationStageError {
        mockValidMigration();

        final String region = "region";
        sut.configureCloudProvider("username", "password", region);

        verify(mockRegionService).storeRegion(region);
    }

    @Test
    void shouldStoreCredentialsOnlyWhenStateIsAuthentication() {
        when(mockMigrationService.getCurrentStage()).thenReturn(MigrationStage.FS_MIGRATION_EXPORT);
        assertThrows(InvalidMigrationStageError.class, () -> sut.configureCloudProvider("garbage", "garbage", "garbage"));
    }

    private void mockValidMigration() {
        when(mockMigrationService.getCurrentStage()).thenReturn(MigrationStage.AUTHENTICATION);
    }

    @Test
    void shouldTransitionToProvisionApplicationStageWhenSuccessful() throws InvalidMigrationStageError {
        mockValidMigration();

        sut.configureCloudProvider("garbage", "garbage", "garbage");

        verify(mockMigrationService).transition(MigrationStage.AUTHENTICATION, MigrationStage.PROVISION_APPLICATION);
    }

    @Test
    void shouldThrowErrorAndNotTransitionWhenUnableToCompleteSuccessfully() throws InvalidAWSRegionException, InvalidMigrationStageError {
        mockValidMigration();

        final String testRegion = "region";
        doThrow(new InvalidAWSRegionException()).when(mockRegionService).storeRegion(testRegion);

        try {
            sut.configureCloudProvider("garbage", "garbage", testRegion);
            fail();
        } catch (RuntimeException rte) {
            assertEquals(InvalidAWSRegionException.class, rte.getCause().getClass());
            verify(mockMigrationService, never()).transition(any(), any());
        }
    }

}