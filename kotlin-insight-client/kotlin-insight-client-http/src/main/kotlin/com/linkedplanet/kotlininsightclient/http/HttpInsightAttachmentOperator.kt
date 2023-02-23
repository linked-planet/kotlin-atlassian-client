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
import arrow.core.computations.either
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment
import com.linkedplanet.kotlininsightclient.http.util.toInsightClientError
import java.io.ByteArrayOutputStream
import java.net.URLConnection
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HttpInsightAttachmentOperator : InsightAttachmentOperator {

    override suspend fun getAttachments(objectId: Int): Either<InsightClientError, List<InsightAttachment>> =
        HttpInsightClientConfig.httpClient.executeRestList<InsightAttachment>(
            "GET",
            "rest/insight/1.0/attachments/object/${objectId}",
            emptyMap(),
            null,
            "application/json",
            object : TypeToken<List<InsightAttachment>>() {}.type
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }

    // TODO: Downloads not working in both
    override suspend fun downloadAttachment(url: String): Either<InsightClientError, ByteArray> =
        HttpInsightClientConfig.httpClient.executeDownload(
            "GET",
            url,
            emptyMap(),
            null,
            null
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }

    // TODO: Uploads not working in both
    override suspend fun uploadAttachment(
        objectId: Int,
        filename: String,
        byteArray: ByteArray,
        comment: String
    ): Either<InsightClientError, List<InsightAttachment>> = either {
        val mimeType = URLConnection.guessContentTypeFromName(filename)
        HttpInsightClientConfig.httpClient.executeUpload(
            "POST",
            "/rest/insight/1.0/attachments/object/${objectId}",
            emptyMap(),
            mimeType,
            filename,
            byteArray
        )
            .mapLeft { it.toInsightClientError() }
            .bind()

        getAttachments(objectId).bind()
    }

    override suspend fun deleteAttachment(attachmentId: Int): Either<InsightClientError, String> =
        HttpInsightClientConfig.httpClient.executeRestCall(
            "DELETE",
            "/rest/insight/1.0/attachments/${attachmentId}",
            emptyMap(),
            null,
            "application/json"
        )
            .map { it.body }
            .mapLeft { it.toInsightClientError() }

    override suspend fun downloadAttachmentZip(objectId: Int): Either<InsightClientError, ByteArray> = either {
        val attachments = getAttachments(objectId).bind()
        val fileMap = attachments.map { attachment ->
            val attachmentContent = downloadAttachment(attachment.url).bind()
            attachment.filename to attachmentContent
        }

        ByteArrayOutputStream().use { byteOutputStream ->
            ZipOutputStream(byteOutputStream).use { zip ->
                fileMap.forEach {
                    val zipEntry1 = ZipEntry(it.first)
                    zip.putNextEntry(zipEntry1)
                    zip.write(it.second)
                    zip.closeEntry()
                }
            }

            byteOutputStream.toByteArray()
        }
    }
}
