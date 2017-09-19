package org.wso2.qa.testlink.extension.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.qa.testlink.extension.exception.RepositoryException;
import org.wso2.qa.testlink.extension.model.CarbonComponent;
import org.wso2.qa.testlink.extension.model.Configurations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CarbonComponentManagement
 */
public class CarbonComponentManagement {

    private static final String CARBON_COMPONENTS_TABLE = "carbonComponents";
    private static final Logger logger = LoggerFactory.getLogger(CarbonComponentManagement.class);

    Configurations configuration = Configurations.getInstance();

    /**
     * Get the component list by giving the product/project name
     *
     * @param productName
     * @return List of components for the product
     * @throws RepositoryException
     * @throws SQLException
     */
    public List<CarbonComponent> getCarbonComponents(String productName) throws RepositoryException, SQLException {

        List<CarbonComponent> components = new ArrayList<>();
        Connection connection = getDBConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            String queryForPreparedStatement = "SELECT * FROM " + CARBON_COMPONENTS_TABLE + " WHERE product = ?";
            preparedStatement = connection.prepareStatement(queryForPreparedStatement);
            preparedStatement.setString(1, productName);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                CarbonComponent carbonComponent = new CarbonComponent(resultSet.getString("component"));
                components.add(carbonComponent);
            }
        } finally {

            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException ignore) {
                }
            }

            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignore) {
                }
            }

            try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return components;
    }

    /**
     * Add a component for a specific product
     *
     * @param productName
     * @param component
     */
    public void addCarbonComponent(String productName, String component) throws RepositoryException, SQLException {
        Connection connection = getDBConnection();

        PreparedStatement preparedStatement = null;

        try {
            final String queryForPreparedStatement = "INSERT INTO " + CARBON_COMPONENTS_TABLE +
                    " VALUES(?,?)";
            preparedStatement = connection.prepareStatement(queryForPreparedStatement);
            preparedStatement.setString(1, productName);
            preparedStatement.setString(2, component);
            preparedStatement.execute();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignore) {
                }
            }

            try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    /**
     * retrieve a list of carbon components with its latest version
     *
     * @param component
     * @return
     */
    public CarbonComponent getCarbonComponentWithLatestBuild(String component)
            throws RepositoryException, SQLException {

        Connection connection = getDBConnection();
        CarbonComponent carbonComponent = new CarbonComponent();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            final String queryForPreparedStatement = "SELECT * FROM " + configuration.getTableName() + " WHERE " +
                    "component = ? AND buildNo = (select max(buildNo) as buildNo from " +
                    configuration.getTableName() + " where component = ?)";

            preparedStatement = connection.prepareStatement(queryForPreparedStatement);
            preparedStatement.setString(1, component);
            preparedStatement.setString(2, component);
            preparedStatement.execute();

            while (resultSet.next()) {
                carbonComponent.setComponentName(component);
                carbonComponent.setComponentVersion(resultSet.getString("buildNo"));
            }
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignore) {
                }
            }

            try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return carbonComponent;
    }

    /**
     * Delete a component
     *
     * @param productName
     * @param component
     * @throws SQLException
     * @throws RepositoryException
     */
    public void removeCarbonComponent(String productName, String component)
            throws SQLException, RepositoryException {
        Connection connection = getDBConnection();

        PreparedStatement preparedStatement = null;

        try {
            final String queryForPreparedStatement = "DELETE FROM " + CARBON_COMPONENTS_TABLE +
                    " WHERE product = ? AND component = ?";
            preparedStatement = connection.prepareStatement(queryForPreparedStatement);
            preparedStatement.setString(1, productName);
            preparedStatement.setString(2, component);
            preparedStatement.execute();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignore) {
                }
            }

            try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    // Create Database Connection
    public Connection getDBConnection() throws RepositoryException {
        Connection dbConnection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RepositoryException("Database driver could not be loaded", e);
        }

        Configurations configurations = Configurations.getInstance();

        String connectionURL = String.format("jdbc:mysql://%s:%s/%s",
                configurations.getDatabaseHost(), configurations.getDatabasePort(), configurations.getDatabaseName());

        String databaseUsername = configurations.getDatabaseUsername();
        String databasePassword = configurations.getDatabasePassword();

        try {
            dbConnection = DriverManager.getConnection(connectionURL, databaseUsername, databasePassword);
            return dbConnection;
        } catch (SQLException e) {
            throw new RepositoryException("Cannot connect to database " + connectionURL, e);
        }
    }

}
