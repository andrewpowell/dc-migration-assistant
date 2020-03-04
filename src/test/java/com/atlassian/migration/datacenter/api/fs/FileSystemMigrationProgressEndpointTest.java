package com.atlassian.migration.datacenter.api.fs;

import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService;
import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileSystemMigrationProgressEndpointTest {

    @Mock
    private FilesystemMigrationService migrationService;

    @Mock
    private FileSystemMigrationReport report;

    private FileSystemMigrationProgressEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new FileSystemMigrationProgressEndpoint(migrationService);
    }

    @Test
    void shouldReturnReportWhenMigrationExists() throws JsonProcessingException {
        when(migrationService.getReport()).thenReturn(report);

        when(report.getStatus()).thenReturn(RUNNING);

        final String testReason = "test reason";
        final Path testFile = Paths.get("file");
        final FailedFileMigration failedFileMigration = new FailedFileMigration(testFile, testReason);
        final Set<FailedFileMigration> failedFiles = new HashSet<>();
        failedFiles.add(failedFileMigration);
        when(report.getFailedFiles()).thenReturn(failedFiles);

        when(report.getCountOfMigratedFiles()).thenReturn(1L);

        final Response response = endpoint.getFilesystemMigrationStatus();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);

        final String responseJson = (String) response.getEntity();
        ObjectReader reader = mapper.reader();
        JsonNode tree = reader.readTree(responseJson);

        final String responseStatus = tree.at("/status").asText();

        final String responseReason = tree.at("/failedFiles/0/reason").asText();
        final String responseFailedFile = tree.at("/failedFiles/0/filePath").asText();

        final Long responseSuccessFileCount = tree.at("/migratedFiles").asLong();

        assertEquals(RUNNING.name(), responseStatus);
        assertEquals(testReason, responseReason);
        assertEquals(testFile.toUri().toString(), responseFailedFile);
        assertEquals(1, responseSuccessFileCount);
    }

    @Test
    void shouldReturnBadRequestWhenNoReportExists() {
        when(migrationService.getReport()).thenReturn(null);

        final Response response = endpoint.getFilesystemMigrationStatus();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("no file system migration exists", response.getEntity());
    }
}
