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
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectAttributeType

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
        val values = insightAttr.value.map {
            if (insightAttr.attributeType == InsightObjectAttributeType.REFERENCE) {
                ObjectEditItemAttributeValue(
                    it.referencedObject!!.id.value
                )
            } else {
                ObjectEditItemAttributeValue(
                    it.value
                )
            }
        }
        ObjectEditItemAttribute(
            insightAttr.attributeId.raw,
            values
        )
    }

internal data class ObjectEditItemAttribute(
    val objectTypeAttributeId: Int,
    val objectAttributeValues: List<ObjectEditItemAttributeValue>
)

internal data class ObjectEditItemAttributeValue(
    val value: Any?
)