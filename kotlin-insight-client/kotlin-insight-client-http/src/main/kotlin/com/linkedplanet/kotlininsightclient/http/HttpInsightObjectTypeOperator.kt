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
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.ObjectTypeNotFoundError
import com.linkedplanet.kotlininsightclient.api.error.asEither
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttributeId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightSchemaId
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.ReferenceKind
import com.linkedplanet.kotlininsightclient.http.model.DefaultType
import com.linkedplanet.kotlininsightclient.http.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.http.model.ObjectTypeSchemaApiResponse
import com.linkedplanet.kotlininsightclient.http.model.ObjectTypeSchemaAttributeApiResponse
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError

class HttpInsightObjectTypeOperator(private val context: HttpInsightClientContext) : InsightObjectTypeOperator {

    override suspend fun getObjectType(objectTypeId: InsightObjectTypeId): Either<InsightClientError, ObjectTypeSchema> = either {
        context.httpClient.executeRest<ObjectTypeSchemaApiResponse>(
            "GET",
            "/rest/insight/1.0/objecttype/${objectTypeId.raw}",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<ObjectTypeSchemaApiResponse>() {}.type
        )
            .map { it.body!! }
            .mapLeft { it.toInsightClientError() }
            .bind()
            .let { populateObjectTypeSchemaAttributes(it).bind() }
            .let { apiResponse ->
                mapToPublicApiModel(apiResponse)
            }
    }

    override suspend fun getObjectTypesBySchemaAndRootObjectType(
        schemaId: InsightSchemaId,
        rootObjectTypeId: InsightObjectTypeId
    ): Either<InsightClientError, List<ObjectTypeSchema>> = either {
        val allObjectTypes = getObjectTypesBySchema(schemaId).bind()
        allObjectTypes
            .firstOrNull { it.id == rootObjectTypeId }
            ?.let { rootObject ->
                listOf(rootObject).plus(findObjectTypeChildren(allObjectTypes, rootObjectTypeId))
            }
            ?: ObjectTypeNotFoundError(rootObjectTypeId).asEither<List<ObjectTypeSchema>>().bind()
    }

    override suspend fun getObjectTypesBySchema(schemaId: InsightSchemaId): Either<InsightClientError, List<ObjectTypeSchema>> =
        either {
            context.httpClient.executeRestList<ObjectTypeSchemaApiResponse>(
                "GET",
                "rest/insight/1.0/objectschema/${schemaId.raw}/objecttypes/flat",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<ObjectTypeSchemaApiResponse>>() {}.type
            )
                .map { it.body }
                .mapLeft { it.toInsightClientError() }
                .bind()
                .map {
                    populateObjectTypeSchemaAttributes(it).bind()
                }.map { apiResponse ->
                    mapToPublicApiModel(apiResponse)
                }
        }

    private fun mapToPublicApiModel(apiResponse: ObjectTypeSchemaApiResponse): ObjectTypeSchema =
        ObjectTypeSchema(
            InsightObjectTypeId(apiResponse.id),
            apiResponse.name,
            apiResponse.attributes.map(::mapToObjectTypeSchemaAttribute),
            apiResponse.parentObjectTypeId?.let { InsightObjectTypeId(it) },
        )

    private fun mapToObjectTypeSchemaAttribute(apiAttributeType: ObjectTypeSchemaAttributeApiResponse): ObjectTypeSchemaAttribute =
        apiAttributeType.run {
            val iId = InsightAttributeId(id)
            return when (InsightObjectAttributeType.parse(type)) {
                InsightObjectAttributeType.DEFAULT -> mapDefaultType(this, iId)

                InsightObjectAttributeType.REFERENCE -> ObjectTypeSchemaAttribute.ReferenceSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    referenceObjectTypeId = InsightObjectTypeId(referenceObjectTypeId ?: -1),
                    referenceKind = ReferenceKind.parse(referenceType?.id)
                )
                InsightObjectAttributeType.USER -> ObjectTypeSchemaAttribute.UserSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.CONFLUENCE -> ObjectTypeSchemaAttribute.ConfluenceSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.GROUP -> ObjectTypeSchemaAttribute.GroupSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.VERSION -> ObjectTypeSchemaAttribute.VersionSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.PROJECT -> ObjectTypeSchemaAttribute.ProjectSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                InsightObjectAttributeType.STATUS -> ObjectTypeSchemaAttribute.StatusSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                else -> ObjectTypeSchemaAttribute.UnknownSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    "HttpInsightObjectOperator: unknown ObjectTypeAttributeApiResponse.id :$id"
                )
            }
        }

    private fun mapDefaultType(apiAttributeType: ObjectTypeSchemaAttributeApiResponse, iId: InsightAttributeId) =
        apiAttributeType.run {
            when (defaultType?.id?.let { DefaultType.parse(it) }) {
                DefaultType.TEXT -> ObjectTypeSchemaAttribute.TextSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.INTEGER -> ObjectTypeSchemaAttribute.IntegerSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.BOOLEAN -> ObjectTypeSchemaAttribute.BoolSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DOUBLE -> ObjectTypeSchemaAttribute.DoubleNumberSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DATE -> ObjectTypeSchemaAttribute.DateSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.TIME -> ObjectTypeSchemaAttribute.TimeSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.DATE_TIME -> ObjectTypeSchemaAttribute.DateTimeSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.URL -> ObjectTypeSchemaAttribute.UrlSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.EMAIL -> ObjectTypeSchemaAttribute.EmailSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.TEXTAREA -> ObjectTypeSchemaAttribute.TextareaSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.IPADDRESS -> ObjectTypeSchemaAttribute.IpaddressSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes
                )
                DefaultType.SELECT -> ObjectTypeSchemaAttribute.SelectSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    options.split(",")
                )
                else -> ObjectTypeSchemaAttribute.UnknownSchema(
                    iId, name, minimumCardinality, maximumCardinality, includeChildObjectTypes,
                    "HttpInsightObjectOperator: got unknown DefaultType: name:${defaultType?.name} id:${defaultType?.id}"
                )
            }
        }

    private suspend fun populateObjectTypeSchemaAttributes(objectTypeSchema: ObjectTypeSchemaApiResponse): Either<InsightClientError, ObjectTypeSchemaApiResponse> =
        either {
            val attributes = context.httpClient.executeRestList<ObjectTypeSchemaAttributeApiResponse>(
                "GET",
                "rest/insight/1.0/objecttype/${objectTypeSchema.id}/attributes",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<ObjectTypeSchemaAttributeApiResponse>>() {}.type
            )
                .map { it.body }
                .mapLeft { it.toInsightClientError() }
                .bind()
            objectTypeSchema.attributes = attributes
            objectTypeSchema
        }

    private fun findObjectTypeChildren(
        objectTypes: List<ObjectTypeSchema>,
        rootObjectTypeId: InsightObjectTypeId
    ): List<ObjectTypeSchema> {
        val directChildren = objectTypes.filter { it.parentObjectTypeId == rootObjectTypeId }
        val transitiveChildren = directChildren.flatMap { child -> findObjectTypeChildren(objectTypes, child.id) }
        return directChildren.plus(transitiveChildren)
    }
}
