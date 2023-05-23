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
import com.linkedplanet.kotlininsightclient.api.model.AttachmentId
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId

/**
 * The InsightAttachmentOperator interface provides methods to interact with attachments belonging to an InsightObject.
 */
interface InsightAttachmentOperator {

    /**
     * Returns a list of attachments for the specified insight object.
     *
     * @param objectId The id of the insight object the attachment belongs to
     * @return Either an [InsightClientError] or a list of [InsightAttachment] objects
     */
    suspend fun getAttachments(objectId: InsightObjectId): Either<InsightClientError, List<InsightAttachment>>

    /**
     * Downloads the attachment content from the specified URL.
     *
     * @param url The URL of the attachment to download
     * @return Either an [InsightClientError] or a [ByteArray] containing the attachment data
     */
    suspend fun downloadAttachment(url: String): Either<InsightClientError, ByteArray>

    /**
     * Downloads a zip file containing all attachments for the specified insight object.
     *
     * @param objectId The id of the insight object to download attachments for
     * @return Either an [InsightClientError] or a [ByteArray] containing the zip file data
     */
    suspend fun downloadAttachmentZip(objectId: InsightObjectId): Either<InsightClientError, ByteArray>

    /**
     * Uploads an attachment to the specified insight object.
     *
     * @param objectId The id of the insight object to upload the attachment to
     * @param filename The name of the attachment file (not the path to the file, so avoid "/")
     * @param byteArray The byte array containing the attachment data
     * @return Either an [InsightClientError] or a list of [InsightAttachment] objects
     */
    suspend fun uploadAttachment(
        objectId: InsightObjectId,
        filename: String,
        byteArray: ByteArray
    ): Either<InsightClientError, InsightAttachment>

    /**
     * Deletes the specified attachment.
     *
     * @param attachmentId The id of the attachment to delete
     * @return Either an [InsightClientError] or Unit if the deletion was successful
     */
    suspend fun deleteAttachment(attachmentId: AttachmentId): Either<InsightClientError, Unit>
}
