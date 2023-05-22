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
package com.linkedplanet.kotlininsightclient.api.experimental

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.identity
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.GenericInsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.addReference
import com.linkedplanet.kotlininsightclient.api.model.clearReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getAttribute
import com.linkedplanet.kotlininsightclient.api.model.setSingleReference
import com.linkedplanet.kotlininsightclient.api.model.setValue
import com.linkedplanet.kotlininsightclient.api.model.setSelectValues
import jdk.jfr.Experimental
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * idea: check type safety when operator is initialized
 * idea: automatically create insight object types when they do not exist already
 * idea: Semi-Automatic but overridable manual mapping
 * idea: class to id map (for automatic child object parsing)
 * optional: insightObjectTypeId
 * optional: attribute name to id map in case one wants to manually specify everything
 */
@Experimental
class GenericInsightObjectOperatorImpl<DomainType : Any>(
    private val klass: KClass<DomainType>,
    // having InsightObject as return type will prevent us to change the internal implementation later on
    private val insightObjectForDomainObject: suspend (objectTypeId: InsightObjectTypeId, domainObject: DomainType) -> Either<InsightClientError, InsightObject?>,
    private val referenceAttributeToValue: suspend (attribute: InsightAttribute) -> Any? = { null },
    private val attributeToReferencedObjectId: suspend (attribute: ObjectTypeSchemaAttribute, Any?) -> List<InsightObjectId> = { _, _ -> emptyList() },
) : GenericInsightObjectOperator<DomainType> {
    private val props: Collection<KProperty1<DomainType, *>> = klass.memberProperties
    var objectTypeSchema: ObjectTypeSchema
    private var attrsMap: Map<String, ObjectTypeSchemaAttribute>

    companion object {
        // maybe we can use injection to gain access to the other operators
        // a more optimal version would skip the intermediate InsightObject anyway and create domain objects directly
        lateinit var insightObjectOperator: InsightObjectOperator
        lateinit var insightObjectTypeOperator: InsightObjectTypeOperator
        lateinit var insightSchemaOperator: InsightSchemaOperator
    }

    init {
        runBlocking {
            objectTypeSchema = objectTypeSchemaFromKClass().orNull()!!
            attrsMap = objectTypeSchema.attributes.associateBy { it.name.lowercase() }
        }
    }

    override suspend fun delete(domainObject: DomainType): Either<InsightClientError, Unit> = either {
        val insightObject = insightObjectForDomainObject(objectTypeSchema.id, domainObject).bind() ?: return@either
        insightObjectOperator.deleteObject(insightObject.id).bind()
    }

    override suspend fun create(domainObject: DomainType): Either<InsightClientError, DomainType> = either {
        val insightObject = createEmptyObject(objectTypeSchema.id)
        setAttributesFromDomainObject(insightObject, domainObject)
        val createdObject = insightObjectOperator.createObject(
            objectTypeSchema.id,
            *insightObject.attributes.toTypedArray(),
            toDomain = ::identity
        ).bind()
        domainObjectByInsightObject(createdObject).bind()
    }

    private fun createEmptyObject(objectTypeId: InsightObjectTypeId): InsightObject {
        return InsightObject(
            objectTypeId,
            InsightObjectId.notPersistedObjectId,
            "",
            "",
            "",
            emptyList(),
            false,
            ""
        )
    }

    override suspend fun update(domainObject: DomainType): Either<InsightClientError, DomainType> = either {
        val insightObject = insightObjectForDomainObject(objectTypeSchema.id, domainObject).bind()!!
        setAttributesFromDomainObject(insightObject, domainObject)
        insightObjectOperator.updateObject(insightObject).bind()
        domainObjectByInsightObject(insightObject).bind()
    }

    private suspend fun setAttributesFromDomainObject(insightObject: InsightObject, domainObject: DomainType) {
        props.forEach { prop ->
            val attribute = attrsMap[prop.name.lowercase()]!!
            val value = prop.get(domainObject)
            when (attribute.type) {
                InsightObjectAttributeType.DEFAULT -> {
                    when (value) {
                        is String -> insightObject.setValue(attribute.id, value)
                        is Int -> insightObject.setValue(attribute.id, value)
                        is Boolean -> insightObject.setValue(attribute.id, value)
                        is Double -> insightObject.setValue(attribute.id, value)
                        is Float -> insightObject.setValue(attribute.id, value.toDouble())
                        is ZonedDateTime -> insightObject.setValue(attribute.id, value, value.toString())
                        is List<Any?> -> insightObject.setSelectValues(attribute.id,
                            (value as? List<*>)?.map(Any?::toString) ?: emptyList())
                        else -> TODO()
                    }
                }
                InsightObjectAttributeType.REFERENCE -> {
                    val referencedObjectIds = attributeToReferencedObjectId(attribute, value)
                    insightObject.clearReferenceValue(attribute.id)
                    if (value is List<Any?>) {
                        referencedObjectIds.forEach {
                            insightObject.addReference(attribute.id, it)
                        }
                    } else {
                        if (referencedObjectIds.isNotEmpty()) {
                            insightObject.setSingleReference(attribute.id, referencedObjectIds.first())
                        }
                    }
                }
                else -> invalidArgumentError<DomainType>("Attribute.type ${attribute.type.name} is not supported")
            }
        }
    }

    private suspend fun objectTypeSchemaFromKClass(): Either<InsightClientError, ObjectTypeSchema> =
        either {
            val klassName = klass.simpleName
            val insightSchemas = insightSchemaOperator.getSchemas().bind()
            val objectTypeSchemas = insightSchemas
                .flatMap { schema -> insightObjectTypeOperator.getObjectTypesBySchema(schema.id).bind() }
            val objectTypeSchema: ObjectTypeSchema = objectTypeSchemas.first { it.name == klassName }
            objectTypeSchema
        }

    override suspend fun getByName(name: String): Either<InsightClientError, DomainType?> = either {
        val insightObject = insightObjectOperator.getObjectByName(objectTypeSchema.id, name, ::identity).bind()
            ?: return@either null
        domainObjectByInsightObject(insightObject).bind()
    }

    override suspend fun getByIQL(
        iql: String,
        withChildren: Boolean,
        pageIndex: Int,
        pageSize: Int
    ): Either<InsightClientError, List<DomainType>> = either {
        val insightObjects =
            insightObjectOperator.getObjectsByIQL(objectTypeSchema.id, iql, withChildren, pageIndex, pageSize, ::identity)
                .bind()
        insightObjects.objects.map {
            domainObjectByInsightObject(it).bind()
        }
    }

    override suspend fun getById(objectId: InsightObjectId): Either<InsightClientError, DomainType?> = either {
        val insightObject = insightObjectOperator.getObjectById(objectId, ::identity).bind()
            ?: return@either null
        domainObjectByInsightObject(insightObject).bind()
    }

    private suspend fun domainObjectByInsightObject(
        insightObject: InsightObject,
    ): Either<InsightClientError, DomainType> = either {
        val constructor = klass.primaryConstructor!!
        val args = constructor.parameters.map { param ->
            val attributeId = attrsMap[param.name!!.lowercase()]!!.id
            val attribute = insightObject.getAttribute(attributeId)
            val mappedValue = when {
                attribute?.isReference() == true -> referenceAttributeToValue(attribute)
                attribute == null -> null
                else -> defaultAttributeToValue(attribute.value, param.type).bind()
            }
            mappedValue

        }.toTypedArray()
        val domainObject = constructor.call(*args)
        domainObject
    }

    private suspend fun defaultAttributeToValue(
        value: ObjectAttributeValue,
        kType: KType
    ): Either<InsightClientError, Any?> = either {
        when(value) {
            is ObjectAttributeValue.Bool -> value.value
            is ObjectAttributeValue.Date -> value.value
            is ObjectAttributeValue.DateTime -> value.value
            is ObjectAttributeValue.DoubleNumber -> value.value
            is ObjectAttributeValue.Email -> value.value
            is ObjectAttributeValue.Integer -> value.value
            is ObjectAttributeValue.Ipaddress -> value.value
            is ObjectAttributeValue.Text -> value.value
            is ObjectAttributeValue.Textarea -> value.value
            is ObjectAttributeValue.Time -> value.value
            is ObjectAttributeValue.Url -> value.value
            is ObjectAttributeValue.Select -> value.values// List<String>
            else -> invalidArgumentError<DomainType?>(
                "kType.classifier ${kType.classifier} is not supported."
            ).bind()
        }
    }

    private fun <T> invalidArgumentError(message: String): Either<InsightClientError, T> = Either.Left(
        InsightClientError(
            "InvalidArgumentError",
            message
        )
    )
}