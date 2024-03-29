/*-
 * #%L
 * kotlin-insight-client-api
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
package com.linkedplanet.kotlininsightclient.sdk

import arrow.core.Either
import com.atlassian.jira.component.ComponentAccessor
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightHistoryOperator
import com.linkedplanet.kotlininsightclient.api.model.Actor
import com.linkedplanet.kotlininsightclient.api.model.InsightHistory
import com.linkedplanet.kotlininsightclient.api.model.InsightHistoryItem
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.linkedplanet.kotlininsightclient.sdk.util.toISOString
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectHistoryBean

object SdkInsightHistoryOperator : InsightHistoryOperator {

    private val objectFacade by lazy { ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade::class.java) }

    override suspend fun getHistory(objectId: InsightObjectId): Either<InsightClientError, InsightHistory> =
        catchAsInsightClientError {
            val historyItems =
                objectFacade.findObjectHistoryBean(objectId.raw).map { objectHistoryBean: ObjectHistoryBean ->
                    objectHistoryBean.run {
                        InsightHistoryItem(
                            id,
                            affectedAttribute,
                            oldValue,
                            newValue,
                            Actor(key = actorUserKey),
                            type,
                            created.toISOString(),
                            objectId
                        )
                    }
                }
            InsightHistory(objectId, historyItems)
        }
}
