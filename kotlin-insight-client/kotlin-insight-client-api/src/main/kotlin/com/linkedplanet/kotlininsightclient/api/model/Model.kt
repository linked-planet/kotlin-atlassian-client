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

data class InsightObjects(
    val searchResult: Int = -1,
    val objects: List<InsightObject> = emptyList()
)

fun InsightObjects.plus(insightObjects: InsightObjects): InsightObjects =
    InsightObjects(
        this.searchResult + insightObjects.searchResult,
        this.objects.plus(insightObjects.objects)
    )

data class InsightObject(
    val objectTypeId: Int,
    var id: Int,
    var objectKey: String,
    var label: String,
    var attributes: List<InsightAttribute>,
    var attachmentsExist: Boolean,
    val objectSelf: String
)

fun InsightObject.getAttribute(id: Int): InsightAttribute? =
    this.attributes.singleOrNull { it.attributeId == id }

fun InsightObject.isReferenceAttribute(id: Int): Boolean =
    getAttribute(id)
        ?.let { it.value.any { value -> value.referencedObject != null } }
        ?: false

fun InsightObject.isValueAttribute(id: Int): Boolean =
    getAttribute(id)
        ?.let { it.value.any { value -> value.value != null } }
        ?: false

fun InsightObject.exists(id: Int): Boolean =
    getAttribute(id) != null

fun InsightObject.clearValueList(id: Int) {
    getAttribute(id)
        ?.value = emptyList()
}

fun InsightObject.getValueList(id: Int): List<Any> =
    getAttribute(id)
        ?.value
        ?.mapNotNull { it.value }
        ?: emptyList()

fun <T> InsightObject.setValueList(id: Int, values: List<T?>) {
    if (!exists(id)) {
        this.createAttribute(id)
    }
    getAttribute(id)
        ?.value = values.map {
        ObjectAttributeValue(it, "", null)
    }
}

fun <T> InsightObject.setValue(id: Int, value: T?) {
    if (!exists(id)) {
        this.createAttribute(id)
    }
    getAttribute(id)
        ?.value = listOf(ObjectAttributeValue(value, "", null))
}

fun <T> InsightObject.removeValue(id: Int, value: T?) {
    getAttribute(id)
        ?.apply { this.value = this.value.filter { cur -> cur.value != value } }
}

fun InsightObject.addValue(id: Int, value: Any?) {
    if (!exists(id)) {
        this.createAttribute(id)
    }
    getAttribute(id)
        ?.apply {
            this.value = this.value + ObjectAttributeValue(value, "", null)
        }
}

fun <T> InsightObject.getValue(id: Int, transform: (Any) -> T): T? =
    getAttribute(id)
        ?.value
        ?.single()
        ?.value
        ?.let { transform(it) }

fun <T> InsightObject.getValueList(id: Int, transform: (Any) -> T): List<T?> =
    getAttribute(id)
        ?.value
        ?.map { transform(it) }
        ?: emptyList()

fun InsightObject.getStringValue(id: Int): String? =
    this.getValue(id) { it.toString() }

fun InsightObject.getIntValue(id: Int): Int? =
    getStringValue(id)?.toInt()

fun InsightObject.getFloatValue(id: Int): Float? =
    getStringValue(id)?.toFloat()

fun InsightObject.getBooleanValue(id: Int): Boolean? =
    getStringValue(id)?.toBoolean()

fun InsightObject.getDateTimeValue(id: Int): ZonedDateTime? =
    getStringValue(id)?.let { ZonedDateTime.parse(it) }

fun InsightObject.getSingleReferenceValue(id: Int): InsightReference? =
    getAttribute(id)
        ?.value
        ?.single()
        ?.referencedObject
        ?.let {
            InsightReference(
                it.objectType!!.id,
                it.objectType!!.name,
                it.id,
                it.objectKey,
                it.label
            )
        }

fun InsightObject.getMultiReferenceValue(id: Int): List<InsightReference> =
    getAttribute(id)
        ?.value
        ?.mapNotNull { it.referencedObject }
        ?.map {
            InsightReference(
                it.objectType!!.id,
                it.objectType!!.name,
                it.id,
                it.objectKey,
                it.label
            )
        }
        ?: emptyList()


