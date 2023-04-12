/*-
 * #%L
 * kotlin-insight-client-sdk
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
package com.linkedplanet.kotlininsightclient.sdk.services

import com.atlassian.jira.component.ComponentAccessor.getComponent
import com.atlassian.jira.config.properties.ApplicationProperties
import com.riadalabs.jira.plugins.insight.services.model.AttachmentBean
import java.util.regex.Pattern


internal class ReverseEngineeredAttachmentUrlResolver {

    private val applicationProperties by lazy { getComponent(ApplicationProperties::class.java) }

    private val pattern = Pattern.compile(".*/(\\d)/?")
    private val INSIGHT_REST_BASE_URL = "/rest/insight/1.0"

    private fun getInsightBaseUrl(): String {
        return applicationProperties.getString("jira.baseurl")!!
    }

    private fun getInsightRestBaseUrl(): String {
        val insightBaseUrl = getInsightBaseUrl()
        return insightBaseUrl + INSIGHT_REST_BASE_URL
    }

    fun buildUrlForAttachment(attachment: AttachmentBean): String {
        val insightRestBaseUrl = getInsightRestBaseUrl()
        return String.format(
            "%s/attachments/%d/%s",
            insightRestBaseUrl,
            attachment.id,
            attachment.filename
        )
    }

    fun parseAttachmentIdFromPathInformation(pathInfo: String): Int {
        val matcher = pattern.matcher(pathInfo)
        if (!matcher.find()) {
            throw IllegalArgumentException("Invalid attachment URL: $pathInfo")
        }
        return matcher.group(1).toInt()
    }

}