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
import com.linkedplanet.kotlininsightclient.InsightClientTest
import com.linkedplanet.kotlininsightclient.api.interfaces.*
import com.linkedplanet.kotlininsightclient.sdk.SdkInsightObjectOperator
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AtlassianPluginsTestRunner::class)
class InsightSdkClientTest : InsightClientTest() {

    private val jiraAuthenticationContext by lazy { ComponentAccessor.getJiraAuthenticationContext() }
    private val userManager by lazy { ComponentAccessor.getUserManager() }

    override val insightObjectOperator: InsightObjectOperator = SdkInsightObjectOperator

    override val insightObjectTypeOperator: InsightObjectTypeOperator
        get() = TODO("Not yet implemented")
    override val insightAttachmentOperator: InsightAttachmentOperator
        get() = TODO("Not yet implemented")
    override val insightSchemaOperator: InsightSchemaOperator
        get() = TODO("Not yet implemented")
    override val insightHistoryOperator: InsightHistoryOperator
        get() = TODO("Not yet implemented")

    @Before
    fun initTest() {
        println("### InsightSdkClientTest.initTest")
        val admin = userManager.getUserByName("admin")
        jiraAuthenticationContext.loggedInUser = admin
    }
}
