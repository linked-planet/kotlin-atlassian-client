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
package com.linkedplanet.kotlininsightclient.api.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Collections.emptyList

// region ID wrapper

@JvmInline
value class InsightObjectId(val raw: Int) {
    companion object {
        val notPersistedObjectId = InsightObjectId(-1)
    }
}

@JvmInline
value class InsightObjectTypeId(val raw: Int)

@JvmInline
value class AttachmentId(val raw: Int)

@JvmInline
value class InsightSchemaId(val raw: Int)

@JvmInline
value class InsightAttributeId(val raw: Int)

// endregion ID wrapper

data class InsightObjectPage<T>(
    val totalFilterCount: Int = -1,
    val objects: List<T> = emptyList(),
)

data class Page<T> (
    val items: List<T>,
    val totalItems: Int,
    val totalPages: Int,
    val currentPageIndex: Int,
    val pageSize: Int
)

fun <T> InsightObjectPage<T>.plus(insightObjectPage: InsightObjectPage<T>): InsightObjectPage<T> =
    InsightObjectPage(
        this.totalFilterCount + insightObjectPage.totalFilterCount,
        this.objects.plus(insightObjectPage.objects)
    )

data class InsightObject(
    val objectTypeId: InsightObjectTypeId,
    val id: InsightObjectId,
    val objectTypeName: String,
    val objectKey: String,
    val label: String,
    var attributes: List<InsightAttribute>,
    val attachmentsExist: Boolean,
    val objectSelf: String
)

data class InsightReference(
    val objectTypeId: InsightObjectTypeId,
    val objectTypeName: String,
    val objectId: InsightObjectId,
    val objectKey: String,
    val objectName: String
)

/**
 * Holds the actual data value(s)
 */
