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
package it

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner
import com.linkedplanet.kotlinjiraclient.JiraClientTest
import com.linkedplanet.kotlinjiraclient.api.field.JiraFieldFactory
import com.linkedplanet.kotlinjiraclient.api.interfaces.*
import com.linkedplanet.kotlinjiraclient.sdk.*
import com.linkedplanet.kotlinjiraclient.sdk.field.SdkJiraField
import com.linkedplanet.kotlinjiraclient.sdk.field.SdkJiraFieldFactory
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AtlassianPluginsTestRunner::class)
class JiraSdkClientTest : JiraClientTest<SdkJiraField>() {

    private val jiraAuthenticationContext by lazy { ComponentAccessor.getJiraAuthenticationContext() }
    private val userManager by lazy { ComponentAccessor.getUserManager() }

    override val issueOperator: JiraIssueOperator<SdkJiraField> get() = SdkJiraIssueOperator
    override val fieldFactory: JiraFieldFactory<SdkJiraField> get() = SdkJiraFieldFactory

    override val projectOperator: JiraProjectOperator get() = SdkJiraProjectOperator
    override val issueTypeOperator: JiraIssueTypeOperator get() = SdkJiraIssueTypeOperator
    override val transitionOperator: JiraTransitionOperator get() = SdkJiraTransitionOperator
    override val commentOperator: JiraCommentOperator get() = SdkJiraCommentOperator
    override val issueLinkOperator: JiraIssueLinkOperator get() = SdkJiraIssueLinkOperator
    override val userOperator: JiraUserOperator get() = SdkJiraUserOperator

    override val issueTypeId: Int get() = 10001 // Story
    override val epicIssueTypeId: Int get() = 10000
    override val projectId: Long get() = 10000
    override val projectKey: String get() = "TEST"

    @Before
    fun initTest() {
        val admin = userManager.getUserByName("admin")
        jiraAuthenticationContext.loggedInUser = admin
    }
}
