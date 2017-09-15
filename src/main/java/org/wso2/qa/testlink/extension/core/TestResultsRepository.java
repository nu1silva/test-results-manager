/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.qa.testlink.extension.core;

import org.apache.commons.lang.StringUtils;
import org.wso2.qa.testlink.extension.exception.RepositoryException;
import org.wso2.qa.testlink.extension.model.CarbonComponent;
import org.wso2.qa.testlink.extension.model.Configurations;
import org.wso2.qa.testlink.extension.model.TestResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// read configs.properties

/**
 * Represents the test results database which is populated at the end of a Jenkins build.
 */
public class TestResultsRepository {


    private static String queryForPreparedStatementUT;
    private static String queryForPreparedStatementIT;

    public Map<String, List<TestResult>> getResults(long buildNo, List<CarbonComponent> carbonComponents)
            throws RepositoryException {

        try {
            // Loading MySQL JDBC driver.
            Class.forName("com.mysql.jdbc.Driver");

        } catch (ClassNotFoundException e) {
            throw new RepositoryException("Database driver could not be loaded", e);
        }

        Configurations configurations = Configurations.getInstance();

        Connection connection = null;


        String connectionURL = String.format("jdbc:mysql://%s:%s/%s",
                configurations.getDatabaseHost(), configurations.getDatabasePort(), configurations.getDatabaseName());

        String databaseUsername = configurations.getDatabaseUsername();
        String databasePassword = configurations.getDatabasePassword();

        try {
            //Todo : Read database connection configurations from a config file
            connection = DriverManager.getConnection(connectionURL, databaseUsername, databasePassword);
        } catch (SQLException e) {
            throw new RepositoryException("Cannot connect to database " + connectionURL, e);
        }

        Map<String, List<TestResult>> testResults;

        try {
            //Get integration test results from the database.
            testResults = getIntegrationTestResults(buildNo, connection);

            // Get unit test results from the database.
            testResults.putAll(getUnitTestResults(carbonComponents, connection));

        } catch (SQLException e) {
            throw new RepositoryException("Cannot fetch test results from the database " + connectionURL, e);
        } finally {
            try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }

        return testResults;
    }

    private Map<String, List<TestResult>> getUnitTestResults(List<CarbonComponent> carbonComponents,
                                                             Connection connection) throws SQLException {

        Configurations configurations = Configurations.getInstance();
        Map<String, List<TestResult>> unitTestResults = new HashMap<String, List<TestResult>>();

        // Return if the components are not available.
        if (carbonComponents == null || carbonComponents.isEmpty()) {
            return unitTestResults;
        }


        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            String queryForPreparedStatement = "SELECT * FROM " + configurations.getTableName() +
                    " WHERE (component,version) IN (%s)";

            StringBuilder componentNameAndVersionParis = new StringBuilder();

            for (CarbonComponent carbonComponent : carbonComponents) {
                componentNameAndVersionParis.append("('");
                componentNameAndVersionParis.append(carbonComponent.getComponentName());
                componentNameAndVersionParis.append("','");
                componentNameAndVersionParis.append(carbonComponent.getComponentVersion());
                componentNameAndVersionParis.append("'),");
            }

            // Remove the trailing , character before setting in the query.
            queryForPreparedStatementUT = String.format(queryForPreparedStatement,
                    StringUtils.removeEnd(componentNameAndVersionParis.toString(), ","));

            preparedStatement = connection.prepareStatement(queryForPreparedStatementUT);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                TestResult testResult = new TestResult();
                testResult.setTestMethod(resultSet.getString("test"));
                testResult.setStatus(resultSet.getString("status"));
                testResult.setPlatform("ANY");

                // Map<String, List<TestResult>> data structure is not 100% matching for the unit tests,
                // since there is not a 'platform' concept for unit tests.

                // TODO : Review the data structure to see whether we need two data structures for
                // TODO : cont. integration tests and unit tests.

                List testResultsList = new ArrayList<TestResult>();
                testResultsList.add(testResult);
                unitTestResults.put(testResult.getTestMethod(), testResultsList);
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
        }

        return unitTestResults;
    }


    private Map<String, List<TestResult>> getIntegrationTestResults(long buildNo, Connection connection)
            throws SQLException {

        Configurations configurations = Configurations.getInstance();
        Map<String, List<TestResult>> testResults = new HashMap<String, List<TestResult>>();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            queryForPreparedStatementIT = String.format("SELECT * FROM %s WHERE buildNo=? ",
                    configurations.getTableName());
            preparedStatement = connection.prepareStatement(queryForPreparedStatementIT);
            preparedStatement.setLong(1, buildNo);
            resultSet = preparedStatement.executeQuery();

            //Extracting data from the result set
            while (resultSet.next()) {
                TestResult testResult = new TestResult();
                testResult.setPlatform(resultSet.getString("platform"));
                testResult.setTestMethod(resultSet.getString("test"));
                testResult.setStatus(resultSet.getString("status"));

                //Assign (list) value of the map for a given key into a variable.
                List<TestResult> resultsForPlatforms = testResults.get(testResult.getTestMethod());

                //If Not exist a key value pair, add a map entry by setting a key and an empty list
                if (resultsForPlatforms == null) {
                    resultsForPlatforms = new ArrayList<TestResult>();
                    testResults.put(testResult.getTestMethod(), resultsForPlatforms);
                }

                //Add Result to the list
                resultsForPlatforms.add(testResult);
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
        }

        return testResults;
    }
}
