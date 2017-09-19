package org.wso2.qa.testlink.extension.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.qa.testlink.extension.core.TestResultsUpdater;
import org.wso2.qa.testlink.extension.exception.RepositoryException;
import org.wso2.qa.testlink.extension.exception.TestResultsManagerException;

import java.sql.SQLException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * TestResultManagementService
 * TODO - Add Basic authentication to restrict API invocation from unauthorized parties.
 */
@Path("/update")
public class TestResultManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TestResultManagementService.class);

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/{product}")
    public Response updateResults(@PathParam("product") String product)
            throws TestResultsManagerException, RepositoryException, SQLException {
        logger.info("Updating results for [" + product + "]");
        TestResultsUpdater resultsUpdater = new TestResultsUpdater(product);
        resultsUpdater.update();
        return Response.status(200).build();
    }
}
