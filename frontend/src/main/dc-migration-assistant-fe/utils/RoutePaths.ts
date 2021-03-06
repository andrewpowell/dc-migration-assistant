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

// eslint-disable-next-line import/no-unresolved
import contextPath from 'wrm/context-path';

const routePrefix = ((): string => {
    const awsMigrationServletPath = '/plugins/servlet/dc-migration-assistant';
    const pathname = contextPath(); // eslint-disable-line no-undef

    if (pathname.includes(awsMigrationServletPath)) {
        return pathname;
    }

    return `${pathname}${awsMigrationServletPath}`;
})();

export const homePath = `${routePrefix}/home`;

export const awsBasePath = `${routePrefix}/aws`;
export const asiConfigurationPath = `${awsBasePath}/asi`;
export const quickstartPath = `${awsBasePath}/provision`;
export const quickstartStatusPath = `${awsBasePath}/provision/status`;
export const awsAuthPath = `${awsBasePath}/auth`;

export const fsPath = `${routePrefix}/fs`;
export const finalSyncPath = `${routePrefix}/final-sync`;

export const warningPath = `${routePrefix}/warning`;

export const validationPath = `${routePrefix}/validation`;

export const migrationErrorPath = `${routePrefix}/migration-error`;
