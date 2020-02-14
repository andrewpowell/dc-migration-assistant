import React, { FunctionComponent, ReactElement } from 'react';
import Form, { ErrorMessage, Field, HelperMessage } from '@atlaskit/form';
import Button from '@atlaskit/button';
import styled from 'styled-components';
import TextField from '@atlaskit/textfield';
import { I18n } from '@atlassian/wrm-react-i18n';
import { AsyncSelect, OptionType } from '@atlaskit/select';

type AWSCreds = {
    accessKeyID: string;
    secretAccessKey: string;
    region: string;
};

/*
Function to submit the AWS credentials. Should return a promise which resolves when the
credentials have been submitted. Should reject with an error message if there was an error submitting the credentials.
*/
type CredSubmitFun = (creds: AWSCreds) => Promise<string>;

/*
Function to get all AWS regions. Should return a promise which resolves
with the AWS regions
*/
type QueryRegionFun = () => Promise<Array<string>>;

type AuthenticateAWSProps = {
    onSubmitCreds: CredSubmitFun;
    getRegions: QueryRegionFun;
};

const CredsSubmitButton = styled(Button)`
    margin-top: 10px;
`;

const RegionSelect: FunctionComponent<{ getRegions: QueryRegionFun }> = (props): ReactElement => {
    const { getRegions } = props;

    // This will be replaced by an API call
    const promiseOptions = (): Promise<Array<OptionType>> => {
        return getRegions().then(regions => {
            return regions.map(region => ({ label: region, value: region, key: region }));
        });
    };

    const LargeAsyncSelect = styled(AsyncSelect)`
        width: 480.4px;
    `;

    return (
        <LargeAsyncSelect
            {...props}
            cacheOptions
            defaultOptions
            isSearchable
            loadOptions={promiseOptions}
        />
    );
};

export const AuthenticateAWS: FunctionComponent<AuthenticateAWSProps> = ({
    onSubmitCreds,
    getRegions,
}): ReactElement => {
    const submitCreds = (formCreds: {
        accessKeyID: string;
        secretAccessKey: string;
        region: OptionType;
    }): void => {
        const { accessKeyID, secretAccessKey, region } = formCreds;
        const creds: AWSCreds = {
            accessKeyID,
            secretAccessKey,
            region: region.value as string,
        };
        onSubmitCreds(creds);
    };

    return (
        <>
            <h1>{I18n.getText('aws.migration.authenticate.aws.title')}</h1>
            <Form onSubmit={submitCreds}>
                {({ formProps }: any): ReactElement => (
                    <form {...formProps}>
                        <Field
                            isRequired
                            label={I18n.getText('aws.migration.authenticate.aws.accessKeyId.label')}
                            name="accessKeyID"
                            defaultValue=""
                        >
                            {({ fieldProps }: any): ReactElement => (
                                <TextField width="xlarge" {...fieldProps} />
                            )}
                        </Field>
                        <Field
                            isRequired
                            label={I18n.getText(
                                'aws.migration.authenticate.aws.secretAccessKey.label'
                            )}
                            name="secretAccessKey"
                            defaultValue=""
                        >
                            {({ fieldProps }: any): ReactElement => (
                                <TextField width="xlarge" {...fieldProps} />
                            )}
                        </Field>
                        <Field
                            label={I18n.getText('aws.migration.authenticate.aws.region.label')}
                            name="region"
                            validate={(value: OptionType): string => {
                                return value ? undefined : 'NO_REGION';
                            }}
                        >
                            {({ fieldProps, error }: any): ReactElement => (
                                <>
                                    <HelperMessage>
                                        {I18n.getText(
                                            'aws.migration.authenticate.aws.region.helper'
                                        )}
                                    </HelperMessage>
                                    <RegionSelect getRegions={getRegions} {...fieldProps} />
                                    {error && (
                                        <ErrorMessage>
                                            {I18n.getText(
                                                'aws.migration.authenticate.aws.region.error'
                                            )}
                                        </ErrorMessage>
                                    )}
                                </>
                            )}
                        </Field>
                        <CredsSubmitButton type="submit" appearance="primary">
                            {I18n.getText('aws.migration.generic.submit')}
                        </CredsSubmitButton>
                    </form>
                )}
            </Form>
        </>
    );
};
