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

package com.atlassian.migration.datacenter.core.aws.infrastructure;

import com.atlassian.migration.datacenter.core.aws.CfnApi;
import com.atlassian.migration.datacenter.core.aws.db.restore.TargetDbCredentialsStorageService;
import com.atlassian.migration.datacenter.spi.exceptions.InvalidMigrationStageError;
import com.atlassian.migration.datacenter.spi.dto.MigrationContext;
import com.atlassian.migration.datacenter.spi.MigrationService;
import com.atlassian.migration.datacenter.spi.MigrationStage;
import com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService;
import com.atlassian.migration.datacenter.spi.infrastructure.InfrastructureDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class QuickstartDeploymentService extends CloudformationDeploymentService implements ApplicationDeploymentService {

    private final Logger logger = LoggerFactory.getLogger(QuickstartDeploymentService.class);
    private static final String QUICKSTART_TEMPLATE_URL = "https://aws-quickstart.s3.amazonaws.com/quickstart-atlassian-jira/templates/quickstart-jira-dc-with-vpc.template.yaml";

    private final MigrationService migrationService;
    private final TargetDbCredentialsStorageService dbCredentialsStorageService;

    public QuickstartDeploymentService(CfnApi cfnApi, MigrationService migrationService, TargetDbCredentialsStorageService dbCredentialsStorageService) {
        super(cfnApi);

        this.migrationService = migrationService;
        this.dbCredentialsStorageService = dbCredentialsStorageService;
    }

    /**
     * Commences the deployment of the AWS Quick Start. It will transition the state machine upon completion of the
     * deployment. If the deployment finishes successfully we transition to the next stage, otherwise we transition
     * to an error. The migration will also transition to an error if the deployment takes longer than an hour.
     *
     * @param deploymentId the stack name
     * @param params       the parameters for the cloudformation template. The key should be the parameter name and the value
     *                     should be the parameter value.
     */
    @Override
    public void deployApplication(String deploymentId, Map<String, String> params) throws InvalidMigrationStageError {
        logger.info("received request to deploy application");
        migrationService.transition(MigrationStage.PROVISION_APPLICATION_WAIT);

        logger.info("deploying application stack");
        super.deployCloudformationStack(QUICKSTART_TEMPLATE_URL, deploymentId, params);

        addDeploymentIdToMigrationContext(deploymentId);

        storeDbCredentials(params);
    }

    @Override
    protected void handleFailedDeployment() {
        logger.error("application stack deployment failed");
        migrationService.error();
    }

    @Override
    protected void handleSuccessfulDeployment() {
        try {
            logger.debug("application stack deployment succeeded");
            migrationService.transition(MigrationStage.PROVISION_MIGRATION_STACK);
        } catch (InvalidMigrationStageError invalidMigrationStageError) {
            logger.error("tried to transition migration from {} but got error: {}.", MigrationStage.PROVISION_APPLICATION_WAIT, invalidMigrationStageError.getMessage());
        }
    }

    private void storeDbCredentials(Map<String, String> params) {
        dbCredentialsStorageService.storeCredentials(params.get("DBPassword"));
    }

    private void addDeploymentIdToMigrationContext(String deploymentId) {
        logger.info("Storing stack name in migration context");

        MigrationContext context = migrationService.getCurrentContext();
        context.setApplicationDeploymentId(deploymentId);
        context.save();
    }

    @Override
    public InfrastructureDeploymentStatus getDeploymentStatus() {
        return super.getDeploymentStatus(migrationService.getCurrentContext().getApplicationDeploymentId());
    }
}
