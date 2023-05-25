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
import com.atlassian.jira.datetime.DateTimeFormatter
import com.atlassian.jira.datetime.DateTimeFormatterFactory
import com.atlassian.jira.datetime.DateTimeStyle
import com.riadalabs.jira.plugins.insight.common.tools.InsightDateTimeFormatter
import com.riadalabs.jira.plugins.insight.services.jira.JIRAPluginService
import io.riada.insight.common.InsightAuthenticationContext
import java.util.*

/**
 * Thin layer around Jira DateTime parsing. Kotlin version of
 * com.riadalabs.jira.plugins.insight.common.tools.InsightDateTimeFormatterInJira
 */
internal class ReverseEngineeredDateTimeFormatterInJira : InsightDateTimeFormatter {

    private val formatterFactory by lazy { getComponent(DateTimeFormatterFactory::class.java) }
    private val authCtx by lazy { getComponent(InsightAuthenticationContext::class.java) }
    private val jiraPluginService by lazy { getComponent(JIRAPluginService::class.java) }

    override fun parseToDate(date: String?): Date? =
        if (date.isNullOrEmpty()) null else getDateFormatter().parse(date)

    override fun parseToDateTime(dateTime: String?): Date? =
        if (dateTime.isNullOrEmpty()) null else getDateTimeFormatter().parse(dateTime)

    override fun parseSystemDate(date: String?): Date? =
        if (date.isNullOrEmpty()) null else getDateFormatter().withSystemZone().parse(date)

    override fun parseSystemDateTime(dateTime: String?): Date? =
        if (dateTime.isNullOrEmpty()) null else getDateTimeFormatter().withSystemZone().parse(dateTime)

    override fun formatDateToString(date: Date?): String? = date?.let {
        try {
            getDateFormatter().format(it)
        } catch (var3: Exception) {
            null
        }
    }

    override fun formatDateTimeToString(date: Date?): String? = date?.let {
        try {
            getDateTimeFormatter().format(it)
        } catch (var3: Exception) {
            null
        }
    }

    private fun getDateFormatter(): DateTimeFormatter {
        var dateTimeFormatter = formatterFactory.formatter()
        authCtx.loggedInUser?.let { user ->
            dateTimeFormatter = dateTimeFormatter.forUser(jiraPluginService.getUser(user.key))
        }
        return dateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).withZone(TimeZone.getTimeZone("UTC"))
    }

    private fun getDateTimeFormatter(): DateTimeFormatter {
        var dateTimeFormatter = formatterFactory.formatter()
        authCtx.loggedInUser?.let { user ->
            dateTimeFormatter = dateTimeFormatter.forUser(jiraPluginService.getUser(user.key))
        }
        return dateTimeFormatter.withStyle(DateTimeStyle.DATE_TIME_PICKER)
    }
}