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

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

public class BlubPluginsTestRunner extends AtlassianPluginsTestRunner {
    private final RunListener runListener;

    public BlubPluginsTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        this.runListener = new PrintlnJavaRunListener();
    }

    @Override
    public void run(RunNotifier notifier) {
        notifier.addListener(runListener);
        super.run(notifier);
    }

}
