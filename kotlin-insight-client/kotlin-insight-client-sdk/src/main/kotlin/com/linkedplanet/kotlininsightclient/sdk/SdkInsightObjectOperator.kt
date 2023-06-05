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
import arrow.core.rightIfNotNull
import com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType
import com.atlassian.jira.config.properties.ApplicationProperties
import com.atlassian.jira.user.util.UserManager
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError.Companion.internalError
import com.linkedplanet.kotlininsightclient.api.error.ObjectNotFoundError
import com.linkedplanet.kotlininsightclient.api.error.ObjectTypeNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.MapToDomain
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightAttributeId
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectPage
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightUser
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue
import com.linkedplanet.kotlininsightclient.api.model.ReferencedObject
import com.linkedplanet.kotlininsightclient.api.model.ReferencedObjectType
import com.linkedplanet.kotlininsightclient.sdk.SdkInsightObjectTypeOperator.typeAttributeBeanToSchema
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredDateTimeFormatterInJira
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectBean
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeValueBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectResultBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.DefaultType
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.Type
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeBean
import com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory
import io.riada.core.collector.model.toDisplayValue
import java.time.ZoneId
import java.util.*

object SdkInsightObjectOperator : InsightObjectOperator {

    override var RESULTS_PER_PAGE: Int = 25

