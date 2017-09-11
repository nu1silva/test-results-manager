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

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionStatus;
import br.eti.kinoshita.testlinkjavaapi.constants.ResponseDetails;
import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseDetails;
import br.eti.kinoshita.testlinkjavaapi.model.Build;
import br.eti.kinoshita.testlinkjavaapi.model.CustomField;
import br.eti.kinoshita.testlinkjavaapi.model.Platform;
import br.eti.kinoshita.testlinkjavaapi.model.ReportTCResultResponse;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan;
import br.eti.kinoshita.testlinkjavaapi.model.TestProject;
import br.eti.kinoshita.testlinkjavaapi.util.TestLinkAPIException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.wso2.qa.testlink.extension.exception.TestLinkException;
import org.wso2.qa.testlink.extension.model.Configurations;
import org.wso2.qa.testlink.extension.model.TestResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * To retrieve and manipulate test plans in TestLink.
 */

public class TestLinkClient {

    private static final Logger logger = Logger.getLogger(TestLinkClient.class);

    private String projectName;
    private String testPlanName;
    private long buildNo;
    private int buildID;

    private TestLinkAPI testLinkAPI;
    private TestPlan testPlan;
    private Platform[] platforms;

    public TestLinkClient(String testPlanName, String projectName, long buildNo) throws TestLinkException {

        this.testPlanName = testPlanName;
        this.projectName = projectName;
        this.buildNo = buildNo;
        this.init();
    }

    // Returns array of test cases retrieve from a test plan
    public TestCase[] getTestCases() throws TestLinkException {

        //Get the list of automated test cases from the test plan
        TestCase[] testCasesOfTestPlan = testLinkAPI.getTestCasesForTestPlan(
                testPlan.getId(),
                null,       /* test cases IDs*/
                null,    /* build ID */
                null,       /* key words IDs */
                null,   /* Key words */
                false,  /* executed */
                null,   /* assigned To */
                null,   /* Executed status */
                null,
                false,                  /*get step info */
                TestCaseDetails.FULL); /* Test case Detail */

        TestProject testProject = testLinkAPI.getTestProjectByName(projectName);


        // NOTE : TestLink API doesn't add custom fields to the test cases when querying.
        //        So we need to do another API call to fetch those.
        Configurations configurations = Configurations.getInstance();
        for (TestCase automatedTestCase : testCasesOfTestPlan) {
            CustomField customFieldForIntegrationTestMethods = testLinkAPI.getTestCaseCustomFieldDesignValue(
                    automatedTestCase.getId(),
                    null, /* testCaseExternalId */
                    automatedTestCase.getVersion(),
                    testProject.getId(),
                    //Constants.CUSTOM_FIELD_INTEGRATION_TEST, todo remove
                    configurations.getIntegrationTestCustomFieldName(),
                    ResponseDetails.FULL);

            //Adding the custom for integration tests.
            automatedTestCase.getCustomFields().add(customFieldForIntegrationTestMethods);

            CustomField customFieldForUnitTestMethods = testLinkAPI.getTestCaseCustomFieldDesignValue(
                    automatedTestCase.getId(),
                    null, /* testCaseExternalId */
                    automatedTestCase.getVersion(),
                    testProject.getId(),
                    //Constants.CUSTOM_FIELD_UNIT_TEST, todo remove
                    configurations.getUnitTestCustomFieldName(),
                    ResponseDetails.FULL);

            //Adding the custom field for unit tests.
            automatedTestCase.getCustomFields().add(customFieldForUnitTestMethods);

        }

        return testCasesOfTestPlan;
    }

    Platform[] getPlatforms() {
        return platforms;
    }

    public void updateTestExecution(List<TestResult> testResultList) throws TestLinkException {

        createBuild();
        List<TestResult> testResults = testResultList;
        TestLinkAPI api = connectToTestLink();

        for (TestResult testResult : testResults) {

            int platformId = getPlatformIdByName(testResult.getPlatform());

            if (platformId != -1) {

                ExecutionStatus executionStatus = convertToExecutionStatus(testResult.getStatus());
                if (!executionStatus.equals(ExecutionStatus.NOT_RUN)) {
                    ReportTCResultResponse response = api.setTestCaseExecutionResult(testResult.getTestCaseId(),
                            null,   //test case external id
                            testPlan.getId(), // test plan id
                            executionStatus, // Executed status
                            buildID,        // build ID
                            Long.toString(buildID),  // Build name
                            null,
                            null,
                            null,
                            platformId,
                            null,
                            null,
                            null);
                }
            } else {
                //Todo This is for temporary error handling
                logger.info("No platform id found for :" + testResult.getPlatform());
            }
        }
    }

    private void init() throws TestLinkException {
        testLinkAPI = connectToTestLink();
        // Fetch master data
        testPlan = testLinkAPI.getTestPlanByName(testPlanName, projectName);
        platforms = testLinkAPI.getTestPlanPlatforms(testPlan.getId());
    }

    private TestLinkAPI connectToTestLink() throws TestLinkException {

        Configurations configurations = Configurations.getInstance();

        String url = configurations.getTestLinkAPIURL();
        String devKey = configurations.getTestLinkAPIKey();
        TestLinkAPI api = null;
        URL testLinkURL = null;

        try {
            testLinkURL = new URL(url);
            logger.debug(String.format("Connecting to TestLink. API URL : '%s', MD5 of the API key : '%s'",
                    url, Arrays.toString(DigestUtils.md5(devKey))));
            api = new TestLinkAPI(testLinkURL, devKey);
            logger.debug("Connected to TestLink successfully.");
        } catch (MalformedURLException e) {
            throw new TestLinkException("TestLink connection URL is incorrect", e);
        } catch (TestLinkAPIException e) {
            throw new TestLinkException("Cannot connect to TestLink", e);
        }
        return api;
    }

    private void createBuild() {
        Build build = testLinkAPI.createBuild(testPlan.getId(), Long.toString(buildNo),
                "Automatically Created by Test-Results-Manager.");
        buildID = build.getId();
    }

    private ExecutionStatus convertToExecutionStatus(String status) {

        if (status.equals("PASS")) {
            return ExecutionStatus.PASSED;

        } else if (status.equals("FAIL")) {
            return ExecutionStatus.FAILED;

        } else {
            return ExecutionStatus.NOT_RUN;
        }
    }

    private int getPlatformIdByName(String platformName) throws TestLinkException {

        for (Platform platform : this.platforms) {
            if (platform.getName().equals(platformName)) {
                return platform.getId();
            }
        }
        return -1;
    }
}
