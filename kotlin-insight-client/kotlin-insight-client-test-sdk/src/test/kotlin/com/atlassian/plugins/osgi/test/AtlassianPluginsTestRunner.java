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
package com.atlassian.plugins.osgi.test;

import com.atlassian.plugins.osgi.test.rest.GsonFactory;
import com.atlassian.plugins.osgi.test.rest.TestResultDetailRepresentation;
import com.google.gson.Gson;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;


/**
 * @since version
 */
public class AtlassianPluginsTestRunner extends BlockJUnit4ClassRunner
{
    // Visible for testing
    static final String BASE_URL = "baseurl";

    // Visible for testing
    static String getResourceUrl(final Description description)
    {
        final String baseUrl = System.getProperty(BASE_URL);
        if (isBlank(baseUrl))
        {
            throw new IllegalStateException(
                    String.format("Missing base URL '%s'; is the '%s' system property set?", baseUrl, BASE_URL));
        }
        return baseUrl + "/rest/atlassiantestrunner/1.0/runtest/" + description.getClassName();
    }

    private static boolean isBlank(final String string)
    {
        return string == null || "".equals(string.trim());
    }

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @throws InitializationError
     *          if the test class is malformed.
     */
    public AtlassianPluginsTestRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
    }

    @Override
    public void run(final RunNotifier notifier)
    {
        notifier.addListener(new PrintlnJavaRunListener());
        
        EachTestNotifier testNotifier = new EachTestNotifier(notifier, getDescription());
        try
        {
            String[] packageParts = getTestClass().getJavaClass().getPackage().getName().split("\\.");
            
            if(null == packageParts || packageParts.length < 1 || !packageParts[0].equals("it"))
            {
                throw new Exception("the class [" + getTestClass().getJavaClass().getName() + "] is annotated with @RunWith(AtlassianPluginsTestRunner.class) but it is not in the 'it.' package." +
                        "\nPlease move the class into the 'it.' package or remove the @RunWith annotation");    
            }
            
            runViaRestCall(getDescription(), notifier);
        }
        catch (AssumptionViolatedException e)
        {
            testNotifier.fireTestIgnored();
        }
        catch (StoppedByUserException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            testNotifier.addFailure(e);
        }
    }

    private void runViaRestCall(Description description, RunNotifier notifier) throws IOException
    {
        String resourceUrl = getResourceUrl(description);

        ClientConfig config = new ClientConfig();
        config.readTimeout(1800000);

        RestClient client = new RestClient(config);

        Resource resource = client.resource(resourceUrl);

        ClientResponse clientResponse = resource.accept(MediaType.APPLICATION_JSON).get();
        
        if(clientResponse.getStatusCode() >= 300)
        {
            throw new IllegalStateException("Could not find resource for test [" + resourceUrl + "]. Status: " + clientResponse.getStatusCode() + " - " + clientResponse.getMessage());
        }

        String response = clientResponse.getEntity(String.class);

        Gson gson = GsonFactory.getGson();

        TestResultDetailRepresentation osgiResult = gson.fromJson(response, TestResultDetailRepresentation.class);
        
        int totalCount = osgiResult.getFailedMethods().size() + osgiResult.getIgnoredMethods().size() + osgiResult.getPassedMethods().size();
        
        if(totalCount < 1)
        {
            Description desc = Description.createSuiteDescription("No tests found in class [" + description.getClassName() + "]", new Annotation[0]);
            Failure classNotFoundFail = new Failure(desc, new Exception("No tests found in class [" + description.getClassName() + "]"));
            notifier.fireTestFailure(classNotFoundFail);
        }
        
        for(String pMethodName : osgiResult.getPassedMethods())
        {
            EachTestNotifier testNotifier = new EachTestNotifier(notifier, Description.createTestDescription(getTestClass().getJavaClass(),pMethodName));
            testNotifier.fireTestStarted();
            testNotifier.fireTestFinished();
        }

        for(String iMethodName : osgiResult.getIgnoredMethods())
        {
            EachTestNotifier testNotifier = new EachTestNotifier(notifier, Description.createTestDescription(getTestClass().getJavaClass(),iMethodName));
            testNotifier.fireTestIgnored();
        }

        for(Map.Entry<String,Failure> entry : osgiResult.getFailedMethods().entrySet())
        {
            String fMethodName = entry.getKey();
            Failure f = entry.getValue();
            EachTestNotifier testNotifier = new EachTestNotifier(notifier, Description.createTestDescription(getTestClass().getJavaClass(),fMethodName));
            testNotifier.addFailure(f.getException());
        }
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
        //Dont validate the constructor
    }
}
