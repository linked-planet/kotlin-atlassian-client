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

import com.linkedplanet.kotlinjiraclient.api.field.JiraFieldFactory
import java.time.ZonedDateTime

object HttpJiraFieldFactory : JiraFieldFactory<HttpJiraField> {
    override fun jiraSummaryField(summary: String) = HttpJiraSummaryField(summary)

    override fun jiraDescriptionField(description: String) = HttpJiraDescriptionField(description)

    override fun jiraProjectField(projectId: Long) = HttpJiraProjectField(projectId)

    override fun jiraIssueTypeField(issueTypeId: Int) = HttpJiraIssueTypeField(issueTypeId)

    override fun jiraIssueTypeNameField(issueTypeName: String) = HttpJiraIssueTypeNameField(issueTypeName)

    override fun jiraAssigneeField(username: String) = HttpJiraAssigneeField(username)

    override fun jiraReporterField(username: String) = HttpJiraReporterField(username)

    override fun jiraEpicLinkField(epicIssueKey: String?) = HttpJiraEpicLinkField(epicIssueKey)

    override fun jiraCustomInsightObjectField(customFieldName: String, insightKey: String?) =
        HttpJiraCustomInsightObjectField(customFieldName, insightKey)

    override fun jiraCustomInsightObjectsField(customFieldName: String, insightKeys: List<String>) =
        HttpJiraCustomInsightObjectsField(customFieldName, insightKeys)

    override fun jiraEpicNameField(epicName: String) = HttpJiraEpicNameField(epicName)

    override fun jiraCustomTextField(customFieldName: String, text: String) =
        HttpJiraCustomTextField(customFieldName, text)

    override fun jiraCustomNumberField(customFieldName: String, number: Number) =
        HttpJiraCustomNumberField(customFieldName, number)

    override fun jiraCustomDateTimeField(customFieldName: String, value: ZonedDateTime) =
        HttpJiraCustomDateTimeField(customFieldName, value)

    override fun jiraCustomRadioField(customFieldName: String, value: String) =
        HttpJiraCustomRadioField(customFieldName, value)

}
