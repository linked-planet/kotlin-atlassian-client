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

import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeAttributeDefaultType // TODO: do not mix our objects with atlassian rest responses

// This file contains objects that are received from Insights REST API

internal data class InsightObjectEntries(
    val totalFilterCount: Int,
    val objectEntries: List<InsightObjectApiResponse>
)

internal data class InsightObjectApiResponse(
    val id: Int,
    val label: String,
    val objectKey: String,
    val objectType: InsightMetaObjectType,
    val attributes: List<InsightAttributeApiResponse>,
    val extendedInfo: InsightExtendedInfo
)

internal data class InsightExtendedInfo/*ApiResponse*/(
    val openIssuesExists: Boolean,
    val attachmentsExists: Boolean
)

internal data class InsightMetaObjectType/*ApiResponse*/(
    val id: Int,
    val name: String,
    val objectSchemaId: Int
)

internal data class InsightAttributeApiResponse(
    val id: Int,
    val objectTypeAttribute: ObjectTypeAttribute?,
    val objectTypeAttributeId: Int,
    val objectId: Int,
    val objectAttributeValues: List<ObjectAttributeValue>
)

internal data class ObjectTypeAttribute/*ApiResponse*/(
    val id: Int,
    val name: String,
    val referenceObjectTypeId: Int,
    val referenceObjectType: InsightMetaObjectType,
    val type: Int,
    val defaultType: ObjectTypeAttributeDefaultType?,
    val options: String,
    val minimumCardinality: Int,
    val maximumCardinality: Int,
    val includeChildObjectTypes: Boolean
)