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
package com.atlassian.migration.datacenter.api.aws

import com.atlassian.migration.datacenter.spi.MigrationService
import com.atlassian.migration.datacenter.spi.MigrationStage
import com.atlassian.migration.datacenter.spi.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.spi.infrastructure.*
import org.slf4j.LoggerFactory
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * REST API Endpoint for managing AWS provisioning.
 */
@Path("/aws/stack")
class CloudFormationEndpoint(private val deploymentService: ApplicationDeploymentService, private val migrationService: MigrationService, private val helperDeploymentService: MigrationInfrastructureDeploymentService) {
    companion object {
        private val log = LoggerFactory.getLogger(CloudFormationEndpoint::class.java)
    }

    val PENDING_MIGRATION_INFR_STATUS = "PREPARING_MIGRATION_INFRASTRUCTURE_DEPLOYMENT"

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun provisionInfrastructure(provisioningConfig: ProvisioningConfig): Response {
        return try {
            val stackName = provisioningConfig.stackName
            deploymentService.deployApplication(stackName, provisioningConfig.params)
            //Should be updated to URI location after get stack details Endpoint is built
            Response.status(Response.Status.ACCEPTED).entity(stackName).build()
        } catch (e: InvalidMigrationStageError) {
            log.error("Migration stage is not valid.", e)
            Response
                .status(Response.Status.CONFLICT)
                .entity(mapOf("error" to e.message))
                .build()
        }
    }

    @GET
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun infrastructureStatus(): Response {
        if (migrationService.currentStage == MigrationStage.PROVISION_APPLICATION_WAIT) {
            return try {
                val status = deploymentService.deploymentStatus
                Response.ok(mapOf("status" to status, "phase" to "app_infra")).build()
            } catch (e: Exception) {
                Response.status(Response.Status.NOT_FOUND).entity(mapOf("error" to e.message)).build()
            }
        }
        if (migrationService.currentStage == MigrationStage.PROVISION_MIGRATION_STACK) {
            return Response.ok(mapOf("status" to PENDING_MIGRATION_INFR_STATUS)).build()
        }
        if (migrationService.currentStage == MigrationStage.PROVISION_MIGRATION_STACK_WAIT) {
            return try {
                val status = helperDeploymentService.deploymentStatus
                Response.ok(mapOf("status" to status, "phase" to "migration_infra")).build()
            } catch (e: Exception) {
                Response.status(Response.Status.NOT_FOUND).entity(mapOf("error" to e.message)).build()
            }        }
        return Response.status(Response.Status.NOT_FOUND).entity(mapOf("error" to "not currently deploying any infrastructure")).build()
    }
}