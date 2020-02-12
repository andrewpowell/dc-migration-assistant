import React, { FunctionComponent, ReactElement, ReactNode, useState, useEffect } from 'react';
import yaml from 'yaml';
import Select from '@atlaskit/select';
import Toggle from '@atlaskit/toggle';

const quickstartUrl =
    'https://dcd-slinghost-templates.s3.amazonaws.com/quickstart-jira-dc-with-vpc.template.parameters.yaml';

type QuickStartParameterYamlNode = {
    Type: string;
    Default: string | number | boolean;
    Description: string;
    AllowedValues?: Array<string | boolean>;
    ConstraintDescription?: string;
    AllowedPattern?: string;
    MaxLength?: number;
    MinLength?: number;
    MaxValue?: number;
    MinValue?: number;
};

/** Quickstart param to atlaskit form component mapping
 *
 * Type: String - Input
 * Type: String, AllowedValues - Select (if values are true or false use Toggle)
 * Type: Number - Input (number)
 * Type: Number, Max/MinValue - Input with min/max
 * Type: String, AllowedPattern/MaxLength/MinLength - Input with constraints
 * Type: List<AWS::EC2::AvailabilityZone::Name> - select with AZ's for region
 */

const createSelectFromQuickstartParam = (
    key: string,
    param: QuickStartParameterYamlNode
): ReactElement => {
    if (param.AllowedValues.length === 2 && typeof param.AllowedValues[0] === 'boolean') {
        return <Toggle key={key} size="large" isDefaultChecked={param.Default as boolean} />;
    }
    return (
        <Select
            key={key}
            options={param.AllowedValues.map(val => ({ label: val, value: val }))}
            value={{ label: param.Default, value: param.Default }}
        />
    );
};

const createAZSelection = (key: string, param: QuickStartParameterYamlNode): ReactElement => {
    return <div key={key} />;
};

const createInputFromQuickstartParam = (
    key: string,
    param: QuickStartParameterYamlNode
): ReactElement => {
    return <div key={key} />;
};

const quickstartParamToAtlaskitFormElement = (
    key: string,
    param: QuickStartParameterYamlNode
): ReactElement => {
    if (param.AllowedValues) {
        return createSelectFromQuickstartParam(key, param);
    }
    if (param.Type === 'List<AWS::EC2::AvailabilityZone::Name>') {
        return createAZSelection(key, param);
    }
    return createInputFromQuickstartParam(key, param);
};

export const QuickstartForm: FunctionComponent = (): ReactElement => {
    const [params, setParams] = useState({});
    const [hasUpdatedTemplate, setHasUpdatedTemplate] = useState(false);

    useEffect(() => {
        if (!hasUpdatedTemplate) {
            fetch(quickstartUrl, {
                method: 'GET',
            })
                .then(resp => resp.text())
                .then(text => {
                    const paramDoc = yaml.parse(text);
                    console.log(paramDoc);
                    setParams(paramDoc.Parameters);
                    setHasUpdatedTemplate(true);
                });
        }
    });

    // console.log(params);

    return (
        <div>
            {Object.entries(params).map((entry: [string, QuickStartParameterYamlNode]) => {
                const [key, value] = entry;
                return quickstartParamToAtlaskitFormElement(key, value);
            })}
        </div>
    );
};
