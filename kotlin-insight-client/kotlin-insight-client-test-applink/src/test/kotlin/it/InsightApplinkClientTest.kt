/*-
 * #%L
 * kotlin-insight-client-test-applink
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
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner
import com.linkedplanet.kotlinhttpclient.atlas.AtlasHttpClient
import com.linkedplanet.kotlininsightclient.InsightClientTest
import com.linkedplanet.kotlininsightclient.api.interfaces.*
import com.linkedplanet.kotlininsightclient.http.*
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AtlassianPluginsTestRunner::class)
class InsightApplinkClientTest constructor(
    private var userAccessor: UserAccessor,
    applicationLinkService: ApplicationLinkService
) : InsightClientTest() {

    private val clientContext: HttpInsightClientContext

    override val insightObjectOperator get() = HttpInsightObjectOperator(clientContext)
    override val insightObjectTypeOperator get() = HttpInsightObjectTypeOperator(clientContext)
    override val insightAttachmentOperator get() = HttpInsightAttachmentOperator(clientContext)
    override val insightSchemaOperator get() = HttpInsightSchemaOperator(clientContext)
    override val insightHistoryOperator get() = HttpInsightHistoryOperator(clientContext)

    init {
        println("AppLinkUrl: ${applicationLinkService.getPrimaryApplicationLink(JiraApplicationType::class.java).displayUrl}")
        val serviceUser = userAccessor.getUserByName("admin")
        AuthenticatedUserThreadLocal.asUser(serviceUser)
        val appLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType::class.java)
        val httpClient = AtlasHttpClient(
            appLink
        )

        clientContext = HttpInsightClientContext("http://localhost:2990", httpClient)
    }

    @Before
    fun initTest() {
        val serviceUser = userAccessor.getUserByName("admin")
        AuthenticatedUserThreadLocal.asUser(serviceUser)
    }

}
