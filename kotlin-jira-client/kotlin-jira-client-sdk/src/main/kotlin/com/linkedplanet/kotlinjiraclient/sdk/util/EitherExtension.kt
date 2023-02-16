/*-
 * #%L
 * kotlin-jira-client-api
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
package com.linkedplanet.kotlinjiraclient.sdk.util

import arrow.core.Either
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError

inline fun <B> Either.Companion.catchJiraClientError(
    error: String? = null,
    message: String? = null,
    f: () -> B
): Either<JiraClientError, B> = catch(f).mapLeft {
    JiraClientError(error ?: it.message.toString(), message ?: it.stackTraceToString())
}