    private val objectFacade by lazy { getOSGiComponentInstanceOfType(ObjectFacade::class.java) }
    private val userManager by lazy { getOSGiComponentInstanceOfType(UserManager::class.java) }
    private val objectTypeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeFacade::class.java) }
    private val objectTypeAttributeFacade by lazy { getOSGiComponentInstanceOfType(ObjectTypeAttributeFacade::class.java) }
    private val iqlFacade by lazy { getOSGiComponentInstanceOfType(IQLFacade::class.java) }
    private val objectAttributeBeanFactory by lazy { getOSGiComponentInstanceOfType(ObjectAttributeBeanFactory::class.java) }
    private val baseUrl by lazy { getOSGiComponentInstanceOfType(ApplicationProperties::class.java).getString("jira.baseurl")!! }
    private val dateTimeFormatter = ReverseEngineeredDateTimeFormatterInJira()

    private val zoneId: ZoneId by lazy { ZoneId.of("Z") }

    override suspend fun <T> getObjectById(
        id: InsightObjectId,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T?> =
        catchAsInsightClientError { objectFacade.loadObjectBean(id.raw) }
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
    override suspend fun updateInsightObject(obj: InsightObject): Either<InsightClientError, InsightObject> =
        catchAsInsightClientError {
            val objectBean = objectFacade.loadObjectBean(obj.id.raw).createMutable()
            setAttributesForObjectBean(obj, objectBean)
            objectBean.objectTypeId = obj.objectTypeId.raw
            objectBean.objectKey = obj.objectKey
            objectFacade.storeObjectBean(objectBean)
        }.flatMap { it.toInsightObject() }

    private suspend fun updateObject(
        obj: InsightObject,
        vararg insightAttributes: InsightAttribute
    ): Either<InsightClientError, InsightObject> = either {
        val attributeMap = obj.attributes.associateBy { it.attributeId }.toMutableMap()
        insightAttributes.forEach {
            attributeMap[it.attributeId] = it
        }
        obj.attributes = attributeMap.values.toList()
        updateInsightObject(obj).bind()
    }

    override suspend fun <T> updateInsightObject(
        objectId: InsightObjectId,
        vararg insightAttributes: InsightAttribute,
        toDomain: MapToDomain<T>
    ): Either<InsightClientError, T> =
        either {
            val obj = getObjectById(objectId, ::identity).bind()
                .rightIfNotNull { ObjectNotFoundError(objectId) }.bind()
            val updated = updateObject(obj, *insightAttributes).bind()
            toDomain(updated).bind()
        }

    private fun setAttributesForObjectBean(
        obj: InsightObject,
        bean: MutableObjectBean
    ) {
        val attributeBeans = obj.attributes.map { insightAttr ->
            val ota = objectTypeAttributeFacade.loadObjectTypeAttribute(insightAttr.attributeId.raw).createMutable()
            when (val value = insightAttr.value) {
                is ObjectAttributeValue.Bool -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.Date -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.DateTime -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.DoubleNumber -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.Email -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.Integer -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.Ipaddress -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.Text -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.Textarea -> beanFromString(bean, ota, value.value.toString())
                is ObjectAttributeValue.Time -> beanFromString(bean, ota, value.value.toString())

                is ObjectAttributeValue.Url -> objectAttributeBeanFactory.createObjectAttributeBeanForObject(
                    bean, ota, *value.values.toTypedArray()
                )
                is ObjectAttributeValue.Select -> objectAttributeBeanFactory.createObjectAttributeBeanForObject(
                    bean, ota, *value.values.toTypedArray()
                )

                is ObjectAttributeValue.Reference -> {
                    val referenceIds = value.referencedObjects.map { it.id.raw }.toTypedArray()
                    objectAttributeBeanFactory.createReferenceAttributeValue(ota) { referenceIds.contains(it.id) }
                }
                is ObjectAttributeValue.User -> {
                    val userKeys = value.users.map { it.key }
                    objectAttributeBeanFactory.createUserAttributeValueByKey(ota, *userKeys.toTypedArray())
                }

                is ObjectAttributeValue.Group -> TODO()
                is ObjectAttributeValue.Project -> TODO()
                is ObjectAttributeValue.Status -> TODO()
                is ObjectAttributeValue.Version -> TODO()
                is ObjectAttributeValue.Confluence -> TODO()
                is ObjectAttributeValue.Unknown -> TODO()
            }
        }
        bean.setObjectAttributeBeans(attributeBeans)
    }

    private fun beanFromString(
        bean: MutableObjectBean,
        ota: MutableObjectTypeAttributeBean,
        asString: String
    ): MutableObjectAttributeBean =
        objectAttributeBeanFactory.createObjectAttributeBeanForObject(bean, ota, asString)

    override suspend fun deleteObject(id: InsightObjectId): Either<InsightClientError, Unit> =
        catchAsInsightClientError {
            objectFacade.deleteObjectBean(id.raw)
        }

    override suspend fun createInsightObject(
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
        val insightObjectId = createInsightObject(objectTypeId, *insightAttributes).bind()
        getObjectById(insightObjectId, toDomain).bind()
            .rightIfNotNull { ObjectNotFoundError(insightObjectId) }.bind()
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
                    .map { toDomain(it).bind() }
            )
        }

    private suspend fun <T> ObjectBean?.toNullableInsightObject(toDomain: MapToDomain<T>): Either<InsightClientError, T?> = either {
        if (this@toNullableInsightObject == null) return@either null
        val asInsightObject = this@toNullableInsightObject.toInsightObject().bind()
        toDomain(asInsightObject).bind()
    }

    private suspend fun ObjectBean.toInsightObject(): Either<InsightClientError, InsightObject> = either {
        val objectType = catchAsInsightClientError { objectTypeFacade.loadObjectType(objectTypeId) }.bind()
        val attributeBeans = catchAsInsightClientError {
            objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectType.id)
        }.bind()
        val hasAttachments = catchAsInsightClientError { objectFacade.findAttachmentBeans(id).isNotEmpty() }.bind()
        mapObjectBeanToInsightObject(
            this@toInsightObject,
            objectType,
            attributeBeans,
            hasAttachments
        ).bind()
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
            ?: ObjectTypeNotFoundError(InsightObjectTypeId(objAttributeBean.objectTypeAttributeId)).left()

    private suspend fun mapAttributeBeanToInsightAttribute(
        objectAttributeBean: ObjectAttributeBean,
        objectTypeAttributeBean: ObjectTypeAttributeBean
    ): Either<InsightClientError, InsightAttribute> = either {
        val value: ObjectAttributeValue = when (objectTypeAttributeBean.type) {
            Type.DEFAULT -> handleDefaultValue(objectAttributeBean, objectTypeAttributeBean).bind()
            Type.REFERENCED_OBJECT -> {
                val referencedObjects = objectAttributeBean.objectAttributeValueBeans.mapNotNull { attribute ->
                    loadReferencedObject(attribute, objectTypeAttributeBean).bind()
                }
                ObjectAttributeValue.Reference(referencedObjects)
            }
            Type.USER -> {
                val users = objectAttributeBean.objectAttributeValueBeans.mapNotNull { attribute ->
                    loadInsightUserByKey(attribute.textValue).bind()
                }
                ObjectAttributeValue.User(users)
            }
            Type.CONFLUENCE -> TODO()
            Type.GROUP -> TODO()
            Type.VERSION -> TODO()
            Type.PROJECT -> TODO()
            Type.STATUS -> TODO()
            else -> internalError("Unsupported objectTypeAttributeBean.type (${objectTypeAttributeBean.type})").bind()
        }
        InsightAttribute(
            InsightAttributeId(objectTypeAttributeBean.id),
            value = value,
            typeAttributeBeanToSchema(objectTypeAttributeBean)
        )
    }

    private suspend fun handleDefaultValue(
        objectAttributeBean: ObjectAttributeBean,
        objectTypeAttributeBean: ObjectTypeAttributeBean
    ): Either<InsightClientError, ObjectAttributeValue> = either {
        val values = objectAttributeBean.objectAttributeValueBeans
        when (objectTypeAttributeBean.defaultType) {
            DefaultType.TEXT -> ObjectAttributeValue.Text(values.firstOrNull()?.textValue)
            DefaultType.INTEGER -> ObjectAttributeValue.Integer(values.firstOrNull()?.integerValue)
            DefaultType.BOOLEAN -> ObjectAttributeValue.Bool(values.firstOrNull()?.booleanValue)
            DefaultType.DOUBLE -> ObjectAttributeValue.DoubleNumber(values.firstOrNull()?.doubleValue)
            DefaultType.DATE -> {
                val date = values.firstOrNull()?.dateValue
                val localDate = date?.toInstant()?.atZone(zoneId)?.toLocalDate()
                val displayValue = date?.let { dateTimeFormatter.formatDateToString(it) }
                ObjectAttributeValue.Date(localDate, displayValue)
            }
            DefaultType.TIME -> {
                val date = values.firstOrNull()?.dateValue
                val localTime = date?.toInstant()?.atZone(zoneId)?.toLocalTime()
                val displayValue = null // Insights original ObjectAssembler does not handle this case at all.
                ObjectAttributeValue.Time(localTime, displayValue)
            }
            DefaultType.DATE_TIME -> {
                val date = values.firstOrNull()?.dateValue
                val zonedDateTime = date?.toInstant()?.atZone(zoneId)
                val displayValue = zonedDateTime?.let { dateTimeFormatter.formatDateTimeToString(Date.from(it.toInstant())) }
                ObjectAttributeValue.DateTime(zonedDateTime, displayValue)
            }
            DefaultType.EMAIL -> ObjectAttributeValue.Email(values.firstOrNull()?.textValue)
            DefaultType.TEXTAREA -> ObjectAttributeValue.Textarea(values.firstOrNull()?.textValue)
            DefaultType.IPADDRESS -> ObjectAttributeValue.Ipaddress(values.firstOrNull()?.textValue)

            // cardinality > 1
            DefaultType.URL -> ObjectAttributeValue.Url(values.map { it.textValue })
            DefaultType.SELECT -> ObjectAttributeValue.Select(values.map { it.textValue })
            else -> internalError("Unsupported DefaultType (${objectTypeAttributeBean.defaultType})").bind()
        }
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
