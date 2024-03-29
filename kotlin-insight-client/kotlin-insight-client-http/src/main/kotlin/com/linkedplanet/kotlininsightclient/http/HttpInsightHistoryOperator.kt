/*-
 * #%L
 * kotlin-insight-client-http
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
package com.linkedplanet.kotlininsightclient.http

import arrow.core.Either
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightHistoryOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightHistory
import com.linkedplanet.kotlininsightclient.api.model.InsightHistoryItem
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

class HttpInsightHistoryOperator(private val context: HttpInsightClientContext) : InsightHistoryOperator {

    override suspend fun getHistory(objectId: InsightObjectId): Either<InsightClientError, InsightHistory> {
        val historyItems = context.httpClient.executeRestList<InsightHistoryItem>(
            "GET",
            "rest/insight/1.0/object/${objectId.raw}/history",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<List<InsightHistoryItem>>() {}.type
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }
        return historyItems.map { InsightHistory(objectId, it) }
    }
}
