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

import arrow.core.getOrElse
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner
import com.linkedplanet.kotlininsightclient.InsightClientTest
import com.linkedplanet.kotlininsightclient.api.interfaces.*
import com.linkedplanet.kotlininsightclient.sdk.*
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AtlassianPluginsTestRunner::class)
class InsightSdkClientTest : InsightClientTest() {

    private val jiraAuthenticationContext by lazy { ComponentAccessor.getJiraAuthenticationContext() }
    private val userManager by lazy { ComponentAccessor.getUserManager() }

    override val insightObjectOperator: InsightObjectOperator = SdkInsightObjectOperator
    override val insightObjectTypeOperator: InsightObjectTypeOperator = SdkInsightObjectTypeOperator
    override val insightAttachmentOperator: InsightAttachmentOperator = SdkInsightAttachmentOperator
    override val insightSchemaOperator: InsightSchemaOperator = SdkInsightSchemaOperator
    override val insightHistoryOperator: InsightHistoryOperator = SdkInsightHistoryOperator

    @Before
    fun initTest() {
        val admin = userManager.getUserByName("admin")
        jiraAuthenticationContext.loggedInUser = admin
    }

    data class AttributeValueTestData(
        val name: String,
        val objectTypeId: Int,
        val attributeId: Int,
        val query: String,
        val exceptionList: String?,
        val page: Int,
        val pageSize: Int,
        val expectedResults: List<String>
    )


    @Test
    fun test1InsightAttributeValuesForTypes() = runBlocking {
        val attributeValueTestData = listOf(
            AttributeValueTestData("TEXT", 47, 116, "", null, 1, 10, listOf("TEXT1")),
            AttributeValueTestData("INT", 47, 117, "", null, 1, 10, listOf("1")),
            AttributeValueTestData("FLOAT", 47, 118, "", null, 1, 10, listOf("1.0")),
            AttributeValueTestData("DATE", 47, 119, "", null, 1, 10, listOf("1/Jan/25")),
            AttributeValueTestData("DATETIME", 47, 120, "", null, 1, 10, listOf("01/Jan/25 12:00 AM")),
            AttributeValueTestData("URL", 47, 121, "", null, 1, 10, listOf("http://localhost:0001/")),
            AttributeValueTestData("EMAIL", 47, 122, "", null, 1, 10, listOf("1@linked-planet.com")),
            AttributeValueTestData("TEXTAREA", 47, 123, "", null, 1, 10, listOf("<p>TEXTAREA_1</p>")),
            AttributeValueTestData("SELECT", 47, 124, "", null, 1, 10, listOf("1")),
            AttributeValueTestData("MULTISELECT", 47, 125, "", null, 1, 10, listOf("1", "2")),
            AttributeValueTestData("IPADDRESS", 47, 126, "", null, 1, 10, listOf("1.1.1.1")),
            AttributeValueTestData("USER", 47, 127, "", null, 1, 10, listOf("test1")),
            AttributeValueTestData("USERS", 47, 128, "", null, 1, 10, listOf("test1", "test2")),
            AttributeValueTestData("GROUP", 47, 129, "", null, 1, 10, listOf("jira-software-users")),
            AttributeValueTestData("GROUPS", 47, 130, "", null, 1, 10, listOf("jira-administrators", "jira-software-users")),
            AttributeValueTestData("PROJECT", 47, 131, "", null, 1, 10, listOf("Test")),
            AttributeValueTestData("PROJECTS", 47, 132, "", null, 1, 10, listOf("Test")),
            AttributeValueTestData("REF", 47, 133, "", null, 1, 10, listOf("Test AG")),
            AttributeValueTestData("REFS", 47, 134, "", null, 1, 10, listOf("Test AG", "Test GmbH")),
        )
        attributeValueTestData.forEach { data ->
            check(data)
        }
    }

    @Test
    fun test2InsightAttributeValuesWithFilter() = runBlocking {
        val tests = listOf(
            AttributeValueTestData(
                "GetPage1", 5, 24, "", null, 1, 10,
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
            ),
            AttributeValueTestData(
                "GetPage2", 5, 24, "", null, 2, 10,
                listOf("11", "12", "13", "14", "15", "16", "17", "18", "19", "20")
            ),
            AttributeValueTestData(
                "Filter", 5, 24, "2", null, 1, 10,
                listOf("2", "12", "20", "21", "22", "23", "24", "25", "26", "27")
            ),
            AttributeValueTestData(
                "ExceptionsAndFilter", 5, 24, "2", "2,20", 1, 10,
                listOf("12", "21", "22", "23", "24", "25", "26", "27", "28", "29")
            ),
            )
        tests.forEach { testData ->
            check(testData)
        }
    }

    private suspend fun check(attributeValueTestData: AttributeValueTestData) {
        val result: List<String> = SdkInsightObjectOperator.getAttributeValues(
            attributeValueTestData.objectTypeId,
            attributeValueTestData.attributeId,
            attributeValueTestData.query,
            attributeValueTestData.exceptionList,
            attributeValueTestData.page,
            attributeValueTestData.pageSize
        ).getOrElse { error ->
            assertTrue("${attributeValueTestData.name} returned Either.Left!", false)
            return
        }.results
        assertThat("${attributeValueTestData.name} test did not pass!", result, equalTo(attributeValueTestData.expectedResults))
    }
}
