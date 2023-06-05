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

import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

interface JiraTransitionOperatorTest<JiraFieldType> : BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun transitions_01Transitions() {
        val issue = jiraIssueTestHelper.createDefaultIssue(fieldFactory.jiraSummaryField("Transition Test Ticket"))

        // get doIt transition
        val transitions = jiraTransitionTestHelper.getAvailableTransitions(issue.key)
        assertThat(transitions.map { it.name }.toSet(), equalTo(setOf("To Do", "Do it")))
        val doIt = transitions.firstOrNull { it.name == "Do it" }!!.id

        // move from "To Do" into "In Progress"
        val transitionToInProgress = jiraTransitionTestHelper.doTransition(issue.key, doIt)
        assertThat(transitionToInProgress, equalTo(true))
        val issueInProgress = jiraIssueTestHelper.getIssueByKey(issue.key)
        assertThat(issueInProgress.status.name, equalTo("In Progress"))

        // get "To Do" transition
        val transitionsFromInProgress = jiraTransitionTestHelper.getAvailableTransitions(issue.key)
        assertThat(transitionsFromInProgress.map { it.name }.toSet(), equalTo(setOf("To Do", "Did it")))
        val fromInProgressTransitions = transitionsFromInProgress.firstOrNull { it.name == "To Do" }!!.id

        // move from "In Progress" to "To Do"
        val transitionToToDo = jiraTransitionTestHelper.doTransition(issue.key, fromInProgressTransitions)
        assertThat(transitionToToDo, equalTo(true))
        val issueTodo = jiraIssueTestHelper.getIssueByKey(issue.key)
        assertThat(issueTodo.status.name, equalTo("To Do"))
    }

    @Test
    fun transitions_02IssueNotFound() {
        val notFound = runBlocking { transitionOperator.getAvailableTransitions("unknownIssueKey") }
        assertThat(notFound.isLeft(), equalTo(true))
    }
}
