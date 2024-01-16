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
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.context.IssueContext
import com.atlassian.jira.issue.context.IssueContextImpl
import com.atlassian.jira.issue.customfields.CustomFieldTypes
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.timezone.TimeZoneManager
import com.linkedplanet.kotlinjiraclient.api.field.JiraAssigneeField
import com.linkedplanet.kotlinjiraclient.api.field.JiraCustomDateTimeField
import com.linkedplanet.kotlinjiraclient.api.field.JiraCustomField
import com.linkedplanet.kotlinjiraclient.api.field.JiraCustomInsightObjectField
import com.linkedplanet.kotlinjiraclient.api.field.JiraCustomInsightObjectsField
import com.linkedplanet.kotlinjiraclient.api.field.JiraCustomNumberField
import com.linkedplanet.kotlinjiraclient.api.field.JiraCustomRadioField
import com.linkedplanet.kotlinjiraclient.api.field.JiraCustomTextField
import com.linkedplanet.kotlinjiraclient.api.field.JiraDescriptionField
import com.linkedplanet.kotlinjiraclient.api.field.JiraEpicLinkField
import com.linkedplanet.kotlinjiraclient.api.field.JiraEpicNameField
import com.linkedplanet.kotlinjiraclient.api.field.JiraIssueTypeField
import com.linkedplanet.kotlinjiraclient.api.field.JiraIssueTypeNameField
import com.linkedplanet.kotlinjiraclient.api.field.JiraProjectField
import com.linkedplanet.kotlinjiraclient.api.field.JiraReporterField
import com.linkedplanet.kotlinjiraclient.api.field.JiraSummaryField
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val customFieldManager by lazy { ComponentAccessor.getCustomFieldManager() }
private val issueTypeManager by lazy { ComponentAccessor.getComponent(IssueTypeManager::class.java) }
private val optionsManager by lazy { ComponentAccessor.getOptionsManager() }

interface SdkJiraField {
    fun render(issue: IssueInputParameters)
}

class SdkJiraSummaryField(summary: String) : JiraSummaryField(summary), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.summary = summary
    }
}

class SdkJiraDescriptionField(description: String) : JiraDescriptionField(description), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.description = description
    }
}

class SdkJiraProjectField(projectId: Long) : JiraProjectField(projectId), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.projectId = projectId
    }
}

class SdkJiraIssueTypeField(issueTypeId: Int) : JiraIssueTypeField(issueTypeId), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.issueTypeId = issueTypeId.toString()
    }
}

class SdkJiraIssueTypeNameField(issueTypeName: String) : JiraIssueTypeNameField(issueTypeName), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.setIssueType(this)
    }
}

class SdkJiraAssigneeField(username: String) : JiraAssigneeField(username), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.assigneeId = username
    }
}

class SdkJiraReporterField(username: String) : JiraReporterField(username), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.reporterId = username
    }
}

class SdkJiraEpicLinkField(epicIssueKey: String?) : JiraEpicLinkField(epicIssueKey), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.addCustomFieldValue(
            customField().id,
            epicIssueKey
        )
    }
}

class SdkJiraCustomInsightObjectField(
    customFieldName: String,
    insightKey: String?
) : JiraCustomInsightObjectField(customFieldName, insightKey), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.setInsightObjects(this, insightKey?.let { listOf(it) })
    }
}

class SdkJiraCustomInsightObjectsField(
    customFieldName: String,
    insightKeys: List<String>
) : JiraCustomInsightObjectsField(customFieldName, insightKeys), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.setInsightObjects(this, insightKeys)
    }
}

class SdkJiraEpicNameField(
    epicName: String
) : JiraEpicNameField(epicName), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.addCustomFieldValue(customField().id, epicName)
    }
}

class SdkJiraCustomTextField(
    customFieldName: String,
    text: String
) : JiraCustomTextField(customFieldName, text), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.addCustomFieldValue(customField().id, text)
    }
}

class SdkJiraCustomNumberField(
    customFieldName: String,
    value: Number
) : JiraCustomNumberField(customFieldName, value), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        issue.addCustomFieldValue(customField().id, number.toString())
    }
}

class SdkJiraCustomDateTimeField(
    customFieldName: String,
    dateTime: ZonedDateTime
) : JiraCustomDateTimeField(customFieldName, dateTime), SdkJiraField {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MMM/yy h:mm a")
    private val timezoneManager by lazy { ComponentAccessor.getComponent(TimeZoneManager::class.java) }

    override fun render(issue: IssueInputParameters) {
        val customField = customField()
        val zonedDateTime = when (customField.customFieldType.key) {
            CustomFieldTypes.DATEPICKER.key -> {
                // dateTime represents a date with day precision.
                // Jira uses DateCFType internally for days that uses a dateConverter using system time,
                // so it stores selected date at 00:00 in system time.
                dateTime.withZoneSameLocal(ZoneId.systemDefault())
            }
            else -> {
                // dateTime actually represents a DateTime, so we want to store date and time with minute precision
                // the dateFormat has no Zone or Offset, so we need to convert to userTime first
                dateTime.withZoneSameInstant(timezoneManager.loggedInUserTimeZone.toZoneId())
            }
        }
        issue.addCustomFieldValue(customField.id, zonedDateTime.format(dateTimeFormatter))
    }
}

class SdkJiraCustomRadioField(
    customFieldName: String,
    value: String
) : JiraCustomRadioField(customFieldName, value), SdkJiraField {
    override fun render(issue: IssueInputParameters) {
        val customField = customField()
        val issueContext: IssueContext = IssueContextImpl(issue.projectId, issue.issueTypeId)
        val relevantConfig = customField.getRelevantConfig(issueContext)
        val option = optionsManager.getOptions(relevantConfig).getOptionForValue(value, null)
        issue.addCustomFieldValue(customField.id, option.optionId.toString())
    }
}

private fun IssueInputParameters.setIssueType(field: JiraIssueTypeNameField) {
    this.issueTypeId = issueTypeManager.issueTypes.firstOrNull { it.name == field.issueTypeName }?.id
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

private fun IssueInputParameters.setInsightObjects(field: JiraCustomField, objectKeys: List<String>?) {
    addCustomFieldValue(field.customField().id, *(objectKeys?.toTypedArray() ?: emptyArray()))
}
