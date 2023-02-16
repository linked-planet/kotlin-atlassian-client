/*-
 * #%L
 * kotlin-jira-client-sdk
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
package com.linkedplanet.kotlinjiraclient.sdk.field

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.timezone.TimeZoneManager
import com.linkedplanet.kotlinjiraclient.api.field.*
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import java.sql.Timestamp
import java.time.ZonedDateTime

private val customFieldManager by lazy { ComponentAccessor.getCustomFieldManager() }
private val userManager by lazy { ComponentAccessor.getUserManager() }
private val issueManager by lazy { ComponentAccessor.getComponent(IssueManager::class.java) }
private val issueTypeManager by lazy { ComponentAccessor.getComponent(IssueTypeManager::class.java) }
private val optionsManager by lazy { ComponentAccessor.getOptionsManager() }
private val timezoneManager by lazy { ComponentAccessor.getComponent(TimeZoneManager::class.java) }

// Service interface to access and manage Insight objects
private val objectFacade by lazy { ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade::class.java) }

interface SdkJiraField {
    fun render(issue: MutableIssue)
}

class SdkJiraSummaryField(summary: String) : JiraSummaryField(summary), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.summary = summary
    }
}

class SdkJiraDescriptionField(description: String) : JiraDescriptionField(description), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.description = description
    }
}

class SdkJiraProjectField(projectId: Long) : JiraProjectField(projectId), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.projectId = projectId
    }
}

class SdkJiraIssueTypeField(issueTypeId: Int) : JiraIssueTypeField(issueTypeId), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.issueTypeId = issueTypeId.toString()
    }
}

class SdkJiraIssueTypeNameField(issueTypeName: String) : JiraIssueTypeNameField(issueTypeName), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.setIssueType(this)
    }
}

class SdkJiraAssigneeField(username: String) : JiraAssigneeField(username), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.assignee = userManager.getUserByName(username)
    }
}

class SdkJiraReporterField(username: String) : JiraReporterField(username), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.reporter = userManager.getUserByName(username)
    }
}

class SdkJiraEpicLinkField(epicIssueKey: String?) : JiraEpicLinkField(epicIssueKey), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.setCustomFieldValue(
            customField(),
            epicIssueKey?.let { issueManager.getIssueByCurrentKey(it) }
        )
    }
}

class SdkJiraCustomInsightObjectField(
    customFieldName: String,
    insightKey: String?
) : JiraCustomInsightObjectField(customFieldName, insightKey), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.setInsightObjects(this, insightKey?.let { listOf(it) })
    }
}

class SdkJiraCustomInsightObjectsField(
    customFieldName: String,
    insightKeys: List<String>
) : JiraCustomInsightObjectsField(customFieldName, insightKeys), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.setInsightObjects(this, insightKeys)
    }
}

class SdkJiraEpicNameField(
    epicName: String
) : JiraEpicNameField(epicName), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.setCustomFieldValue(customField(), epicName)
    }
}

class SdkJiraCustomTextField(
    customFieldName: String,
    text: String
) : JiraCustomTextField(customFieldName, text), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.setCustomFieldValue(customField(), text)
    }
}

class SdkJiraCustomNumberField(
    customFieldName: String,
    value: Number
) : JiraCustomNumberField(customFieldName, value), SdkJiraField {
    override fun render(issue: MutableIssue) {
        issue.setCustomFieldValue(customField(), number.toDouble())
    }
}

class SdkJiraCustomDateTimeField(
    customFieldName: String,
    dateTime: ZonedDateTime
) : JiraCustomDateTimeField(customFieldName, dateTime), SdkJiraField {
    override fun render(issue: MutableIssue) {
        val dateTimeInUserTimeZone = dateTime.withZoneSameInstant(timezoneManager.loggedInUserTimeZone.toZoneId())
        issue.setCustomFieldValue(
            customField(),
            Timestamp.from(dateTimeInUserTimeZone.toInstant())
        )
    }
}

class SdkJiraCustomRadioField(
    customFieldName: String,
    value: String
) : JiraCustomRadioField(customFieldName, value), SdkJiraField {
    override fun render(issue: MutableIssue) {
        val customField = customFieldManager.getCustomFieldObjectsByName(customFieldName).single()
        val option = optionsManager.getOptions(customField.getRelevantConfig(issue)).getOptionForValue(value, null)
        issue.setCustomFieldValue(customField(), option)
    }
}

private fun MutableIssue.setIssueType(field: JiraIssueTypeNameField) {
    issueType = issueTypeManager.issueTypes.firstOrNull { it.name == field.issueTypeName }
        ?: throw IllegalArgumentException("Unknown Issue Type ${field.issueTypeName}")
}

private fun JiraCustomField.customField(): CustomField {
    val fields = customFieldManager.getCustomFieldObjectsByName(customFieldName)
    when {
        fields.isEmpty() -> throw IllegalArgumentException("Field name is unknown")
        fields.size > 1 -> throw IllegalArgumentException("Field name is not unique")
        else -> return fields.firstOrNull()!!
    }
}

private fun MutableIssue.setInsightObjects(field: JiraCustomField, objectKeys: List<String>?) {
    val beans = objectKeys?.map { objectKey -> objectFacade.loadObjectBean(objectKey) }
    setCustomFieldValue(field.customField(), beans)
}
