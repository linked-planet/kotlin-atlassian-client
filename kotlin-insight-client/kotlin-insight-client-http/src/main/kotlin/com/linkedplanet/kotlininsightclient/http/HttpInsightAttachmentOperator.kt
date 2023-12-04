/*-
 * #%L
 * kotlin-insight-client-http
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
package com.linkedplanet.kotlininsightclient.http

import arrow.core.Either
import arrow.core.raise.either
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlinatlassianclientcore.common.error.asEither
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.error.OtherNotFoundError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.model.AttachmentId
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.http.util.catchAsInsightClientError
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URLConnection
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class HttpInsightAttachmentOperator(private val context: HttpInsightClientContext) : InsightAttachmentOperator {

    override suspend fun getAttachments(objectId: InsightObjectId): Either<InsightClientError, List<InsightAttachment>> =
        context.httpClient.executeRestList<InsightAttachment>(
            "GET",
            "rest/insight/1.0/attachments/object/${objectId.raw}",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<List<InsightAttachment>>() {}.type
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }

    override suspend fun downloadAttachment(url: String): Either<InsightClientError, InputStream> =
        context.httpClient.executeDownload(
            "GET",
            url,
            emptyMap(),
            null,
            null
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }

    override suspend fun uploadAttachment(
        objectId: InsightObjectId,
        filename: String,
        inputStream: InputStream
    ): Either<InsightClientError, InsightAttachment> = either {
        val mimeType = URLConnection.guessContentTypeFromName(filename)
        context.httpClient.executeUpload(
            "POST",
            "/rest/insight/1.0/attachments/object/${objectId.raw}",
            emptyMap(),
            mimeType,
            filename,
            inputStream
        )
            .mapLeft { it.toInsightClientError() }
            .bind()

        getAttachments(objectId).bind()
            .firstOrNull { it.filename == filename }
            ?: OtherNotFoundError(
                "Attachment with Filename ($filename) for " +
                        "object (id=$objectId) was created but could not be retrieved."
            ).asEither<InsightClientError, InsightAttachment>().bind()
    }

    override suspend fun deleteAttachment(attachmentId: AttachmentId): Either<InsightClientError, Unit> =
        context.httpClient.executeRestCall(
            "DELETE",
            "/rest/insight/1.0/attachments/${attachmentId.raw}",
            emptyMap(),
            null,
            "application/json"
        )
            .map { /*to Unit */ }
            .mapLeft { it.toInsightClientError() }

    override suspend fun downloadAttachmentZip(objectId: InsightObjectId): Either<InsightClientError, InputStream> =
        either {
            val attachments = getAttachments(objectId).bind()
            val fileMap: Map<String, InputStream> = attachments.map { attachment ->
                val attachmentContent = downloadAttachment(attachment.url).bind()
                attachment.filename to attachmentContent
            }.toMap()
            zipInputStreamForMultipleInputStreams(fileMap).bind()
        }

    private fun zipInputStreamForMultipleInputStreams(
        fileMap: Map<String, InputStream>
    ): Either<InsightClientError, InputStream> =
        catchAsInsightClientError {
            val pipeInputStream = PipedInputStream()
            PipedOutputStream(pipeInputStream).use { outputStream ->
                ZipOutputStream(outputStream).use { zipOutputStream ->
                    fileMap.forEach { (filename, inputStream) ->
                        val zipEntry = ZipEntry(filename)
                        zipOutputStream.putNextEntry(zipEntry)
                        inputStream.copyTo(zipOutputStream)
                        zipOutputStream.closeEntry()
                    }
                }
            }
            pipeInputStream
        }
}
