package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.exceptions.FileUploadException;
import com.atlassian.migration.datacenter.spi.fs.reporting.FailedFileMigration;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.nullness.Opt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.swing.text.html.Option;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class S3Uploader implements Uploader {
    private static final Logger logger = LoggerFactory.getLogger(S3Uploader.class);
    private static final int MS_TO_WAIT_FOR_CRAWLER = 500;
    private static final int MAX_OPEN_CONNECTIONS = 50;
    private static final long MAXIMUM_FILE_SIZE_TO_UPLOAD = 5 * 1024 * 1024 * 1024L; // 5GB  https://docs.aws.amazon.com/AmazonS3/latest/dev/UploadingObjects.html

    private final FileSystemMigrationErrorReport report;
    private final FileSystemMigrationProgress progress;
    private final Queue<S3UploadOperation> responsesQueue = new LinkedList<>();
    private final S3UploadConfig config;

    public S3Uploader(S3UploadConfig config, FileSystemMigrationErrorReport report, FileSystemMigrationProgress progress) {
        this.config = config;
        this.report = report;
        this.progress = progress;
    }

    @Override
    public void upload(BlockingQueue<Optional<Path>> queue) throws FileUploadException
    {
        try {
            for (Optional<Path> opt = queue.take(); opt.isPresent(); opt = queue.take()) {
                uploadFile(opt.get());
            }
        } catch (InterruptedException e) {
            String msg = "InterruptedException while fetching file from queue";
            logger.error(msg, e);
            throw new FileUploadException(msg,e);
        }
        responsesQueue.forEach(this::handlePutObjectResponse);
        logger.info("Finished uploading files to S3");
    }

    private void uploadFile(Path path)
    {
        if (responsesQueue.size() >= MAX_OPEN_CONNECTIONS) {
            responsesQueue.forEach(this::handlePutObjectResponse);
        }

        if (Files.exists(path)) {
            String key = config.getSharedHome().relativize(path).toString();
            if (path.toFile().length() > MAXIMUM_FILE_SIZE_TO_UPLOAD) {
                logger.debug("File {} is larger than {}, running multipart upload", path, FileUtils.byteCountToDisplaySize(MAXIMUM_FILE_SIZE_TO_UPLOAD));

                final S3MultiPartUploader multiPartUploader = new S3MultiPartUploader(config, path.toFile(), key);
                try {
                    multiPartUploader.upload();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error when running multi-part upload for file {} with exception {}", path, e.getMessage());
                }
            } else {
                final PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .build();
                final CompletableFuture<PutObjectResponse> response = config.getS3AsyncClient().putObject(putRequest, path);
                final S3UploadOperation uploadOperation = new S3UploadOperation(path, response);
                responsesQueue.add(uploadOperation);

                progress.reportFileUploadCommenced();
            }
        } else {
            addFailedFile(path, String.format("File doesn't exist: %s", path));
        }
    }

    private void handlePutObjectResponse(S3UploadOperation operation) {
        try {
            final PutObjectResponse evaluatedResponse = operation.response.get();
            if (!evaluatedResponse.sdkHttpResponse().isSuccessful()) {
                final String errorMessage = String.format(
                        "Error when uploading %s to S3, %s",
                        operation.path,
                        evaluatedResponse.sdkHttpResponse().statusText());
                addFailedFile(operation.path, errorMessage);
            } else {
                progress.reportFileMigrated();
            }
        } catch (InterruptedException | ExecutionException e) {
            addFailedFile(operation.path, e.getMessage());
        }
    }

    private void addFailedFile(Path path, String reason) {
        report.reportFileNotMigrated(new FailedFileMigration(path, reason));
        logger.error("File {} wasn't uploaded. Reason: {}", path, reason);
    }

    private static class S3UploadOperation {
        Path path;
        CompletableFuture<PutObjectResponse> response;

        S3UploadOperation(Path path, CompletableFuture<PutObjectResponse> response) {
            this.path = path;
            this.response = response;
        }
    }
}
