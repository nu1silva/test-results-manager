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
import org.apache.log4j.MDC;
import org.wso2.qa.testlink.extension.model.Configurations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet context listener which does the initializations tasks.
 */
public class ContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(ContextListener.class);

    public void contextInitialized(ServletContextEvent servletContextEvent) {

        Configurations configurations = Configurations.getInstance();

        Properties properties = new Properties();

        InputStream propertiesFileInputStream = ContextListener.class.getClassLoader().
                getResourceAsStream("test-results-manager.properties");

        try {
            properties.load(propertiesFileInputStream);
            configurations.init(properties);
        } catch (IOException e) {
            LOGGER.error("Could not read the configurations.", e);

            // Try to stop the application starting, by throwing an exception.
            throw new RuntimeException("Could not read the configurations.", e);
        }

    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        MDC.clear();
    }
}