sealed class InsightAttribute(
    val attributeId: InsightAttributeId,
    val schema: ObjectTypeSchemaAttribute?
) {
    fun isValueAttribute(): Boolean = when(this){
        is Text -> true
        is Integer -> true
        is Bool -> true
        is DoubleNumber -> true
        is Select -> true
        is Date -> true
        is Time -> true
        is DateTime -> true
        is Url -> true
        is Email -> true
        is Textarea -> true
        is Ipaddress -> true

        is Unknown -> false
        is Reference -> false
        is User -> false
        is Confluence -> false
        is Group -> false
        is Version -> false
        is Project -> false
        is Status -> false
    }
    fun isReference() : Boolean = this is Reference


    class Text(attributeId: InsightAttributeId, val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Integer(attributeId: InsightAttributeId,val value: Int?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Bool(attributeId: InsightAttributeId,val value: Boolean?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class DoubleNumber(attributeId: InsightAttributeId,val value: Double?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Date(attributeId: InsightAttributeId,val value: LocalDate?, val displayValue: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    /**
     * Note that time is part of the Enum inside the SDK, but is not selectable through the Insight GUI
     */
    class Time(attributeId: InsightAttributeId,val value: LocalTime?, val displayValue: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class DateTime(attributeId: InsightAttributeId,val value: ZonedDateTime?, val displayValue: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Email(attributeId: InsightAttributeId,val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Textarea(attributeId: InsightAttributeId,val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Ipaddress(attributeId: InsightAttributeId,val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)

    //  cardinality > 1
    class Url(attributeId: InsightAttributeId,val values: List<String>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Select(attributeId: InsightAttributeId,val values: List<String>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)

    // non default types
    class Reference(attributeId: InsightAttributeId,val referencedObjects: List<ReferencedObject>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class User(attributeId: InsightAttributeId,val users: List<InsightUser>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)
    class Confluence(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema) // A value that describes a page in Confluence
    class Group(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema) // The Insight Group type
    class Version(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema) // Value describing a version in Jira
    class Project(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?): InsightAttribute(attributeId, schema) // Value that represents a Jira project
    class Status(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema) // An Insight status type that can be associated with objects Cardinality:0-1
    class Unknown(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema)

    override fun toString(): String = when (this) {
        is Text -> value ?: ""
        is Integer -> value?.toString() ?: ""
        is Bool -> value?.toString() ?: ""
        is Date -> value?.toString() ?: ""
        is DoubleNumber -> value?.toString() ?: ""
        is Email -> value ?: ""
        is Ipaddress -> value ?: ""
        is Textarea -> value ?: ""
        is DateTime -> value?.toString() ?: ""
        is Time -> value?.toString() ?: ""

        is Url ->  values.joinToString(",")
        is Select -> values.joinToString(",")

        is Reference -> referencedObjects.joinToString(",") { it.objectKey }
        is User -> users.joinToString(",") { it.key }

        is Group -> "" // TODO
        is Project -> ""
        is Status -> ""
        is Version -> ""
        is Confluence -> ""
        is Unknown -> ""
    }

    companion object {
        infix fun InsightAttributeId.toValue(text: String?) =
            Text(this, value = text, schema = null)
        infix fun InsightAttributeId.toValue(value: Int?) =
            Integer(this, value = value, schema = null)

        infix fun InsightAttributeId.toValue(value: Boolean?) =
            Bool(this, value = value, schema = null)
        infix fun InsightAttributeId.toValue(value: Double?) =
            DoubleNumber(this, value = value, schema = null)
        infix fun InsightAttributeId.toValue(value: LocalDate?) =
            Date(this, value = value, schema = null, displayValue = null)
        infix fun InsightAttributeId.toValue(value: LocalTime?) =
            Time(this, value = value, schema = null, displayValue = null)
        infix fun InsightAttributeId.toValue(value: ZonedDateTime?) =
            DateTime(this, value = value, schema = null, displayValue = null)
        infix fun InsightAttributeId.toEmailValue(text: String?) =
            Email(this, value = text, schema = null)
        infix fun InsightAttributeId.toTextareaValue(text: String?) =
            Textarea(this, value = text, schema = null)
        infix fun InsightAttributeId.toIpaddressValue(text: String?) =
            Ipaddress(this, value = text, schema = null)

        infix fun InsightAttributeId.toSelectValues(values: List<String>) =
            Select(this, values = values, schema = null)

        infix fun InsightAttributeId.toUrlValues(values: List<String>) =
            Url(this, values = values, schema = null)

        infix fun InsightAttributeId.toReference(referencedObjectId: InsightObjectId?) =
            Reference(
                this,
                referencedObjects = listOfNotNull(referencedObjectId?.let {
                    ReferencedObject(it, "", "", null)
                }),
                schema = null,
            )

        infix fun InsightAttributeId.toReferences(referencedObjectIds: List<InsightObjectId>) =
            Reference(
                this,
                referencedObjects = referencedObjectIds.map {
                    ReferencedObject(it, "", "", null)
                },
                schema = null,
            )
    }

}

// region InsightObjectTypeOperator
data class ObjectTypeSchema(
    val id: InsightObjectTypeId,
    val name: String,
    val attributes: List<ObjectTypeSchemaAttribute>,
    val parentObjectTypeId: InsightObjectTypeId?
)

sealed class ObjectTypeSchemaAttribute {

    abstract val id: InsightAttributeId
    abstract val name: String // attributeName
    abstract val minimumCardinality: Int
    abstract val maximumCardinality: Int
    abstract val includeChildObjectTypes: Boolean

    fun isValueAttribute(): Boolean = when(this){
        is Text -> true
        is Integer -> true
        is Bool -> true
        is DoubleNumber -> true
        is Select -> true
        is Date -> true
        is Time -> true
        is DateTime -> true
        is Url -> true
        is Email -> true
        is Textarea -> true
        is Ipaddress -> true

        is Unknown -> false
        is Reference -> false
        is User -> false
        is Confluence -> false
        is Group -> false
        is Version -> false
        is Project -> false
        is Status -> false
    }

    fun isReference() : Boolean = this is Reference

    class Select(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
        val options: List<String>,
    ) : ObjectTypeSchemaAttribute() // Select is the only DefaultType with maximumCardinality > 1

    class Reference(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
        val referenceObjectTypeId: InsightObjectTypeId, // objectTypeId of the referenced object
        val referenceKind: ReferenceKind
    ) : ObjectTypeSchemaAttribute()

    class Unknown(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
        val debugDescription: String
    ) : ObjectTypeSchemaAttribute()

    // region types having just the superclass attributes
    data class Text(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Integer(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Bool(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class DoubleNumber(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Date(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Time(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class DateTime(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Url(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Email(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Textarea(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Ipaddress(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()

    class User(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Confluence(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Group(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Version(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Project(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()
    class Status(
        override val id: InsightAttributeId,
        override val name: String,
        override val minimumCardinality: Int,
        override val maximumCardinality: Int,
        override val includeChildObjectTypes: Boolean,
    ) : ObjectTypeSchemaAttribute()

    // endregion types having just the superclass attributes
}

/**
 * This is the "Additional Value" one can select when choosing object as the attribute type.
 */
enum class ReferenceKind(var referenceKindId: Int) {
    UNKNOWN(-1),
    DEPENDENCY(1),
    LINK(2),
    REFERENCE(3),
    FINANCIAL(4),
    TECHNICAL(5);

    companion object {
        fun parse(referenceKindId: Int?): ReferenceKind =
            ReferenceKind.values().singleOrNull { it.referenceKindId == referenceKindId } ?: UNKNOWN
    }
}

// endregion InsightObjectTypeOperator

// region InsightSchemaOperator
data class InsightSchema(
    val id: InsightSchemaId,
    val name: String,
    val objectCount: Int,
    val objectTypeCount: Int
)
// endregion InsightSchemaOperator

data class InsightUser(
    val displayName: String,
    val name: String,
    val emailAddress: String,
    val key: String
)

data class ReferencedObject(
    var id: InsightObjectId,
    var label: String,
    var objectKey: String,
    var objectType: ReferencedObjectType?
)

data class ReferencedObjectType(
    val id: InsightObjectTypeId,
    val name: String
)

// region InsightHistoryOperator
data class InsightHistory(
    val objectId: InsightObjectId,
    val historyItems: List<InsightHistoryItem>
)

data class InsightHistoryItem(
    val id: Int,
    val affectedAttribute: String?,
    val oldValue: String?,
    val newValue: String?,
    val actor: Actor,
    val type: Int,
    val created: String, // updated is neither available through sdk nor ktor
    val objectId: InsightObjectId
)

data class Actor(
    val key: String
)
// endregion InsightHistoryOperator

// region InsightAttachmentOperator
data class InsightAttachment(
    val id: AttachmentId,
    val author: String,
    val mimeType: String,
    val filename: String,
    val filesize: String, // for human display e.g. 10.1 kB
    val created: String, // ISO 8601 String
    val comment: String, // we are only able to download them but can not create attachments with comments
    val url: String
)
// endregion InsightAttachmentOperator