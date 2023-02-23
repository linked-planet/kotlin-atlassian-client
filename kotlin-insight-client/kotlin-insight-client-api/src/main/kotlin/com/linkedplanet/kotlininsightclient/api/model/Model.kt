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
    val objectType: ObjectTypeSchema,
    var id: Int,
    var objectKey: String,
    var label: String,
    var attributes: List<InsightAttribute>,
    var attachmentsExist: Boolean,
    val objectSelf: String
)

fun InsightObject.getAttributeNames(): List<ObjectTypeSchemaAttribute> =
    objectType.attributes

fun InsightObject.getAttributeType(name: String): String? =
    objectType.attributes.firstOrNull { it.name == name }?.defaultType?.name

fun InsightObject.isReferenceAttribute(name: String): Boolean =
    this.attributes
        .filter { it.attributeName == name }
        .singleOrNull { it.value.any { it.referencedObject != null } }
        ?.let { true }
        ?: false

fun InsightObject.isValueAttribute(name: String): Boolean =
    this.attributes.filter { it.attributeName == name }.any { it.value.any { it.value != null } }

fun InsightObject.exists(name: String): Boolean =
    this.attributes.firstOrNull { it.attributeName == name } != null

fun InsightObject.getStringValue(name: String): String? =
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?.firstOrNull()
        ?.value
        ?.toString()

fun InsightObject.clearValueList(name: String) {
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value = emptyList()
}

fun InsightObject.getValueList(name: String): List<Any> {
    return this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?.mapNotNull { it.value }
        ?: emptyList<Any>()
}

fun InsightObject.setValueList(name: String, values: List<Any?>) {
    val attribute = this.attributes
        .firstOrNull { it.attributeName == name }
    if (attribute == null) {
        this.createAttribute(name)
    }
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value = values.map {
        ObjectAttributeValue(
            it,
            "",
            null
        )
    }
}

fun InsightObject.removeValue(name: String, value: Any?) {
    val values = this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?: emptyList()
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value = values.filter { it.value != value }
}

private fun InsightObject.createAttribute(name: String) {
    // TODO: error handling
    this.objectType.attributes
        .firstOrNull { it.name == name }
        ?.let { attribute ->
            this.attributes = this.attributes + InsightAttribute(
                attribute.id,
                attribute.name,
                emptyList()
            )
        }
}

fun InsightObject.addValue(name: String, value: Any?) {
    val exists = this.attributes
        .firstOrNull { it.attributeName == name }
    if (exists == null) {
        this.createAttribute(name)
    }
    val values = this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?: emptyList()
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value = values + ObjectAttributeValue(
        value,
        "",
        null
    )
}

fun InsightObject.setValue(name: String, value: Any?) {
    val exists = this.attributes
        .firstOrNull { it.attributeName == name }
    if (exists == null) {
        this.createAttribute(name)
    }
    val valueList = this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?: emptyList()
    if (valueList.isEmpty()) {
        this.attributes
            .firstOrNull { it.attributeName == name }
            ?.value = valueList + ObjectAttributeValue(
            value,
            "",
            null
        )
    } else {
        valueList
            .firstOrNull()
            ?.value = value
    }
}

fun InsightObject.setStringValue(name: String, value: String) {
    this.setValue(name, value)
}

fun InsightObject.getIntValue(name: String): Int? =
    getStringValue(name)
        ?.toInt()

fun InsightObject.setIntValue(name: String, value: Int?) {
    this.setValue(name, value)
}

fun InsightObject.getFloatValue(name: String): Float? =
    getStringValue(name)
        ?.toFloat()

fun InsightObject.setFloatValue(name: String, value: Float?) {
    this.setValue(name, value)
}

fun InsightObject.getBooleanValue(name: String): Boolean? =
    getStringValue(name)
        ?.toBoolean()

fun InsightObject.setBooleanValue(name: String, value: Boolean?) {
    this.setValue(name, value)
}

fun InsightObject.getDateTimeValue(name: String): ZonedDateTime? =
    getStringValue(name)
        ?.let { ZonedDateTime.parse(it) }

fun InsightObject.setDateTimeValue(name: String, value: ZonedDateTime?) {
    this.setValue(name, value.toString())
}

fun InsightObject.getSingleReference(name: String): InsightReference? =
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?.firstOrNull()
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


fun InsightObject.removeReference(name: String, objId: Int) {
    val objReferences = this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?.mapNotNull { it.referencedObject }
        ?: emptyList()
    val resultReferences = objReferences
        .filter { it.id != objId }
        .map {
            ObjectAttributeValue(
                null,
                null,
                it
            )
        }
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value = resultReferences
}

fun InsightObject.clearReferences(name: String) {
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value = emptyList()
}

fun InsightObject.addReference(name: String, objId: Int) {
    val references = this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?: emptyList()
    val added = ObjectAttributeValue(
        null,
        null,
        ReferencedObject(
            objId,
            "",
            "",
            null
        )

    )
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value = (references + added)
}

fun InsightObject.setSingleReference(name: String, objId: Int) {
    this.clearReferences(name)
    this.addReference(name, objId)
}

fun InsightObject.getMultiReference(name: String): List<InsightReference> =
    this.attributes
        .firstOrNull { it.attributeName == name }
        ?.value
        ?.mapNotNull { it.referencedObject }
        ?.map { reference ->
            InsightReference(
                reference.objectType!!.id,
                reference.objectType!!.name,
                reference.id,
                reference.objectKey,
                reference.label
            )
        }
        ?: emptyList()

fun InsightObject.toDTO(): InsightObjectDTO {
    val attributeMapping = this.attributes.map {
        it.attributeName to (this.getStringValue(it.attributeName) ?: "")
    }.toMap()
    return InsightObjectDTO(
        this.objectType,
        this.id,
        this.getStringValue("Name") ?: "",
        attributeMapping
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
    val attributeName: String,
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

data class InsightSchemas(
    val objectschemas: List<InsightSchema>
)

data class InsightSchema(
    val id: Int,
    val name: String
)

data class InsightObjectApiResponse(
    val id: Int,
    val label: String,
    val objectKey: String,
    val objectType: ObjectType,
    val attributes: List<InsightAttributeApiResponse>,
    val extendedInfo: InsightExtendedInfo
)

data class InsightExtendedInfo(
    val openIssuesExists: Boolean,
    val attachmentsExists: Boolean
)

data class InsightObjectDTO(
    val objectType: ObjectTypeSchema,
    val id: Int,
    val name: String,
    val attributes: Map<String, String>
)

data class ObjectType(
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
    val referenceObjectType: ObjectType
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

data class InsightSchemaDescription(
    val id: Int,
    val name: String,
    val objectTypes: List<InsightObjectTypeDescription>
)

data class InsightAttributeDescription(
    val id: Int,
    val name: String,
    val type: String
)

data class InsightObjectTypeDescription(
    val id: Int,
    val name: String,
    val parentObjectTypeId: Int? = null,
    val attributes: List<InsightAttributeDescription>
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
