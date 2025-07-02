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
package com.linkedplanet.kotlinjiraclient.sdk.util

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.Field
import com.atlassian.jira.issue.fields.FieldException
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem
import com.atlassian.jira.issue.fields.rest.RestAwareField
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls
import com.atlassian.jira.rest.v2.issue.IncludedFields
import com.atlassian.jira.rest.v2.issue.IssueBean
import com.atlassian.jira.rest.v2.issue.builder.BeanBuilderFactory
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.JsonObject
import com.google.gson.JsonParser as GsonJsonParser
import com.linkedplanet.kotlinjiraclient.api.model.IssueQueryParams
import com.linkedplanet.kotlinjiraclient.sdk.field.FieldAccessorImpl
import org.slf4j.LoggerFactory
import javax.ws.rs.core.UriBuilder

/**
 * Converts a Jira Issue to Json.
 * Uses the same building blocks as jira itself to create the json, so should be compatible with Jiras REST api.
 * Uses IssueBean internally, which is Jiras Json Object.
 */
class IssueJsonConverter {

    private val log = LoggerFactory.getLogger(FieldAccessorImpl::class.java)
    private val fieldLayoutManager = ComponentAccessor.getFieldLayoutManager()
    private val fieldManager = ComponentAccessor.getFieldManager()
    private var _beanBuilderFactory = ComponentAccessor.getOSGiComponentInstanceOfType(BeanBuilderFactory::class.java)
    private val beanBuilderFactory: BeanBuilderFactory
        get() {
            // For unknown reasons the factory is sometimes null. This workaround tries to fetch the instance again.
            if (_beanBuilderFactory == null) {
                log.info("_beanBuilderFactory is null. Using getOSGiComponentInstanceOfType to recover.")
                _beanBuilderFactory = ComponentAccessor.getOSGiComponentInstanceOfType(BeanBuilderFactory::class.java)
            }
            if (_beanBuilderFactory == null) {
                log.info("_beanBuilderFactory is null. Using getComponent to recover.")
                _beanBuilderFactory = ComponentAccessor.getComponent(BeanBuilderFactory::class.java)
            }
            if (_beanBuilderFactory == null) {
                log.error("_beanBuilderFactory is neither provided by getComponent nor OSGi. Giving up. ")
            }
            return _beanBuilderFactory
        }
    private val jiraBaseUrls: JiraBaseUrls = ComponentAccessor.getComponent(JiraBaseUrls::class.java)
    private val uriBuilder: UriBuilder = UriBuilder.fromPath(jiraBaseUrls.restApi2BaseUrl())

    private val jacksonObjectMapper = jacksonObjectMapper().apply {
        // we need to detect private fields, because some classes do not use public getters for JsonProperties
        // See IssueRefJsonBean, which uses a modern fluent, no getter "data class" approach
        setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

        setAnnotationIntrospector(object : JacksonAnnotationIntrospector() {
            override fun findNameForSerialization(a: Annotated): PropertyName? {
                if (!a.hasAnnotation(JsonProperty::class.java)
                    && !a.hasAnnotation(JsonInclude::class.java)
                    && !a.hasAnnotation(javax.xml.bind.annotation.XmlElement::class.java)
                    && !a.hasAnnotation(javax.xml.bind.annotation.XmlAttribute::class.java)
                ) {
                    return null
                }
                return super.findNameForSerialization(a) ?: PropertyName.NO_NAME
            }
        })
    }

    @Throws(FieldException::class)
    fun createJsonIssue(
        issue: Issue,
        queryParams: IssueQueryParams,
    ): JsonObject {
        val expanded = queryParams.expanded.joinToString(",")
        val issueBean: IssueBean = beanBuilderFactory
            .newIssueBeanBuilder2(IncludedFields.includeNavigableByDefault(null), expanded, uriBuilder)
            .build(issue)
        this.addOrderableFieldsToBean(issueBean, issue)
        this.addAvailableNavigableFieldsToBean(issueBean, issue)

        // Jackson is the official way now to serialize Beans. GSON will fail due to infinite loops in the model.
        val jackson = jacksonObjectMapper
        val jacksonJson: JsonNode = jackson.valueToTree(issueBean)

        return GsonJsonParser.parseString(jacksonJson.toString()).asJsonObject // expose as GSON for API compatibility
    }

    @Throws(FieldException::class)
    private fun addOrderableFieldsToBean(bean: IssueBean, issue: Issue) {
        val fieldLayoutItems = fieldLayoutManager.getFieldLayout(issue).fieldLayoutItems
        fieldLayoutItems
            .filter { !bean.hasField(it.orderableField.id) }
            .forEach { fieldLayoutItem ->
                val field = fieldLayoutItem.orderableField
                field.addJsonFromIssue(issue, fieldLayoutItem, bean)
            }
    }

    @Throws(FieldException::class)
    private fun addAvailableNavigableFieldsToBean(bean: IssueBean, issue: Issue) {
        fieldManager
            .allAvailableNavigableFields
            .filter { !bean.hasField(it.id) }
            .forEach { field ->
                field.addJsonFromIssue(issue, null, bean)
            }
    }

    private fun Field.addJsonFromIssue(issue: Issue, fieldLayoutItem: FieldLayoutItem?, bean: IssueBean) {
        val json = (this as? RestAwareField)?.getJsonFromIssue(issue, false, fieldLayoutItem)
        if (json == null) log.warn("${javaClass.simpleName} with name ${this.name} with id $id not rendered in JSON")
        if (json != null && json.standardData != null) {
            bean.addField(this, json, false)
        }
    }
}
