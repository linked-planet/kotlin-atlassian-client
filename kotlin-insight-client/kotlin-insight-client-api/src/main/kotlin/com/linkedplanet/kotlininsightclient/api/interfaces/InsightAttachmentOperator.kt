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
package com.linkedplanet.kotlininsightclient.api.interfaces

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment

interface InsightAttachmentOperator {

    suspend fun getAttachments(objectId: Int): Either<InsightClientError, List<InsightAttachment>>

    suspend fun downloadAttachment(url: String): Either<InsightClientError, ByteArray?>

    suspend fun downloadAttachmentZip(objectId: Int): Either<InsightClientError, ByteArray>

    suspend fun uploadAttachment(
        objectId: Int,
        filename: String,
        byteArray: ByteArray,
        comment: String = ""
    ): Either<InsightClientError, List<InsightAttachment>>

    suspend fun deleteAttachment(attachmentId: Int): Either<InsightClientError, String>
}
