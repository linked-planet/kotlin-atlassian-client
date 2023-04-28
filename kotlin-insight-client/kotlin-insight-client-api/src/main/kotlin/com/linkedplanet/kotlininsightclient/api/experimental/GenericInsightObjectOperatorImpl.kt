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
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.GenericInsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectAttributeType
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.addReference
import com.linkedplanet.kotlininsightclient.api.model.getAttribute
import com.linkedplanet.kotlininsightclient.api.model.isReference
import com.linkedplanet.kotlininsightclient.api.model.setSingleReference
import com.linkedplanet.kotlininsightclient.api.model.setValue
import com.linkedplanet.kotlininsightclient.api.model.setValueList
import jdk.jfr.Experimental
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

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
    private val insightObjectForDomainObject: suspend (objectTypeId: Int, domainObject: DomainType) -> Either<InsightClientError, InsightObject?>,
    private val referenceAttributeToValue: suspend (attribute: InsightAttribute) -> Any? = { null },
    private val attributeToReferencedObjectId: suspend (attribute: ObjectTypeSchemaAttribute, Any?) -> List<Int> = { _, _ -> emptyList() },
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

    override suspend fun create(domainObject: DomainType): Either<InsightClientError, Int> = either {
        val insightObject = insightObjectOperator.createObject(objectTypeSchema.id) { io ->
            setAttributesFromDomainObject(io, domainObject)
        }.bind()
        insightObject.id
    }

    override suspend fun update(domainObject: DomainType): Either<InsightClientError, Int> = either {
        val insightObject = insightObjectForDomainObject(objectTypeSchema.id, domainObject).bind()!!
        setAttributesFromDomainObject(insightObject, domainObject)
        insightObjectOperator.updateObject(insightObject).bind()
        insightObject.objectTypeId
    }

    private suspend fun setAttributesFromDomainObject(insightObject: InsightObject, domainObject: DomainType) {
        props.forEach { prop ->
            val attribute = attrsMap[prop.name.lowercase()]!!
            val value = prop.get(domainObject)
            when (attribute.type) {
                InsightObjectAttributeType.DEFAULT -> {
                    if (value is List<Any?>) {
                        insightObject.setValueList(attribute.id, value)
                    } else {
                        insightObject.setValue(attribute.id, value)
                    }
                }
                InsightObjectAttributeType.REFERENCE -> {
                    val referencedObjectIds = attributeToReferencedObjectId(attribute, value)
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
        val insightObject = insightObjectOperator.getObjectByName(objectTypeSchema.id, name).bind()
            ?: return@either null
        domainObjectByInsightObject(insightObject).bind()
    }

    override suspend fun getByIQL(
        iql: String,
        withChildren: Boolean,
        pageFrom: Int,
        perPage: Int
    ): Either<InsightClientError, List<DomainType>> = either {
        val insightObjects =
            insightObjectOperator.getObjectsByIQL(objectTypeSchema.id, iql, withChildren, pageFrom, perPage)
                .bind()
        insightObjects.objects.map {
            domainObjectByInsightObject(it).bind()
        }
    }

    override suspend fun getById(objectId: Int): Either<InsightClientError, DomainType?> = either {
        val insightObject = insightObjectOperator.getObjectById(objectId).bind()
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
                param.type.isSubtypeOf(typeOf<List<Any>>()) -> extractListValues(param, attribute).bind()
                else -> defaultAttributeToValue(attribute?.value?.single()?.value?.toString(), param.type).bind()
            }
            mappedValue

        }.toTypedArray()
        val domainObject = constructor.call(*args)
        domainObject
    }

    private suspend fun extractListValues(
        param: KParameter,
        attribute: InsightAttribute?
    ): Either<InsightClientError, List<Any?>> = either {
        if (attribute == null) return@either emptyList()
        val genericTypeParam: KType = param.type.arguments.first().type!!
        attribute.value.map { objectAttributeValue ->
            defaultAttributeToValue(objectAttributeValue.value.toString(), genericTypeParam).bind()
        }
    }

    private suspend fun defaultAttributeToValue(
        valueAsString: String?,
        kType: KType
    ): Either<InsightClientError, Any?> = either {
        val value: String? = valueAsString
        value?.let {
            when (kType.classifier) {
                String::class -> value
                Int::class -> value.toInt()
                Boolean::class -> value.toBoolean()
                ZonedDateTime::class -> ZonedDateTime.parse(value)
                Double::class -> value.toDouble()
                Byte::class -> value.toByte()
                Short::class -> value.toShort()
                Long::class -> value.toLong()
                Float::class -> value.toFloat()
                Number::class -> value.toDouble()
                else -> invalidArgumentError<DomainType?>(
                    "kType.classifier ${kType.classifier} is not supported."
                ).bind()
            }
        }
    }

    private fun <T> invalidArgumentError(message: String): Either<InsightClientError, T> = Either.Left(
        InsightClientError(
            "InvalidArgumentError",
            message
        )
    )
}