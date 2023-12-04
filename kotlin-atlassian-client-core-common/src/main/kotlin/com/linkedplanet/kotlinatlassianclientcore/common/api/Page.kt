/*-
 * #%L
 * kotlin-atlassian-client-core-common
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
package com.linkedplanet.kotlinatlassianclientcore.common.api

import javax.validation.constraints.NotNull

open class Page<T> (
    @field:NotNull val items: List<T>,
    @field:NotNull val totalItems: Int,
    @field:NotNull val totalPages: Int,
    @field:NotNull val currentPageIndex: Int,
    @field:NotNull val pageSize: Int
) {
    constructor() : this(emptyList<T>(), 0, 0, 0, 1)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Page<*>) return false

        if (items != other.items) return false
        if (totalItems != other.totalItems) return false
        if (totalPages != other.totalPages) return false
        if (currentPageIndex != other.currentPageIndex) return false
        if (pageSize != other.pageSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = items.hashCode()
        result = 31 * result + totalItems
        result = 31 * result + totalPages
        result = 31 * result + currentPageIndex
        result = 31 * result + pageSize
        return result
    }

    override fun toString(): String {
        return "Page(items=$items, totalItems=$totalItems, totalPages=$totalPages, currentPageIndex=$currentPageIndex, pageSize=$pageSize)"
    }

}
