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

import com.linkedplanet.kotlinjiraclient.util.orFail
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

interface JiraProjectOperatorTest<JiraFieldType>: BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun projects_01GetProjects() {
        val projects = runBlocking {
            projectOperator.getProjects()
        }.orFail()

        assertThat(projects.size, equalTo(1))
        assertThat(projects.first().id, equalTo(10000L))
        assertThat(projects.first().key, equalTo("TEST"))
        assertThat(projects.first().name, equalTo("Test"))
        assertThat(projects.first().url, equalTo("http://localhost:2990/rest/api/2/project/10000"))
        assertThat(projects.first().avatarUrl, equalTo("http://localhost:2990/secure/projectavatar?avatarId=10324"))
    }

    @Test
    fun projects_02GetProject() {
        val project = runBlocking {
            projectOperator.getProject(projectId)
        }.orFail()

        assertThat(project.id, equalTo(10000L))
        assertThat(project.key, equalTo("TEST"))
        assertThat(project.name, equalTo("Test"))
        assertThat(project.url, equalTo("http://localhost:2990/rest/api/2/project/10000"))
        assertThat(project.avatarUrl, equalTo("http://localhost:2990/secure/projectavatar?avatarId=10324"))
    }
}
