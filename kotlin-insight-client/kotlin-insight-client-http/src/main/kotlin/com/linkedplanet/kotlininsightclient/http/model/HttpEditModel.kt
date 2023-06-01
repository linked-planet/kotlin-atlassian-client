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
package com.linkedplanet.kotlininsightclient.http.model

import com.linkedplanet.kotlininsightclient.api.model.InsightObject
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue
import java.time.format.DateTimeFormatter

// this is serialized and sent to insight, so it is not part of the model we control
internal data class ObjectEditItem(
    val objectTypeId: Int,
    val attributes: List<ObjectEditItemAttribute>
)

internal fun InsightObject.toEditObjectItem() =
    ObjectEditItem(
        objectTypeId.raw,
        getEditAttributes()
    )

internal fun InsightObject.getEditAttributes(): List<ObjectEditItemAttribute> =
    this.attributes.map { insightAttr ->
        val values : List<Any?> = when (val attr = insightAttr.value) {
            is ObjectAttributeValue.Text -> listOf(attr.value)
            is ObjectAttributeValue.Integer -> listOf(attr.value)
            is ObjectAttributeValue.Bool -> listOf(attr.value.toString())
            is ObjectAttributeValue.Time -> listOfNotNull(attr.value?.format(DateTimeFormatter.ISO_TIME))
            is ObjectAttributeValue.Date -> listOfNotNull(attr.value?.format(DateTimeFormatter.ISO_DATE))
            is ObjectAttributeValue.DateTime -> listOfNotNull(attr.value?.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
            is ObjectAttributeValue.DoubleNumber -> listOf(attr.value)
            is ObjectAttributeValue.Email -> listOf(attr.value)
            is ObjectAttributeValue.Ipaddress -> listOf(attr.value)
            is ObjectAttributeValue.Textarea -> listOf(attr.value)

            is ObjectAttributeValue.Url -> attr.values
            is ObjectAttributeValue.Select -> attr.values

            is ObjectAttributeValue.Reference -> attr.referencedObjects.map { it.id.raw }
            is ObjectAttributeValue.User -> attr.users.map { it.key } //TODO: needs a test

            is ObjectAttributeValue.Group -> TODO()
            is ObjectAttributeValue.Project -> TODO()
            is ObjectAttributeValue.Status -> TODO()
            is ObjectAttributeValue.Version -> TODO()
            is ObjectAttributeValue.Confluence -> TODO()
            is ObjectAttributeValue.Unknown -> TODO()
        }

        ObjectEditItemAttribute(
            insightAttr.attributeId.raw,
            values.map { ObjectEditItemAttributeValue(it) }
        )
    }

internal data class ObjectEditItemAttribute(
    val objectTypeAttributeId: Int,
    val objectAttributeValues: List<ObjectEditItemAttributeValue>
)

internal data class ObjectEditItemAttributeValue(
    val value: Any?
)