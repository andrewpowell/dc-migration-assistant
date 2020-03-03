package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFilesystemMigrationProgress;
import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.DONE;
import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.FAILED;
import static com.atlassian.migration.datacenter.spi.fs.reporting.FilesystemMigrationStatus.RUNNING;

@Component
public class S3FilesystemMigrationService implements FilesystemMigrationService {
    private static final Logger logger = LoggerFactory.getLogger(S3FilesystemMigrationService.class);

    private static final int NUM_UPLOAD_THREADS = Integer.getInteger("NUM_UPLOAD_THREADS", 2);
    private static final String BUCKET_NAME = System.getProperty("S3_TARGET_BUCKET_NAME", "slingshot-test-2");

    private final AwsCredentialsProvider credentialsProvider;
    private final RegionService regionService;
    private final JiraHome jiraHome;

    private FileSystemMigrationReport report;
    private AtomicBoolean isDoneCrawling;
    private ConcurrentLinkedQueue<Path> uploadQueue;
    private S3UploadConfig config;

    public S3FilesystemMigrationService(RegionService regionService,
                                        AwsCredentialsProvider credentialsProvider,
                                        @ComponentImport JiraHome jiraHome) {
        this.regionService = regionService;
        this.credentialsProvider = credentialsProvider;
        this.jiraHome = jiraHome;
    }

    @Override
    public boolean isRunning() {
        return report != null && report.getStatus().equals(RUNNING);
    }

    @Override
    public FileSystemMigrationReport getReport() {
        return report;
    }

    /**
     * Start filesystem migration to S3 bucket. This is a blocking operation and should be started from ExecutorService
     * or preferably from ScheduledJob
     */
    @Override
    public void startMigration() {
        if (isRunning()) {
            return;
        }

        initialiseMigration();

        CompletionService<Void> uploadResults = startUploadingFromQueue();

        populateUploadQueue();

        waitForUploadsToComplete(uploadResults);

        finaliseMigration();
    }

    private void initialiseMigration() {
        report = new DefaultFileSystemMigrationReport(new DefaultFileSystemMigrationErrorReport(), new DefaultFilesystemMigrationProgress());
        isDoneCrawling = new AtomicBoolean(false);
        uploadQueue = new ConcurrentLinkedQueue<>();
        S3AsyncClient s3AsyncClient = buildS3Client();
        config = new S3UploadConfig(getS3Bucket(), s3AsyncClient, getSharedHomeDir());
    }

    private S3AsyncClient buildS3Client() {
        return S3AsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(regionService.getRegion()))
                .build();
    }

    private CompletionService<Void> startUploadingFromQueue() {
        ExecutorService uploadService = Executors.newFixedThreadPool(NUM_UPLOAD_THREADS);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(uploadService);

        Runnable uploaderFunction = () -> {
            Uploader uploader = new S3Uploader(config, report, report);
            uploader.upload(uploadQueue, isDoneCrawling);
        };

        Collections.nCopies(NUM_UPLOAD_THREADS, uploaderFunction)
                .forEach(runnable -> completionService.submit(runnable, null));

        return completionService;
    }

    private void populateUploadQueue() {
        Crawler homeCrawler = new DirectoryStreamCrawler(report);
        try {
            homeCrawler.crawlDirectory(getSharedHomeDir(), uploadQueue);
        } catch (IOException e) {
            logger.error("Failed to traverse home directory for S3 transfer", e);
            report.setStatus(FAILED);
        } finally {
            // FIXME: the uploader will continue uploading until the queue is empty even though we probably need to abort in this scenario as it's indeterminate whether all files have been uploaded or not (should we try fix this now or create a bug and follow up?)
            isDoneCrawling.set(true);
            logger.info("Finished traversing directory [{}], {} files are remaining to upload", config.getSharedHome(), uploadQueue.size());
        }
    }

    private void waitForUploadsToComplete(CompletionService<Void> uploadResults) {
        IntStream.range(0, NUM_UPLOAD_THREADS)
                .forEach(i -> {
                    try {
                        uploadResults.take().get();
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Failed to upload home directory to S3", e);
                        report.setStatus(FAILED);
                    }
                });
    }

    private void finaliseMigration() {
        if (!report.getStatus().equals(FAILED)) {
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
