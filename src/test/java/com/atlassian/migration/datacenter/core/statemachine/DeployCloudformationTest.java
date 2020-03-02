package com.atlassian.migration.datacenter.core.statemachine;

import com.atlassian.migration.datacenter.core.aws.CfnApi;
import com.atlassian.migration.datacenter.spi.statemachine.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_IN_PROGRESS;

@ExtendWith(MockitoExtension.class)
class DeployCloudformationTest {

    private DeployCloudformation sut;

    @Mock
    CfnApi mockCfnApi;

    @Mock
    AWSMigrationContext mockMigrationContext;

    @BeforeEach
    void setUp() {
        sut = new DeployCloudformation(mockMigrationContext, mockCfnApi);
    }

    @Test
    void shouldTransitionToCloudformationWaiting() {
        State nextState = sut.nextState();

        assertEquals(WaitCloudformation.class, nextState.getClass());
    }

    @Test
    void shouldInitiateDeploymentWhenRun() {
        final Map<String, String> params = new HashMap<>();
        params.put("Parameter1", "value");
        final String stackName = "stack-name";
        final String templateUrl = "s3://bucket/template.yml";

        when(mockMigrationContext.getAppStackName()).thenReturn(stackName);
        when(mockMigrationContext.getAppTemplateURL()).thenReturn(templateUrl);
        when(mockMigrationContext.getAppTemplateParameters()).thenReturn(params);

        sut.run();

        verify(mockCfnApi).provisionStack(templateUrl, stackName, params);
    }

    @Test
    void shouldIndicateReadyToTransitionWhenStackHasStartedToDeploy() {
        final String stackName = "stack-name";
        when(mockMigrationContext.getAppStackName()).thenReturn(stackName);

        when(mockCfnApi.getStack(stackName)).thenReturn(Optional.of(Stack.builder().build()));

        assertTrue(sut.readyToTransition());
    }

    @Test
    void shouldNotIndicateReadyToTransitionWhenStackIsNotDeploying() {
        final String stackName = "stack-name";
        when(mockMigrationContext.getAppStackName()).thenReturn(stackName);

        when(mockCfnApi.getStack(stackName)).thenReturn(Optional.empty());

        assertFalse(sut.readyToTransition());
    }
}