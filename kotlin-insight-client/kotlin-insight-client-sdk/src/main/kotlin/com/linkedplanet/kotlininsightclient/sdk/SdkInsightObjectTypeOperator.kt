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
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.api.model.DefaultType
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightSchemaId
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.ReferenceKind
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean

object SdkInsightObjectTypeOperator : InsightObjectTypeOperator {

    private val objectTypeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeFacade::class.java) }
    private val objectTypeAttributeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeAttributeFacade::class.java) }

    override suspend fun getObjectType(objectTypeId: InsightObjectTypeId): Either<InsightClientError, ObjectTypeSchema> =
        catchAsInsightClientError {
            val objectTypeBean = objectTypeFacade.loadObjectType(objectTypeId.raw)
            objectTypeSchemaForBean(objectTypeBean)
        }

    override suspend fun getObjectTypesBySchema(schemaId: InsightSchemaId): Either<InsightClientError, List<ObjectTypeSchema>> =
        catchAsInsightClientError {
            objectTypeFacade.findObjectTypeBeansFlat(schemaId.raw)
                .map(::objectTypeSchemaForBean)
        }

    override suspend fun getObjectTypesBySchemaAndRootObjectType(
        schemaId: InsightSchemaId,
        rootObjectTypeId: InsightObjectTypeId
    ): Either<InsightClientError, List<ObjectTypeSchema>> =
        catchAsInsightClientError {
            objectTypeFacade.findObjectTypeBeanChildrens(rootObjectTypeId.raw)
                .map(::objectTypeSchemaForBean)
        }

    private fun objectTypeSchemaForBean(objectTypeBean: ObjectTypeBean): ObjectTypeSchema {
        val attributes = attributesForObjectType(objectTypeBean.id)
        return ObjectTypeSchema(
            InsightObjectTypeId(objectTypeBean.id),
            objectTypeBean.name,
            attributes,
            objectTypeBean.parentObjectTypeId?.let { InsightObjectTypeId(it) }
        )
    }

    private fun attributesForObjectType(objectTypeId: Int): List<ObjectTypeSchemaAttribute> =
        objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeId).map { bean: ObjectTypeAttributeBean ->
            typeAttributeBeanToSchema(bean)
        }


    internal fun typeAttributeBeanToSchema(bean: ObjectTypeAttributeBean) =
        bean.run {
            ObjectTypeSchemaAttribute(
                id,
                name,
                defaultType?.let(::mapDefaultType),
                options,
                minimumCardinality,
                maximumCardinality,
                referenceTypeBean?.run { ReferenceKind.parse(id) },
                isIncludeChildObjectTypes,
                referenceObjectTypeId?.let { InsightObjectTypeId(it) },
                mapAttributeType(type)
            )
        }

    private fun mapAttributeType(type: ObjectTypeAttributeBean.Type): InsightObjectAttributeType =
        when(type){
            ObjectTypeAttributeBean.Type.DEFAULT -> InsightObjectAttributeType.DEFAULT
            ObjectTypeAttributeBean.Type.REFERENCED_OBJECT -> InsightObjectAttributeType.REFERENCE
            ObjectTypeAttributeBean.Type.USER -> InsightObjectAttributeType.USER
            ObjectTypeAttributeBean.Type.CONFLUENCE -> InsightObjectAttributeType.CONFLUENCE
            ObjectTypeAttributeBean.Type.GROUP -> InsightObjectAttributeType.GROUP
            ObjectTypeAttributeBean.Type.VERSION -> InsightObjectAttributeType.VERSION
            ObjectTypeAttributeBean.Type.PROJECT -> InsightObjectAttributeType.PROJECT
            ObjectTypeAttributeBean.Type.STATUS -> InsightObjectAttributeType.STATUS
        }

    private fun mapDefaultType(defaultType: ObjectTypeAttributeBean.DefaultType) =
        when (defaultType) {
            ObjectTypeAttributeBean.DefaultType.NONE -> null
            ObjectTypeAttributeBean.DefaultType.TEXT -> DefaultType.TEXT
            ObjectTypeAttributeBean.DefaultType.INTEGER -> DefaultType.INTEGER
            ObjectTypeAttributeBean.DefaultType.BOOLEAN -> DefaultType.BOOLEAN
            ObjectTypeAttributeBean.DefaultType.DOUBLE -> DefaultType.DOUBLE
            ObjectTypeAttributeBean.DefaultType.DATE -> DefaultType.DATE
            ObjectTypeAttributeBean.DefaultType.TIME -> DefaultType.TIME
            ObjectTypeAttributeBean.DefaultType.DATE_TIME -> DefaultType.DATE_TIME
            ObjectTypeAttributeBean.DefaultType.URL -> DefaultType.URL
            ObjectTypeAttributeBean.DefaultType.EMAIL -> DefaultType.EMAIL
            ObjectTypeAttributeBean.DefaultType.TEXTAREA -> DefaultType.TEXTAREA
            ObjectTypeAttributeBean.DefaultType.SELECT -> DefaultType.SELECT
            ObjectTypeAttributeBean.DefaultType.IPADDRESS -> DefaultType.IPADDRESS
        }
}
