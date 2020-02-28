package com.atlassian.migration.datacenter.api.fs;

import com.atlassian.migration.datacenter.spi.fs.FilesystemMigrationService;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/migration/fs")
public class FileSystemMigrationProgressEndpoint {

    private final FilesystemMigrationService migrationService;

    public FileSystemMigrationProgressEndpoint(FilesystemMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response getFilesystemMigrationStatus() throws JsonProcessingException {
        FileSystemMigrationReport report = migrationService.getReport();
        if (report == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("no file system migration exists")
                    .build();
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);

        return Response
                .ok(mapper.writeValueAsString(report))
                .build();
    }
}
