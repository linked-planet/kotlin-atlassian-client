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
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightSchema
import com.linkedplanet.kotlininsightclient.http.model.HttpInsightSchemaListApiResponse
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

open class HttpInsightSchemaOperator(
    private val context: HttpInsightClientContext
) :
    InsightSchemaOperator {

    override suspend fun getSchemas(): Either<InsightClientError, List<InsightSchema>> =
        context.httpClient.executeRest<HttpInsightSchemaListApiResponse>(
            "GET",
            "/rest/insight/1.0/objectschema/list",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<HttpInsightSchemaListApiResponse>() {}.type
        )
            .map { it.body!!.objectschemas }
            .mapLeft { it.toInsightClientError() }

    override suspend fun getSchema(id: Int): Either<InsightClientError, InsightSchema> =
        context.httpClient.executeRest<InsightSchema>(
            "GET",
            "/rest/insight/1.0/objectschema/$id",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<InsightSchema>() {}.type
        )
            .map { it.body!! }
            .mapLeft { it.toInsightClientError() }
}
