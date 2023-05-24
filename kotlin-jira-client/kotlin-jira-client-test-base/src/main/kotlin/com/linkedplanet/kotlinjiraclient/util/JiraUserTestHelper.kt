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
package com.linkedplanet.kotlinjiraclient.util

import com.linkedplanet.kotlinjiraclient.api.model.JiraUser
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue

class JiraUserTestHelper {

    companion object {
        fun checkAdminUser(jiraUser: JiraUser?) =
            checkUser("admin", "admin", "admin@admin.com", jiraUser)

        fun checkTestUser1(jiraUser: JiraUser?) =
            checkUser("test1", "test1", "simon.jahreiss@linked-planet.com", jiraUser)

        fun checkTestUser2(jiraUser: JiraUser?) =
            checkUser("test2", "test2", "simon.jahreiss@linked-planet.com", jiraUser)

        fun checkUser(expectedName: String, expectedDisplayName: String, expectedEmail: String, jiraUser: JiraUser?) {
            assertThat(jiraUser, notNullValue())
            assertThat(jiraUser!!.name, equalTo(expectedName))
            assertThat(jiraUser.displayName, equalTo(expectedDisplayName))
            assertThat(jiraUser.emailAddress, equalTo(expectedEmail))
            assertTrue(jiraUser.key.isNotEmpty())
            assertThat(jiraUser.avatarUrl, notNullValue())
            assertTrue(jiraUser.avatarUrl!!.startsWith("https://www.gravatar.com/avatar"))
        }
    }
}
