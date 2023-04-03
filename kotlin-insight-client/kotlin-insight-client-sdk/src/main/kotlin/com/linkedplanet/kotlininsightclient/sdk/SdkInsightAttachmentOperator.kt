/*-
 * #%L
 * kotlin-insight-client-sdk
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
package com.linkedplanet.kotlininsightclient.sdk

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment

object SdkInsightAttachmentOperator: InsightAttachmentOperator {

    override suspend fun getAttachments(objectId: Int): Either<InsightClientError, List<InsightAttachment>> {
        TODO("Not yet implemented")
    }

    override suspend fun downloadAttachment(url: String): Either<InsightClientError, ByteArray?> {
        TODO("Not yet implemented")
    }

    override suspend fun downloadAttachmentZip(objectId: Int): Either<InsightClientError, ByteArray> {
        TODO("Not yet implemented")
    }

    override suspend fun uploadAttachment(
        objectId: Int,
        filename: String,
        byteArray: ByteArray,
        comment: String
    ): Either<InsightClientError, List<InsightAttachment>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAttachment(attachmentId: Int): Either<InsightClientError, String> {
        TODO("Not yet implemented")
    }
}
