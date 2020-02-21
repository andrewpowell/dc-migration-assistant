package com.atlassian.migration.datacenter.core.aws.auth;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Component
public class AtlassianPluginAWSCredentialsProvider implements AwsCredentialsProvider, AWSCredentialsProvider {

    private final ReadCredentialsService readCredentialsService;

    @Autowired
    public AtlassianPluginAWSCredentialsProvider(ReadCredentialsService readCredentialsService) {
        this.readCredentialsService = readCredentialsService;
    }

    /**
     * AWS SDK V1 credentials API
     * @return AWS Credentials to be used with SDK V1 clients
     */
    @Override
    public AWSCredentials getCredentials() {
        if (accessKeyIsDefined() && secretKeyIsDefined()) {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return readCredentialsService.getAccessKeyId();
                }

                @Override
                public String getAWSSecretKey() {
                    return readCredentialsService.getSecretAccessKey();
                }
            };
        }
        return new DefaultAWSCredentialsProviderChain().getCredentials();
    }

    private boolean secretKeyIsDefined() {
        return readCredentialsService.getSecretAccessKey() != null && !readCredentialsService.getSecretAccessKey().equals("");
    }

    private boolean accessKeyIsDefined() {
        return readCredentialsService.getAccessKeyId() != null && !readCredentialsService.getAccessKeyId().equals("");
    }

    /**
     * AWS SDK V1 credentials API
     * Refreshes the AWS credentials provided by this provider
     */
    @Override
    public void refresh() {
        new DefaultAWSCredentialsProviderChain().refresh();
    }

    /**
     * AWS SDK V2 credentials API
     * @return AWS Credentials to be used with SDK V2 clients
     */
    @Override
    public AwsCredentials resolveCredentials() {
        if(accessKeyIsDefined() && secretKeyIsDefined()) {
            return new AwsCredentials() {
                @Override
                public String accessKeyId() {
                    return readCredentialsService.getAccessKeyId();
                }

                @Override
                public String secretAccessKey() {
                    return readCredentialsService.getSecretAccessKey();
                }
            };
        }
        return DefaultCredentialsProvider.create().resolveCredentials();
    }
}
