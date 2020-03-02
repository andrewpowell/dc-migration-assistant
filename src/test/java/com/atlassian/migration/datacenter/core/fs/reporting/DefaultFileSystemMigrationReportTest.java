package com.atlassian.migration.datacenter.core.fs.reporting;

import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress;
import com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.FAILED;
import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.NOT_STARTED;
import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DefaultFileSystemMigrationReportTest {

    private DefaultFileSystemMigrationReport sut;

    @Spy
    FileSystemMigrationProgress progress;

    @Spy
    FileSystemMigrationErrorReport errors;

    @BeforeEach
    void setUp() {
        sut = new DefaultFileSystemMigrationReport(errors, progress);
    }

    @Test
    void testStatusInitiallyNotStarted() {
        assertEquals(NOT_STARTED, sut.getStatus());
    }

    @Test
    void testSetsStatus() {
        sut.setStatus(FAILED);

        assertEquals(FAILED, sut.getStatus());
    }

    @Test
    void shouldDelegatesToWrappedErrorReport() {
        final Path testFile = Paths.get("file");
        final String testReason = "test";
        final FailedFileMigration failedFileMigration = new FailedFileMigration(testFile, testReason);
        sut.reportFileNotMigrated(failedFileMigration);

        verify(errors).reportFileNotMigrated(failedFileMigration);

        sut.getFailedFiles();
        verify(errors).getFailedFiles();
    }

    @Test
    void shouldDelegateToWrappedProgress() {
        final Path path = Paths.get("file");
        sut.reportFileMigrated(path);

        verify(progress).reportFileMigrated(path);

        sut.getMigratedFiles();

        verify(progress).getMigratedFiles();
    }

    @Test
    void shouldGiveDurationBetweenStartedAndGetElapsedTime() {
        Clock testClock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault());
        sut.setClock(testClock);

        sut.setStatus(RUNNING);

        assertEquals(Duration.ZERO, sut.getElapsedTime());

        sut.setClock(Clock.offset(testClock, Duration.ofDays(1).plusSeconds(5)));

        assertEquals(1L, sut.getElapsedTime().toDays());
    }

    @Test
    void shouldNotRestartTimerWhenTransitioningFromRunningToRunning() {
        Clock testClock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault());
        sut.setClock(testClock);

        sut.setStatus(RUNNING);

        sut.setClock(Clock.offset(testClock, Duration.ofSeconds(10)));
        assertEquals(10L, sut.getElapsedTime().getSeconds());

        sut.setStatus(RUNNING);
        assertEquals(10L, sut.getElapsedTime().getSeconds());

        sut.setClock(Clock.offset(testClock, Duration.ofSeconds(20)));
        assertEquals(20L, sut.getElapsedTime().getSeconds());
    }

    @ParameterizedTest
    @EnumSource(value = FilesystemMigrationStatus.class, names = {"DONE", "FAILED"})
    void shouldNotIncrementElapsedTimeAfterMigrationEnds(FilesystemMigrationStatus status) {
        Clock testClock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault());
        sut.setClock(testClock);

        sut.setStatus(RUNNING);

        sut.setClock(Clock.offset(testClock, Duration.ofSeconds(10)));
        assertEquals(10L, sut.getElapsedTime().getSeconds());

        sut.setStatus(status);
        sut.setClock(Clock.offset(testClock, Duration.ofSeconds(20)));
        assertEquals(10L, sut.getElapsedTime().getSeconds());
    }
}
