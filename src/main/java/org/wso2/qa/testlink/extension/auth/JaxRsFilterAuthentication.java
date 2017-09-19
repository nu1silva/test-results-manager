package org.wso2.qa.testlink.extension.auth;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import java.io.IOException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

/**
 *  auth
 */
@Provider
public class JaxRsFilterAuthentication implements ContainerRequestFilter {

    public static final String AUTHENTICATION_HEADER = "Authorization";

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        String authCredentials = containerRequest.getHeaderValue(AUTHENTICATION_HEADER);

        // better injected
        AuthenticationService authenticationService = new AuthenticationService();

        boolean authenticationStatus = false;
        try {
            authenticationStatus = authenticationService
                    .authenticate(authCredentials);
        } catch (IOException e) {
            try {
                throw new IOException("IO", e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        if (!authenticationStatus) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return containerRequest;
    }
}
