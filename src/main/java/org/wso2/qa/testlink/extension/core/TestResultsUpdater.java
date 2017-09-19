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

import br.eti.kinoshita.testlinkjavaapi.model.Platform;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.qa.testlink.extension.exception.RepositoryException;
import org.wso2.qa.testlink.extension.exception.TestLinkException;
import org.wso2.qa.testlink.extension.exception.TestResultsManagerException;
import org.wso2.qa.testlink.extension.model.CarbonComponent;
import org.wso2.qa.testlink.extension.model.TestResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads test results from the database and update TestLink accordingly.
 */

public class TestResultsUpdater {

    private static Logger logger = Logger.getLogger(TestResultsUpdater.class);

    private String projectName;
    private String testPlanName;
    private long buildNo;
    private List<CarbonComponent> carbonComponents = new ArrayList<CarbonComponent>();

    CarbonComponentManagement componentManagement = new CarbonComponentManagement();

    public TestResultsUpdater(String projectName, String testPlanName, long buildNo,
                              List<CarbonComponent> carbonComponents) {

        this.projectName = projectName;
        this.testPlanName = testPlanName;
        this.buildNo = buildNo;
        this.carbonComponents = carbonComponents;
    }

    public TestResultsUpdater(String projectName) {
        this.projectName = projectName;
    }

    public void update() throws TestResultsManagerException, SQLException, RepositoryException {

        TestResultsRepository testResultsRepository = new TestResultsRepository();

        Map<String, List<TestResult>> testResults = null;

        logger.info("retrieving components available for product " + projectName);
        List<CarbonComponent> componentList = componentManagement.getCarbonComponents(projectName);

        logger.info("retrieving the latest version available for the components");
        for (CarbonComponent carbonComponent : componentList) {
            carbonComponent.setComponentVersion(String.valueOf(
                    componentManagement.getCarbonComponentWithLatestBuild(carbonComponent.getComponentName())));
            logger.info(carbonComponent.toString());
        }

        try {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Retrieving test results for the build number '%d' and carbon " +
                        "components '%s'", buildNo, carbonComponents));
            }

            testResults = testResultsRepository.getResults(buildNo, carbonComponents);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Retrieved %d test results for the build number '%d' and carbon" +
                        " components '%s'", testResults.size(), buildNo, carbonComponents));
            }
        } catch (RepositoryException e) {
            throw new TestResultsManagerException("Error occurred while retrieving test results from the" +
                    " repository", e);
        }

        TestLinkClient testLinkClient = null;
        try {
            testLinkClient = new TestLinkClient(testPlanName, projectName, buildNo);
        } catch (TestLinkException e) {
            throw new TestResultsManagerException("Error occurred while connecting to TestLink", e);
        }

        TestCase[] testCases = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Retrieving test cases for the test plan name '%s' and the project" +
                        " name '%s'", testPlanName, projectName));
            }

            testCases = testLinkClient.getTestCases();

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Retrieved %d test cases for the test plan name '%s' and the project" +
                        " name '%s'", testCases.length, testPlanName, projectName));
            }
        } catch (TestLinkException e) {
            throw new TestResultsManagerException("Error occurred while retrieving test cases from TestLink", e);
        }

        Platform[] platforms = testLinkClient.getPlatforms();

        if (logger.isDebugEnabled()) {
            logger.debug("Available platforms : " + StringUtils.join(platforms, ","));
        }

        // Let the processing begin!!!
        Processor processor = new Processor(testResults, testCases, platforms);

        // This might get removed and get pushed to a database
        List<TestResult> updatedTestResults = processor.getProcessedResults();

        try {
            testLinkClient.updateTestExecution(updatedTestResults);
        } catch (TestLinkException e) {
            throw new TestResultsManagerException("Error occurred while updating test execution results" +
                    " in TestLink", e);
        }

        //Todo To be removed : Added to print test execution object arrayList
        for (TestResult executionResult : updatedTestResults) {
            logger.info("ExecutionResult : " + executionResult.toString());
        }
    }
}
