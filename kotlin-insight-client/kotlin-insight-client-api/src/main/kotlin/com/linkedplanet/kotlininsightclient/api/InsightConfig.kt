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
package com.linkedplanet.kotlininsightclient.api

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.model.InsightSchemaDescription
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaCacheOperatorInterface
import kotlinx.coroutines.runBlocking
import com.linkedplanet.kotlinhttpclient.api.http.BaseHttpClient
import com.linkedplanet.kotlinhttpclient.error.DomainError

object InsightConfig {

    lateinit var baseUrl: String
    lateinit var httpClient: BaseHttpClient
    lateinit var insightSchemaCacheOperator: InsightSchemaCacheOperatorInterface
    var objectSchemas: List<ObjectTypeSchema> = emptyList()
    var schemaDescriptionCache: List<InsightSchemaDescription> = emptyList()


    fun <T: BaseHttpClient> init(
        baseUrlIn: String,
        httpClientIn: T,
        insightSchemaOperator: InsightSchemaCacheOperatorInterface
    ): Either<DomainError, Unit> {
        baseUrl = baseUrlIn
        httpClient = httpClientIn
        return runBlocking {
            insightSchemaOperator.updateSchemaCache()
        }
    }
}
