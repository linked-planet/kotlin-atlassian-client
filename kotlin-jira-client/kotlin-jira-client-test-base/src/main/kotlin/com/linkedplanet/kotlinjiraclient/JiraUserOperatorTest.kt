/*-
 * #%L
 * kotlin-jira-client-test-base
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
package com.linkedplanet.kotlinjiraclient

import com.linkedplanet.kotlinjiraclient.util.JiraUserTestHelper
import com.linkedplanet.kotlinjiraclient.util.rightAssertedJiraClientError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

interface JiraUserOperatorTest<JiraFieldType> : BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun users_01GetUsersByProjectKey() {
        println("### START users_01GetUsersByProjectKey")

        val users = runBlocking { userOperator.getUsersByProjectKey(projectKey) }.rightAssertedJiraClientError()
        assertEquals(2, users.size)

        val admin = users.firstOrNull { it.name == "admin" }
        JiraUserTestHelper.checkAdminUser(admin)

        val test1 = users.firstOrNull { it.name == "test1" }
        JiraUserTestHelper.checkTestUser1(test1)

        println("### END users_01GetUsersByProjectKey")
    }

    @Test
    fun users_02GetAssignableUsersByProjectKey() {
        println("### START users_02GetAssignableUsersByProjectKey")

        val users =
            runBlocking { userOperator.getAssignableUsersByProjectKey(projectKey) }.rightAssertedJiraClientError()
        assertEquals(3, users.size)

        val admin = users.firstOrNull { it.name == "admin" }
        JiraUserTestHelper.checkAdminUser(admin)

        val test1 = users.firstOrNull { it.name == "test1" }
        JiraUserTestHelper.checkTestUser1(test1)

        val test2 = users.firstOrNull { it.name == "test2" }
        JiraUserTestHelper.checkTestUser2(test2)

        println("### END users_02GetAssignableUsersByProjectKey")
    }

    @Test
    fun users_03GetProjectAdminUsers() {
        println("### START users_03GetProjectAdminUsers")

        val users = runBlocking { userOperator.getProjectAdminUsers(projectKey) }.rightAssertedJiraClientError()
        assertEquals(1, users.size)

        val admin = users.firstOrNull { it.name == "admin" }
        JiraUserTestHelper.checkAdminUser(admin)

        println("### END users_03GetProjectAdminUsers")
    }

    @Test
    fun users_04GetSystemAdminUsers() {
        println("### START users_04GetSystemAdminUsers")

        val users = runBlocking { userOperator.getSystemAdminUsers() }.rightAssertedJiraClientError()
        assertEquals(1, users.size)

        val admin = users.firstOrNull { it.name == "admin" }
        JiraUserTestHelper.checkAdminUser(admin)

        println("### END users_04GetSystemAdminUsers")
    }
}
