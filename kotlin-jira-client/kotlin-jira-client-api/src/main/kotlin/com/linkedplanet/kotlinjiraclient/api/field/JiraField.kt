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
import java.time.format.DateTimeFormatter

val JIRA_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

abstract class JiraCustomField(
    val customFieldName: String
)

abstract class JiraSummaryField(val summary: String)

abstract class JiraDescriptionField(val description: String)

abstract class JiraProjectField(val projectId: Long)

abstract class JiraIssueTypeField(val issueTypeId: Int)

abstract class JiraIssueTypeNameField(val issueTypeName: String)

abstract class JiraAssigneeField(val username: String)

abstract class JiraReporterField(val username: String)

abstract class JiraEpicLinkField(
    val epicIssueKey: String?
) : JiraCustomField("Epic Link")

abstract class JiraCustomInsightObjectField(
    customFieldName: String,
    val insightKey: String?
) : JiraCustomField(customFieldName)

abstract class JiraCustomInsightObjectsField(
    customFieldName: String,
    val insightKeys: List<String>
) : JiraCustomField(customFieldName)

abstract class JiraEpicNameField(
    val epicName: String
) : JiraCustomField("Epic Name")

abstract class JiraCustomTextField(
    customFieldName: String,
    val text: String
) : JiraCustomField(customFieldName)

abstract class JiraCustomNumberField(
    customFieldName: String,
    val number: Number
) : JiraCustomField(customFieldName)

abstract class JiraCustomDateTimeField(
    customFieldName: String,
    val dateTime: ZonedDateTime
) : JiraCustomField(customFieldName)

abstract class JiraCustomRadioField(
    customFieldName: String,
    val value: String
) : JiraCustomField(customFieldName)
