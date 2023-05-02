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

// endregion ID wrapper

data class InsightObjects(
    val totalFilterCount: Int = -1,
    val objects: List<InsightObject> = emptyList()
)

fun InsightObjects.plus(insightObjects: InsightObjects): InsightObjects =
    InsightObjects(
        this.totalFilterCount + insightObjects.totalFilterCount,
        this.objects.plus(insightObjects.objects)
    )

data class InsightObject(
    val objectTypeId: InsightObjectTypeId,
    var id: InsightObjectId,
    val objectTypeName: String,
    var objectKey: String,
    var label: String,
    var attributes: List<InsightAttribute>,
    var attachmentsExist: Boolean,
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
    val attributeId: Int,
    val attributeName: String?,
    val attributeType: InsightObjectAttributeType,
    val defaultType: ObjectTypeAttributeDefaultType?,
    val options: String?,
    val minimumCardinality: Int?,
    val maximumCardinality: Int?,
    var value: List<ObjectAttributeValue>,
)

// region InsightObjectTypeOperator
data class ObjectTypeSchema(
    val id: InsightObjectTypeId,
    val name: String,
    val attributes: List<ObjectTypeSchemaAttribute>,
    val parentObjectTypeId: InsightObjectTypeId?
)

data class ObjectTypeSchemaAttribute(
    val id: Int,
    val name: String,
    val defaultType: DefaultType?,
    val options: String,
    val minimumCardinality: Int,
    val maximumCardinality: Int,
    val referenceKind: ReferenceKind?,
    val includeChildObjectTypes: Boolean,
    val referenceObjectTypeId: InsightObjectTypeId?, // objectTypeId of the referenced object
    val type: InsightObjectAttributeType
)

// if attributeType is default, this determines which kind of default type the value is
enum class DefaultType(var defaultTypeId: Int) {
//    NONE(-1),  HTTP API models this with null, sdk with NONE
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

data class ObjectTypeAttributeDefaultType(
    val id: Int,
    val name: String
)
// endregion InsightObjectTypeOperator

// region InsightSchemaOperator
data class InsightSchema(
    val id: InsightSchemaId,
    val name: String,
    val objectCount: Int,
    val objectTypeCount: Int
)
// endregion InsightSchemaOperator

data class ObjectAttributeValue(
    var value: Any?,
    var displayValue: Any?,
    var referencedObject: ReferencedObject?,
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