/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.migration.datacenter.core.aws.db.restore;

import com.atlassian.migration.datacenter.core.aws.infrastructure.AWSMigrationHelperDeploymentService;
import com.atlassian.migration.datacenter.core.aws.ssm.SSMApi;
import com.atlassian.migration.datacenter.core.aws.ssm.SuccessfulSSMCommandConsumer;
import com.atlassian.migration.datacenter.core.fs.download.s3sync.EnsureSuccessfulSSMCommandConsumer;
import com.atlassian.migration.datacenter.spi.exceptions.DatabaseMigrationFailure;
import com.atlassian.migration.datacenter.spi.exceptions.InvalidMigrationStageError;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

import java.util.Collections;

public class SsmPsqlDatabaseRestoreService {

    private final int maxCommandRetries;

    private final SSMApi ssm;
    private final AWSMigrationHelperDeploymentService migrationHelperDeploymentService;

    private final String restoreDocumentName = "restoreDatabaseBackupToRDS";

    private String commandId;

    SsmPsqlDatabaseRestoreService(SSMApi ssm, int maxCommandRetries,
                                  AWSMigrationHelperDeploymentService migrationHelperDeploymentService) {
        this.ssm = ssm;
        this.maxCommandRetries = maxCommandRetries;
        this.migrationHelperDeploymentService = migrationHelperDeploymentService;
    }

    public SsmPsqlDatabaseRestoreService(SSMApi ssm,
                                         AWSMigrationHelperDeploymentService migrationHelperDeploymentService) {
        this(ssm, 10, migrationHelperDeploymentService);
    }

    public void restoreDatabase(DatabaseRestoreStageTransitionCallback restoreStageTransitionCallback)
            throws DatabaseMigrationFailure, InvalidMigrationStageError {
        String dbRestorePlaybook = migrationHelperDeploymentService.getDbRestoreDocument();
        String migrationInstanceId = migrationHelperDeploymentService.getMigrationHostInstanceId();

        restoreStageTransitionCallback.assertInStartingStage();

        this.commandId = ssm.runSSMDocument(dbRestorePlaybook, migrationInstanceId, Collections.emptyMap());

        SuccessfulSSMCommandConsumer consumer = new EnsureSuccessfulSSMCommandConsumer(ssm, commandId,
                migrationInstanceId);

        restoreStageTransitionCallback.transitionToServiceWaitStage();

        try {
            consumer.handleCommandOutput(maxCommandRetries);
            restoreStageTransitionCallback.transitionToServiceNextStage();
        } catch (SuccessfulSSMCommandConsumer.UnsuccessfulSSMCommandInvocationException
                | SuccessfulSSMCommandConsumer.SSMCommandInvocationProcessingError e) {
            final String errorMessage = "Error restoring database. Either download of database dump from S3 failed or pg_restore failed";
            restoreStageTransitionCallback
                    .transitionToServiceErrorStage(String.format("%s. %s", errorMessage, e.getMessage()));
            throw new DatabaseMigrationFailure(errorMessage, e);
        }
    }

    public SsmCommandResult fetchCommandResult() throws SsmCommandNotInitialisedException {
        if (getCommandId() == null) {
            throw new SsmCommandNotInitialisedException("SSM command was not executed");
        }
        String migrationInstanceId = migrationHelperDeploymentService.getMigrationHostInstanceId();

        final GetCommandInvocationResponse response = ssm.getSSMCommand(getCommandId(), migrationInstanceId);

        final SsmCommandResult ssmCommandOutputs = new SsmCommandResult();
        ssmCommandOutputs.errorMessage = response.standardErrorContent();
        ssmCommandOutputs.consoleUrl = String.format(
                "https://console.aws.amazon.com/s3/buckets/%s/%s/%s/%s/awsrunShellScript/%s/",
                migrationHelperDeploymentService.getMigrationS3BucketName(),
                ssm.getSsmS3KeyPrefix(),
                response.commandId(),
                response.instanceId(),
                restoreDocumentName);

        return ssmCommandOutputs;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getRestoreDocumentName() {
        return restoreDocumentName;
    }

    public static class SsmCommandResult {
        public String consoleUrl;
        public String errorMessage;
    }

    public static class SsmCommandNotInitialisedException extends Exception {
        public SsmCommandNotInitialisedException(String message) {
            super(message);
        }
    }
}
