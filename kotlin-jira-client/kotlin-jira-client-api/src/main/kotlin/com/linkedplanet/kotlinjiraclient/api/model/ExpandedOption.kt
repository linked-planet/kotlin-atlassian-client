/*-
 * #%L
 * kotlin-jira-client-api
 * %%
 * Copyright (C) 2022 - 2025 linked-planet GmbH
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
package com.linkedplanet.kotlinjiraclient.api.model

enum class ExpandedOption(val key: String) {
    VERSIONED_REPRESENTATIONS("versionedRepresentations"),
    RENDERED_FIELDS("renderedFields"),
    NAMES("names"),
    SCHEMA("schema"),
    TRANSITIONS("transitions"),
    OPERATIONS("operations"),
    EDITMETA("editmeta"),
    CHANGELOG("changelog");

    companion object {
        fun fromKey(key: String): ExpandedOption? {
            return values().find { it.key == key }
        }
    }

    override fun toString(): String {
        return this.key
    }
}
