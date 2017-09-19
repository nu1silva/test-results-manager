package org.wso2.qa.testlink.extension.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.qa.testlink.extension.core.CarbonComponentManagement;
import org.wso2.qa.testlink.extension.exception.RepositoryException;

import java.sql.SQLException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Component Management API
 * <p>
 * TODO - Add Basic authentication to restrict API invocation from unauthorized parties.
 */
@Path("/components")
public class CarbonComponentService {

    private static final Logger logger = LoggerFactory.getLogger(CarbonComponentService.class);
    CarbonComponentManagement componentManagment = new CarbonComponentManagement();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}")
    public Response getCarbonComponents(@PathParam("product") String product)
            throws SQLException, RepositoryException {
        logger.info("retrieving components for [" + product + "]");
        return Response.status(200).entity(componentManagment.getCarbonComponents(product)).build();
    }

    @POST
    @Path("/{product}/{component}")
    public Response createComponent(@PathParam("product") String product, @PathParam("component") String component)
            throws SQLException, RepositoryException {
        logger.info("Create component [" + component + "] for product [" + product + "]");
        componentManagment.addCarbonComponent(product, component);
        return Response.status(201).build();
    }

    @DELETE
    @Path("/{product}/{component}")
    public Response deleteComponent(@PathParam("product") String product, @PathParam("component") String component)
            throws SQLException, RepositoryException {
        logger.info("Delete component [" + component + "] from product [" + product + "]");
        componentManagment.removeCarbonComponent(product, component);
        return Response.status(200).build();
    }
}
