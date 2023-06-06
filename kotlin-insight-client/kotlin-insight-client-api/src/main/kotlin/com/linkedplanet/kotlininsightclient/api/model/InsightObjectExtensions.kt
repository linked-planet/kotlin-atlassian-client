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

import java.time.LocalDate
import java.time.ZonedDateTime

fun InsightObject.getAttribute(id: InsightAttributeId): InsightAttribute? =
    this.attributes.singleOrNull { it.attributeId == id }

inline fun <reified T : InsightAttribute> InsightObject.getAttributeAs(id: InsightAttributeId): T? =
    getAttribute(id) as? T

inline fun <reified T : InsightAttribute> InsightObject.getAttributeByNameAs(name: String): T? =
    getAttributeByName(name) as? T

fun InsightObject.getAttributeIdByName(name: String) = getAttributeByName(name)?.attributeId

fun InsightObject.getAttributeByName(name: String): InsightAttribute? =
    this.attributes.firstOrNull { it.schema?.name == name }

fun InsightObject.isReferenceAttribute(id: InsightAttributeId): Boolean =
    getAttribute(id)?.isReference() ?: false

fun InsightObject.isValueAttribute(id: InsightAttributeId): Boolean =
    getAttribute(id)?.isValueAttribute() ?: false

fun InsightObject.exists(id: InsightAttributeId): Boolean =
    getAttribute(id) != null

fun InsightObject.setUrlValues(attributeId: InsightAttributeId, values: List<String>) {
    this.attributes = attributes
        .filter { it.attributeId != attributeId } +
            InsightAttribute.Url(attributeId, values, null)
}

fun InsightObject.getUrlValues(id: InsightAttributeId): List<String> =
    getAttributeAs<InsightAttribute.Url>(id)?.values ?: emptyList()

// region ObjectAttributeValue.Select
fun InsightObject.getSelectValues(id: InsightAttributeId): List<String> =
    getAttributeAs<InsightAttribute.Select>(id)?.values ?: emptyList()

fun InsightObject.setSelectValues(attributeId: InsightAttributeId, values: List<String>) {
    this.attributes = attributes
        .filter { it.attributeId != attributeId } +
            InsightAttribute.Select(attributeId, values, null)
}

fun InsightObject.removeSelectValue(id: InsightAttributeId, value: String) {
    val modifiedList = getSelectValues(id).filter { it != value }
    setSelectValues(id, modifiedList)
}

fun InsightObject.addSelectValue(id: InsightAttributeId, value: String) {
    val modifiedList = getSelectValues(id) + value
    setSelectValues(id, modifiedList)
}

fun InsightObject.clearSelectValues(id: InsightAttributeId) =
    setSelectValues(id, emptyList())
// endregion ObjectAttributeValue.Select


// region setters
fun InsightObject.setValue(id: InsightAttributeId, value: InsightAttribute) {
    this.attributes = attributes
        .filter { it.attributeId != id } +
            value
}

fun InsightObject.setValue(id: InsightAttributeId, value: String?) {
    setValue(id, InsightAttribute.Text(id, value, null))
}

fun InsightObject.setValue(id: InsightAttributeId, value: Int?) {
    setValue(id, InsightAttribute.Integer(id, value, null))
}

fun InsightObject.setValue(id: InsightAttributeId, value: Boolean?) {
    setValue(id, InsightAttribute.Bool(id, value, null))
}

fun InsightObject.setValue(id: InsightAttributeId, value: Double?) {
    setValue(id, InsightAttribute.DoubleNumber(id, value, null))
}

fun InsightObject.setValue(id: InsightAttributeId, value: LocalDate?) {
    setValue(id, InsightAttribute.Date(id, value, null, null))
}

fun InsightObject.setValue(id: InsightAttributeId, value: ZonedDateTime?) {
    setValue(id, InsightAttribute.DateTime(id, value, null, null))
}
// endregion setters


// region getters
fun <T> InsightObject.getValue(id: InsightAttributeId, transform: (InsightAttribute) -> T): T? =
    getAttribute(id)
        ?.let { transform(it) }

fun <T> InsightObject.getValueByName(name: String, transform: (InsightAttribute) -> T): T? =
    getAttributeIdByName(name)
        ?.let { getValue(it, transform) }

fun InsightObject.getStringValue(id: InsightAttributeId): String? =
    this.getValue(id) { it.toString() }
fun InsightObject.getIntValue(id: InsightAttributeId): Int? =
    getAttributeAs<InsightAttribute.Integer>(id)?.value
fun InsightObject.getDoubleValue(id: InsightAttributeId): Double? =
    getAttributeAs<InsightAttribute.DoubleNumber>(id)?.value
fun InsightObject.getBooleanValue(id: InsightAttributeId): Boolean? =
    getAttributeAs<InsightAttribute.Bool>(id)?.value
fun InsightObject.getDateValue(id: InsightAttributeId): LocalDate? =
    getAttributeAs<InsightAttribute.Date>(id)?.value
fun InsightObject.getDateTimeValue(id: InsightAttributeId): ZonedDateTime? =
    getAttributeAs<InsightAttribute.DateTime>(id)?.value
//endregion getters


//region ObjectAttributeValue.User
fun InsightObject.getUserList(id: InsightAttributeId): List<InsightUser> =
    getAttributeAs<InsightAttribute.User>(id)?.users ?: emptyList()
// endregion user


// region ObjectAttributeValue.Reference
fun InsightObject.getSingleReferenceValue(id: InsightAttributeId): InsightReference? =
    getAttribute(id)
        ?.let { it as? InsightAttribute.Reference }
        ?.referencedObjects
        ?.firstOrNull()
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
        ?.let { it as? InsightAttribute.Reference }
        ?.referencedObjects
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

fun InsightObject.removeReference(attributeId: InsightAttributeId, referencedObjectId: InsightObjectId) {
    val existingList = getAttributeAs<InsightAttribute.Reference>(attributeId)?.referencedObjects ?: emptyList()
    val referenceAttributeList = existingList.filter { it.id != referencedObjectId }
    this.attributes = attributes
        .filter { it.attributeId != attributeId } +
            InsightAttribute.Reference(attributeId, referenceAttributeList, null)
}

fun InsightObject.clearReferenceValue(attributeId: InsightAttributeId) {
    this.attributes = attributes
        .filter { it.attributeId != attributeId } +
            InsightAttribute.Reference(attributeId, emptyList(), null)
}

fun InsightObject.addReference(attributeId: InsightAttributeId, referencedObjectId: InsightObjectId) {
    val existingList = getAttributeAs<InsightAttribute.Reference>(attributeId)?.referencedObjects ?: emptyList()
    val referenceAttributeList = existingList + ReferencedObject(referencedObjectId, "", "", null)
    this.attributes = attributes
        .filter { it.attributeId != attributeId } +
            InsightAttribute.Reference(attributeId, referenceAttributeList, null)
}

fun InsightObject.setSingleReference(id: InsightAttributeId, referencedObjectId: InsightObjectId) {
    this.clearReferenceValue(id)
    this.addReference(id, referencedObjectId)
}
// endregion