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
package com.linkedplanet.kotlininsightclient.api.interfaces

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.model.InsightSchema

/**
 * This interface provides methods to retrieve information about Insight schemas.
 */
interface InsightSchemaOperator {

    /**
     * Retrieves a list of all Insight schemas.
     *
     * @return Either an [InsightClientError] or a list of [InsightSchema]s.
     */
    suspend fun getSchemas(): Either<InsightClientError, List<InsightSchema>>

    /**
     * Retrieves a specific Insight schema by its ID.
     *
     * @param id The ID of the schema to retrieve.
     * @return Either an [InsightClientError] or the [InsightSchema] with the specified ID.
     */
    suspend fun getSchema(id: Int): Either<InsightClientError, InsightSchema>
}
