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
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
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
    this.attributes.map { attr ->
        val values : List<Any?> = when (attr) {
            is InsightAttribute.Text -> listOf(attr.value)
            is InsightAttribute.Integer -> listOf(attr.value)
            is InsightAttribute.Bool -> listOf(attr.value.toString())
            is InsightAttribute.Time -> listOfNotNull(attr.value?.format(DateTimeFormatter.ISO_TIME))
            is InsightAttribute.Date -> listOfNotNull(attr.value?.format(DateTimeFormatter.ISO_DATE))
            is InsightAttribute.DateTime -> listOfNotNull(attr.value?.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
            is InsightAttribute.DoubleNumber -> listOf(attr.value)
            is InsightAttribute.Email -> listOf(attr.value)
            is InsightAttribute.Ipaddress -> listOf(attr.value)
            is InsightAttribute.Textarea -> listOf(attr.value)

            is InsightAttribute.Url -> attr.values
            is InsightAttribute.Select -> attr.values

            is InsightAttribute.Reference -> attr.referencedObjects.map { it.id.raw }
            is InsightAttribute.User -> attr.users.map { it.key }

            // TODO support additional attribute types
            is InsightAttribute.Group -> emptyList()
            is InsightAttribute.Project -> emptyList()
            is InsightAttribute.Status -> emptyList()
            is InsightAttribute.Version -> emptyList()
            is InsightAttribute.Confluence -> emptyList()

            is InsightAttribute.Unknown -> emptyList()
        }

        ObjectEditItemAttribute(
            attr.attributeId.raw,
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