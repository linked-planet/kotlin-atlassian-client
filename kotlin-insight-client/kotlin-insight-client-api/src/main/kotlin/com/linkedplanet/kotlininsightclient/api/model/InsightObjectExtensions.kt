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
@file:Suppress("unused")

package com.linkedplanet.kotlininsightclient.api.model

import java.time.ZonedDateTime

private fun InsightObject.createAttribute(id: InsightAttributeId, attributeType: InsightObjectAttributeType) {
    this.attributes = this.attributes + InsightAttribute(
        attributeId = id,
        attributeType = attributeType,
        value = emptyList(),
        null
    )
}

fun InsightObject.getAttribute(id: InsightAttributeId): InsightAttribute? =
    this.attributes.singleOrNull { it.attributeId == id }

fun InsightObject.getAttributeIdByName(name: String) = getAttributeByName(name)?.attributeId

fun InsightObject.getAttributeByName(name: String): InsightAttribute? =
    this.attributes.firstOrNull { it.schema?.name == name }

fun InsightObject.isReferenceAttribute(id: InsightAttributeId): Boolean = getAttribute(id)?.isReference() ?: false

fun InsightAttribute.isReference() : Boolean = attributeType == InsightObjectAttributeType.REFERENCE

fun InsightObject.isValueAttribute(id: InsightAttributeId): Boolean =
    getAttribute(id)
        ?.attributeType == InsightObjectAttributeType.DEFAULT

fun InsightObject.exists(id: InsightAttributeId): Boolean =
    getAttribute(id) != null

fun InsightObject.clearValueList(id: InsightAttributeId) {
    getAttribute(id)
        ?.value = emptyList()
}

fun InsightObject.getValueList(id: InsightAttributeId): List<Any> =
    getAttribute(id)
        ?.value
        ?.mapNotNull { it.value }
        ?: emptyList()

fun <T> InsightObject.setValueList(id: InsightAttributeId, values: List<T?>) {
    if (!exists(id)) {
        this.createAttribute(id, InsightObjectAttributeType.DEFAULT)
    }
    getAttribute(id)
        ?.value = values.map {
        ObjectAttributeValue(
            value = it,
            displayValue = "",
            referencedObject = null,
            user = null
        )
    }
}

fun <T> InsightObject.setValue(id: InsightAttributeId, value: T?) {
    if (!exists(id)) {
        this.createAttribute(id, InsightObjectAttributeType.DEFAULT)
    }
    getAttribute(id)
        ?.value = listOf(ObjectAttributeValue(
        value = value,
        displayValue = "",
        referencedObject = null,
        user = null)
    )
}

fun <T> InsightObject.removeValue(id: InsightAttributeId, value: T?) {
    getAttribute(id)
        ?.apply { this.value = this.value.filter { cur -> cur.value != value } }
}

fun InsightObject.addValue(id: InsightAttributeId, value: Any?) {
    if (!exists(id)) {
        this.createAttribute(id, InsightObjectAttributeType.DEFAULT)
    }
    getAttribute(id)
        ?.apply {
            this.value = this.value + ObjectAttributeValue(
                value = value,
                displayValue = "",
                referencedObject = null,
                user = null
            )
        }
}

fun <T> InsightObject.getValue(id: InsightAttributeId, transform: (Any) -> T): T? =
    getAttribute(id)
        ?.value
        ?.firstOrNull()
        ?.value
        ?.let { transform(it) }

fun <T> InsightObject.getValueByName(name: String, transform: (Any) -> T): T? =
    getAttributeIdByName(name)
        ?.let { getValue(it, transform) }

fun <T> InsightObject.getValueList(id: InsightAttributeId, transform: (Any) -> T): List<T?> =
    getAttribute(id)
        ?.value
        ?.map { transform(it) }
        ?: emptyList()

fun <T> InsightObject.getValueListByName(name: String, transform: (Any) -> T): List<T?> =
    getAttributeIdByName(name)
        ?.let { getValueList(it, transform) }
        ?: emptyList()

fun InsightObject.getStringValue(id: InsightAttributeId): String? =
    this.getValue(id) { it.toString() }

fun InsightObject.getIntValue(id: InsightAttributeId): Int? =
    getStringValue(id)?.toInt()

fun InsightObject.getFloatValue(id: InsightAttributeId): Float? =
    getStringValue(id)?.toFloat()

fun InsightObject.getBooleanValue(id: InsightAttributeId): Boolean? =
    getStringValue(id)?.toBoolean()

fun InsightObject.getDateTimeValue(id: InsightAttributeId): ZonedDateTime? =
    getStringValue(id)?.let { ZonedDateTime.parse(it) }

fun InsightObject.getSingleReferenceValue(id: InsightAttributeId): InsightReference? =
    getAttribute(id)
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

fun InsightObject.getMultiReferenceValue(id: InsightAttributeId): List<InsightReference> =
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

fun InsightObject.getUserList(id: InsightAttributeId): List<InsightUser> =
    this.attributes
        .firstOrNull { it.attributeId == id }
        ?.value
        ?.mapNotNull { it.user }
        ?: emptyList()

fun InsightObject.removeReference(attributeId: InsightAttributeId, referencedObjectId: InsightObjectId) {
    getAttribute(attributeId)
        ?.let {
            it.value = it.value.filter { cur -> cur.referencedObject?.id != referencedObjectId }
        }
}

fun InsightObject.clearReferenceValue(id: InsightAttributeId) {
    getAttribute(id)
        ?.value = emptyList()
}

fun InsightObject.addReference(attributeId: InsightAttributeId, referencedObjectId: InsightObjectId) {
    if (!exists(attributeId)) {
        this.createAttribute(attributeId, InsightObjectAttributeType.REFERENCE)
    }
    getAttribute(attributeId)
        ?.let {
            it.value = (it.value + ObjectAttributeValue(
                value = null,
                displayValue = null,
                referencedObject = ReferencedObject(
                    referencedObjectId,
                    "",
                    "",
                    null
                ),
                user = null
            ))
        }
}

fun InsightObject.setSingleReference(id: InsightAttributeId, referencedObjectId: InsightObjectId) {
    this.clearReferenceValue(id)
    this.addReference(id, referencedObjectId)
}