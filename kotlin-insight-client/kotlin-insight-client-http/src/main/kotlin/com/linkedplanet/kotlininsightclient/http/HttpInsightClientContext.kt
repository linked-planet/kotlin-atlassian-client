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

import com.linkedplanet.kotlinhttpclient.api.http.BaseHttpClient
import com.linkedplanet.kotlininsightclient.api.model.InsightSchemaDescription
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import kotlinx.coroutines.runBlocking

class HttpInsightClientContext(val baseUrl: String, val httpClient: BaseHttpClient) {
    private val insightSchemaCacheOperator = HttpInsightSchemaCacheOperator(this)

    var objectSchemas: List<ObjectTypeSchema> = emptyList()
    var schemaDescriptionCache: List<InsightSchemaDescription> = emptyList()

    init {
        runBlocking {
            insightSchemaCacheOperator.updateSchemaCache()
        }
    }
}
