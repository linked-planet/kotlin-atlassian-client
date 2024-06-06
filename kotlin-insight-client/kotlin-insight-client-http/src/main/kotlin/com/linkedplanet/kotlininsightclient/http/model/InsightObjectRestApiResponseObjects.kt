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

// This file contains objects that are received from Insights REST API

internal data class InsightObjectEntriesApiResponse(
    val totalFilterCount: Int,
    val objectEntries: List<InsightObjectApiResponse>
)

internal data class InsightObjectApiResponse(
    val id: Int,
    val label: String,
    val objectKey: String,
    val objectType: InsightMetaObjectTypeApiResponse,
    val attributes: List<InsightAttributeApiResponse>,
    val extendedInfo: InsightExtendedInfoApiResponse
)

internal data class InsightExtendedInfoApiResponse(
    val openIssuesExists: Boolean,
    val attachmentsExists: Boolean
)

internal data class InsightMetaObjectTypeApiResponse(
    val id: Int,
    val name: String,
    val objectSchemaId: Int
)

internal data class InsightAttributeApiResponse(
    val id: Int,
    val objectTypeAttribute: ObjectTypeAttributeApiResponse?,
    val objectTypeAttributeId: Int,
    val objectId: Int,
    val objectAttributeValues: List<ObjectAttributeValueApiResponse>
)

internal data class ObjectTypeAttributeApiResponse(
    val id: Int,
    val name: String,
    val referenceObjectTypeId: Int,
    val referenceObjectType: InsightMetaObjectTypeApiResponse,
    val type: Int,
    val defaultType: ObjectTypeAttributeDefaultTypeApiResponse?,
    val options: String,
    val minimumCardinality: Int,
    val maximumCardinality: Int,
    val includeChildObjectTypes: Boolean
)

internal data class ObjectAttributeValueApiResponse(
    var value: Any?,
    var displayValue: Any?,
    var referencedObject: ReferencedObjectApiResponse?,
    var user: AtlassianUserApiResponse?,
    var group: AtlassianGroupApiResponse?,
    var confluencePage: AtlassianConfluencePageApiResponse?,
    var version: AtlassianVersionApiResponse?,
    var project: AtlassianProjectApiResponse?,
    var status: AtlassianStatusApiResponse?,

)

internal data class AtlassianGroupApiResponse(
    val avatarUrl: String,
    val name: String,
)

internal data class AtlassianConfluencePageApiResponse(
    val id: String,
    val title: String,
    val url: String,
)

internal data class AtlassianVersionApiResponse(
    val avatarUrl: String,
    val id: String,
    val name: String,
    val url: String,
)

internal data class AtlassianProjectApiResponse(
    val avatarUrl: String,
    val id: String,
    val name: String,
    val key: String,
    val url: String,
)

internal data class AtlassianStatusApiResponse(
    val id: String,
    val name: String,
    val description: String,
    val category: Int, // ACTIVE == 1, INACTIVE == 0, PENDING == 2
    val objectSchemaId: Int,
)

internal data class AtlassianUserApiResponse(
    val displayName: String,
    val name: String,
    val emailAddress: String?,
    val key: String,
    val avatarUrl: String?
)

internal data class ReferencedObjectApiResponse(
    var id: Int,
    var label: String,
    var objectKey: String,
    var objectType: ReferencedObjectTypeApiResponse?
)

internal data class ReferencedObjectTypeApiResponse(
    val id: Int,
    val name: String
)

internal data class ObjectTypeAttributeDefaultTypeApiResponse(
    val id: Int,
    val name: String
)

internal data class ObjectTypeSchemaAttributeReferenceTypeApiResponse(
    val id: Int, // see Model.ReferenceKind
    val name: String,
    val type: Int
)