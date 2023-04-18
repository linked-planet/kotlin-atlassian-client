/*-
 * #%L
 * kotlin-insight-client-sdk
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
import arrow.core.computations.either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.ObjectTypeNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.api.model.InsightObjects
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue
import com.linkedplanet.kotlininsightclient.api.model.ReferencedObject
import com.linkedplanet.kotlininsightclient.api.model.ReferencedObjectType
import com.linkedplanet.kotlininsightclient.api.model.isReferenceAttribute
import com.linkedplanet.kotlininsightclient.api.model.toEditObjectItem
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectResultBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory
import io.riada.core.collector.model.toDisplayValue

object SdkInsightObjectOperator : InsightObjectOperator {

    override var RESULTS_PER_PAGE: Int = 25

    private val objectFacade by lazy { getOSGiComponentInstanceOfType(ObjectFacade::class.java) }
    private val objectTypeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeFacade::class.java) }
    private val objectTypeAttributeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeAttributeFacade::class.java) }
    private val iqlFacade by lazy { getOSGiComponentInstanceOfType(IQLFacade::class.java) }
    private val objectAttributeBeanFactory by lazy { getOSGiComponentInstanceOfType(ObjectAttributeBeanFactory::class.java) }

    override suspend fun getObjectById(id: Int): Either<InsightClientError, InsightObject?> =
        catchAsInsightClientError { objectFacade.loadObjectBean(id) }
            .flatMap { it.toNullableInsightObject() }

    override suspend fun getObjectByKey(key: String): Either<InsightClientError, InsightObject?> =
        catchAsInsightClientError { objectFacade.loadObjectBean(key) }
            .flatMap { it.toNullableInsightObject() }

    override suspend fun getObjectByName(objectTypeId: Int, name: String): Either<InsightClientError, InsightObject?> =
        catchAsInsightClientError {
            val iql = "objectTypeId=$objectTypeId AND Name=\"$name\""
            val objs = iqlFacade.findObjects(iql)
            objs.firstOrNull()
        }.flatMap { it.toNullableInsightObject() }

    override suspend fun getObjects(
        objectTypeId: Int,
        withChildren: Boolean,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> =
        catchAsInsightClientError {
            val iql = getIQLWithChildren(objectTypeId, withChildren)
            iqlFacade.findObjects(iql, (pageFrom - 1) * perPage, perPage)
        }.flatMap { it.toInsightObjects() }

    override suspend fun getObjectsByIQL(
        objectTypeId: Int,
        withChildren: Boolean,
        iql: String,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> =
        catchAsInsightClientError {
            val compositeIql = getIQLWithChildren(objectTypeId, withChildren) + " AND " + iql
            iqlFacade.findObjects(compositeIql, (pageFrom - 1) * perPage, perPage)
        }.flatMap { it.toInsightObjects() }

    override suspend fun getObjectsByIQL(
        iql: String,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> =
        catchAsInsightClientError {
            iqlFacade.findObjects(iql, (pageFrom - 1) * perPage, perPage)
        }.flatMap { it.toInsightObjects() }

    override suspend fun getObjectCount(iql: String): Either<InsightClientError, Int> =
        catchAsInsightClientError {
            val objs = iqlFacade.findObjects(iql)
            objs.size
        }

    @Suppress("DEPRECATION") // fix it with the newest insight version
    override suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject> =
        catchAsInsightClientError {
            val objectBean = objectFacade.loadObjectBean(obj.id).createMutable()
            setAttributesForObjectBean(obj, objectBean)
            objectBean.objectTypeId = obj.objectTypeId
            objectBean.objectKey = obj.objectKey
            objectFacade.storeObjectBean(objectBean)
        }.flatMap { it.toInsightObject() }

    private fun setAttributesForObjectBean(
        obj: InsightObject,
        objectBean: MutableObjectBean
    ) {
        val editAttributes = obj.toEditObjectItem().attributes.map { editItemAttribute ->
            val ota = objectTypeAttributeFacade.loadObjectTypeAttribute(editItemAttribute.objectTypeAttributeId)
                .createMutable()
            val values = editItemAttribute.objectAttributeValues.map { it.value.toString() }.toTypedArray()

            if (obj.isReferenceAttribute(editItemAttribute.objectTypeAttributeId)) {
                objectAttributeBeanFactory.createReferenceAttributeValue(ota) { values.contains(it.id.toString()) }
            } else {
                objectAttributeBeanFactory.createObjectAttributeBeanForObject(objectBean, ota, *values)
            }
        }
        objectBean.setObjectAttributeBeans(editAttributes)
    }

    override suspend fun deleteObject(id: Int): Either<InsightClientError, Unit> =
        catchAsInsightClientError {
            objectFacade.deleteObjectBean(id)
        }

    override suspend fun createObject(
        objectTypeId: Int,
        func: (InsightObject) -> Unit // to configure the model
    ): Either<InsightClientError, InsightObject> =
        catchAsInsightClientError {
            val freshInsightObject = createEmptyDomainObject(objectTypeId)
            func(freshInsightObject)
            val objectTypeBean = objectTypeFacade.loadObjectType(objectTypeId)
            val freshObjectBean = objectTypeBean.createMutableObjectBean()
            setAttributesForObjectBean(freshInsightObject, freshObjectBean)
            objectFacade.storeObjectBean(freshObjectBean)
        }.flatMap { it.toInsightObject() }

    private fun createEmptyDomainObject(objectTypeId: Int): InsightObject {
        val objectTypeBean = objectTypeFacade.loadObjectType(objectTypeId)
        return InsightObject(
            objectTypeId = objectTypeId,
            id = -1,
            objectTypeName = objectTypeBean.name,
            objectKey = "",
            label = "",
            attributes = emptyList(),
            attachmentsExist = false,
            objectSelf = ""
        )
    }

    private suspend fun ObjectResultBean.toInsightObjects(): Either<InsightClientError, InsightObjects> = either {
        InsightObjects(
            totalFilterSize,
            objects.map { it.toInsightObject().bind() })
    }

    private suspend fun ObjectBean?.toNullableInsightObject(): Either<InsightClientError, InsightObject?> {
        if (this == null) return Either.Right(null)
        return this.toInsightObject()
    }

    private suspend fun ObjectBean.toInsightObject(): Either<InsightClientError, InsightObject> =
        catchAsInsightClientError {

            val objectType = objectTypeFacade.loadObjectType(objectTypeId)
            val objectTypeAttributeBeans = objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectType.id)
            val hasAttachments = objectFacade.findAttachmentBeans(id).isNotEmpty()
            return@toInsightObject createInsightObject(
                this@toInsightObject,
                objectType,
                objectTypeAttributeBeans,
                hasAttachments
            )
        }

    private suspend fun createInsightObject(
        objectBean: ObjectBean,
        objectTypeBean: ObjectTypeBean,
        objectTypeAttributeBeans: List<ObjectTypeAttributeBean>,
        hasAttachments: Boolean
    ): Either<InsightClientError, InsightObject> = either {
        val attributes = objectBean.objectAttributeBeans.map { objAttributeBean ->
            val objTypeAttributeBean = objectTypeAttributeBeans.typeForBean(objAttributeBean).bind()
            createInsightAttribute(objAttributeBean, objTypeAttributeBean).bind()
        }

        InsightObject(
            objectBean.objectTypeId,
            objectBean.id,
            objectTypeBean.name,
            objectBean.objectKey,
            objectBean.label,
            attributes,
            hasAttachments,
            attributes.singleOrNull { it.attributeName == "Link" }?.toDisplayValue() as? String ?: "NO SELF!!!"
        )
    }

    private fun List<ObjectTypeAttributeBean>.typeForBean(
        objAttributeBean: ObjectAttributeBean
    ): Either<InsightClientError, ObjectTypeAttributeBean> =
        singleOrNull { it.id == objAttributeBean.objectTypeAttributeId }
            ?.right()
            ?: ObjectTypeNotFoundError().left()

    private suspend fun createInsightAttribute(
        objectAttributeBean: ObjectAttributeBean, objectTypeAttributeBean: ObjectTypeAttributeBean
    ): Either<InsightClientError, InsightAttribute> = either {
        InsightAttribute(
            objectTypeAttributeBean.id,
            objectTypeAttributeBean.name,
            InsightObjectAttributeType.parse(objectTypeAttributeBean.type.typeId),
            objectAttributeBean.objectAttributeValueBeans.map {
                if (objectTypeAttributeBean.isObjectReference) {
                    val refObj = getObjectById(it.referencedObjectBeanId).bind()
                    ObjectAttributeValue(
                        it.value,
                        "${refObj?.label} (${refObj?.objectKey})",
                        refObj?.let {
                            ReferencedObject(
                                refObj.id,
                                refObj.label,
                                refObj.objectKey,
                                ReferencedObjectType(
                                    refObj.objectTypeId,
                                    refObj.objectTypeName
                                )
                            )
                        }
                    )
                } else {
                    ObjectAttributeValue(it.value, it.textValue, null)
                }
            }
        )
    }

    private fun getIQLWithChildren(objTypeId: Int, withChildren: Boolean): String =
        if (withChildren) {
            "objectType = objectTypeAndChildren(\"$objTypeId\")"
        } else {
            "objectTypeId=$objTypeId"
        }
}
