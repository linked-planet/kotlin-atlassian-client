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

import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.Field
import com.atlassian.jira.issue.fields.FieldException

/**
 * A helper service to access JIRA fields and their values.
 */
interface FieldAccessor {
    /**
     * Gets a list of all JIRA fields that are exportable (see [com.atlassian.jira.issue.export.ExportableSystemField]
     * and [com.atlassian.jira.issue.export.customfield.ExportableCustomFieldType]).
     *
     * @return The list of exportable JIRA fields
     * @throws FieldException in case of a field access error
     */
    @Throws(FieldException::class)
    fun getAllExportableJiraFields(): List<Field?>

    /**
     * Tries to get a field value from the given issue by interpreting the given identifier as a *Custom field ID* or
     * *Custom field name* (ID precedes name).
     *
     * @param issue      The issue to get the field value from
     * @param identifier *Custom field ID* or *Custom field name*
     * @return The field value.<br></br>
     * Null, if the field could not be found, is not an exportable custom field
     * (see [com.atlassian.jira.issue.export.customfield.ExportableCustomFieldType])
     * or an ambiguous *Custom field name* was given.
     */
    fun getCustomFieldValueFromIssue(issue: Issue, identifier: String): String?

    /**
     * Tries to get a field value from the given issue by interpreting the given identifier as a *System field ID*.
     *
     * @param issue      The issue to get the field value from
     * @param identifier *System field ID*
     * @return The field value.<br></br>
     * Null, if the field could not be found or is not an exportable field
     * (see [com.atlassian.jira.issue.export.ExportableSystemField]).
     */
    fun getSystemFieldValueFromIssue(issue: Issue, identifier: String): String?
}
