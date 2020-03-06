package com.atlassian.migration.datacenter.core.aws;

import com.atlassian.migration.datacenter.core.aws.region.RegionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackInstanceNotFoundException;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Test Integration test. No mocks involved
 */
class CfnApiIT {

    public static final String S3_CFN_STACK_URL = "https://dcd-slinghost-templates.s3-ap-southeast-2.amazonaws.com/tests/create_s3_bucket.yaml";
    private CfnApi cfnApi;

    @BeforeEach
    void setUp() {
        RegionService regionManager = new RegionService() {
            @Override
            public String getRegion() { return System.getenv("AWS_DEFAULT_REGION"); }

            @Override
            public void storeRegion(String string) { throw new UnsupportedOperationException(); }
        };
        cfnApi = new CfnApi(DefaultCredentialsProvider.create(), regionManager);
    }

    @Test
    @Disabled("Disabled until LocalStack is Created")
    public void shouldGetStatusOfExistingCfnStack() {
        String stackArn = "arn:aws:cloudformation:ap-southeast-2:887764444972:stack/jira-analytics-stack-713-001/842750e0-4bbe-11ea-bada-0615bd2a67d8";
        StackStatus status = cfnApi.getStatus(stackArn);
        assertEquals(StackStatus.CREATE_COMPLETE, status);
    }


    @Test
    @Disabled("Disabled until LocalStack is Created")
    public void shouldRaiseExceptionWhenStackDoesNotExist() {
        String stackArn = "arn:aws:cloudformation:ap-southeast-2:1231231231:stack/i-do-not-exist";
        Assertions.assertThrows(StackInstanceNotFoundException.class, () -> {
            cfnApi.getStatus(stackArn);
        });
    }

    @Test
    @Disabled("Disabled until LocalStack is Created")
    public void shouldGetExistingCfnStack() {
        String stackArn = "arn:aws:cloudformation:ap-southeast-2:887764444972:stack/jira-analytics-stack-713-001/842750e0-4bbe-11ea-bada-0615bd2a67d8";
        Optional<Stack> stack = cfnApi.getStack(stackArn);
        assertEquals(StackStatus.CREATE_COMPLETE, stack.get().stackStatus());
    }

    @Test
    @Disabled("Disabled until LocalStack is Created")
        //TODO: Delete stack when complete after each test
    void shouldProvisionNewCfnStack() {
        String random = UUID
                .randomUUID().toString().split("-")[0];
        String stackName = String.format("mothra-test-%s", random);
        Optional<String> provisionedStackId = cfnApi.provisionStack(S3_CFN_STACK_URL, stackName, new HashMap<String, String>() {{
            put("S3BucketName", stackName + "-bucket");
        }});

        assertNotNull(provisionedStackId.get());

        try {
            awaitStackCreation(provisionedStackId.get(), cfnApi::getStatus).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assertions.fail("Timeout while waiting for stack creation to complete", e);
        }
    }

    private CompletableFuture<String> awaitStackCreation(String stackId, Function<String, StackStatus> statusFunc) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        ScheduledFuture<?> scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            StackStatus status = statusFunc.apply(stackId);
            if (Objects.equals(status, StackStatus.CREATE_COMPLETE)) {
                completableFuture.complete(stackId);
            }
        }, 0, 10, TimeUnit.SECONDS);

        completableFuture.whenComplete((result, thrown) -> {
            scheduledFuture.cancel(true);
        });

        return completableFuture;
    }
}