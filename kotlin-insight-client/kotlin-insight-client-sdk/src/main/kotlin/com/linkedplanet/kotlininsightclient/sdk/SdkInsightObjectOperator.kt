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
import arrow.core.left
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.ObjectTypeNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.*
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.services.model.*
import io.riada.core.collector.model.toDisplayValue
import org.slf4j.LoggerFactory
import javax.inject.Named
import com.atlassian.jira.component.ComponentAccessor

@Named
object SdkInsightObjectOperator : InsightObjectOperator {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override var RESULTS_PER_PAGE: Int = 25

    @ComponentImport
    lateinit var objectFacade: ObjectFacade

    @ComponentImport
    lateinit var objectTypeFacade: ObjectTypeFacade

    @ComponentImport
    lateinit var objectSchemaFacade: ObjectSchemaFacade

    @ComponentImport
    lateinit var objectTypeAttributeFacade: ObjectTypeAttributeFacade

    private val iqlFacade by lazy { ComponentAccessor.getOSGiComponentInstanceOfType(IQLFacade::class.java) }

//    @Inject
//    fun init(
//        @ComponentImport objectFacade: ObjectFacade,
//        @ComponentImport objectTypeFacade: ObjectTypeFacade,
//        @ComponentImport objectSchemaFacade: ObjectSchemaFacade,
//        @ComponentImport objectTypeAttributeFacade: ObjectTypeAttributeFacade,
//    ) {
//        this.objectFacade = objectFacade
//        this.objectTypeFacade = objectTypeFacade
//        this.objectSchemaFacade = objectSchemaFacade
//        this.objectTypeAttributeFacade = objectTypeAttributeFacade
//    }

    override suspend fun getObjectById(id: Int): Either<InsightClientError, InsightObject?> =
        objectFacade.loadObjectBean(id)
            ?.toInsightObject()
            ?: Either.Right(null)

    override suspend fun getObjectByKey(key: String): Either<InsightClientError, InsightObject?> =
        objectFacade.loadObjectBean(key)
            ?.toInsightObject()
            ?: Either.Right(null)

    override suspend fun getObjectByName(objectTypeId: Int, name: String): Either<InsightClientError, InsightObject?> {
        val objs = iqlFacade.findObjects("objectTypeId=$objectTypeId AND Name=$name")
        return if (objs.size > 0) {
            val obj = objs.first()
            obj.toInsightObject()
        } else {
            Either.Right(null)
        }
    }

    override suspend fun getObjects(
        objectTypeId: Int,
        withChildren: Boolean,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> {
        val iql = getIQLWithChildren(objectTypeId, withChildren)
        val objs = iqlFacade.findObjects(iql, (pageFrom - 1) * perPage, perPage)
        return objs.toInsightObjects()
    }

    override suspend fun getObjectsByIQL(
        objectTypeId: Int,
        withChildren: Boolean,
        iql: String,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> {
        TODO("Not yet implemented")
    }

    override suspend fun getObjectsByIQL(
        iql: String,
        pageFrom: Int,
        pageTo: Int?,
        perPage: Int
    ): Either<InsightClientError, InsightObjects> {
        TODO("Not yet implemented")
    }

    override suspend fun getObjectCount(iql: String): Either<InsightClientError, Int> {
        TODO("Not yet implemented")
    }

    override suspend fun updateObject(obj: InsightObject): Either<InsightClientError, InsightObject> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteObject(id: Int): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createObject(
        objectTypeId: Int,
        func: (InsightObject) -> Unit
    ): Either<InsightClientError, InsightObject> {
        TODO("Not yet implemented")
    }

    private suspend fun ObjectResultBean.toInsightObjects(): Either<InsightClientError, InsightObjects> = either {
        InsightObjects(
            this@toInsightObjects.totalFilterSize,
            this@toInsightObjects.objects.map { it.toInsightObject().bind() })
    }

    private suspend fun ObjectBean.toInsightObject(): Either<InsightClientError, InsightObject> {
        return try {
            val objectType = objectTypeFacade.loadObjectType(this.objectTypeId)
            val objectTypeAttributeBeans = objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectType.id)
            val hasAttachments = objectFacade.findAttachmentBeans(id).isNotEmpty()
            createInsightObject(this, objectType, objectTypeAttributeBeans, hasAttachments)
        } catch (e: Exception) {
            InsightClientError.fromException(e).left()
        }
    }

    private suspend fun createInsightObject(
        objectBean: ObjectBean,
        objectTypeBean: ObjectTypeBean,
        objectTypeAttributeBeans: List<ObjectTypeAttributeBean>,
        hasAttachments: Boolean
    ): Either<InsightClientError, InsightObject> = either {
        val attributes = objectBean.objectAttributeBeans.map { objAttributeBean ->
            val objTypeAttributeBean =
                objectTypeAttributeBeans
                    .singleOrNull { it.id == objAttributeBean.objectTypeAttributeId }
                    ?: ObjectTypeNotFoundError().left().bind()
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
            attributes.single { it.attributeName == "Link" }.toDisplayValue() as String
        )
    }

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
