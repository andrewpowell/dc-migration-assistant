package com.aws.awsmigrationplugin;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.permission.PermissionEnforcer;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TemplateServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServlet.class);

    private final SoyTemplateRenderer templateRenderer;
    private final PermissionEnforcer permissionEnforcer;
    private final LoginUriProvider loginUriProvider;

    public TemplateServlet(@ComponentImport final SoyTemplateRenderer templateRenderer,
                           @ComponentImport final PermissionEnforcer permissionEnforcer,
                           @ComponentImport final LoginUriProvider loginUriProvider) {
        this.templateRenderer = templateRenderer;
        this.permissionEnforcer = permissionEnforcer;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (permissionEnforcer.isSystemAdmin()) {
                render(response);
            } else if (permissionEnforcer.isAuthenticated()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                response.sendRedirect(loginUriProvider.getLoginUri(URI.create(request.getRequestURL().toString())).toASCIIString());
            }
        } catch (IOException exception) {
            logger.error("Unable to render template", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void render(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_HTML);
        templateRenderer.render(
            response.getWriter(),
            "com.aws.awsmigrationplugin.aws-migration-plugin:aws-migration-plugin-templates",
            "awsmigrationplugin.init", 
            Collections.emptyMap()
        );
    }
}
