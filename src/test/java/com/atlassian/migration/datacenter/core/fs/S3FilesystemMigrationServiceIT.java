package com.atlassian.migration.datacenter.core.fs;

import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.migration.datacenter.core.aws.auth.AtlassianPluginAWSCredentialsProvider;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService;
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;

@Tag("integration")
@ExtendWith({LocalstackDockerExtension.class, MockitoExtension.class})
@LocalstackDockerProperties(services = {"s3"})
class S3FilesystemMigrationServiceIT {
    @TempDir
    Path dir;
    @Mock
    RegionService regionService;

    @Mock
    AtlassianPluginAWSCredentialsProvider credentialsProvider;

    @Mock
    JiraHome jiraHome;

    @BeforeEach
    void setup() throws Exception {
        Path file = dir.resolve(UUID.randomUUID().toString());
        String rand = String.format("Testing string %s", Instant.now());
        Files.write(file, Collections.singleton(rand));
    }

    @Test
    void testSuccessfulDirectoryMigration(@TempDir Path dir) {
        when(regionService.getRegion()).thenReturn(Region.US_EAST_1.toString());
        when(jiraHome.getHome()).thenReturn(dir.toFile());

        FilesystemMigrationService fsService = new S3FilesystemMigrationService(regionService, credentialsProvider, jiraHome);

        fsService.startMigration();

        Assertions.assertNotEquals(FilesystemMigrationStatus.FAILED, fsService.getProgress().getStatus());
    }
}
