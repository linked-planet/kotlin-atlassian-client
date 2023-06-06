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
import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.OtherInsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat

/**
 * Uses hamcrest matchers, so the test result tells you which exception was thrown
 */
fun <T> Either<InsightClientError, T>.orFail(): T {
    when (this) {
        is Either.Left<InsightClientError> -> assertThat(this.value.message+" error:"+this.value.error, equalTo("No Error at all!"))
        is Either.Right -> return this.value
    }
    return (this as Either<InsightClientError, T>).orNull()!!
}

fun <T> Either<InsightClientError, T>.asError(): InsightClientError = when (this) {
    is Either.Left<InsightClientError> -> this.value
    is Either.Right ->{
        assertThat(this, equalTo(OtherInsightClientError("","")))
        OtherInsightClientError("unreachable code!", "$this")
    }
}

suspend fun InsightObjectOperator.makeSureObjectWithNameDoesNotExist(objectTypeId: InsightObjectTypeId, name: String) {
    getObjectsByIQL(objectTypeId, "Name = \"$name\"", toDomain = ::identity).orFail().objects.forEach {
        deleteObject(it.id)
    }
    assertThat(getObjectByName(objectTypeId, name, toDomain = ::identity).orFail(), equalTo(null))
    //if the former assertion failed that means deleteObject is not working, so this attachment test fails too
}

suspend fun autoClean(clean: suspend () -> Unit, testCode: suspend () -> Unit){
    try {
        clean()
        testCode()
    } finally {
        clean()
    }
}