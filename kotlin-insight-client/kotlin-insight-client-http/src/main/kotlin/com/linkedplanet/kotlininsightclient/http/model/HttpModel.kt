/*-
 * #%L
 * kotlin-insight-client-http
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

import com.linkedplanet.kotlininsightclient.api.model.InsightSchema

internal data class HttpInsightSchemaListApiResponse(
    val objectschemas: List<InsightSchema>
)

internal data class ObjectUpdateApiResponse(
    val id: Int,
    val objectKey: String
)

/**
 * See type attribute in response of https://insight-javadoc.riada.io/insight-javadoc-8.6/insight-rest/#object__id__attributes_get
 */
internal enum class InsightObjectAttributeType(val attributeTypeId: Int) {
    UNKNOWN(-1),
    DEFAULT(0),
    REFERENCE(1),
    USER(2),
    CONFLUENCE(3),
    GROUP(4),
    VERSION(5),
    PROJECT(6),
    STATUS(7);

    companion object {
        fun parse(value: Int) =
            values().singleOrNull { it.attributeTypeId == value } ?: UNKNOWN
    }
}

// if attributeType is default, this determines which kind of default type the value is
internal enum class DefaultType(var defaultTypeId: Int) {
    // NONE(-1),  // HTTP API models this with null, sdk with NONE
    TEXT(0),
    INTEGER(1),
    BOOLEAN(2),
    DOUBLE(3),
    DATE(4),
    TIME(5),
    DATE_TIME(6),
    URL(7),
    EMAIL(8),
    TEXTAREA(9),
    SELECT(10),
    IPADDRESS(11);

    companion object {
        fun parse(defaultTypeId: Int): DefaultType? =
            DefaultType.values().singleOrNull { it.defaultTypeId == defaultTypeId }
    }
}