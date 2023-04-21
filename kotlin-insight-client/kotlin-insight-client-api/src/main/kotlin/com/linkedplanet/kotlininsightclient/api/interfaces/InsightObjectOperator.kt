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
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjects

interface InsightObjectOperator {

    var RESULTS_PER_PAGE: Int

    suspend fun getObjects(
        objectTypeId: Int,
        withChildren: Boolean = false,
        pageFrom: Int = 1,
        perPage: Int = RESULTS_PER_PAGE
    ): Either<InsightClientError, InsightObjects>

    suspend fun getObjectById(id: Int): Either<InsightClientError, InsightObject?>

    suspend fun getObjectByKey(key: String): Either<InsightClientError, InsightObject?>

    suspend fun getObjectByName(objectTypeId: Int, name: String): Either<InsightClientError, InsightObject?>

    suspend fun getObjectsByIQL(
        objectTypeId: Int,
        iql: String,
        withChildren: Boolean = false,
        pageFrom: Int = 1,
        perPage: Int = RESULTS_PER_PAGE
    ): Either<InsightClientError, InsightObjects>

    suspend fun getObjectsByIQL(
        iql: String,
        pageFrom: Int = 1,
        perPage: Int = RESULTS_PER_PAGE
    ): Either<InsightClientError, InsightObjects>

    suspend fun getObjectCount(iql: String): Either<InsightClientError, Int>

    suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject>

    suspend fun deleteObject(id: Int): Either<InsightClientError, Unit>

    suspend fun createObject(
        objectTypeId: Int,
        func: (InsightObject) -> Unit
    ): Either<InsightClientError, InsightObject>
}
