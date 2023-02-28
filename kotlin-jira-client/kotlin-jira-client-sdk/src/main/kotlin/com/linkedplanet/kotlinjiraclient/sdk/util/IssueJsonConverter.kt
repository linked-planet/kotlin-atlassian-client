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
import com.google.gson.*
import com.linkedplanet.kotlinjiraclient.sdk.field.FieldAccessorImpl
import org.slf4j.LoggerFactory
import javax.ws.rs.core.UriBuilder
import javax.xml.bind.annotation.XmlTransient

/**
 * Converts a Jira Issue to Json.
 * Uses the same building blocks as jira itself to create the json, so should be compatible with Jiras REST api.
 * Uses IssueBean internally, which is Jiras Json Object.
 */
class IssueJsonConverter {

    private val log = LoggerFactory.getLogger(FieldAccessorImpl::class.java)
    private val fieldLayoutManager by lazy { ComponentAccessor.getFieldLayoutManager() }
    private val fieldManager by lazy { ComponentAccessor.getFieldManager() }
    private val beanBuilderFactory = ComponentAccessor.getOSGiComponentInstanceOfType(BeanBuilderFactory::class.java)
    private val jiraBaseUrls: JiraBaseUrls = ComponentAccessor.getComponent(JiraBaseUrls::class.java)
    private val uriBuilder: UriBuilder = UriBuilder.fromPath(jiraBaseUrls.restApi2BaseUrl())
    private val gson by lazy { setupGson() }


    @Throws(FieldException::class)
    fun createJsonIssue(issue: Issue): JsonObject {
        val expand = "names,transitions"
        val issueBean: IssueBean = beanBuilderFactory
            .newIssueBeanBuilder2(IncludedFields.includeNavigableByDefault(null), expand, uriBuilder)
            .build(issue)
        this.addOrderableFieldsToBean(issueBean, issue)
        this.addAvailableNavigableFieldsToBean(issueBean, issue)
        this.apply { }

        return gson.toJsonTree(issueBean).asJsonObject
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

    private fun setupGson() =
        GsonBuilder()
            .setExclusionStrategies(object : ExclusionStrategy {
                override fun shouldSkipField(f: FieldAttributes?): Boolean {
                    return f?.getAnnotation(XmlTransient::class.java) != null
                }

                override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                    return false
                }
            })
            .create()
}
