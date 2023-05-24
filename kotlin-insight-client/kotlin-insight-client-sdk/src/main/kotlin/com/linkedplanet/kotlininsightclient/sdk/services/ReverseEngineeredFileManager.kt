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
package com.linkedplanet.kotlininsightclient.sdk.services

import com.atlassian.jira.component.ComponentAccessor.getComponent
import com.atlassian.jira.config.util.AttachmentPathManager
import com.atlassian.jira.issue.AttachmentManager
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Partial Kotlin Version of io.riada.insight.persistence.FileManager
 *
 * The public API offers ways to upload an attachment File, but there is no way to download the File.
 * The original File is not accessible at all by our projects class loader.
 */
internal class ReverseEngineeredFileManager{

    private val attachmentManager by lazy { getComponent(AttachmentManager::class.java) }
    private val attachmentPathManager by lazy { getComponent(AttachmentPathManager::class.java) }

    @Throws(FileNotFoundException::class)
    fun getObjectAttachmentContent(objectId: Int, attachmentFileName: String): InputStream {
        return getInputStream(this.getObjectAttachmentDirectory(objectId), attachmentFileName)
    }

    @Throws(FileNotFoundException::class)
    private fun getInputStream(directory: File, fileName: String): InputStream {
        return FileInputStream(File(directory, fileName))
    }

    private fun getObjectAttachmentDirectory(objectId: Int): File {
        return this.getDirectoryName("object/$objectId")
    }

    private fun getDirectoryName(objectDirectoryName: String): File {
        val applicationDirectory = File(getAttachmentPath(), "insight")
        return this.getDirectoryOrCreateIfNotExist(applicationDirectory, objectDirectoryName)
    }

    private fun getDirectoryOrCreateIfNotExist(applicationDirectory: File, objectDirectoryName: String): File {
        val returnDir = File(applicationDirectory, objectDirectoryName)
        if (!returnDir.exists()) {
            returnDir.mkdirs()
        }
        return returnDir
    }

    private fun getAttachmentPath(): String =
        if (attachmentManager.attachmentsEnabled()) {
            attachmentPathManager.attachmentPath
        } else {
            attachmentPathManager.defaultAttachmentPath
        }

}