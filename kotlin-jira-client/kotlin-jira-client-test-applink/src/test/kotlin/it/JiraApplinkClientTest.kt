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

import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.applinks.api.application.jira.JiraApplicationType
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.UserAccessor
import com.linkedplanet.kotlinhttpclient.atlas.AtlasHttpClient
import com.linkedplanet.kotlinjiraclient.JiraClientTest
import com.linkedplanet.kotlinjiraclient.api.field.JiraFieldFactory
import com.linkedplanet.kotlinjiraclient.api.interfaces.*
import com.linkedplanet.kotlinjiraclient.http.*
import com.linkedplanet.kotlinjiraclient.http.field.HttpJiraField
import com.linkedplanet.kotlinjiraclient.http.field.HttpJiraFieldFactory
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(PrintlnAtlassianPluginsTestRunner::class)
class JiraApplinkClientTest constructor(
    private var userAccessor: UserAccessor,
    applicationLinkService: ApplicationLinkService
) : JiraClientTest<HttpJiraField>() {

    private val clientContext: HttpJiraClientContext

    override val issueOperator: JiraIssueOperator<HttpJiraField> get() = HttpJiraIssueOperator(clientContext)
    override val fieldFactory: JiraFieldFactory<HttpJiraField> get() = HttpJiraFieldFactory

    override val projectOperator: JiraProjectOperator get() = HttpJiraProjectOperator(clientContext)
    override val issueTypeOperator: JiraIssueTypeOperator get() = HttpJiraIssueTypeOperator(clientContext)
    override val transitionOperator: JiraTransitionOperator get() = HttpJiraTransitionOperator(clientContext)
    override val commentOperator: JiraCommentOperator get() = HttpJiraCommentOperator(clientContext)
    override val issueLinkOperator: JiraIssueLinkOperator get() = HttpJiraIssueLinkOperator(clientContext)
    override val userOperator: JiraUserOperator get() = HttpJiraUserOperator(clientContext)

    override val issueTypeId: Int get() = 10001
    override val epicIssueTypeId: Int get() = 10000
    override val projectId: Long get() = 10000
    override val projectKey: String get() = "TEST"

    init {
        val primaryApplicationLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType::class.java)

        val serviceUser = userAccessor.getUserByName("admin")
        AuthenticatedUserThreadLocal.asUser(serviceUser)

        val httpClient = AtlasHttpClient(primaryApplicationLink)
        clientContext = HttpJiraClientContext(primaryApplicationLink.rpcUrl.toString(), httpClient)
    }

    @Before
    fun initTest() {
        val serviceUser = userAccessor.getUserByName("admin")
        AuthenticatedUserThreadLocal.asUser(serviceUser)
    }
}
