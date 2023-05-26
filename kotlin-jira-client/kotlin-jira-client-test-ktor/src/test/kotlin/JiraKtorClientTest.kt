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
import com.linkedplanet.kotlinhttpclient.ktor.KtorHttpClient
import com.linkedplanet.kotlinjiraclient.JiraClientTest
import com.linkedplanet.kotlinjiraclient.api.field.JiraFieldFactory
import com.linkedplanet.kotlinjiraclient.api.interfaces.*
import com.linkedplanet.kotlinjiraclient.http.*
import com.linkedplanet.kotlinjiraclient.http.field.HttpJiraField
import com.linkedplanet.kotlinjiraclient.http.field.HttpJiraFieldFactory
import org.junit.runner.RunWith

@RunWith(PrintlnTestRunner::class)
class JiraKtorClientTest : JiraClientTest<HttpJiraField>() {

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

    private val clientContext: HttpJiraClientContext

    init {
        val httpClient = KtorHttpClient(
            "http://localhost:2990",
            "admin",
            "admin"
        )
        clientContext = HttpJiraClientContext("http://localhost:2990", httpClient)
    }
}
