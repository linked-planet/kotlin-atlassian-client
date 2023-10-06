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
@file:Suppress("CanBeParameter", "unused") // we want clients to access the additional information

package com.linkedplanet.kotlinatlassianclientcore.common.error

import arrow.core.Either
import javax.validation.constraints.NotNull

open class AtlassianClientError(
    @field:NotNull val error: String,
    @field:NotNull val message: String,
    @field:NotNull val stacktrace: String = ""
) {
    companion object
}

fun <ERROR : AtlassianClientError, T> ERROR.asEither(): Either<ERROR, T> = Either.Left(this)
