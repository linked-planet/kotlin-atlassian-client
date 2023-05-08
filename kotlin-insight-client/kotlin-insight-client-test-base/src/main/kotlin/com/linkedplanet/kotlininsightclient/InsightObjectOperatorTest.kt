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

import arrow.core.identity
import com.linkedplanet.kotlininsightclient.api.experimental.GenericInsightObjectOperatorImpl
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.addValue
import com.linkedplanet.kotlininsightclient.api.model.getAttributeByName
import com.linkedplanet.kotlininsightclient.api.model.getAttributeIdByName
import com.linkedplanet.kotlininsightclient.api.model.getMultiReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getSingleReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getStringValue
import com.linkedplanet.kotlininsightclient.api.model.getUserList
import com.linkedplanet.kotlininsightclient.api.model.getValueList
import com.linkedplanet.kotlininsightclient.api.model.removeValue
import com.linkedplanet.kotlininsightclient.api.model.setSingleReference
import com.linkedplanet.kotlininsightclient.api.model.setValue
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.junit.Test

interface InsightObjectOperatorTest {
    val insightObjectOperator: InsightObjectOperator
    val insightObjectTypeOperator: InsightObjectTypeOperator
    val insightSchemaOperator: InsightSchemaOperator

    @Test
    fun testGenericInsightObjectOperatorCrud() = runBlocking {
        val countryOperator =
            GenericInsightObjectOperatorImpl(Country::class,
                insightObjectForDomainObject = { objectTypeId, domainObject: Country ->
                    insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity)
                }
            )
        val companyOperator =
            GenericInsightObjectOperatorImpl(Company::class,
                insightObjectForDomainObject = { objectTypeId, domainObject: Company ->
                    insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity)
                },
                referenceAttributeToValue = { insightAttribute ->
                    val movie = insightAttribute.value.first().referencedObject!!.id
                    val eitherMovie = countryOperator.getById(movie)
                    eitherMovie.orNull()!!
                },
                attributeToReferencedObjectId = { schema: ObjectTypeSchemaAttribute, obj: Any? ->
                    val country = obj as Country
                    listOfNotNull(
                        insightObjectOperator.getObjectByName(schema.referenceObjectTypeId!!, country.name, ::identity)
                            .orNull()?.id
                    )
                }
            )

        val country = Country(
            name = "United States of America",
            shortName = "USA"
        )
        val company = Company(
            name = "Boring Company",
            country = country,
        )

        companyOperator.delete(company).orFail()
        countryOperator.delete(country).orFail()
        try {
            countryOperator.create(country).orFail()
            val countryByName = countryOperator.getByName(country.name).orFail()
            assertThat(countryByName, equalTo(country))
            companyOperator.create(company).orFail()
            val companyByName = companyOperator.getByName(company.name).orFail()
            assertThat(companyByName, equalTo(company))
        } finally {
            companyOperator.delete(company).orFail()
            countryOperator.delete(country).orFail()
        }
    }

    @Test
    fun testGenericInsightObjectOperatorCrudWithListAttribute() = runBlocking {
        println("### START object_testGenericInsightObjectOperatorCrudWithListAttribute")

        val simpleObjectOperator = GenericInsightObjectOperatorImpl(SimpleObject::class,
            insightObjectForDomainObject = { objectTypeId, domainObject: SimpleObject ->
                insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity)
            }
        )
        val testWithListsOperator = GenericInsightObjectOperatorImpl(TestWithLists::class,
            insightObjectForDomainObject = { objectTypeId, domainObject: TestWithLists ->
                insightObjectOperator.getObjectByName(objectTypeId, domainObject.name, ::identity)
            },
            referenceAttributeToValue = { insightAttribute ->
                val listOfObjects = insightAttribute.value
                    .mapNotNull { it.referencedObject?.id }
                    .mapNotNull { id -> simpleObjectOperator.getById(id).orNull() }
                listOfObjects
            },
            attributeToReferencedObjectId = { schema: ObjectTypeSchemaAttribute, domainObjects ->
                (domainObjects as List<*>)
                    .mapNotNull { it as? SimpleObject }
                    .mapNotNull {
                        insightObjectOperator.getObjectByName(schema.referenceObjectTypeId!!, it.name, ::identity).orNull()?.id
                    }
            }
        )

        val simpleObjects = simpleObjectOperator.getByIQL("Name in (Object2, Object3)").orFail()
        val objWithLists = TestWithLists(
            name = "CreatedByIntegrationTest",
            itemList = simpleObjects,
            stringList = listOf("A", "B", "C")
        )

        testWithListsOperator.delete(objWithLists).orFail()
        try {
            testWithListsOperator.create(objWithLists).orFail()
            val byName = testWithListsOperator.getByName(objWithLists.name).orFail()
            assertThat(byName, equalTo(objWithLists))
        } finally {
            testWithListsOperator.delete(objWithLists).orFail()
        }
    }

    @Test
    fun testObjectListWithFlatReference() {
        println("### START object_testObjectListWithFlatReference")
        val companies = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Company.id, toDomain = ::identity).orNull()!!.objects
        }
        assertThat(companies.size, equalTo(2))

        val firstCompany = companies.firstOrNull { it.id == InsightObjectId(1) }
        assertThat(firstCompany, notNullValue())
        assertThat(firstCompany!!.id, equalTo(InsightObjectId(1)))
        assertThat(firstCompany.objectKey, equalTo("IT-1"))
        assertThat(firstCompany.label, equalTo("Test GmbH"))

        // Name
        assertThat(firstCompany.getAttributeByName(TestAttributes.CompanyName.attributeName), notNullValue())
        assertThat(
            firstCompany.getAttributeIdByName(TestAttributes.CompanyName.attributeName),
            equalTo(TestAttributes.CompanyName.attributeId)
        )
        assertThat(firstCompany.getStringValue(TestAttributes.CompanyName.attributeId), equalTo("Test GmbH"))

        // Country
        assertThat(firstCompany.getAttributeByName(TestAttributes.CompanyCountry.attributeName), notNullValue())
        assertThat(
            firstCompany.getAttributeIdByName(TestAttributes.CompanyCountry.attributeName),
            equalTo(TestAttributes.CompanyCountry.attributeId)
        )
        assertThat(
            firstCompany.getSingleReferenceValue(TestAttributes.CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertThat(firstCompany.attachmentsExist, equalTo(false))

        val secondCompany = companies.firstOrNull { it.id == InsightObjectId(2) }
        assertThat(secondCompany, notNullValue())
        assertThat(secondCompany!!.id, equalTo(InsightObjectId(2)))
        assertThat(secondCompany.objectKey, equalTo("IT-2"))
        assertThat(secondCompany.label, equalTo("Test AG"))

        // Name
        assertThat(secondCompany.getAttributeByName(TestAttributes.CompanyName.attributeName), notNullValue())
        assertThat(
            secondCompany.getAttributeIdByName(TestAttributes.CompanyName.attributeName),
            equalTo(TestAttributes.CompanyName.attributeId)
        )
        assertThat(secondCompany.getStringValue(TestAttributes.CompanyName.attributeId), equalTo("Test AG"))

        // Country
        assertThat(secondCompany.getAttributeByName(TestAttributes.CompanyCountry.attributeName), notNullValue())
        assertThat(
            secondCompany.getAttributeIdByName(TestAttributes.CompanyCountry.attributeName),
            equalTo(TestAttributes.CompanyCountry.attributeId)
        )
        assertThat(
            secondCompany.getSingleReferenceValue(TestAttributes.CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertThat(secondCompany.attachmentsExist, equalTo(true))

        println("### END object_testObjectListWithFlatReference")
    }

    @Test
    fun testObjectListWithResolvedReference() {
        println("### START object_testObjectListWithResolvedReference")
        val companies = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Company.id, toDomain = ::identity).orNull()?.objects
        }
        assertThat(companies, notNullValue())
        assertThat(companies!!.size, equalTo(2))

        val company = companies.firstOrNull { it.id == InsightObjectId(1) }
        assertThat(company, notNullValue())

        val country = runBlocking {
            insightObjectOperator.getObjectById(
                company!!.getSingleReferenceValue(TestAttributes.CompanyCountry.attributeId)!!.objectId,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(country.getStringValue(TestAttributes.CountryName.attributeId), equalTo("Germany"))
        assertThat(country.getStringValue(TestAttributes.CountryShortName.attributeId), equalTo("DE"))
        println("### END object_testObjectListWithResolvedReference")
    }

    @Test
    fun testObjectById() {
        println("### START object_testObjectById")
        val company = runBlocking {
            insightObjectOperator.getObjectById(InsightObjectId(1), toDomain = ::identity).orFail()!!
        }
        assertThat(company.id, equalTo(InsightObjectId(1)))
        assertThat(company.objectKey, equalTo("IT-1"))
        assertThat(company.label, equalTo("Test GmbH"))
        assertThat(company.getStringValue(TestAttributes.CompanyName.attributeId), equalTo("Test GmbH"))
        assertThat(
            company.getSingleReferenceValue(TestAttributes.CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertThat(company.objectTypeName, equalTo("Company"))
        assertThat(company.objectTypeId, equalTo(InsightObjectTypeId(1)))
        assertThat(company.objectSelf, endsWith("secure/insight/assets/IT-1"))
        assertThat(company.objectSelf, startsWith("http"))

        assertThat(company.attachmentsExist, equalTo(false))
        println("### END object_testObjectById")
    }

    @Test
    fun testGetObjectsByObjectTypeName() = runBlocking {
        println("### START object_testGetObjecsByObjectTypeName")
        val objs = insightObjectOperator.getObjectsByObjectTypeName("Country", toDomain = ::identity).orFail()
        val allCountryNames = objs.map { it.getStringValue(TestAttributes.CountryName.attributeId) }
        assertThat(allCountryNames, Matchers.hasItem("Germany")) // among other items
    }

    @Test
    fun testObjectWithListAttributes() {
        println("### START object_testObjectWithListAttributes")
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()

        val references = obj.getMultiReferenceValue(TestAttributes.TestWithListsItemList.attributeId)
        val idList = references.map { it.objectId }
        val nameList = references.map { it.objectName }
        val refList = references.map { insightReference ->
            runBlocking {
                insightObjectOperator.getObjectById(insightReference.objectId, toDomain = ::identity).orNull()!!
            }
        }
        val firstNameList = refList.map { it.getStringValue(TestAttributes.SimpleObjectFirstname.attributeId) }

        assertThat(idList, equalTo(listOf(35, 36, 37).map { InsightObjectId(it) }))
        assertThat(nameList, equalTo(listOf("Object1", "Object2", "Object3")))
        assertThat(firstNameList, equalTo(listOf("F1", "F2", "F3")))
        println("### END object_testObjectWithListAttributes")
    }

    @Test
    fun testAddingSelectList() {
        println("### START object_testAddingSelectList")
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()
        val results = obj.getValueList(TestAttributes.TestWithListsStringList.attributeId)
        assertThat(results, equalTo(emptyList()))
        obj.addValue(TestAttributes.TestWithListsStringList.attributeId, "A")
        obj.addValue(TestAttributes.TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateObject(obj).orNull() }

        val obj2 = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()
        val results2 = obj2.getValueList(TestAttributes.TestWithListsStringList.attributeId)
        assertThat(results2, containsInAnyOrder("A", "B"))
        obj2.removeValue(TestAttributes.TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateObject(obj2, ::identity, ::identity).orNull() }

        val obj3 = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()
        val results3 = obj3.getValueList(TestAttributes.TestWithListsStringList.attributeId)
        assertThat(results3, equalTo(listOf("A")))
        obj3.removeValue(TestAttributes.TestWithListsStringList.attributeId, "A")
        runBlocking { insightObjectOperator.updateObject(obj3, ::identity, ::identity).orNull() }

        val obj4 = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()
        val results4 = obj4.getValueList(TestAttributes.TestWithListsStringList.attributeId)
        assertThat(results4, equalTo(emptyList()))
        println("### END object_testAddingSelectList")
    }

    @Test
    fun testCreateAndDelete() {
        println("### START object_testCreateAndDelete")
        runBlocking {
            // Check England does not exist
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(InsightObjectType.Country.id, "England")
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(InsightObjectType.Company.id, "MyTestCompany GmbH")

            // Create and check direct result
            val country1 = insightObjectOperator.createObject(
                InsightObjectType.Country.id,
                {
                    it.setValue(TestAttributes.CountryName.attributeId, "England")
                    it.setValue(TestAttributes.CountryShortName.attributeId, "GB")
                }, ::identity
            ).orFail()

            val company1 = insightObjectOperator.createObject(
                InsightObjectType.Company.id,
                {
                    it.setValue(TestAttributes.CompanyName.attributeId, "MyTestCompany GmbH")
                    it.setSingleReference(TestAttributes.CompanyCountry.attributeId, country1.id)
                }, ::identity
            ).orFail()

            assertThat(country1.id.value, greaterThan(0))
            assertThat(country1.getStringValue(TestAttributes.CountryKey.attributeId)!!.isNotBlank(), equalTo(true))
            assertThat(
                company1.getSingleReferenceValue(TestAttributes.CompanyCountry.attributeId)!!.objectId.value,
                greaterThan(0)
            )
            assertThat(
                company1.getSingleReferenceValue(TestAttributes.CompanyCountry.attributeId)!!.objectKey.isNotBlank(),
                equalTo(true)
            )

            // Check England does exist
            val countryReference = company1.getSingleReferenceValue(TestAttributes.CompanyCountry.attributeId)!!
            val countryAfterCreate =
                insightObjectOperator.getObjectByName(InsightObjectType.Country.id, "England", toDomain = ::identity).orNull()!!
            val companyAfterCreate =
                insightObjectOperator.getObjectByName(InsightObjectType.Company.id, "MyTestCompany GmbH", toDomain = ::identity).orNull()!!
            assertThat(countryAfterCreate.id, equalTo(countryReference.objectId))
            assertThat(
                countryAfterCreate.getStringValue(TestAttributes.CountryKey.attributeId),
                equalTo(countryReference.objectKey)
            )
            assertThat(
                countryAfterCreate.getStringValue(TestAttributes.CountryName.attributeId),
                equalTo(countryReference.objectName)
            )
            assertThat(companyAfterCreate.id, equalTo(company1.id))

            // Check Delete
            insightObjectOperator.deleteObject(countryReference.objectId)
            insightObjectOperator.deleteObject(company1.id)
            val companyAfterDelete = insightObjectOperator.getObjectById(countryReference.objectId, ::identity).orNull()
            val countryAfterDelete = insightObjectOperator.getObjectById(country1.id, ::identity).orNull()
            assertThat(companyAfterDelete, equalTo(null))
            assertThat(countryAfterDelete, equalTo(null))
        }
        println("### END object_testCreateAndDelete")
    }

    @Test
    fun testFilter() {
        println("### START object_testFilter")
        runBlocking {
            val countries =
                insightObjectOperator.getObjectsByIQL(InsightObjectType.Country.id, """"ShortName"="DE"""", toDomain = ::identity)
                    .orNull()!!.objects
            assertThat(countries.size, equalTo(1))
            assertThat(countries.first().getStringValue(TestAttributes.CountryShortName.attributeId), equalTo("DE"))
            assertThat(countries.first().getStringValue(TestAttributes.CountryName.attributeId), equalTo("Germany"))
        }
        println("### END object_testFilter")
    }

    @Test
    fun testUpdate() {
        println("### START object_testUpdate")
        runBlocking {
            var country = insightObjectOperator.getObjectByName(InsightObjectType.Country.id, "Germany", toDomain = ::identity).orNull()!!
            assertThat(country.getStringValue(TestAttributes.CountryName.attributeId), equalTo("Germany"))
            assertThat(country.getStringValue(TestAttributes.CountryShortName.attributeId), equalTo("DE"))
            country.setValue(TestAttributes.CountryShortName.attributeId, "ED")
            country = runBlocking { insightObjectOperator.updateObject(country, ::identity, ::identity).orNull()!! }

            val country2 = insightObjectOperator.getObjectByName(InsightObjectType.Country.id, "Germany", toDomain = ::identity).orNull()!!
            assertThat(country2.getStringValue(TestAttributes.CountryName.attributeId), equalTo("Germany"))
            assertThat(country2.getStringValue(TestAttributes.CountryShortName.attributeId), equalTo("ED"))

            var countryAfterUpdate =
                insightObjectOperator.getObjectByName(InsightObjectType.Country.id, "Germany", toDomain = ::identity).orNull()!!
            assertThat(countryAfterUpdate.getStringValue(TestAttributes.CountryName.attributeId), equalTo("Germany"))
            assertThat(countryAfterUpdate.getStringValue(TestAttributes.CountryShortName.attributeId), equalTo("ED"))
            countryAfterUpdate.setValue(TestAttributes.CountryShortName.attributeId, "DE")
            countryAfterUpdate = runBlocking { insightObjectOperator.updateObject(countryAfterUpdate, ::identity, ::identity).orNull()!! }

            val countryAfterReUpdate =
                insightObjectOperator.getObjectByName(InsightObjectType.Country.id, "Germany", toDomain = ::identity).orNull()!!
            assertThat(
                countryAfterReUpdate.getStringValue(TestAttributes.CountryName.attributeId),
                equalTo("Germany")
            )
            assertThat(
                countryAfterReUpdate.getStringValue(TestAttributes.CountryShortName.attributeId),
                equalTo("DE")
            )
        }
        println("### END object_testUpdate")
    }

    @Test
    fun testGetObjectsWithoutChildren() {
        println("### START object_testGetObjectsWithoutChildren")
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Abstract.id, withChildren = false, toDomain = ::identity).orNull()!!
        }
        assertThat(objectResponse.totalFilterCount, equalTo(0))

        val objects = objectResponse.objects
        assertThat(objects, equalTo(emptyList()))

        println("### END testGetObjectsWithoutChildren")
    }

    @Test
    fun testGetObjectsWithChildren() {
        println("### START object_testGetObjectsWithChildren")
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Abstract.id, withChildren = true, toDomain = ::identity).orNull()!!
        }
        assertThat(objectResponse.totalFilterCount, equalTo(2))

        val objects = objectResponse.objects
        assertThat(objects.size, equalTo(2))

        val firstObj = objects.first()
        assertThat(firstObj.id, equalTo(InsightObjectId(94)))

        val secondObj = objects[1]
        assertThat(secondObj.id, equalTo(InsightObjectId(95)))

        println("### END testGetObjectsWithChildren")
    }

    @Test
    fun testGetObjectsWithChildrenPaginated() {
        println("### START object_testGetObjectsWithChildrenPaginated")

        // results 1 and 2
        val allINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 0,
                pageSize = 2,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(allINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val allObjects = allINSIGHTOBJECTList.objects
        assertThat(allObjects.size, equalTo(2))
        assertThat(allObjects[0].id, equalTo(InsightObjectId(94)))
        assertThat(allObjects[1].id, equalTo(InsightObjectId(95)))

        // results 1 and 2
        val allExplINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 0,
                pageSize = 5,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(allExplINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val allExplObjects = allExplINSIGHTOBJECTList.objects
        assertThat(allExplObjects.size, equalTo(2))
        assertThat(allExplObjects[0].id, equalTo(InsightObjectId(94)))
        assertThat(allExplObjects[1].id, equalTo(InsightObjectId(95)))

        // result 1
        val firstINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 0,
                pageSize = 1,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(firstINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val firstObjects = firstINSIGHTOBJECTList.objects
        assertThat(firstObjects.size, equalTo(1))
        assertThat(firstObjects[0].id, equalTo(InsightObjectId(94)))

        // result 2
        val secondINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 1,
                pageSize = 1,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(secondINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val secondObjects = secondINSIGHTOBJECTList.objects
        assertThat(secondObjects.size, equalTo(1))
        assertThat(secondObjects[0].id, equalTo(InsightObjectId(95)))

        // page doesn't exist
        val emptyINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 2,
                pageSize = 2,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(firstINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val emptyObjects = emptyINSIGHTOBJECTList.objects
        assertThat(emptyObjects, equalTo(emptyList()))

        println("### END object_testGetObjectsWithChildrenPaginated")
    }

    @Test
    fun testUserAttribute() {
        println("### START object_testUserAttribute")

        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.User.id, toDomain = ::identity).map { it.objects.firstOrNull() }.orFail()
        }
        assertThat(obj, notNullValue())
        val userAttr = obj!!.getUserList(TestAttributes.UserTestUser.attributeId)
        assertThat(userAttr.size, equalTo(1))
        assertThat(userAttr.first().name, equalTo("admin"))
        val usersAttr = obj.getUserList(TestAttributes.UserTestUsers.attributeId)
        assertThat(usersAttr.size, equalTo(2))
        assertThat(usersAttr.map { it.name }, hasItems("admin", "test1"))

        println("### END object_testUserAttribute")
    }

    @Test
    fun testObjectCount() = runBlocking {
        println("### START object_testGetObjectsWithChildrenPaginated")
        val count = insightObjectOperator.getObjectCount("objectType = Many").orFail()
        assertThat(count, equalTo(55))
    }

}
