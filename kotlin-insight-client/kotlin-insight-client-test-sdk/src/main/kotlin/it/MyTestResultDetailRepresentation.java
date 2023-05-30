/*-
 * #%L
 * kotlin-jira-client-api
 * %%
 * Copyright (C) 2022 - 2023 linked-planet GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package it;

import com.atlassian.plugins.osgi.test.rest.TestDetailRepresentation;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since version
 */
public class MyTestResultDetailRepresentation extends TestDetailRepresentation
{

    @JsonProperty private Result testResult;
    @JsonProperty private Set<String> passedMethods;
    @JsonProperty private Set<String> ignoredMethods;
    @JsonProperty private Map<String,Failure> failedMethods;


    @JsonCreator
    public MyTestResultDetailRepresentation(@JsonProperty("classname") String classname, @JsonProperty("testResult") Result testResult)
    {
        super(classname);
        
        this.testResult = testResult;

        this.passedMethods = new HashSet<String>();
        this.ignoredMethods = new HashSet<String>();
        this.failedMethods = new HashMap<String, Failure>();
    }

    public Result getTestResult()
    {
        return testResult;
    }

    public Set<String> getPassedMethods()
    {
        return passedMethods;
    }

    public Set<String> getIgnoredMethods()
    {
        return ignoredMethods;
    }

    public Map<String,Failure> getFailedMethods()
    {
        return failedMethods;
    }
}
