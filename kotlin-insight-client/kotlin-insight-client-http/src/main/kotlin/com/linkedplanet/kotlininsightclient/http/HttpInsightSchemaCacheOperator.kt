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
import arrow.core.computations.either
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaCacheOperator
import com.linkedplanet.kotlininsightclient.api.model.*
import org.joda.time.DateTime

object HttpInsightSchemaCacheOperator :
    InsightSchemaCacheOperator {

    override var lastUpdate: DateTime? = null

    override suspend fun updateSchemaCache(): Either<InsightClientError, Unit> =
        either {
            val newCache = getSchemaCache().bind()
            val newSchema = HttpInsightObjectTypeOperator.loadAllObjectTypeSchemas().bind()
            HttpInsightClientConfig.objectSchemas = newSchema
            HttpInsightClientConfig.schemaDescriptionCache = newCache
            lastUpdate = DateTime.now()
        }


    override suspend fun getSchemaCache(): Either<InsightClientError, List<InsightSchemaDescription>> =
        either {
            val schemas = HttpInsightSchemaOperator.getSchemas().bind().objectschemas
            schemas.map { schema ->
                val objectTypes = HttpInsightObjectTypeOperator.getObjectTypesBySchema(schema.id).bind()
                val objectTypeDescriptions = objectTypes.map {
                    val attributes =
                        it.attributes.map { InsightAttributeDescription(it.id, it.name, it.defaultType?.name ?: "") }
                    InsightObjectTypeDescription(it.id, it.name, it.parentObjectTypeId, attributes)
                }
                InsightSchemaDescription(schema.id, schema.name, objectTypeDescriptions)
            }
        }
}
