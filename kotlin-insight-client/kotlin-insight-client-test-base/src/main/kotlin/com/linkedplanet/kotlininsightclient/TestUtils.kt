/*-
 * #%L
 * kotlin-insight-client-test-base
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
package com.linkedplanet.kotlininsightclient

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat

/**
 * Uses hamcrest matchers, so the test result tells you which exception was thrown
 */
fun <T> Either<InsightClientError, T>.orFail(): T {
    when (this) {
        is Either.Left<InsightClientError> -> assertThat(this, equalTo(null))
        is Either.Right -> return this.value
    }
    return (this as Either<InsightClientError, T>).orNull()!!
}

fun <T> Either<InsightClientError, T>.asError(): InsightClientError {
    when (this) {
        is Either.Left<InsightClientError> -> return this.value
        is Either.Right -> assertThat(this, equalTo(InsightClientError("","")))
    }
    return InsightClientError("error is neither left nor right", "$this")
}