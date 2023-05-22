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

import java.time.ZonedDateTime
import java.util.Collections.emptyList

// region ID wrapper

@JvmInline
value class InsightObjectId(val value: Int) {
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

/**
 * See type attribute in response of https://insight-javadoc.riada.io/insight-javadoc-8.6/insight-rest/#object__id__attributes_get
 */
enum class InsightObjectAttributeType(val attributeTypeId: Int) {
    UNKNOWN(-1),
    DEFAULT(0),
    REFERENCE(1),
    USER(2),
    CONFLUENCE(3),
    GROUP(4),
    VERSION(5),
    PROJECT(6),
    STATUS(7);

    companion object {
        fun parse(value: Int) =
            values().singleOrNull { it.attributeTypeId == value } ?: UNKNOWN
    }
}

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
data class InsightAttribute(
    val attributeId: InsightAttributeId,
    var value: ObjectAttributeValue,
    val schema: ObjectTypeSchemaAttribute?
) {
    fun isValueAttribute(): Boolean = when(value){
        is ObjectAttributeValue.Text -> true
        is ObjectAttributeValue.Integer -> true
        is ObjectAttributeValue.Bool -> true
        is ObjectAttributeValue.DoubleNumber -> true
        is ObjectAttributeValue.Select -> true
        is ObjectAttributeValue.Date -> true
        is ObjectAttributeValue.Time -> true
        is ObjectAttributeValue.DateTime -> true
        is ObjectAttributeValue.Url -> true
        is ObjectAttributeValue.Email -> true
        is ObjectAttributeValue.Textarea -> true
        is ObjectAttributeValue.Ipaddress -> true

        is ObjectAttributeValue.Unknown -> false
        is ObjectAttributeValue.Reference -> false
        is ObjectAttributeValue.User -> false
        is ObjectAttributeValue.Confluence -> false
        is ObjectAttributeValue.Group -> false
        is ObjectAttributeValue.Version -> false
        is ObjectAttributeValue.Project -> false
        is ObjectAttributeValue.Status -> false
    }

    fun isReference() : Boolean = value is ObjectAttributeValue.Reference

    companion object {
        infix fun InsightAttributeId.toValue(text: String) = InsightAttribute(
            this,
            value = ObjectAttributeValue.Text(text),
            schema = null, // null during creation
        )
        infix fun InsightAttributeId.toValue(value: Int) = InsightAttribute(
            this,
            value = ObjectAttributeValue.Integer(value),
            schema = null, // null during creation
        )
        infix fun InsightAttributeId.toValue(referencedObjectId: InsightObjectId?) = InsightAttribute(
            this,
            value = ObjectAttributeValue.Reference(listOfNotNull(referencedObjectId?.let {
                (ReferencedObject(
                    it,
                    "",
                    "",
                    null
                ))
            })),
            schema = null, // null during creation
        )
        infix fun InsightAttributeId.toReference(referencedObjectId: InsightObjectId?) = toValue(referencedObjectId)

    }

}

// region InsightObjectTypeOperator
data class ObjectTypeSchema(
    val id: InsightObjectTypeId,
    val name: String,
    val attributes: List<ObjectTypeSchemaAttribute>,
    val parentObjectTypeId: InsightObjectTypeId?
)

data class ObjectTypeSchemaAttribute(
    val id: InsightAttributeId,
    val name: String,
    val defaultType: DefaultType?, // only set when the type is a default type
    val options: String, // comma separated list of String for default type Select (comma is invalid character inside an option)
    val minimumCardinality: Int,
    val maximumCardinality: Int,
    val referenceKind: ReferenceKind?,
    val includeChildObjectTypes: Boolean,
    val referenceObjectTypeId: InsightObjectTypeId?, // objectTypeId of the referenced object
    val type: InsightObjectAttributeType
)

// if attributeType is default, this determines which kind of default type the value is
enum class DefaultType(var defaultTypeId: Int) {
    // NONE(-1),  // HTTP API models this with null, sdk with NONE
    TEXT(0),
    INTEGER(1),
    BOOLEAN(2),
    DOUBLE(3),
    DATE(4),
    TIME(5),
    DATE_TIME(6),
    URL(7),
    EMAIL(8),
    TEXTAREA(9),
    SELECT(10),
    IPADDRESS(11);

    companion object {
        fun parse(defaultTypeId: Int): DefaultType? =
            DefaultType.values().singleOrNull { it.defaultTypeId == defaultTypeId }
    }
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
        fun parse(referenceKindId: Int): ReferenceKind =
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

sealed class ObjectAttributeValue{

    class Text(val value: String?) : ObjectAttributeValue()
    class Integer(val value: Int?) : ObjectAttributeValue()
    class Bool(val value: Boolean?) : ObjectAttributeValue()
    class DoubleNumber(val value: Double?) : ObjectAttributeValue()
    class Date(val value: ZonedDateTime?, val displayValue: String?) : ObjectAttributeValue()
    class Time(val value: ZonedDateTime?, val displayValue: String?) : ObjectAttributeValue()
    class DateTime(val value: ZonedDateTime?, val displayValue: String?) : ObjectAttributeValue()
    class Url(val value: String?) : ObjectAttributeValue()
    class Email(val value: String?) : ObjectAttributeValue()
    class Textarea(val value: String?) : ObjectAttributeValue()
    class Ipaddress(val value: String?) : ObjectAttributeValue()
    class Select(val values: List<String>) : ObjectAttributeValue() // only default value with cardinality > 1


    class Reference(val referencedObjects: List<ReferencedObject>) : ObjectAttributeValue()
    class User(val users: List<InsightUser>) : ObjectAttributeValue()
    class Confluence : ObjectAttributeValue() // A value that describes a page in Confluence
    class Group : ObjectAttributeValue() // The Insight Group type
    class Version : ObjectAttributeValue() // Value describing a version in Jira
    class Project: ObjectAttributeValue() // Value that represents a Jira project
    class Status : ObjectAttributeValue() // An Insight status type that can be associated with objects Cardinality:0-1
    class Unknown : ObjectAttributeValue()

    override fun toString(): String = when (this) {
        is Text -> value ?: ""
        is Integer -> value?.toString() ?: ""
        is Bool -> value?.toString() ?: ""
        is Date -> value?.toString() ?: ""
        is DoubleNumber -> value?.toString() ?: ""
        is Email -> value ?: ""
        is Url -> value ?: ""
        is Ipaddress -> value ?: ""
        is Textarea -> value ?: ""
        is DateTime -> value?.toString() ?: ""
        is Time -> value?.toString() ?: ""
        is Select -> values.joinToString(",")

        is Reference -> referencedObjects.joinToString(",") { it.objectKey }
        is User -> users.joinToString(",") { it.key }

        is Group -> "" // TODO
        is Project -> "" // TODO
        is Status -> "" // TODO
        is Version -> "" // TODO
        is Confluence -> "" // TODO
        is Unknown -> ""
    }
}

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