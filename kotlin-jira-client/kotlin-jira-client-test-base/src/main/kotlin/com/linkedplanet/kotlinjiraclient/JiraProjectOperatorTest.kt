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

import com.linkedplanet.kotlinjiraclient.util.rightAssertedJiraClientError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

interface JiraProjectOperatorTest<JiraFieldType>: BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun projects_01GetProjects() {
        println("### START projects_01GetProjects")

        val projects = runBlocking {
            projectOperator.getProjects()
        }.rightAssertedJiraClientError()

        assertEquals(1, projects.size)
        assertEquals("10000", projects.first().id)
        assertEquals("TEST", projects.first().key)
        assertEquals("Test", projects.first().name)

        println("### END projects_01GetProjects")
    }

    @Test
    fun projects_02GetProject() {
        println("### START projects_02GetProject")

        val project = runBlocking {
            projectOperator.getProject(projectId)
        }.rightAssertedJiraClientError()

        assertEquals("10000", project.id)
        assertEquals("TEST", project.key)
        assertEquals("Test", project.name)

        println("### END projects_02GetProject")
    }
}
