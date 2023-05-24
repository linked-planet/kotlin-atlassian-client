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
import arrow.core.identity
import arrow.core.left
import arrow.core.right
import arrow.core.rightIfNotNull
import com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType
import com.atlassian.jira.config.properties.ApplicationProperties
import com.atlassian.jira.user.util.UserManager
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.ObjectTypeNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.MapToDomain
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightAttributeId
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectPage
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightUser
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue
import com.linkedplanet.kotlininsightclient.api.model.ReferencedObject
import com.linkedplanet.kotlininsightclient.api.model.ReferencedObjectType
import com.linkedplanet.kotlininsightclient.api.model.isReferenceAttribute
import com.linkedplanet.kotlininsightclient.sdk.SdkInsightObjectTypeOperator.typeAttributeBeanToSchema
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeValueBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectResultBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.Type
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory
import io.riada.core.collector.model.toDisplayValue

object SdkInsightObjectOperator : InsightObjectOperator {

    override var RESULTS_PER_PAGE: Int = 25

    private val objectFacade by lazy { getOSGiComponentInstanceOfType(ObjectFacade::class.java) }
    private val userManager by lazy { getOSGiComponentInstanceOfType(UserManager::class.java) }
    private val objectTypeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeFacade::class.java) }
    private val objectTypeAttributeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeAttributeFacade::class.java) }
    private val iqlFacade by lazy { getOSGiComponentInstanceOfType(IQLFacade::class.java) }
    private val objectAttributeBeanFactory by lazy { getOSGiComponentInstanceOfType(ObjectAttributeBeanFactory::class.java) }
    private val baseUrl by lazy { getOSGiComponentInstanceOfType(ApplicationProperties::class.java).getString("jira.baseurl")!! }

    override suspend fun <T> getObjectById(
        id: InsightObjectId,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?> =
        catchAsInsightClientError { objectFacade.loadObjectBean(id.value) }
            .flatMap<InsightClientError, ObjectBean?, T?> { it.toNullableInsightObject(toDomain) }

    override suspend fun <T> getObjectByKey(
        key: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?> =
        catchAsInsightClientError { objectFacade.loadObjectBean(key) }
            .flatMap { it.toNullableInsightObject(toDomain) }

    override suspend fun <T> getObjectByName(
        objectTypeId: InsightObjectTypeId,
        name: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?> =
        catchAsInsightClientError {
            val iql = "objectTypeId=${objectTypeId.raw} AND Name=\"$name\""
            val objs = iqlFacade.findObjects(iql)
            objs.firstOrNull()
        }.flatMap { it.toNullableInsightObject(toDomain) }

    override suspend fun <T> getObjectsByObjectTypeName(
        objectTypeName: String,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, List<T>> {
        val iql = "objectType=$objectTypeName"
        return getObjectsByIQL(iql, 0, Int.MAX_VALUE, toDomain).map { it.objects }
    }

    override suspend fun <T> getObjects(
        objectTypeId: InsightObjectTypeId,
        withChildren: Boolean,
        pageIndex: Int,
        pageSize: Int,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>> =
        catchAsInsightClientError {
            val iql = getIQLWithChildren(objectTypeId, withChildren)
            iqlFacade.findObjects(iql, pageIndex * pageSize, pageSize)
        }.flatMap { it.toInsightObjectPage(toDomain) }

    override suspend fun <T> getObjectsByIQL(
        objectTypeId: InsightObjectTypeId,
        iql: String,
        withChildren: Boolean,
        pageIndex: Int,
        pageSize: Int,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>> =
        catchAsInsightClientError {
            val compositeIql = getIQLWithChildren(objectTypeId, withChildren) + " AND " + iql
            iqlFacade.findObjects(compositeIql, pageIndex * pageSize, pageSize)
        }.flatMap { it.toInsightObjectPage(toDomain) }

    override suspend fun <T> getObjectsByIQL(
        iql: String,
        pageIndex: Int,
        pageSize: Int,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, InsightObjectPage<T>> =
        catchAsInsightClientError {
            iqlFacade.findObjects(iql, pageIndex * pageSize, pageSize)
        }.flatMap { it.toInsightObjectPage(toDomain) }

    override suspend fun getObjectCount(iql: String): Either<InsightClientError, Int> =
        catchAsInsightClientError {
            val objs = iqlFacade.findObjects(iql, 0, 1)
            objs.totalFilterSize
        }

    @Suppress("DEPRECATION")
    override suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject> =
        catchAsInsightClientError {
            val objectBean = objectFacade.loadObjectBean(obj.id.value).createMutable()
            setAttributesForObjectBean(obj, objectBean)
            objectBean.objectTypeId = obj.objectTypeId.raw
            objectBean.objectKey = obj.objectKey
            objectFacade.storeObjectBean(objectBean)
        }.flatMap { it.toInsightObject() }

    override suspend fun updateObject(
        obj: InsightObject,
        vararg insightAttributes: InsightAttribute
    ): Either<InsightClientError, InsightObject> = either {
        val attributeMap = obj.attributes.associateBy { it.attributeId }.toMutableMap()
        insightAttributes.forEach {
            attributeMap[it.attributeId] = it
        }
        obj.attributes = attributeMap.values.toList()
        updateObject(obj).bind()
    }

    override suspend fun <T> updateObject(
        objectId: InsightObjectId,
        vararg insightAttributes: InsightAttribute,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T> =
        either {
            val obj = getObjectById(objectId, ::identity).bind().rightIfNotNull {
                InsightClientError(
                    "InsightObject update failed.",
                    "Could not retrieve the object."
                )
            }.bind()
            updateObject(obj, *insightAttributes).bind()
        }
            .map{ toDomain(it) }
    private fun setAttributesForObjectBean(
        obj: InsightObject,
        objectBean: MutableObjectBean
    ) {
        val attributeBeans = obj.attributes.map { insightAttr ->
            val ota = objectTypeAttributeFacade.loadObjectTypeAttribute(insightAttr.attributeId.raw).createMutable()
            if (obj.isReferenceAttribute(insightAttr.attributeId)) {
                val values = insightAttr.value.map { it.referencedObject!!.id.value }.toTypedArray()
                objectAttributeBeanFactory.createReferenceAttributeValue(ota) { values.contains(it.id) }
            } else {
                val values = insightAttr.value.map { it.value.toString() }.toTypedArray()
                objectAttributeBeanFactory.createObjectAttributeBeanForObject(objectBean, ota, *values)
            }
        }
        objectBean.setObjectAttributeBeans(attributeBeans)
    }

    override suspend fun deleteObject(id: InsightObjectId): Either<InsightClientError, Unit> =
        catchAsInsightClientError {
            objectFacade.deleteObjectBean(id.value)
        }

    override suspend fun createObject(
        objectTypeId: InsightObjectTypeId,
        vararg insightAttributes: InsightAttribute
    ): Either<InsightClientError, InsightObjectId> =
        catchAsInsightClientError {
            val objectTypeBean = objectTypeFacade.loadObjectType(objectTypeId.raw)
            val freshInsightObject = createEmptyDomainObject(objectTypeId, objectTypeBean)
            freshInsightObject.attributes = insightAttributes.toList()
            val freshObjectBean = objectTypeBean.createMutableObjectBean()
            setAttributesForObjectBean(freshInsightObject, freshObjectBean)
            val bean = objectFacade.storeObjectBean(freshObjectBean)
            InsightObjectId(bean.id)
        }

    override suspend fun <T> createObject(
        objectTypeId: InsightObjectTypeId,
        vararg insightAttributes: InsightAttribute,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T> = either {
        val insightObjectId = createObject(objectTypeId, *insightAttributes).bind()
        getObjectById(insightObjectId, toDomain).bind().rightIfNotNull {
            InsightClientError(
                "InsightObject create failed.",
                "Could not retrieve the object after seemingly successful creation."
            )
        }.bind()
    }

    private fun createEmptyDomainObject(
        objectTypeId: InsightObjectTypeId,
        objectTypeBean: ObjectTypeBean
    ): InsightObject {
        return InsightObject(
            objectTypeId = objectTypeId,
            id = InsightObjectId.notPersistedObjectId,
            objectTypeName = objectTypeBean.name,
            objectKey = "",
            label = "",
            attributes = emptyList(),
            attachmentsExist = false,
            objectSelf = ""
        )
    }

    private suspend fun <T> ObjectResultBean.toInsightObjectPage(toDomain: MapToDomain<T>): Either<InsightClientError, InsightObjectPage<T>> =
        either {
            InsightObjectPage(
                totalFilterSize,
                objects
                    .map { it.toInsightObject().bind() }
                    .map{ toDomain(it) }
            )
        }

    private suspend fun <T> ObjectBean?.toNullableInsightObject(toDomain: MapToDomain<T>): Either<InsightClientError, T?> {
        if (this == null) return Either.Right(null)
        return this.toInsightObject().map{ toDomain(it) }
    }

    private suspend fun ObjectBean.toInsightObject(): Either<InsightClientError, InsightObject> =
        catchAsInsightClientError {
            val objectType = objectTypeFacade.loadObjectType(objectTypeId)
            val objectTypeAttributeBeans = objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectType.id)
            val hasAttachments = objectFacade.findAttachmentBeans(id).isNotEmpty()
            return@toInsightObject mapObjectBeanToInsightObject(
                this@toInsightObject,
                objectType,
                objectTypeAttributeBeans,
                hasAttachments
            )
        }

    private suspend fun mapObjectBeanToInsightObject(
        objectBean: ObjectBean,
        objectTypeBean: ObjectTypeBean,
        objectTypeAttributeBeans: List<ObjectTypeAttributeBean>,
        hasAttachments: Boolean
    ): Either<InsightClientError, InsightObject> = either {
        val attributes = objectBean.objectAttributeBeans.map { objAttributeBean ->
            val objTypeAttributeBean = objectTypeAttributeBeans.typeForBean(objAttributeBean).bind()
            mapAttributeBeanToInsightAttribute(objAttributeBean, objTypeAttributeBean).bind()
        }
        val objectSelf = "${baseUrl}/secure/insight/assets/${objectBean.objectKey}"
        InsightObject(
            InsightObjectTypeId(objectBean.objectTypeId),
            InsightObjectId(objectBean.id),
            objectTypeBean.name,
            objectBean.objectKey,
            objectBean.label,
            attributes,
            hasAttachments,
            attributes.singleOrNull { it.schema?.name == "Link" }?.toDisplayValue() as? String ?: objectSelf
        )
    }

    private fun List<ObjectTypeAttributeBean>.typeForBean(
        objAttributeBean: ObjectAttributeBean
    ): Either<InsightClientError, ObjectTypeAttributeBean> =
        singleOrNull { it.id == objAttributeBean.objectTypeAttributeId }
            ?.right()
            ?: ObjectTypeNotFoundError().left()

    private suspend fun mapAttributeBeanToInsightAttribute(
        objectAttributeBean: ObjectAttributeBean, objectTypeAttributeBean: ObjectTypeAttributeBean
    ): Either<InsightClientError, InsightAttribute> = either {
        InsightAttribute(
            InsightAttributeId(objectTypeAttributeBean.id),
            InsightObjectAttributeType.parse(objectTypeAttributeBean.type.typeId),
            value = objectAttributeBean.objectAttributeValueBeans.map { attribute ->
                when (objectTypeAttributeBean.type) {
                    Type.REFERENCED_OBJECT -> {
                        val referencedObject = loadReferencedObject(attribute, objectTypeAttributeBean).bind()
                        val displayValue = "${referencedObject?.label} (${referencedObject?.objectKey})"
                        ObjectAttributeValue(null, displayValue, referencedObject, null)
                    }
                    Type.USER -> {
                        val user = loadInsightUserByKey(attribute.textValue).bind()
                        ObjectAttributeValue(null, null, null, user)
                    }
                    else -> ObjectAttributeValue(attribute.value, attribute.textValue, null, null)
                }
            },
            typeAttributeBeanToSchema(objectTypeAttributeBean)
        )
    }

    /**
     * Loads the full object that is referenced to create the compact [ReferencedObject].
     * This might be slower than expected by the caller, but Insights own code also loads
     * the full object to resolve the label and objectKey for the reference.
     * see ObjectResource.assembleObjectAttributeValueEntry
     */
    private fun loadReferencedObject(
        attributeBean: ObjectAttributeValueBean,
        objectTypeAttributeBean: ObjectTypeAttributeBean
    ): Either<InsightClientError, ReferencedObject?> =
        catchAsInsightClientError {
            objectFacade.loadObjectBean(attributeBean.referencedObjectBeanId)?.let { refObjBean ->
                ReferencedObject(
                    InsightObjectId(attributeBean.referencedObjectBeanId),
                    refObjBean.label,
                    refObjBean.objectKey,
                    ReferencedObjectType(
                        InsightObjectTypeId(objectTypeAttributeBean.referenceObjectTypeId),
                        objectTypeAttributeBean.name
                    )
                )
            }
        }

    private fun loadInsightUserByKey(userKey: String): Either<InsightClientError, InsightUser?> =
        catchAsInsightClientError {
            userManager.getUserByKey(userKey)?.run {
                InsightUser(displayName, name, emailAddress, key)
            }
        }

    private fun getIQLWithChildren(objTypeId: InsightObjectTypeId, withChildren: Boolean): String =
        if (withChildren) {
            "objectType = objectTypeAndChildren(\"${objTypeId.raw}\")"
        } else {
            "objectTypeId=${objTypeId.raw}"
        }
}
