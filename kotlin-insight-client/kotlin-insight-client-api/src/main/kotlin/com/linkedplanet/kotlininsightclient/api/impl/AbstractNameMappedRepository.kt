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
package com.linkedplanet.kotlininsightclient.api.impl

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.right
import arrow.core.sequenceEither
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.InvalidArgumentInsightClientError
import com.linkedplanet.kotlininsightclient.api.error.asEither
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toReferences
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toSelectValues
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchema
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.getAttribute
import jdk.jfr.Experimental
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * This is an experimental implementation that maps DomainObjects to InsightObjects automatically by name of the
 * class/type and attribute names
 *
 * idea: check type safety when operator is initialized
 * idea: automatically create insight object types when they do not exist already
 * idea: Semi-Automatic but overridable manual mapping
 * idea: class to id map (for automatic child object parsing)
 * optional: attribute name to id map in case one wants to manually specify everything
 */
@Experimental
abstract class AbstractNameMappedRepository<DomainType : Any>(
    private val klass: KClass<DomainType>
) : AbstractInsightObjectRepository<DomainType>() {

    abstract val insightObjectTypeOperator: InsightObjectTypeOperator
    abstract val insightSchemaOperator: InsightSchemaOperator
    override var RESULTS_PER_PAGE: Int = Int.MAX_VALUE
    override val objectTypeId: InsightObjectTypeId get() = objectTypeSchema.id
    @Suppress("MemberVisibilityCanBePrivate") // this is public, so clients could use it to add missing functionality
    protected val objectTypeSchema: ObjectTypeSchema by lazy { objectTypeSchemaFromKClass().orNull()!! }

    private val props: Collection<KProperty1<DomainType, *>> = klass.memberProperties
    private val attrsMap: Map<String, ObjectTypeSchemaAttribute> by lazy {
        objectTypeSchema.attributes.associateBy { it.name.lowercase() }
    }

    abstract suspend fun referenceAttributeToValue(attribute: InsightAttribute) : Any?

    abstract suspend fun attributeToReferencedObjectId(schemaAttribute: ObjectTypeSchemaAttribute, value: Any?): List<InsightObjectId>

    override suspend fun toDomain(insightObject: InsightObject): Either<InsightClientError, DomainType> =
        domainObjectByInsightObject(insightObject)

    override suspend fun attributesFromDomain(domainObject: DomainType): Either<InsightClientError, List<InsightAttribute>> =
        props.mapNotNull { prop ->
            val attributeType: ObjectTypeSchemaAttribute = attrsMap[prop.name.lowercase()] ?: return@mapNotNull null
            val value: Any? = prop.get(domainObject)
            when {
                attributeType.isValueAttribute -> mapValueAttribute(value, attributeType)

                attributeType is ObjectTypeSchemaAttribute.ReferenceSchema -> {
                    val referencedObjectIds = attributeToReferencedObjectId(attributeType, value)
                    (attributeType.id toReferences referencedObjectIds).right()
                }

                // User, Group, Version ...

                else -> InvalidArgumentInsightClientError(
                    "Attribute.type ${attributeType.name} is not supported"
                ).asEither()
            }
        }.sequenceEither()

    private fun mapValueAttribute(
        value: Any?,
        attributeType: ObjectTypeSchemaAttribute
    ): Either<InsightClientError, InsightAttribute> =
        when (value) {
            is String -> (attributeType.id toValue value).right()
            is Int -> (attributeType.id toValue value).right()
            is Boolean -> (attributeType.id toValue value).right()
            is Double -> (attributeType.id toValue value).right()
            is Float -> (attributeType.id toValue value.toDouble()).right()
            is ZonedDateTime -> (attributeType.id toValue value).right()
            is List<Any?> -> (attributeType.id toSelectValues ((value as? List<*>)?.map(Any?::toString)
                ?: emptyList())).right()
            else -> InvalidArgumentInsightClientError(
                "Attribute.type ${attributeType.name} is not supported"
            ).asEither()
        }

    private fun objectTypeSchemaFromKClass(): Either<InsightClientError, ObjectTypeSchema> =
        runBlocking {
            either {
                val klassName = klass.simpleName
                val insightSchemas = insightSchemaOperator.getSchemas().bind()
                val objectTypeSchemas = insightSchemas
                    .flatMap { schema -> insightObjectTypeOperator.getObjectTypesBySchema(schema.id).bind() }
                objectTypeSchemas.first { it.name == klassName }
            }
        }

    private suspend fun domainObjectByInsightObject(
        insightObject: InsightObject,
    ): Either<InsightClientError, DomainType> = either {
        val constructor = klass.primaryConstructor!!
        val args = constructor.parameters.map { param ->
            val attributeId = attrsMap[param.name?.lowercase()]?.id ?: return@map null
            val attribute = insightObject.getAttribute(attributeId)
            val mappedValue = when {
                attribute?.let { it is InsightAttribute.Reference } == true -> referenceAttributeToValue(attribute)
                attribute == null -> null
                else -> defaultAttributeToValue(attribute, param.type).bind()
            }
            mappedValue

        }.toTypedArray()
        val domainObject = constructor.call(*args)
        domainObject
    }

    private suspend fun defaultAttributeToValue(
        attribute: InsightAttribute,
        kType: KType
    ): Either<InsightClientError, Any?> = either {
        when (attribute) {
            is InsightAttribute.Bool -> attribute.value
            is InsightAttribute.Date -> attribute.value
            is InsightAttribute.DateTime -> attribute.value
            is InsightAttribute.DoubleNumber -> attribute.value
            is InsightAttribute.Email -> attribute.value
            is InsightAttribute.Integer -> attribute.value
            is InsightAttribute.Ipaddress -> attribute.value
            is InsightAttribute.Text -> attribute.value
            is InsightAttribute.Textarea -> attribute.value
            is InsightAttribute.Time -> attribute.value
            is InsightAttribute.Url -> attribute.values
            is InsightAttribute.Select -> attribute.values// List<String>
            else -> InvalidArgumentInsightClientError(
                "kType.classifier ${kType.classifier} is not supported."
            ).asEither<DomainType?>(
            ).bind()
        }
    }
}