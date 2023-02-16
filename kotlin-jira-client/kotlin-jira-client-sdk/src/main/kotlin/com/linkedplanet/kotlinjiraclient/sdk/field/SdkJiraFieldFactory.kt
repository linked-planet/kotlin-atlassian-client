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

import com.linkedplanet.kotlinjiraclient.api.field.JiraFieldFactory
import java.time.ZonedDateTime

object SdkJiraFieldFactory : JiraFieldFactory<SdkJiraField> {
    override fun jiraSummaryField(summary: String) = SdkJiraSummaryField(summary)

    override fun jiraDescriptionField(description: String) = SdkJiraDescriptionField(description)

    override fun jiraProjectField(projectId: Long) = SdkJiraProjectField(projectId)

    override fun jiraIssueTypeField(issueTypeId: Int) = SdkJiraIssueTypeField(issueTypeId)

    override fun jiraIssueTypeNameField(issueTypeName: String) = SdkJiraIssueTypeNameField(issueTypeName)

    override fun jiraAssigneeField(username: String) = SdkJiraAssigneeField(username)

    override fun jiraReporterField(username: String) = SdkJiraReporterField(username)

    override fun jiraEpicLinkField(epicIssueKey: String?) = SdkJiraEpicLinkField(epicIssueKey)

    override fun jiraCustomInsightObjectField(customFieldName: String, insightKey: String?) =
        SdkJiraCustomInsightObjectField(customFieldName, insightKey)

    override fun jiraCustomInsightObjectsField(customFieldName: String, insightKeys: List<String>) =
        SdkJiraCustomInsightObjectsField(customFieldName, insightKeys)

    override fun jiraEpicNameField(epicName: String) = SdkJiraEpicNameField(epicName)

    override fun jiraCustomTextField(customFieldName: String, text: String) =
        SdkJiraCustomTextField(customFieldName, text)

    override fun jiraCustomNumberField(customFieldName: String, number: Number) =
        SdkJiraCustomNumberField(customFieldName, number)

    override fun jiraCustomDateTimeField(customFieldName: String, value: ZonedDateTime) =
        SdkJiraCustomDateTimeField(customFieldName, value)

    override fun jiraCustomRadioField(customFieldName: String, value: String) =
        SdkJiraCustomRadioField(customFieldName, value)

}

