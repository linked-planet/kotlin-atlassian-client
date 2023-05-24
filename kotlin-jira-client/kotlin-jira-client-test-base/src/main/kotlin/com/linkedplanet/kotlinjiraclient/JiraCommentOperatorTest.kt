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
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

interface JiraCommentOperatorTest<JiraFieldType> : BaseTestConfigProvider<JiraFieldType> {

    @Test
    fun comments_01CreateComment() {
        println("### START comments_01CreateComment")

        val testName = "comments_01CreateComment"
        val (_, comment) = jiraCommentTestHelper.createIssueWithComment(testName)

        assertThat(comment.content, equalTo(testName))
        assertThat(comment.author, equalTo("admin"))

        println("### END comments_01CreateComment")
    }

    @Test
    fun comments_02GetCommentsEmpty() {
        println("### START comments_02GetCommentsEmpty")

        val issue = jiraIssueTestHelper.createDefaultIssue(fieldFactory.jiraSummaryField("Comment Test Ticket"))

        val comments = runBlocking {
            commentOperator.getComments(issue.key)
        }.rightAssertedJiraClientError()

        assertThat(comments, notNullValue())
        assertThat(comments.size, equalTo(0))

        println("### END comments_02GetCommentsEmpty")
    }

    @Test
    fun comments_03GetComments() {
        println("### START comments_03GetComments")

        val testName = "comments_03GetComments"
        val (issue, _) = jiraCommentTestHelper.createIssueWithComment(testName)

        val comments = runBlocking {
            commentOperator.getComments(issue.key)
        }.rightAssertedJiraClientError()

        assertThat(comments, notNullValue())
        assertThat(comments.size, equalTo(1))

        val comment = comments.first()
        assertThat(comment.content, equalTo(testName))
        assertThat(comment.author, equalTo("admin"))

        println("### END comments_03GetComments")
    }

    @Test
    fun comments_04UpdateComment() {
        println("### START comments_04UpdateComment")

        val (issue, commentToUpdate) = jiraCommentTestHelper.createIssueWithComment("comments_04UpdateComment")

        runBlocking {
            commentOperator.updateComment(
                issue.key,
                commentToUpdate.id,
                "Test-Update"
            )
        }.rightAssertedJiraClientError()

        val comments = runBlocking { commentOperator.getComments(issue.key) }.rightAssertedJiraClientError()
        assertThat(comments.size, equalTo(1))
        val comment = comments.first()

        assertThat(comment.content, equalTo("Test-Update"))
        assertThat(comment.author, equalTo("admin"))

        println("### END comments_04UpdateComment")
    }

    @Test
    fun comments_05DeleteComment() {
        println("### START comments_05DeleteComment")

        val (issue, commentToDelete) = jiraCommentTestHelper.createIssueWithComment("comments_05DeleteComment")

        runBlocking { commentOperator.deleteComment(issue.key, commentToDelete.id) }.rightAssertedJiraClientError()

        val commentsAfterDeletion =
            runBlocking { commentOperator.getComments(issue.key) }.rightAssertedJiraClientError()
        assertThat(commentsAfterDeletion.size, equalTo(0))

        println("### END comments_05DeleteComment")
    }
}
