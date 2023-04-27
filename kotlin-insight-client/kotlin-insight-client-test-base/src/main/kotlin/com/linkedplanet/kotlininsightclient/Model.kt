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

enum class SchemaObject(val id: Int) {
    ITest(1)
}

enum class InsightObject(val id: Int) {
    Company(1),
    Country(2),
    TestWithLists(4),
    SimpleObject(3),
    Many(5),
    Abstract(6)
}

enum class InsightAttribute(val attributeId: Int, val attributeName: String) {
    CompanyName(2, "Name"),
    CompanyCountry(10, "Country"),

    CountryKey(5, "Key"),
    CountryName(6, "Name"),
    CountryShortName(9, "ShortName"),

    TestWithListsItemList(21, "ItemList"),
    TestWithListsStringList(22, "StringList"),

    SimpleObjectFirstname(15, "Firstname"),
    SimpleObjectLastname(16, "Lastname"),
}

data class Company(
    val name: String,
    val country: Country?
)

data class Country(
    val name: String,
    val shortName: String,
)

data class SimpleObject(
    val name: String,
    val firstName: String,
    val lastName: String,
)

data class TestWithLists(
    val name: String,
    val itemList: List<SimpleObject>,
    val stringList: List<String>,
)