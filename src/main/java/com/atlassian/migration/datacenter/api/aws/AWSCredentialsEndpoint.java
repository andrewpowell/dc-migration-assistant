package com.atlassian.migration.datacenter.api.aws;

import com.atlassian.migration.datacenter.core.aws.auth.CredentialStorage;
import com.atlassian.migration.datacenter.core.aws.auth.ProbeAWSAuth;
import lombok.Data;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("aws/credentials")
public class AWSCredentialsEndpoint {

    private final static Logger LOGGER = Logger.getLogger(AWSCredentialsEndpoint.class);

    private final CredentialStorage credentialsManagement;
    private final ProbeAWSAuth probe;

    @Autowired
    public AWSCredentialsEndpoint(CredentialStorage credentialsManagement, ProbeAWSAuth probe) {
        this.credentialsManagement = credentialsManagement;
        this.probe = probe;
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response getAWSCredentials() {
        AWSCredentialsWebObject dto = new AWSCredentialsWebObject();
        dto.setAccessKeyId(credentialsManagement.getAccessKeyId());
        dto.setSecretAccessKey(credentialsManagement.getSecretAccessKey());
        if (dto.getAccessKeyId() == null || dto.getSecretAccessKey() == null) {
            LOGGER.error("Failed to retrieve AWS Credentials from plugin storage.");
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } else {
            return Response.ok(dto).build();
        }
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response storeAWSCredentials(AWSCredentialsWebObject credentials) {
        credentialsManagement.setAccessKeyId(credentials.getAccessKeyId());
        credentialsManagement.setSecretAccessKey(credentials.getSecretAccessKey());

        return Response
                .ok()
                .build();
    }

    @POST
    @Path("v1/test")
    @Produces(APPLICATION_JSON)
    public Response testCredentialsSDKV1() {
        return Response.ok(probe.probeSDKV1()).build();
    }

    @Data
    @JsonAutoDetect
    static class AWSCredentialsWebObject {
        private String accessKeyId;
        private String secretAccessKey;
        private String region;
    }

}
