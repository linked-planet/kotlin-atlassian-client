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

fun InsightObject.getAttribute(id: Int): InsightAttribute? =
    this.attributes.singleOrNull { it.attributeId == id }

fun InsightObject.getAttributeIdByName(name: String) = getAttributeByName(name)?.attributeId

fun InsightObject.getAttributeByName(name: String): InsightAttribute? =
    this.attributes.firstOrNull { it.attributeName == name }

fun InsightObject.isReferenceAttribute(id: Int): Boolean = getAttribute(id)?.isReference() ?: false

fun InsightAttribute.isReference() : Boolean = attributeType == InsightObjectAttributeType.REFERENCE

fun InsightObject.isValueAttribute(id: Int): Boolean =
    getAttribute(id)
        ?.attributeType == InsightObjectAttributeType.DEFAULT

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

fun <T> InsightObject.setValueList(id: Int, values: List<T?>) = setValueList(id, null, values)

fun <T> InsightObject.setValueList(id: Int, name: String? = null, values: List<T?>) {
    if (!exists(id)) {
        this.createAttribute(id, name, InsightObjectAttributeType.DEFAULT)
    }
    getAttribute(id)
        ?.value = values.map {
        ObjectAttributeValue(it, "", null)
    }
}

fun <T> InsightObject.setValue(id: Int, value: T?) = setValue(id, null, value)

fun <T> InsightObject.setValue(id: Int, name: String? = null, value: T?) {
    if (!exists(id)) {
        this.createAttribute(id, name, InsightObjectAttributeType.DEFAULT)
    }
    getAttribute(id)
        ?.value = listOf(ObjectAttributeValue(value, "", null))
}

fun <T> InsightObject.removeValue(id: Int, value: T?) {
    getAttribute(id)
        ?.apply { this.value = this.value.filter { cur -> cur.value != value } }
}

fun InsightObject.addValue(id: Int, value: Any?) = addValue(id, null, value)

fun InsightObject.addValue(id: Int, name: String? = null, value: Any?) {
    if (!exists(id)) {
        this.createAttribute(id, name, InsightObjectAttributeType.DEFAULT)
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

fun <T> InsightObject.getValueByName(name: String, transform: (Any) -> T): T? =
    getAttributeIdByName(name)
        ?.let { getValue(it, transform) }

fun <T> InsightObject.getValueList(id: Int, transform: (Any) -> T): List<T?> =
    getAttribute(id)
        ?.value
        ?.map { transform(it) }
        ?: emptyList()

fun <T> InsightObject.getValueListByName(name: String, transform: (Any) -> T): List<T?> =
    getAttributeIdByName(name)
        ?.let { getValueList(it, transform) }
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

fun InsightObject.addReference(attributeId: Int, referencedObjectId: Int) =
    addReference(attributeId, null, referencedObjectId)

fun InsightObject.addReference(attributeId: Int, name: String?, referencedObjectId: Int) {
    if (!exists(attributeId)) {
        this.createAttribute(attributeId, name, InsightObjectAttributeType.REFERENCE)
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

fun InsightObject.setSingleReference(id: Int, referencedObjectId: Int) =
    setSingleReference(id, null, referencedObjectId)

fun InsightObject.setSingleReference(id: Int, name: String?, referencedObjectId: Int) {
    this.clearReferenceValue(id)
    this.addReference(id, name, referencedObjectId)
}

fun InsightObject.toEditObjectItem() =
    ObjectEditItem(
        objectTypeId,
        getEditAttributes()
    )

fun InsightObject.getEditAttributes(): List<ObjectEditItemAttribute> =
    this.attributes.map { insightAttr ->
        val values = insightAttr.value.map {
            if (insightAttr.attributeType == InsightObjectAttributeType.REFERENCE) {
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

private fun InsightObject.createAttribute(id: Int, name: String? = null, attributeType: InsightObjectAttributeType) {
    this.attributes = this.attributes + InsightAttribute(
        id,
        name,
        attributeType,
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
    val attributeName: String?,
    val attributeType: InsightObjectAttributeType,
    var value: List<ObjectAttributeValue>
)

data class ObjectTypeSchema(
    val id: Int,
    val name: String,
    var attributes: List<ObjectTypeSchemaAttribute>,
    val parentObjectTypeId: Int?
)

// TODO:hgthis is sent as gson to the insight api, so this is not really our model object but a DTO
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
    val referenceObjectType: InsightMetaObjectType,
    val type: Int
)

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

data class InsightHistory(
    val objectId: Int,
    val historyItems: List<InsightHistoryItem>
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
    val filesize: String, // for human display e.g. 10.1 kB
    val created: String, // ISO 8601 String
    val comment: String,
    val url: String
)
