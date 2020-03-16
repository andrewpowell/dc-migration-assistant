package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFilesystemMigrationProgress;
import com.atlassian.migration.datacenter.core.util.UploadQueue;
import com.atlassian.migration.datacenter.dto.Migration;
import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static com.atlassian.migration.datacenter.spi.MigrationStage.FS_MIGRATION_COPY;
import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.DONE;
import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.FAILED;
import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.RUNNING;

@Component
public class S3FilesystemMigrationService implements FilesystemMigrationService {
    private static final Logger logger = LoggerFactory.getLogger(S3FilesystemMigrationService.class);

    private static final String BUCKET_NAME = System.getProperty("S3_TARGET_BUCKET_NAME", "trebuchet-testing");

    private final S3AsyncClient s3AsyncClient;
    private final JiraHome jiraHome;
    private final MigrationService migrationService;
    private final SchedulerService schedulerService;

    private FileSystemMigrationReport report;


    //TODO: Region Service and provider will be replaced by the S3 Client
    public S3FilesystemMigrationService(S3AsyncClient s3AsyncClient,
                                        JiraHome jiraHome,
                                        MigrationService migrationService,
                                        SchedulerService schedulerService)
    {
        this.s3AsyncClient = s3AsyncClient;
        this.jiraHome = jiraHome;
        this.migrationService = migrationService;
        this.schedulerService = schedulerService;
    }

    @Override
    public boolean isRunning() {
        return this.migrationService.getCurrentStage().equals(MigrationStage.WAIT_FS_MIGRATION_COPY);
    }

    @Override
    public FileSystemMigrationReport getReport() {
        return report;
    }

    @Override
    public Boolean scheduleMigration() {
        Migration currentMigration = this.migrationService.getCurrentMigration();
        if (currentMigration.getStage() != FS_MIGRATION_COPY) {
            return false;
        }

        final JobRunnerKey runnerKey = JobRunnerKey.of(S3UploadJobRunner.KEY);
        JobId jobId = JobId.of(S3UploadJobRunner.KEY + currentMigration.getID());
        logger.info("Starting filesystem migration");

        //TODO: Can the job runner be injected? It has no state
        schedulerService.registerJobRunner(runnerKey, new S3UploadJobRunner(this));
        logger.info("Registered new job runner for S3");

        JobConfig jobConfig = JobConfig.forJobRunnerKey(runnerKey)
                .withSchedule(null) // run now
                .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER);
        try {
            logger.info("Scheduling new job for S3 upload runner");

            this.migrationService.transition(MigrationStage.FS_MIGRATION_COPY, MigrationStage.WAIT_FS_MIGRATION_COPY);

            schedulerService.scheduleJob(jobId, jobConfig);
        } catch (SchedulerServiceException | InvalidMigrationStageError e) {
            logger.error("Exception when scheduling S3 upload job", e);
            this.schedulerService.unscheduleJob(jobId);
            migrationService.error();
            return false;
        }
        return true;
    }

    /**
     * Start filesystem migration to S3 bucket. This is a blocking operation and should be started from ExecutorService
     * or preferably from ScheduledJob
     */
    @Override
    public void startMigration() throws InvalidMigrationStageError {
        if (isRunning()) {
            logger.warn("Filesystem migration is currently in progress, aborting new execution.");
            return;
        }

        migrationService.transition(MigrationStage.FS_MIGRATION_COPY, MigrationStage.WAIT_FS_MIGRATION_COPY);

        report = new DefaultFileSystemMigrationReport(new DefaultFileSystemMigrationErrorReport(), new DefaultFilesystemMigrationProgress());
        report.setStatus(RUNNING);

        Crawler homeCrawler = new DirectoryStreamCrawler(report, report);

        S3UploadConfig s3UploadConfig = new S3UploadConfig(getS3Bucket(), s3AsyncClient, getSharedHomeDir());
        Uploader s3Uploader = new S3Uploader(s3UploadConfig, report, report);

        FilesystemUploader fsUploader = new FilesystemUploader(homeCrawler, s3Uploader);
        fsUploader.uploadDirectory(getSharedHomeDir());


        if (report.getStatus().equals(DONE)) {
            this.migrationService.transition(MigrationStage.WAIT_FS_MIGRATION_COPY, MigrationStage.OFFLINE_WARNING);
        } else if (!report.getStatus().equals(FAILED)) {
            this.migrationService.error();
            report.setStatus(DONE);
        }
    }
    
    private String getS3Bucket() {
        return BUCKET_NAME;
    }

    private Path getSharedHomeDir() {
        return jiraHome.getHome().toPath();
    }
}