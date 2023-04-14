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
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.security.MessageDigest

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
            assertThat(attachments.size, equalTo(1))
            assertThat(firstAttachment.id, equalTo(1))
            assertThat(firstAttachment.mimeType, equalTo("application/pdf"))
            assertThat(firstAttachment.author, equalTo("admin"))
            assertThat(firstAttachment.comment, equalTo(""))
            assertThat(firstAttachment.filename, equalTo("TestAttachment.pdf"))
            assertThat(firstAttachment.filesize, equalTo("10.1 kB"))
            assertThat(firstAttachment.created, equalTo("2023-02-21T07:06:08.208Z"))

            val downloadContent = insightAttachmentOperator.downloadAttachment(attachments.first().url).orNull()!!
            val sha256HashIS = calculateSha256(downloadContent)
            assertThat(sha256HashIS, equalTo("fd411837a51c43670e8d7367e64f72dbbcda5016f59988547c12d067505ef75b"))
        }
        println("### END attachment_testGetAndDownloadAttachments")
    }


    private fun calculateSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it -> str + "%02x".format(it) }
}
