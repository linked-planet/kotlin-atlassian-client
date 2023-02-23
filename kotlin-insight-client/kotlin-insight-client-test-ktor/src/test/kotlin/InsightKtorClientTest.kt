/*-
 * #%L
 * kotlin-insight-client-test-ktor
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
import com.linkedplanet.kotlinhttpclient.ktor.KtorHttpClient
import com.linkedplanet.kotlininsightclient.AbstractMainTest
import com.linkedplanet.kotlininsightclient.http.HttpInsightClientConfig
import com.linkedplanet.kotlininsightclient.http.HttpInsightSchemaCacheOperator
import org.junit.BeforeClass


class InsightKtorClientTest : AbstractMainTest() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            println("#### Starting setUp")
            val httpClient = KtorHttpClient(
                "http://localhost:2990",
                "admin",
                "admin"
            )
            HttpInsightClientConfig.init("http://localhost:2990", httpClient, HttpInsightSchemaCacheOperator)
        }
    }
}
