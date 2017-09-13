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
package org.wso2.qa.testlink.extension.web;

import org.apache.log4j.Logger;
import org.wso2.qa.testlink.extension.exception.TestResultsManagerException;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The error handler servlet, which logs the exception properly and sends out the error message.
 */
public class ErrorHandlerServlet extends HttpServlet {

    private static final long serialVersionUID = 42L;

    private static final Logger LOGGER = Logger.getLogger(ErrorHandlerServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String servletName = (String) request.getAttribute("javax.servlet.error.servlet_name");

        if (servletName == null) {
            servletName = "Unknown";
        }

        String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
        if (requestUri == null) {
            requestUri = "Unknown";
        }

        String responsePayload;
        String errorMessage = String.format("Error occurred while serving the request. Status Code :" +
                " '%d' Servlet Name : '%s' Request URI : '%s'", statusCode, servletName, requestUri);

        // If the immediate cause is a TestResultsManagerException, the log message and the response payload should be
        // based on whether the error is a runtime error or not.
        // Runtime errors are not logged in this class since they should be logged where they originated and
        // the response payload should contain any 'Exception' related info.
        if (throwable instanceof TestResultsManagerException) {

            TestResultsManagerException testResultsManagerException = (TestResultsManagerException) throwable;

            if (!testResultsManagerException.isRuntimeError()) {
                responsePayload = String.format("{\"reason\":\"%s\", \"exceptionClass\":\"%s\"}",
                        throwable.getMessage(), throwable.getCause().getClass().getName());
                LOGGER.error(errorMessage, throwable);
            } else {
                responsePayload = String.format("{\"reason\":\"%s\"}", testResultsManagerException.getMessage());
            }
        } else {
            responsePayload = String.format("{\"reason\":\"%s\", \"exceptionClass\":\"%s\"}",
                    throwable.getMessage(), throwable.getClass().getName());
            LOGGER.error(errorMessage, throwable);
        }

        // Set response content type
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.println(responsePayload);
    }
}
