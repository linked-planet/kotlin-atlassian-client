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
import com.linkedplanet.kotlinhttpclient.error.DomainError
import com.linkedplanet.kotlininsightclient.api.InsightConfig
import com.linkedplanet.kotlininsightclient.api.interfaces.AttachmentOperatorInterface
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment
import java.io.ByteArrayOutputStream
import java.net.URLConnection
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object AttachmentOperator : AttachmentOperatorInterface {

    override suspend fun getAttachments(objectId: Int): Either<DomainError, List<InsightAttachment>> = either {
        val result: Either<DomainError, List<InsightAttachment>> =
            InsightConfig.httpClient.executeRestList<InsightAttachment>(
                "GET",
                "rest/insight/1.0/attachments/object/${objectId}",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<InsightAttachment>>() {}.type
            ).map { it.body }
        result.bind()
    }

    // TODO: Downloads not working in both
    override suspend fun downloadAttachment(url: String): Either<DomainError, ByteArray?> = either {
        val result: Either<DomainError, ByteArray?> = InsightConfig.httpClient.executeDownload(
            "GET",
            url,
            emptyMap(),
            null,
            null
        ).map { it.body }
        result.bind()
    }

    // TODO: Uploads not working in both
    override suspend fun uploadAttachment(
        objectId: Int,
        filename: String,
        byteArray: ByteArray,
        comment: String
    ): Either<DomainError, List<InsightAttachment>> = either {
        val mimeType = URLConnection.guessContentTypeFromName(filename)
        val result = InsightConfig.httpClient.executeUpload(
            "POST",
            "/rest/insight/1.0/attachments/object/${objectId}",
            emptyMap(),
            mimeType,
            filename,
            byteArray
        ).bind()
        getAttachments(objectId).bind()
    }

    override suspend fun deleteAttachment(attachmentId: Int): Either<DomainError, String> = either {
        val result = InsightConfig.httpClient.executeRestCall(
            "DELETE",
            "/rest/insight/1.0/attachments/${attachmentId}",
            emptyMap(),
            null,
            "application/json"
        ).map { it.body }.bind()
        result
    }

    override suspend fun downloadAttachmentZip(objectId: Int): Either<DomainError, ByteArray> {
        val zipContent: Either<DomainError, ByteArray> = either {
            val fileMap = getAttachments(objectId).bind().mapNotNull { attachment ->
                downloadAttachment(attachment.url).bind()?.let {
                    attachment.filename to it
                }
            }.toMap()
            ByteArrayOutputStream().use { byteOutputStream ->
                ZipOutputStream(byteOutputStream).use { zip ->
                    fileMap.forEach {
                        val zipEntry1 = ZipEntry(it.key)
                        zip.putNextEntry(zipEntry1)
                        zip.write(it.value)
                        zip.closeEntry()
                    }
                }
                byteOutputStream.toByteArray()
            }
        }
        return zipContent
    }
}
