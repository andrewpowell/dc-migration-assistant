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

import com.atlassian.migration.datacenter.core.exceptions.InvalidMigrationStageError
import com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService
import com.atlassian.migration.datacenter.spi.infrastructure.ApplicationDeploymentService.ApplicationDeploymentStatus
import com.atlassian.migration.datacenter.spi.infrastructure.ProvisioningConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.services.cloudformation.model.StackInstanceNotFoundException
import javax.ws.rs.core.Response

@ExtendWith(MockKExtension::class)
internal class CloudFormationEndpointTest {
    @MockK(relaxUnitFun = true)
    lateinit var deploymentService: ApplicationDeploymentService

    @InjectMockKs
    lateinit var endpoint: CloudFormationEndpoint

    @BeforeEach
    fun init() = MockKAnnotations.init(this)

    @Test
    fun shouldAcceptRequestToProvisionCloudFormationStack() {
        val stackName = "stack-name"
        val provisioningConfig = ProvisioningConfig("url", stackName, HashMap())
        val response = endpoint.provisionInfrastructure(provisioningConfig)
        assertEquals(Response.Status.ACCEPTED.statusCode, response.status)
        assertEquals(provisioningConfig.stackName, response.entity)
        verify { deploymentService.deployApplication(stackName, HashMap()) }
    }

    @Test
    fun shouldBeConflictWhenCurrentMigrationStageIsNotValid() {
        val provisioningConfig = ProvisioningConfig("url", "stack-name", HashMap())
        val errorMessage = "migration status is FUBAR"
        every {
            deploymentService.deployApplication(
                provisioningConfig.stackName,
                provisioningConfig.params
            )
        } throws InvalidMigrationStageError(errorMessage)
        val response = endpoint.provisionInfrastructure(provisioningConfig)
        assertEquals(Response.Status.CONFLICT.statusCode, response.status)
        assertEquals(errorMessage, (response.entity as Map<*, *>)["error"])
    }

    @Test
    fun shouldGetCurrentProvisioningStatusForGivenStackId() {
        val expectedStatus = ApplicationDeploymentStatus.CREATE_IN_PROGRESS
        every { deploymentService.deploymentStatus } returns expectedStatus
        val response = endpoint.infrastructureStatus()
        assertEquals(Response.Status.OK.statusCode, response.status)
        assertEquals(expectedStatus, (response.entity as Map<*, *>)["status"])
    }

    @Test
    fun shouldGetHandleErrorWhenStatusCannotBeRetrieved() {
        val expectedErrorMessage = "stack Id not found"
        every { deploymentService.deploymentStatus } throws StackInstanceNotFoundException.builder()
            .message(expectedErrorMessage).build()
        val response = endpoint.infrastructureStatus()
        assertEquals(Response.Status.NOT_FOUND.statusCode, response.status)
        assertEquals(expectedErrorMessage, (response.entity as Map<*, *>)["error"])
    }
}