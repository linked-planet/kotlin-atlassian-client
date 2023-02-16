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
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.export.ExportableSystemField
import com.atlassian.jira.issue.export.FieldExportPart
import com.atlassian.jira.issue.export.customfield.ExportableCustomFieldType
import com.atlassian.jira.issue.fields.*
import com.atlassian.jira.util.I18nHelper
import java.util.ArrayList
import java.util.Collections
import java.util.stream.Collectors
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import javax.inject.Named

@Named
class FieldAccessorImpl(
    private val fieldManager: FieldManager = ComponentAccessor.getFieldManager(),
    private val customFieldManager: CustomFieldManager = ComponentAccessor.getCustomFieldManager(),
    private val i18nHelper: I18nHelper = ComponentAccessor.getI18nHelperFactory()
        .getInstance(ComponentAccessor.getJiraAuthenticationContext().loggedInUser)
) : FieldAccessor {

    private val LOG = LoggerFactory.getLogger(FieldAccessorImpl::class.java)
    private val VALUE_REPRESENTATION_IDS_BY_FIELD_ID: Map<String, String> = mapOf(
        "issuekey" to "key",
        "project" to "projectName"
    )

    @Throws(FieldException::class)
    override fun getAllExportableJiraFields(): List<Field> {
        val fields = fieldManager.allAvailableNavigableFields.stream()
            .filter { field ->
                !field.name.startsWith("?")
                        && (field is ExportableSystemField
                        || (field is CustomField && field.customFieldType is ExportableCustomFieldType))
            }
            .sorted { field1: NavigableField, field2: NavigableField ->
                StringUtils.compare(i18nHelper.getText(field1.nameKey), i18nHelper.getText(field2.nameKey))
            }
            .collect(Collectors.toList())
        return Collections.unmodifiableList(fields)
    }

    override fun getCustomFieldValueFromIssue(issue: Issue, identifier: String): String? {
        var customField = customFieldManager.getCustomFieldObject(identifier)
        if (customField == null) {
            val customFieldObjects = customFieldManager.getCustomFieldObjectsByName(identifier)
            when {
                customFieldObjects.isEmpty() -> {
                    LOG.warn("Custom field '{}' not found.", identifier)
                    return null
                }
                customFieldObjects.size > 1 -> {
                    LOG.error("Ambiguous custom field name {}. Use the custom field ID instead.", identifier)
                    return null
                }
                else -> customField = customFieldObjects.first()!!
            }
        }
        val customFieldType = customField.customFieldType!!
        if (customFieldType !is ExportableCustomFieldType) {
            LOG.warn("Value of custom field '{}' is not exportable.", identifier)
            return null
        }
        val fieldValues: MutableList<String?> = ArrayList()
        (customFieldType)
            .getRepresentationFromIssue(issue, CustomFieldExportContext(customField, i18nHelper))
            .parts
            .stream()
            .filter { fieldExportPart: FieldExportPart ->
                (StringUtils.equals(
                    fieldExportPart.id,
                    VALUE_REPRESENTATION_IDS_BY_FIELD_ID.getOrDefault(identifier, identifier)
                )
                        || StringUtils.equals(fieldExportPart.itemLabel, identifier))
            }
            .findAny()
            .ifPresent { fieldExportPart: FieldExportPart ->
                fieldExportPart.values.forEach { value: String? ->
                    fieldValues.add(value)
                }
            }
        return StringUtils.join(fieldValues, ", ")
    }

    override fun getSystemFieldValueFromIssue(issue: Issue, identifier: String): String? =
        when (val field = fieldManager.getField(identifier)) {
            null -> {
                LOG.warn("System field '{}' not found.", identifier)
                null
            }
            !is ExportableSystemField -> {
                LOG.warn("Value of system field '{}' is not exportable.", identifier)
                null
            }
            else -> {
                val fieldValues: MutableList<String?> = ArrayList()
                (field as ExportableSystemField)
                    .getRepresentationFromIssue(issue)
                    .getPartWithId(VALUE_REPRESENTATION_IDS_BY_FIELD_ID.getOrDefault(identifier, identifier))
                    .ifPresent { fieldExportPart: FieldExportPart ->
                        fieldExportPart.values.forEach { value: String? ->
                            fieldValues.add(value)
                        }
                    }
                StringUtils.join(fieldValues, ", ")
            }
        }

    private class CustomFieldExportContext(private val customField: CustomField, private val i18nHelper: I18nHelper) :
        com.atlassian.jira.issue.export.customfield.CustomFieldExportContext {
        override fun getCustomField() = customField
        override fun getI18nHelper() = i18nHelper
        override fun getDefaultColumnHeader(): String =
            i18nHelper.getText(customField.columnHeadingKey, customField.name)
    }
}
