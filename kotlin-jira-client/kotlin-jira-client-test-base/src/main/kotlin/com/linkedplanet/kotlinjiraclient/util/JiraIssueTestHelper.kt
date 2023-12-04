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

import arrow.core.Either
import arrow.core.raise.either
import com.google.gson.*
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.field.JIRA_DATE_TIME_FORMATTER
import com.linkedplanet.kotlinjiraclient.api.field.JiraFieldFactory
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraIssueOperator
import com.linkedplanet.kotlinjiraclient.api.model.JiraStatus
import com.linkedplanet.kotlinjiraclient.api.model.JiraTransition
import com.linkedplanet.kotlinjiraclient.api.resolveConfig
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking

class JiraIssueTestHelper<JiraFieldType>(
    private val issueOperator: JiraIssueOperator<JiraFieldType>,
    private val fieldFactory: JiraFieldFactory<JiraFieldType>,
    private val issueTypeId: Int,
    private val epicIssueTypeId: Int,
    private val projectId: Long
) {
    fun createDefaultIssue(vararg fields: JiraFieldType) = createIssue(issueTypeId, *fields)

    fun createEpic(vararg fields: JiraFieldType) = createIssue(epicIssueTypeId, *fields)

    fun createIssue(jiraIssueTypeId: Int, vararg fields: JiraFieldType) =
        runBlocking {
            val combinedFields = listOf(
                fieldFactory.jiraProjectField(projectId),
                fieldFactory.jiraIssueTypeField(jiraIssueTypeId),
            ).plus(fields)
            issueOperator.createIssue(projectId, jiraIssueTypeId, combinedFields)
        }.rightAssertedJiraClientError()

    fun getIssueByKey(key: String) = runBlocking {
        issueOperator.getIssueByKey(key, ::issueParser)
    }.rightAssertedJiraClientError()
}

data class Story(
    val key: String,
    val projectId: Long?,
    val issueTypeId: Int?,
    val summary: String?,
    val description: String?,
    val assignee: String?,
    val reporter: String?,
    val radio: String?,
    val zonedDateTime: ZonedDateTime?,
    val singleText: String?,
    val multiText: String?,
    val doubleNumber: Double?,
    val intNumber: Int?,
    val insightObjectKey: String?,
    val insightObjectsKeys: List<String>?,
    val status: JiraStatus,
    val epicKey: String?,
    val transitions: List<JiraTransition>
)

suspend fun issueParser(jsonObject: JsonObject, map: Map<String, String>): Either<JiraClientError, Story> =
    either {
        val fields = jsonObject.get("fields").asJsonObject

        fun fieldByName(name: String): JsonElement? =
            fields
                .get(resolveConfig(name, map))
                ?.let { if (it.isJsonNull) null else it }

        val key = jsonObject.get("key").asString
        val summary = fields.getNullSafe("summary")?.asString
        val description = fields.getNullSafe("description")?.asString
        val projectId = fields.getNullSafe("project")?.asJsonObject?.getNullSafe("id")?.asLong
        val issueTypeId = fields.getNullSafe("issuetype")?.asJsonObject?.getNullSafe("id")?.asInt
        val assignee = fields.getNullSafe("assignee")?.asJsonObject?.getNullSafe("name")?.asString
        val reporter = fields.getNullSafe("reporter")?.asJsonObject?.getNullSafe("name")?.asString
        val radio = fieldByName("Radio")?.asJsonObject?.get("value")?.asString
        val zonedDateTime =
            fieldByName("ZonedDateTime")?.asString?.let { ZonedDateTime.parse(it, JIRA_DATE_TIME_FORMATTER) }
        val singleText = fieldByName("SingleText")?.asString
        val multiText = fieldByName("MultiText")?.asString
        val doubleNumber = fieldByName("DoubleNumber")?.asDouble
        val intNumber = fieldByName("IntNumber")?.asDouble?.toInt()
        val insightObjectKey = fieldByName("InsightObject")
            ?.asJsonArray?.parseInsightFieldKey()
        val insightObjectsKeys = fieldByName("InsightObjects")
            ?.asJsonArray?.parseInsightFieldKeys()

        val statusObject: JsonObject = fields.get("status").asJsonObject
        val status = JiraStatus(
            statusObject.get("id").asString,
            statusObject.get("name").asString,
            statusObject.get("statusCategory").asJsonObject.get("key").asString
        )

        val transitions =
            jsonObject.get("transitions").asJsonArray
                .map {
                    val transition = it.asJsonObject
                    val name = transition.get("name").asString
                    val id = transition.get("id").asString
                    JiraTransition(id, name)
                }

        val epicKey: String? = fieldByName("Epic Link")?.asString

        Story(
            key,
            projectId,
            issueTypeId,
            summary,
            description,
            assignee,
            reporter,
            radio,
            zonedDateTime,
            singleText,
            multiText,
            doubleNumber,
            intNumber,
            insightObjectKey,
            insightObjectsKeys,
            status,
            epicKey,
            transitions
        )
    }

fun JsonObject.getNullSafe(fieldName: String) =
    if (get(fieldName)?.isJsonNull ?: true) null else get(fieldName)

fun JsonArray.parseInsightFieldKey(): String = firstOrNull()?.asString?.parseInsightFieldKey() ?: ""

fun JsonArray.parseInsightFieldKeys(): List<String> = map { it.asString.parseInsightFieldKey() }

fun String.parseInsightFieldKey(): String {
    val startIndex = indexOf('(')
    val endIndex = indexOf(')')
    return substring(startIndex + 1, endIndex)
}
