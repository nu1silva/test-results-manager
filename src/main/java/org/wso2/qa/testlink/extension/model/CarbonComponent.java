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
package org.wso2.qa.testlink.extension.model;

/**
 * Stores carbon component information.
 */

public class CarbonComponent {

    private String componentName;
    private String componentVersion;

    public CarbonComponent(String componentName, String componentVersion) {
        this.componentName = componentName;
        this.componentVersion = componentVersion;
    }

    public CarbonComponent(String component) {
        this.componentName = component;
    }

    public CarbonComponent() {
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    @Override
    public String toString() {
        return "CarbonComponent{" +
                "componentName='" + componentName + '\'' +
                ", componentVersion='" + componentVersion + '\'' +
                '}';
    }
}
