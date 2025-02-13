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

import com.linkedplanet.kotlinjiraclient.api.model.ExpandedOption.NAMES
import com.linkedplanet.kotlinjiraclient.api.model.ExpandedOption.TRANSITIONS

/**
 * Controls expansion and other parameters of the returned Jira Issue.
 *
 * The following parameters are planed for the future: fields, fieldsByKeys, properties
 */
data class IssueQueryParams(
    val expanded: List<ExpandedOption> = listOf(NAMES, TRANSITIONS)
)
