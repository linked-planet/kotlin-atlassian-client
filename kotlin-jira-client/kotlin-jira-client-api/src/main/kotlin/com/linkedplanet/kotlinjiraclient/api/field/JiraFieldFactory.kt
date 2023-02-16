/*-
 * #%L
 * kotlin-jira-client-api
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
package com.linkedplanet.kotlinjiraclient.api.field

import java.time.ZonedDateTime

interface JiraFieldFactory<JiraFieldType> {
    fun jiraSummaryField(summary: String): JiraFieldType
    fun jiraDescriptionField(description: String): JiraFieldType
    fun jiraProjectField(projectId: Long): JiraFieldType
    fun jiraIssueTypeField(issueTypeId: Int): JiraFieldType
    fun jiraIssueTypeNameField(issueTypeName: String): JiraFieldType
    fun jiraAssigneeField(username: String): JiraFieldType
    fun jiraReporterField(username: String): JiraFieldType
    fun jiraEpicLinkField(epicIssueKey: String?): JiraFieldType
    fun jiraCustomInsightObjectField(customFieldName: String, insightKey: String?): JiraFieldType
    fun jiraCustomInsightObjectsField(customFieldName: String, insightKeys: List<String>): JiraFieldType
    fun jiraEpicNameField(epicName: String): JiraFieldType
    fun jiraCustomTextField(customFieldName: String, text: String): JiraFieldType
    fun jiraCustomNumberField(customFieldName: String, number: Number): JiraFieldType
    fun jiraCustomDateTimeField(customFieldName: String, value: ZonedDateTime): JiraFieldType
    fun jiraCustomRadioField(customFieldName: String, value: String): JiraFieldType
}
