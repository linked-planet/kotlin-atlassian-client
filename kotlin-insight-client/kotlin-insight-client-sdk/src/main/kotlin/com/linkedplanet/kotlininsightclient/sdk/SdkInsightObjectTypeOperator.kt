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
import com.linkedplanet.kotlininsightclient.api.model.InsightAttributeId
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

    override suspend fun getObjectTypesBySchema(
        schemaId: InsightSchemaId,
        withChildren: Boolean
    ): Either<InsightClientError, List<ObjectTypeSchema>> =
        catchAsInsightClientError {
            val objectTypeBeans: List<ObjectTypeBean> = if (withChildren) {
                objectTypeFacade.findObjectTypeBeansFlat(schemaId.raw)
            } else {
                objectTypeFacade.findObjectTypeBeans(schemaId.raw)
            }
            objectTypeBeans
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


    internal fun typeAttributeBeanToSchema(bean: ObjectTypeAttributeBean): ObjectTypeSchemaAttribute =
        bean.run {
            val iId = InsightAttributeId(id)
            when(type){
                ObjectTypeAttributeBean.Type.DEFAULT -> mapDefaultType(iId)

                ObjectTypeAttributeBean.Type.REFERENCED_OBJECT -> ObjectTypeSchemaAttribute.ReferenceSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes,
                    InsightObjectTypeId(referenceObjectTypeId ?: -1),
                    ReferenceKind.parse(referenceTypeBean.id)
                )

                ObjectTypeAttributeBean.Type.USER -> ObjectTypeSchemaAttribute.UserSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
                )

                ObjectTypeAttributeBean.Type.CONFLUENCE -> ObjectTypeSchemaAttribute.ConfluenceSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
                )
                ObjectTypeAttributeBean.Type.GROUP -> ObjectTypeSchemaAttribute.GroupSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
                )
                ObjectTypeAttributeBean.Type.VERSION -> ObjectTypeSchemaAttribute.VersionSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
                )
                ObjectTypeAttributeBean.Type.PROJECT -> ObjectTypeSchemaAttribute.ProjectSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
                )
                ObjectTypeAttributeBean.Type.STATUS -> ObjectTypeSchemaAttribute.StatusSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
                )
                else -> ObjectTypeSchemaAttribute.UnknownSchema(
                    iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes,
                    "SdkInsightObjectOperator: got type $type for Attribute with ID: $iId"
                )
            }
        }

    private fun ObjectTypeAttributeBean.mapDefaultType(iId: InsightAttributeId) =
        when (defaultType) {
            ObjectTypeAttributeBean.DefaultType.TEXT -> ObjectTypeSchemaAttribute.TextSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.INTEGER -> ObjectTypeSchemaAttribute.IntegerSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.BOOLEAN -> ObjectTypeSchemaAttribute.BoolSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.DOUBLE -> ObjectTypeSchemaAttribute.DoubleNumberSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.DATE -> ObjectTypeSchemaAttribute.DateSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.TIME -> ObjectTypeSchemaAttribute.TimeSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.DATE_TIME -> ObjectTypeSchemaAttribute.DateTimeSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.URL -> ObjectTypeSchemaAttribute.UrlSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.EMAIL -> ObjectTypeSchemaAttribute.EmailSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.TEXTAREA -> ObjectTypeSchemaAttribute.TextareaSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.IPADDRESS -> ObjectTypeSchemaAttribute.IpaddressSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes
            )
            ObjectTypeAttributeBean.DefaultType.SELECT -> ObjectTypeSchemaAttribute.SelectSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes,
                options.split(",")
            )
            ObjectTypeAttributeBean.DefaultType.NONE -> ObjectTypeSchemaAttribute.UnknownSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes,
                "SdkInsightObjectOperator: got DefaultType with DefaultType.None for Attribute with ID: $iId"
            )
            else -> ObjectTypeSchemaAttribute.UnknownSchema(
                iId, name, minimumCardinality, maximumCardinality, isIncludeChildObjectTypes,
                "SdkInsightObjectOperator: got unknown DefaultType $defaultType for Attribute with ID: $iId"
            )
        }
}
