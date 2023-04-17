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
package com.linkedplanet.kotlininsightclient.sdk

import arrow.core.Either
import com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeAttributeDefaultType
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttributeReferenceType
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.DefaultType
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

object SdkInsightObjectTypeOperator : InsightObjectTypeOperator {

    private val objectTypeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeFacade::class.java) }
    private val objectTypeAttributeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeAttributeFacade::class.java) }

    override suspend fun getObjectType(objectTypeId: Int): Either<InsightClientError, ObjectTypeSchema> =
        catchAsInsightClientError {
            val objectTypeBean = objectTypeFacade.loadObjectType(objectTypeId)
            objectTypeSchemaForBean(objectTypeBean)
        }

    override suspend fun getObjectTypesBySchema(schemaId: Int): Either<InsightClientError, List<ObjectTypeSchema>> =
        catchAsInsightClientError {
            objectTypeFacade.findObjectTypeBeansFlat(schemaId)
                .map(::objectTypeSchemaForBean)
        }

    override suspend fun getObjectTypesBySchemaAndRootObjectType(
        schemaId: Int,
        rootObjectTypeId: Int
    ): Either<InsightClientError, List<ObjectTypeSchema>> =
        catchAsInsightClientError {
            objectTypeFacade.findObjectTypeBeanChildrens(rootObjectTypeId)
                .map(::objectTypeSchemaForBean)
        }

    private fun objectTypeSchemaForBean(objectTypeBean: ObjectTypeBean): ObjectTypeSchema {
        val attributes = attributesForObjectType(objectTypeBean.id)
        return ObjectTypeSchema(
            objectTypeBean.id,
            objectTypeBean.name,
            attributes,
            objectTypeBean.parentObjectTypeId
        )
    }

    private fun attributesForObjectType(objectTypeId: Int): List<ObjectTypeSchemaAttribute> =
        objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeId).map { bean ->
            bean.run {
                ObjectTypeSchemaAttribute(
                    id,
                    name,
                    defaultType?.let(::mapDefaultType),
                    options,
                    minimumCardinality,
                    maximumCardinality,
                    referenceTypeBean?.run { ObjectTypeSchemaAttributeReferenceType(id, name) }
                )
            }
        }

    private fun mapDefaultType(defaultType: DefaultType) =
        when (defaultType) {
            DefaultType.NONE -> null
            else -> ObjectTypeAttributeDefaultType(
                defaultType.defaultTypeId,
                defaultType.name.upperCaseToPascalCase()
            )
        }

    private fun String.upperCaseToPascalCase() =
        split("_").joinToString("") { it.lowercase().capitalizeAsciiOnly() }

}
