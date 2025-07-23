/*-
 * #%L
 * kotlin-insight-client-sdk
 * %%
 * Copyright (C) 2022 - 2024 linked-planet GmbH
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
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.properties.ApplicationProperties
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.util.NaturalOrderStringComparator
import com.linkedplanet.kotlinatlassianclientcore.common.api.StatusAttribute
import com.linkedplanet.kotlinatlassianclientcore.common.api.StatusCategory
import com.linkedplanet.kotlinatlassianclientcore.common.api.ConfluencePage
import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraGroup
import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraProject
import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraUser
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError.Companion.internalError
import com.linkedplanet.kotlininsightclient.api.error.ObjectNotFoundError
import com.linkedplanet.kotlininsightclient.api.error.ObjectTypeNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.MapToDomain
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.model.*
import com.linkedplanet.kotlininsightclient.sdk.SdkInsightObjectTypeOperator.typeAttributeBeanToSchema
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredDateTimeFormatterInJira
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredVersionAssembler
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.linkedplanet.kotlininsightclient.sdk.util.getOSGiComponent
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ConfigureFacade
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
import com.riadalabs.jira.plugins.insight.services.model.StatusTypeBean
import com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory
import kotlinx.coroutines.runBlocking
import java.time.ZoneId
import java.util.*
import kotlin.math.min

object SdkInsightObjectOperator : InsightObjectOperator {

    override var RESULTS_PER_PAGE: Int = 25

    private val objectFacade : ObjectFacade by getOSGiComponent()
    private val objectTypeFacade: ObjectTypeFacade by getOSGiComponent()
    private val configureFacade: ConfigureFacade by getOSGiComponent()
    private val userManager: UserManager by getOSGiComponent()
    private val objectTypeAttributeFacade: ObjectTypeAttributeFacade by getOSGiComponent()
    private val iqlFacade: IQLFacade by getOSGiComponent()
    private val objectAttributeBeanFactory: ObjectAttributeBeanFactory by getOSGiComponent()
    private val applicationProperties: ApplicationProperties by getOSGiComponent()
    private val projectService: ProjectService by getOSGiComponent()

    private val versionAssembler = ReverseEngineeredVersionAssembler()
    private val dateTimeFormatter = ReverseEngineeredDateTimeFormatterInJira()
    private val avatarService = ComponentAccessor.getAvatarService()
    private val jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()

    private val zoneId: ZoneId = ZoneId.of("Z")

    private fun user() = jiraAuthenticationContext.loggedInUser
    private fun baseUrl() = applicationProperties.jiraBaseUrl

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
            val obj = (getObjectById(objectId, ::identity).bind()
                ?.right() ?: ObjectNotFoundError(objectId).left<ObjectNotFoundError>()).bind()
            val updated = updateObject(obj, *insightAttributes).bind()
            toDomain(updated).bind()
        }

    private fun setAttributesForObjectBean(
        obj: InsightObject,
        bean: MutableObjectBean
    ) {
        val attributeBeans = obj.attributes.map { attr ->
            val ota = objectTypeAttributeFacade.loadObjectTypeAttribute(attr.attributeId.raw).createMutable()
            when (attr) {
                is InsightAttribute.Bool -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.Date -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.DateTime -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.DoubleNumber -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.Email -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.Integer -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.Ipaddress -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.Text -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.Textarea -> beanFromString(bean, ota, attr.value.toString())
                is InsightAttribute.Time -> beanFromString(bean, ota, attr.value.toString())

                is InsightAttribute.Url -> objectAttributeBeanFactory.createObjectAttributeBeanForObject(
                    bean, ota, *attr.values.toTypedArray()
                )
                is InsightAttribute.Select -> objectAttributeBeanFactory.createObjectAttributeBeanForObject(
                    bean, ota, *attr.values.toTypedArray()
                )

                is InsightAttribute.Reference -> {
                    val referenceIds = attr.referencedObjects.map { it.id.raw }.toTypedArray()
                    objectAttributeBeanFactory.createReferenceAttributeValue(ota) { referenceIds.contains(it.id) }
                }
                is InsightAttribute.User -> {
                    val userKeys = attr.users.map { it.key }
                    objectAttributeBeanFactory.createUserAttributeValueByKey(ota, *userKeys.toTypedArray())
                }
                is InsightAttribute.Group -> {
                    val groupNames = attr.groups.map { it.name }
                    objectAttributeBeanFactory.createGroupAttributeValueByNames(ota, *groupNames.toTypedArray())
                }

                // TODO test additional attribute types
                is InsightAttribute.Project -> {
                    val projectIds = attr.projects.map { it.id }
                    objectAttributeBeanFactory.createProjectAttributeValue(ota) { projectIds.contains(it.id()) }
                }
                is InsightAttribute.Status -> {
                    val statusId = attr.status?.id
                    objectAttributeBeanFactory.createStatusAttributeValue(ota) { statusId != null && it.id == statusId }
                }
                is InsightAttribute.Version -> {
                    val versionIds = attr.versions.map { it.id.toLong() }
                    objectAttributeBeanFactory.createVersionAttributeValue(ota) { versionIds.contains(it.id()) }
                }
                is InsightAttribute.Confluence -> {
                    val pageIds = attr.pages.map { it.id.toLong() }
                    objectAttributeBeanFactory.createConfluenceAttributeValue(ota, *pageIds.toTypedArray())
                }
                is InsightAttribute.Unknown -> {
                    objectAttributeBeanFactory.createObjectAttributeBeanForObject(bean, ota)
                }
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
        (getObjectById(insightObjectId, toDomain).bind()
            ?.right() ?: ObjectNotFoundError(insightObjectId).left<ObjectNotFoundError>()).bind()
    }

    suspend fun getAttributeValues(
        objectTypeId: Int,
        attributeId: Int,
        query: String?,
        exceptionList: String?,
        page: Int,
        pageSize: Int
    ): Either<InsightClientError, AttributeValueResponse> = catchAsInsightClientError {
        val exceptions = exceptionList?.split(",")?.map { it.lowercase() }?: emptyList()
        val objectTypeAttributeBean =
            objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeId).firstOrNull { it.id == attributeId }
                ?: throw RuntimeException("objectType not found")

        val objects: List<ObjectBean> = iqlFacade.findObjects("objectTypeId = $objectTypeId", 0, 1000000).objects
        val allAttributeValues = objects.flatMap { it ->
            val attributeBean = it.objectAttributeBeans.firstOrNull { it.objectTypeAttributeId == attributeId }
            attributeBean?.let { runBlocking { mapAttributeBeanToInsightAttribute(it, objectTypeAttributeBean)
                .getOrNull() } }?.let {
                if(it.isMulti) {
                    it.displayValues?: emptyList()
                } else {
                    it.displayValue?.let {listOf(it)}?: emptyList()
                }
            }?: emptyList()
        }.toSet().sortedWith(NaturalOrderStringComparator.CASE_INSENSITIVE_ORDER)
        val filteredAttributeValues = allAttributeValues.filter {!exceptions.contains(it.lowercase()) }
            .filter { query.isNullOrEmpty() || it.lowercase().contains(query.lowercase()) }
        val pages = (filteredAttributeValues.size + pageSize - 1) / pageSize
        val paginationIndex = ((page-1).takeIf { it >= 0 }?:0)*pageSize
        val paginationEndIndex = min(paginationIndex+pageSize, filteredAttributeValues.size)
        val paginatedValues = filteredAttributeValues.subList(paginationIndex, paginationEndIndex)
        AttributeValueResponse(
            page,
            pages,
            pageSize,
            paginatedValues
        )
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
        val objectSelf =
            attributes
                .singleOrNull { it.schema?.name == "Link" }
                ?.toString()
                ?: "${baseUrl()}/secure/insight/assets/${objectBean.objectKey}"

        InsightObject(
            InsightObjectTypeId(objectBean.objectTypeId),
            InsightObjectId(objectBean.id),
            objectTypeBean.name,
            objectBean.objectKey,
            objectBean.label,
            attributes,
            hasAttachments,
            objectSelf
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
        val attributeId = InsightAttributeId(objectTypeAttributeBean.id)
        val schema = typeAttributeBeanToSchema(objectTypeAttributeBean)
        // see insight core ObjectAttributeBeanFactoryImpl.class for reference
        when (objectTypeAttributeBean.type) {
            Type.DEFAULT -> {
                handleDefaultValue(attributeId, schema, objectAttributeBean, objectTypeAttributeBean).bind()
            }
            Type.REFERENCED_OBJECT -> {
                val referencedObjects = objectAttributeBean.objectAttributeValueBeans.mapNotNull { attribute ->
                    loadReferencedObject(attribute, objectTypeAttributeBean).bind()
                }
                InsightAttribute.Reference(attributeId, referencedObjects, schema)
            }
            Type.USER -> {
                val users = objectAttributeBean.objectAttributeValueBeans.mapNotNull { attribute ->
                    loadAtlassianUserByKey(attribute.textValue).bind()
                }
                InsightAttribute.User(attributeId, users, schema)
            }
            Type.GROUP -> {
                val groups = objectAttributeBean.objectAttributeValueBeans.mapNotNull { attribute ->
                    JiraGroup(
                        attribute.textValue,
                        "${baseUrl()}/download/resources/com.riadalabs.jira.plugins.insight/images/${"group-logo.jpg"}"
                    )
                }
                InsightAttribute.Group(attributeId, groups, schema)
            }
            Type.VERSION -> {
                val versions = objectAttributeBean.objectAttributeValueBeans.mapNotNull { attribute: ObjectAttributeValueBean ->
                    versionAssembler.assembleVersion(attribute.integerValue.toLong())
                }
                InsightAttribute.Version(attributeId, versions, schema)
            }
            Type.CONFLUENCE -> { // TODO: add full support
                //resolve confluence pages; see DocumentationAssemblerInJira in plugins:insight:10.4.2
                val pageIds = objectAttributeBean.objectAttributeValueBeans.mapNotNull { it.integerValue }
                val pages = pageIds.map { ConfluencePage(it, "Loading Confluence Pages is not Implemented!", "#") }
                InsightAttribute.Confluence(attributeId, pages, schema)
            }
            Type.PROJECT -> { // see ProjectAssembler.class and ObjectAttributeBeanFactoryImpl.createProjectAttributeValue
                val projects = objectAttributeBean.objectAttributeValueBeans
                    .mapNotNull { projectService.getProjectById(user(), it.integerValue.toLong()).project }
                    .map {
                        val url = "${baseUrl()}/browse/${it.key}"
                        val avatarUrl = "${baseUrl()}/secure/projectavatar?pid=${it.id}"
                        JiraProject(it.id, it.key, it.name, url, avatarUrl)
                    }

                InsightAttribute.Project(attributeId, projects, schema)
            }
            Type.STATUS -> {
                val assetStatus = objectAttributeBean.objectAttributeValueBeans.firstOrNull()?.let { valueBean ->
                    val objectTypeBean = objectTypeFacade.loadObjectType(objectTypeAttributeBean.objectTypeId)
                    val allStatusTypeBeans = configureFacade.findAllStatusTypeBeans(objectTypeBean.objectSchemaId)
                    val statusTypeBean: StatusTypeBean = allStatusTypeBeans
                        .firstOrNull { it.id == valueBean.integerValue }
                        ?: return@let null
                    val categoryEnum = StatusCategory.from(statusTypeBean.category)
                        ?: return@let null
                    statusTypeBean.run {
                        StatusAttribute(id, name, categoryEnum, objectSchemaId, description)
                    }
                }
                InsightAttribute.Status(attributeId, assetStatus, schema)
            }
            else -> internalError("Unsupported objectTypeAttributeBean.type (${objectTypeAttributeBean.type})").bind()
        }
    }

    private suspend fun handleDefaultValue(
        id: InsightAttributeId,
        schema: ObjectTypeSchemaAttribute,
        objectAttributeBean: ObjectAttributeBean,
        objectTypeAttributeBean: ObjectTypeAttributeBean
    ): Either<InsightClientError, InsightAttribute> = either {
        val values = objectAttributeBean.objectAttributeValueBeans
        when (objectTypeAttributeBean.defaultType) {
            DefaultType.TEXT -> InsightAttribute.Text(id, values.firstOrNull()?.textValue, schema)
            DefaultType.INTEGER -> InsightAttribute.Integer(id,values.firstOrNull()?.integerValue, schema)
            DefaultType.BOOLEAN -> InsightAttribute.Bool(id,values.firstOrNull()?.booleanValue, schema)
            DefaultType.DOUBLE -> InsightAttribute.DoubleNumber(id,values.firstOrNull()?.doubleValue, schema)
            DefaultType.DATE -> {
                val date = values.firstOrNull()?.dateValue
                val localDate = date?.toInstant()?.atZone(zoneId)?.toLocalDate()
                val displayValue = date?.let { dateTimeFormatter.formatDateToString(it) }
                InsightAttribute.Date(id,localDate, displayValue, schema)
            }
            DefaultType.TIME -> {
                val date = values.firstOrNull()?.dateValue
                val localTime = date?.toInstant()?.atZone(zoneId)?.toLocalTime()
                val displayValue = null // Insights original ObjectAssembler does not handle this case at all.
                InsightAttribute.Time(id,localTime, schema, displayValue)
            }
            DefaultType.DATE_TIME -> {
                val date = values.firstOrNull()?.dateValue
                val zonedDateTime = date?.toInstant()?.atZone(zoneId)
                val displayValue = zonedDateTime?.let { dateTimeFormatter.formatDateTimeToString(Date.from(it.toInstant())) }
                InsightAttribute.DateTime(id,zonedDateTime, schema, displayValue)
            }
            DefaultType.EMAIL -> InsightAttribute.Email(id,values.firstOrNull()?.textValue, schema)
            DefaultType.TEXTAREA -> InsightAttribute.Textarea(id,values.firstOrNull()?.textValue, schema)
            DefaultType.IPADDRESS -> InsightAttribute.Ipaddress(id,values.firstOrNull()?.textValue, schema)

            // cardinality > 1
            DefaultType.URL -> InsightAttribute.Url(id,values.map { it.textValue }, schema)
            DefaultType.SELECT -> InsightAttribute.Select(id,values.map { it.textValue }, schema)
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

    private fun loadAtlassianUserByKey(userKey: String): Either<InsightClientError, JiraUser?> =
        catchAsInsightClientError {
            userManager.getUserByKey(userKey)?.run {
                val avatarUrl = avatarService.getAvatarURL(this, this).toASCIIString()
                JiraUser(key, name, emailAddress, avatarUrl, displayName)
            }
        }

    private fun getIQLWithChildren(objTypeId: InsightObjectTypeId, withChildren: Boolean): String =
        if (withChildren) {
            "objectType = objectTypeAndChildren(\"${objTypeId.raw}\")"
        } else {
            "objectTypeId=${objTypeId.raw}"
        }
}
