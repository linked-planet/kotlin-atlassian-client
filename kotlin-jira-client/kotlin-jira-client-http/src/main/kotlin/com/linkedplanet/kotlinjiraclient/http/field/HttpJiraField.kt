/*-
 * #%L
 * kotlin-jira-client-http
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
package com.linkedplanet.kotlinjiraclient.http.field

import com.google.gson.*
import com.linkedplanet.kotlinjiraclient.api.field.*
import com.linkedplanet.kotlinjiraclient.api.resolveConfig
import java.time.ZonedDateTime

interface HttpJiraField {
    fun render(jsonObject: JsonObject, mappings: Map<String, String>)
}

class HttpJiraSummaryField(summary: String) : JiraSummaryField(summary), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        jsonObject.addProperty("summary", summary)
    }
}

class HttpJiraDescriptionField(description: String) : JiraDescriptionField(description), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        jsonObject.addProperty("description", description)
    }
}

class HttpJiraProjectField(projectId: Long) : JiraProjectField(projectId), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val projectJson = JsonObject()
        projectJson.addProperty("id", projectId)
        jsonObject.add("project", projectJson)
    }
}

class HttpJiraIssueTypeField(issueTypeId: Int) : JiraIssueTypeField(issueTypeId), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val projectJson = JsonObject()
        projectJson.addProperty("id", issueTypeId)
        jsonObject.add("issuetype", projectJson)
    }
}

class HttpJiraIssueTypeNameField(issueTypeName: String) : JiraIssueTypeNameField(issueTypeName), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val projectJson = JsonObject()
        projectJson.addProperty("name", issueTypeName)
        jsonObject.add("issuetype", projectJson)
    }
}

class HttpJiraAssigneeField(username: String) : JiraAssigneeField(username), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig("assignee", mappings)
        val userJson = JsonObject()
        userJson.addProperty("name", username)
        jsonObject.add(fieldName, userJson)
    }
}

class HttpJiraReporterField(username: String) : JiraReporterField(username), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig("reporter", mappings)
        val userJson = JsonObject()
        userJson.addProperty("name", username)
        jsonObject.add(fieldName, userJson)
    }
}

class HttpJiraEpicLinkField(epicIssueKey: String?) : JiraEpicLinkField(epicIssueKey), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig("Epic Link", mappings)
        jsonObject.addProperty(fieldName, epicIssueKey)
    }
}

class HttpJiraCustomInsightObjectField(
    customFieldName: String,
    insightKey: String?
) : JiraCustomInsightObjectField(customFieldName, insightKey), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig(customFieldName, mappings)
        val jsonArray = JsonArray()
        val wrapper = JsonObject()
        wrapper.addProperty("key", insightKey)
        jsonArray.add(wrapper)
        jsonObject.add(fieldName, jsonArray)
    }
}

class HttpJiraCustomInsightObjectsField(
    customFieldName: String,
    insightKeys: List<String>
) : JiraCustomInsightObjectsField(customFieldName, insightKeys), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig(customFieldName, mappings)
        val jsonArray = JsonArray()
        insightKeys.forEach { objectKey ->
            val wrapper = JsonObject()
            wrapper.addProperty("key", objectKey)
            jsonArray.add(wrapper)
        }
        jsonObject.add(fieldName, jsonArray)
    }
}

class HttpJiraEpicNameField(
    value: String
) : JiraEpicNameField(value), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig(customFieldName, mappings)
        jsonObject.addProperty(fieldName, epicName)
    }
}

class HttpJiraCustomTextField(
    customFieldName: String,
    text: String
) : JiraCustomTextField(customFieldName, text), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig(customFieldName, mappings)
        jsonObject.addProperty(fieldName, text)
    }
}

class HttpJiraCustomNumberField(
    customFieldName: String,
    number: Number
) : JiraCustomNumberField(customFieldName, number), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig(customFieldName, mappings)
        jsonObject.addProperty(fieldName, number.toDouble())
    }
}

class HttpJiraCustomDateTimeField(
    customFieldName: String,
    dateTime: ZonedDateTime
) : JiraCustomDateTimeField(customFieldName, dateTime), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val dateToJiraDateString = JIRA_DATE_TIME_FORMATTER.format(dateTime)
        val fieldName = resolveConfig(customFieldName, mappings)
        jsonObject.addProperty(fieldName, dateToJiraDateString)
    }
}

class HttpJiraCustomRadioField(
    customFieldName: String,
    value: String
) : JiraCustomRadioField(customFieldName, value), HttpJiraField {
    override fun render(jsonObject: JsonObject, mappings: Map<String, String>) {
        val fieldName = resolveConfig(customFieldName, mappings)
        val valueIn = run {
            val wrapper = JsonObject()
            wrapper.add("value", JsonPrimitive(value))
            wrapper
        }
        jsonObject.add(fieldName, valueIn)
    }
}
