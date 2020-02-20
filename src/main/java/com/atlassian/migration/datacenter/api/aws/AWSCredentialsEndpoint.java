package com.atlassian.migration.datacenter.api.aws;

import com.atlassian.migration.datacenter.core.aws.auth.CredentialsFetcher;
import com.atlassian.migration.datacenter.core.aws.auth.CredentialsStorer;
import com.atlassian.migration.datacenter.core.aws.auth.InvalidAWSRegionException;
import com.atlassian.migration.datacenter.core.aws.auth.ProbeAWSAuth;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("aws/credentials")
public class AWSCredentialsEndpoint {

    private final CredentialsStorer credentialsStorer;
    private final CredentialsFetcher credentialsFetcher;
    private final ProbeAWSAuth probe;

    @Autowired
    public AWSCredentialsEndpoint(CredentialsStorer credentialsStorer, CredentialsFetcher credentialsFetcher, ProbeAWSAuth probe) {
        this.credentialsStorer = credentialsStorer;
        this.credentialsFetcher = credentialsFetcher;
        this.probe = probe;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response storeAWSCredentials(AWSCredentialsWebObject credentials) {
        try {
            credentialsStorer.storeRegion(credentials.getRegion());
        } catch(InvalidAWSRegionException e) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }

        credentialsStorer.storeAccessKeyId(credentials.getAccessKeyId());
        credentialsStorer.storeSecretAccessKey(credentials.getSecretAccessKey());

        return Response
                .noContent()
                .build();
    }

    @POST
    @Path("v1/test")
    @Produces(APPLICATION_JSON)
    public Response testCredentialsSDKV1() {
        return Response.ok(probe.probeSDKV1()).build();
    }

    @POST
    @Path("v2/test")
    @Produces(APPLICATION_JSON)
    public Response testCredentialsSDKV2() {
        return Response.ok(probe.probeSDKV2()).build();
    }

    @GET
    @Path("region")
    @Produces(APPLICATION_JSON)
    public Response getRegion() {
        return Response.ok(credentialsFetcher.getRegion()).build();
    }
}
