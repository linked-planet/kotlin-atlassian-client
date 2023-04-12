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

import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

interface InsightAttachmentOperatorTest {
    val insightObjectOperator: InsightObjectOperator
    val insightAttachmentOperator: InsightAttachmentOperator

    @Test
    fun testAttachments() {
        println("### START attachment_testGetAndDownloadAttachments")
        runBlocking {
            val country = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            val attachments = insightAttachmentOperator.getAttachments(country.id).orNull() ?: emptyList()
            val firstAttachment = attachments.first()
            assertEquals(1, attachments.size)
            assertEquals(1, firstAttachment.id)
            assertEquals("application/pdf", firstAttachment.mimeType)
            assertEquals("admin", firstAttachment.author)
            assertEquals("", firstAttachment.comment)
            assertEquals("TestAttachment.pdf", firstAttachment.filename)
//            assertEquals("10.1 kB", firstAttachment.filesize) //TODO: use long, so the client can decide how to format

            val downloadContent = insightAttachmentOperator.downloadAttachment(attachments.first().url).orNull()!!
            val sha256HashIS = calculateSha256(downloadContent)
            assertEquals("fd411837a51c43670e8d7367e64f72dbbcda5016f59988547c12d067505ef75b", sha256HashIS)
        }
        println("### END attachment_testGetAndDownloadAttachments")
    }


    private fun calculateSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it -> str + "%02x".format(it) }
}
