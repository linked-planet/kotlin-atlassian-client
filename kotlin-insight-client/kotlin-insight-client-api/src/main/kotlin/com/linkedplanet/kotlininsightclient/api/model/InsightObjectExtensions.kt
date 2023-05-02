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
import java.util.*

private fun InsightObject.createAttribute(id: Int, name: String? = null, attributeType: InsightObjectAttributeType) {
    this.attributes = this.attributes + InsightAttribute(
        attributeId = id,
        attributeName = name,
        attributeType = attributeType,
        defaultType = null,
        options = null,
        minimumCardinality = null,
        maximumCardinality = null,
        value = Collections.emptyList()
    )
}

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
        ?.value = Collections.emptyList()
}

fun InsightObject.getValueList(id: Int): List<Any> =
    getAttribute(id)
        ?.value
        ?.mapNotNull { it.value }
        ?: Collections.emptyList()

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
        ?: Collections.emptyList()

fun <T> InsightObject.getValueListByName(name: String, transform: (Any) -> T): List<T?> =
    getAttributeIdByName(name)
        ?.let { getValueList(it, transform) }
        ?: Collections.emptyList()

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
        ?: Collections.emptyList()


fun InsightObject.removeReference(attributeId: Int, referencedObjectId: InsightObjectId) {
    getAttribute(attributeId)
        ?.let {
            it.value = it.value.filter { cur -> cur.referencedObject?.id != referencedObjectId }
        }
}

fun InsightObject.clearReferenceValue(id: Int) {
    getAttribute(id)
        ?.value = Collections.emptyList()
}

fun InsightObject.addReference(attributeId: Int, referencedObjectId: InsightObjectId) =
    addReference(attributeId, null, referencedObjectId)

fun InsightObject.addReference(attributeId: Int, name: String?, referencedObjectId: InsightObjectId) {
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

fun InsightObject.setSingleReference(id: Int, referencedObjectId: InsightObjectId) =
    setSingleReference(id, null, referencedObjectId)

fun InsightObject.setSingleReference(id: Int, name: String?, referencedObjectId: InsightObjectId) {
    this.clearReferenceValue(id)
    this.addReference(id, name, referencedObjectId)
}