fun InsightObject.removeReference(attributeId: Int, referencedObjectId: Int) {
    getAttribute(attributeId)
        ?.let {
            it.value = it.value.filter { cur -> cur.referencedObject?.id != referencedObjectId }
        }
}

fun InsightObject.clearReferenceValue(id: Int) {
    getAttribute(id)
        ?.value = emptyList()
}

fun InsightObject.addReference(attributeId: Int, referencedObjectId: Int) {
    if (!exists(attributeId)) {
        this.createAttribute(attributeId)
    }
    getAttribute(attributeId)
        ?.let {
            it.value = (it.value + ObjectAttributeValue(
                null,
                null,
                ReferencedObject(
                    referencedObjectId,
                    "",
                    "",
                    null
                )

            ))
        }
}

fun InsightObject.setSingleReference(id: Int, referencedObjectId: Int) {
    this.clearReferenceValue(id)
    this.addReference(id, referencedObjectId)
}

fun InsightObject.getEditAttributes() =
    this.attributes.map { insightAttr ->
        val values = insightAttr.value.map {
            if (it.referencedObject != null) {
                ObjectEditItemAttributeValue(
                    it.referencedObject!!.id
                )
            } else {
                ObjectEditItemAttributeValue(
                    it.value
                )
            }
        }
        ObjectEditItemAttribute(
            insightAttr.attributeId,
            values
        )
    }

private fun InsightObject.createAttribute(id: Int) {
    this.attributes = this.attributes + InsightAttribute(
        id,
        emptyList()
    )
}

data class InsightReference(
    val objectTypeId: Int,
    val objectTypeName: String,
    val objectId: Int,
    val objectKey: String,
    val objectName: String
)

data class InsightAttribute(
    val attributeId: Int,
    var value: List<ObjectAttributeValue>
)

data class ObjectTypeSchema(
    val id: Int,
    val name: String,
    var attributes: List<ObjectTypeSchemaAttribute>,
    val parentObjectTypeId: Int?
)

data class ObjectEditItem(
    val objectTypeId: Int,
    val attributes: List<ObjectEditItemAttribute>
)

data class ObjectEditItemAttribute(
    val objectTypeAttributeId: Int,
    val objectAttributeValues: List<ObjectEditItemAttributeValue>
)

data class ObjectEditItemAttributeValue(
    val value: Any?
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
    val id: Int,
    val name: String
)

data class ObjectTypeAttributeDefaultType(
    val id: Int,
    val name: String
)

data class InsightObjectEntries(
    val totalFilterCount: Int,
    val objectEntries: List<InsightObjectApiResponse>
)

data class InsightSchema(
    val id: Int,
    val name: String,
    val objectCount: Int,
    val objectTypeCount: Int
)

data class InsightObjectApiResponse(
    val id: Int,
    val label: String,
    val objectKey: String,
    val objectType: InsightMetaObjectType,
    val attributes: List<InsightAttributeApiResponse>,
    val extendedInfo: InsightExtendedInfo
)

data class InsightExtendedInfo(
    val openIssuesExists: Boolean,
    val attachmentsExists: Boolean
)

data class InsightMetaObjectType(
    val id: Int,
    val name: String,
    val objectSchemaId: Int
)


data class InsightAttributeApiResponse(
    val id: Int,
    val objectTypeAttribute: ObjectTypeAttribute?,
    val objectTypeAttributeId: Int,
    val objectId: Int,
    val objectAttributeValues: List<ObjectAttributeValue>
)

data class ObjectTypeAttribute(
    val id: Int,
    val name: String,
    val referenceObjectTypeId: Int,
    val referenceObjectType: InsightMetaObjectType
)

data class ObjectAttributeValue(
    var value: Any?,
    var displayValue: Any?,
    var referencedObject: ReferencedObject?
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

data class InsightHistoryItem(
    val id: Int,
    val affectedAttribute: String,
    val newValue: String,
    val actor: Actor,
    val type: Int,
    val created: String,
    val updated: String,
    val objectId: Int
)

data class Actor(
    val name: String
)

data class ObjectUpdateResponse(
    val id: Int,
    val objectKey: String
)

data class InsightAttachment(
    val id: Int,
    val author: String,
    val mimeType: String,
    val filename: String,
    val filesize: String,
    val created: String,
    val comment: String,
    val commentOutput: String,
    val url: String
)
