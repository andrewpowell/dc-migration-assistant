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
package com.atlassian.migration.datacenter.spi.infrastructure

class ProvisioningConfig(val stackName: String, params: Map<String, Any>, val deploymentMode: DeploymentMode) {
    // We need this unused constructor so that Jackson can deserialize JSON into the provisioning config
    constructor() : this("", mapOf(), DeploymentMode.STANDALONE)

    var params = params.mapValues { it.value.toString() }

    enum class DeploymentMode {
        WITH_NETWORK, STANDALONE;
    }
}