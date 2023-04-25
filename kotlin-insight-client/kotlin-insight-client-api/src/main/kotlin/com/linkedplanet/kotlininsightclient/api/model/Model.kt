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
    val objectTypeId: Int,
    var id: Int,
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
enum class InsightObjectAttributeType(val value: Int) {
    DEFAULT(0),
    REFERENCE(1),
    UNKNOWN(-1);

    companion object {
        fun parse(value: Int) =
            values().singleOrNull { it.value == value } ?: UNKNOWN
    }
}

data class InsightReference(
    val objectTypeId: Int,
    val objectTypeName: String,
    val objectId: Int,
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
    val id: Int,
    val name: String,
    var attributes: List<ObjectTypeSchemaAttribute>,
    val parentObjectTypeId: Int?
)

data class ObjectTypeSchemaAttribute(
    val id: Int,
    val name: String,
    val defaultType: ObjectTypeAttributeDefaultType?,
    val options: String,
    val minimumCardinality: Int,
    val maximumCardinality: Int,
    val referenceType: ObjectTypeSchemaAttributeReferenceType?
)

data class ObjectTypeSchemaAttributeReferenceType(
    val id: Int, // id 3, name = "Reference"; prefer an ENUM
    val name: String
)

data class ObjectTypeAttributeDefaultType(
    val id: Int,
    val name: String
)
// endregion InsightObjectTypeOperator

// region InsightSchemaOperator
data class InsightSchema(
    val id: Int,
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
    var id: Int,
    var label: String,
    var objectKey: String,
    var objectType: ReferencedObjectType?
)

data class ReferencedObjectType(
    val id: Int,
    val name: String
)

// region InsightHistoryOperator
data class InsightHistory(
    val objectId: Int,
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
    val objectId: Int
)

data class Actor(
    val key: String
)
// endregion InsightHistoryOperator

// region InsightAttachmentOperator
data class InsightAttachment(
    val id: Int,
    val author: String,
    val mimeType: String,
    val filename: String,
    val filesize: String, // for human display e.g. 10.1 kB
    val created: String, // ISO 8601 String
    val comment: String, // we are only able to download them but can not create attachments with comments
    val url: String
)
// endregion InsightAttachmentOperator