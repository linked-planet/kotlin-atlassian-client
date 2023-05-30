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

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class PrintlnJavaRunListener extends RunListener {
    @Override
    public void testRunStarted(Description description) throws Exception {
        System.out.println(description.getDisplayName());
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        System.out.println("finished with success:" + result.wasSuccessful());
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        System.out.println(failure.getDescription().getDisplayName());
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        System.out.println(description.getDisplayName());
    }

}
