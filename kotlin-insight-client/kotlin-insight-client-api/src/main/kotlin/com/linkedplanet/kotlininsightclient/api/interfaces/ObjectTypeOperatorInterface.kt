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
import com.linkedplanet.kotlinhttpclient.error.DomainError
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema

interface ObjectTypeOperatorInterface {

    suspend fun loadAllObjectTypeSchemas(): Either<DomainError, List<ObjectTypeSchema>>

    suspend fun loadObjectTypeSchemas(schemaId: Int): Either<DomainError, List<ObjectTypeSchema>>

    suspend fun reloadObjectTypeSchema(schemaId: Int, name: String): Either<DomainError, Unit>

    suspend fun reloadObjectTypeSchema(schemaId: Int, id: Int): Either<DomainError, Unit>

    suspend fun getObjectTypesBySchema(schemaId: Int): Either<DomainError, List<ObjectTypeSchema>>

    suspend fun getObjectTypesBySchemaAndRootObjectType(
        schemaId: Int,
        rootObjectTypeId: Int
    ): Either<DomainError, List<ObjectTypeSchema>>

    suspend fun getObjectTypeSchemas(schemaId: Int): Either<DomainError, List<ObjectTypeSchema>>

    suspend fun populateObjectTypeSchemaAttributes(objectTypeSchema: ObjectTypeSchema): Either<DomainError, ObjectTypeSchema>
}
