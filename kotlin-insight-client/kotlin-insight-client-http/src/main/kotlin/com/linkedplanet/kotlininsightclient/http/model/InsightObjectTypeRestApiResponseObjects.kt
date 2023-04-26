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
package com.linkedplanet.kotlininsightclient.http.model

internal data class ObjectTypeSchemaApiResponse(
    val id: Int,
    val name: String,
    var attributes: List<ObjectTypeSchemaAttributeApiResponse>,
    val parentObjectTypeId: Int?,

    val type: Int,
    val objectSchemaId: Int,
    val objectCount: Int,
    val inherited: Boolean,
    val abstractObjectType: Boolean,
    val parentObjectTypeInherited: Boolean
)

internal data class ObjectTypeSchemaAttributeApiResponse(
    val id: Int,
    val name: String,
    val defaultType: ObjectTypeAttributeDefaultTypeApiResponse?,
    val options: String,
    val minimumCardinality: Int,
    val maximumCardinality: Int,
    val referenceType: ObjectTypeSchemaAttributeReferenceTypeApiResponse?,
    val includeChildObjectTypes: Boolean,
    val referenceObjectTypeId: Int?,
    val type: Int // AttributeType 0=Default 1=Reference
    // uniqueAttribute: Boolean
    // hidden: Boolean
)
