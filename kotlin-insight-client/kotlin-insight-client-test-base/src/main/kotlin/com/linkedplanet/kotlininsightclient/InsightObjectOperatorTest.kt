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

import com.linkedplanet.kotlininsightclient.api.experimental.GenericInsightObjectOperatorImpl
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.addValue
import com.linkedplanet.kotlininsightclient.api.model.getAttributeByName
import com.linkedplanet.kotlininsightclient.api.model.getAttributeIdByName
import com.linkedplanet.kotlininsightclient.api.model.getMultiReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getSingleReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getStringValue
import com.linkedplanet.kotlininsightclient.api.model.getValueList
import com.linkedplanet.kotlininsightclient.api.model.removeValue
import com.linkedplanet.kotlininsightclient.api.model.setSingleReference
import com.linkedplanet.kotlininsightclient.api.model.setValue
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert.*
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
                    insightObjectOperator.getObjectByName(objectTypeId, domainObject.name)
                }
            )
        val companyOperator =
            GenericInsightObjectOperatorImpl(Company::class,
                insightObjectForDomainObject = { objectTypeId, domainObject: Company ->
                    insightObjectOperator.getObjectByName(objectTypeId, domainObject.name)
                },
                referenceAttributeToValue = { insightAttribute ->
                    val movie = insightAttribute.value.first().referencedObject!!.id
                    val eitherMovie = countryOperator.getById(movie)
                    eitherMovie.orNull()!!
                },
                attributeToReferencedObjectId = { schema: ObjectTypeSchemaAttribute, obj: Any? ->
                    val country = obj as Country
                    listOfNotNull(
                        insightObjectOperator.getObjectByName(schema.referenceObjectTypeId!!, country.name)
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
                insightObjectOperator.getObjectByName(objectTypeId, domainObject.name)
            }
        )
        val testWithListsOperator = GenericInsightObjectOperatorImpl(TestWithLists::class,
            insightObjectForDomainObject = { objectTypeId, domainObject: TestWithLists ->
                insightObjectOperator.getObjectByName(objectTypeId, domainObject.name)
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
                        insightObjectOperator.getObjectByName(schema.referenceObjectTypeId!!, it.name).orNull()?.id
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
            insightObjectOperator.getObjects(InsightObject.Company.id).orNull()!!.objects
        }
        assertTrue(companies.size == 2)

        val firstCompany = companies.firstOrNull { it.id == InsightObjectId(1) }
        assertNotNull(firstCompany)
        assertThat(firstCompany!!.id, equalTo(InsightObjectId(1)))
        assertThat(firstCompany.objectKey, equalTo("IT-1"))
        assertThat(firstCompany.label, equalTo("Test GmbH"))

        // Name
        assertNotNull(firstCompany.getAttributeByName(InsightAttribute.CompanyName.attributeName))
        assertThat(
            firstCompany.getAttributeIdByName(InsightAttribute.CompanyName.attributeName),
            equalTo(InsightAttribute.CompanyName.attributeId)
        )
        assertThat(firstCompany.getStringValue(InsightAttribute.CompanyName.attributeId), equalTo("Test GmbH"))

        // Country
        assertNotNull(firstCompany.getAttributeByName(InsightAttribute.CompanyCountry.attributeName))
        assertThat(
            firstCompany.getAttributeIdByName(InsightAttribute.CompanyCountry.attributeName),
            equalTo(InsightAttribute.CompanyCountry.attributeId)
        )
        assertThat(
            firstCompany.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertFalse(firstCompany.attachmentsExist)

        val secondCompany = companies.firstOrNull { it.id == InsightObjectId(2) }
        assertNotNull(secondCompany)
        assertThat(secondCompany!!.id, equalTo(InsightObjectId(2)))
        assertThat(secondCompany.objectKey, equalTo("IT-2"))
        assertThat(secondCompany.label, equalTo("Test AG"))

        // Name
        assertNotNull(secondCompany.getAttributeByName(InsightAttribute.CompanyName.attributeName))
        assertThat(
            secondCompany.getAttributeIdByName(InsightAttribute.CompanyName.attributeName),
            equalTo(InsightAttribute.CompanyName.attributeId)
        )
        assertThat(secondCompany.getStringValue(InsightAttribute.CompanyName.attributeId), equalTo("Test AG"))

        // Country
        assertNotNull(secondCompany.getAttributeByName(InsightAttribute.CompanyCountry.attributeName))
        assertThat(
            secondCompany.getAttributeIdByName(InsightAttribute.CompanyCountry.attributeName),
            equalTo(InsightAttribute.CompanyCountry.attributeId)
        )
        assertThat(
            secondCompany.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertTrue(secondCompany.attachmentsExist)

        println("### END object_testObjectListWithFlatReference")
    }

    @Test
    fun testObjectListWithResolvedReference() {
        println("### START object_testObjectListWithResolvedReference")
        val companies = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Company.id).orNull()?.objects
        }
        assertNotNull(companies)
        assertThat(companies!!.size, equalTo(2))

        val company = companies.firstOrNull { it.id == InsightObjectId(1) }
        assertNotNull(company)

        val country = runBlocking {
            insightObjectOperator.getObjectById(
                company!!.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectId
            ).orNull()!!
        }
        assertThat(country.getStringValue(InsightAttribute.CountryName.attributeId), equalTo("Germany"))
        assertThat(country.getStringValue(InsightAttribute.CountryShortName.attributeId), equalTo("DE"))
        println("### END object_testObjectListWithResolvedReference")
    }

    @Test
    fun testObjectById() {
        println("### START object_testObjectById")
        val company = runBlocking {
            insightObjectOperator.getObjectById(InsightObjectId(1)).orNull()!!
        }
        assertThat(company.id, equalTo(InsightObjectId(1)))
        assertThat(company.objectKey, equalTo("IT-1"))
        assertThat(company.label, equalTo("Test GmbH"))
        assertThat(company.getStringValue(InsightAttribute.CompanyName.attributeId), equalTo("Test GmbH"))
        assertThat(
            company.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertThat(company.objectTypeName, equalTo("Company"))
        assertThat(company.objectTypeId, equalTo(InsightObjectTypeId(1)))

        assertFalse(company.attachmentsExist)
        println("### END object_testObjectById")
    }

    @Test
    fun testGetObjectsByObjectTypeName() = runBlocking {
        println("### START object_testGetObjecsByObjectTypeName")
        val objs = insightObjectOperator.getObjectsByObjectTypeName("Country").orFail()
        val allCountryNames = objs.map { it.getStringValue(InsightAttribute.CountryName.attributeId) }
        assertThat(allCountryNames, Matchers.hasItem("Germany")) // among other items
    }

    @Test
    fun testObjectWithListAttributes() {
        println("### START object_testObjectWithListAttributes")
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()

        val references = obj.getMultiReferenceValue(InsightAttribute.TestWithListsItemList.attributeId)
        val idList = references.map { it.objectId }
        val nameList = references.map { it.objectName }
        val refList = references.map { insightReference ->
            runBlocking {
                insightObjectOperator.getObjectById(insightReference.objectId).orNull()!!
            }
        }
        val firstNameList = refList.map { it.getStringValue(InsightAttribute.SimpleObjectFirstname.attributeId) }

        assertThat(idList, equalTo(listOf(35, 36, 37).map { InsightObjectId(it) }))
        assertThat(nameList, equalTo(listOf("Object1", "Object2", "Object3")))
        assertThat(firstNameList, equalTo(listOf("F1", "F2", "F3")))
        println("### END object_testObjectWithListAttributes")
    }

    @Test
    fun testAddingSelectList() {
        println("### START object_testAddingSelectList")
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results = obj.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertTrue(results.isEmpty())
        obj.addValue(InsightAttribute.TestWithListsStringList.attributeId, "A")
        obj.addValue(InsightAttribute.TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateObject(obj).orNull() }

        val obj2 = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results2 = obj2.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertThat(results2.size, equalTo(2))
        assertTrue(results2.contains("A"))
        assertTrue(results2.contains("B"))
        obj2.removeValue(InsightAttribute.TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateObject(obj2).orNull() }

        val obj3 = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results3 = obj3.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertThat(results3.size, equalTo(1))
        assertTrue(results3.contains("A"))
        obj3.removeValue(InsightAttribute.TestWithListsStringList.attributeId, "A")
        runBlocking { insightObjectOperator.updateObject(obj3).orNull() }

        val obj4 = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results4 = obj4.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertTrue(results4.isEmpty())
        println("### END object_testAddingSelectList")
    }

    @Test
    fun testCreateAndDelete() {
        println("### START object_testCreateAndDelete")
        runBlocking {
            // Check England does not exist
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(InsightObject.Country.id, "England")
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(InsightObject.Company.id, "MyTestCompany GmbH")

            // Create and check direct result
            val country1 = insightObjectOperator.createObject(InsightObject.Country.id) {
                it.setValue(InsightAttribute.CountryName.attributeId, "England")
                it.setValue(InsightAttribute.CountryShortName.attributeId, "GB")
            }.orFail()

            val company1 = insightObjectOperator.createObject(InsightObject.Company.id) {
                it.setValue(InsightAttribute.CompanyName.attributeId, "MyTestCompany GmbH")
                it.setSingleReference(InsightAttribute.CompanyCountry.attributeId, country1.id)
            }.orFail()

            assertTrue(country1.id.value > 0)
            assertTrue(country1.getStringValue(InsightAttribute.CountryKey.attributeId)!!.isNotBlank())
            assertTrue(company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectId.value > 0)
            assertTrue(company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectKey.isNotBlank())

            // Check England does exist
            val countryReference = company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!
            val countryAfterCreate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "England").orNull()!!
            val companyAfterCreate =
                insightObjectOperator.getObjectByName(InsightObject.Company.id, "MyTestCompany GmbH").orNull()!!
            assertTrue(countryAfterCreate.id == countryReference.objectId)
            assertThat(
                countryAfterCreate.getStringValue(InsightAttribute.CountryKey.attributeId),
                equalTo(countryReference.objectKey)
            )
            assertThat(
                countryAfterCreate.getStringValue(InsightAttribute.CountryName.attributeId),
                equalTo(countryReference.objectName)
            )
            assertThat(companyAfterCreate.id, equalTo(company1.id))

            // Check Delete
            insightObjectOperator.deleteObject(countryReference.objectId)
            insightObjectOperator.deleteObject(company1.id)
            val companyAfterDelete = insightObjectOperator.getObjectById(countryReference.objectId).orNull()
            val countryAfterDelete = insightObjectOperator.getObjectById(country1.id).orNull()
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
                insightObjectOperator.getObjectsByIQL(InsightObject.Country.id, """"ShortName"="DE"""")
                    .orNull()!!.objects
            assertTrue(countries.size == 1)
            assertThat(countries.first().getStringValue(InsightAttribute.CountryShortName.attributeId), equalTo("DE"))
            assertThat(countries.first().getStringValue(InsightAttribute.CountryName.attributeId), equalTo("Germany"))
        }
        println("### END object_testFilter")
    }

    @Test
    fun testUpdate() {
        println("### START object_testUpdate")
        runBlocking {
            var country = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertThat(country.getStringValue(InsightAttribute.CountryName.attributeId), equalTo("Germany"))
            assertThat(country.getStringValue(InsightAttribute.CountryShortName.attributeId), equalTo("DE"))
            country.setValue(InsightAttribute.CountryShortName.attributeId, "ED")
            country = runBlocking { insightObjectOperator.updateObject(country).orNull()!! }

            val country2 = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertThat(country2.getStringValue(InsightAttribute.CountryName.attributeId), equalTo("Germany"))
            assertThat(country2.getStringValue(InsightAttribute.CountryShortName.attributeId), equalTo("ED"))

            var countryAfterUpdate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertThat(countryAfterUpdate.getStringValue(InsightAttribute.CountryName.attributeId), equalTo("Germany"))
            assertThat(countryAfterUpdate.getStringValue(InsightAttribute.CountryShortName.attributeId), equalTo("ED"))
            countryAfterUpdate.setValue(InsightAttribute.CountryShortName.attributeId, "DE")
            countryAfterUpdate = runBlocking { insightObjectOperator.updateObject(countryAfterUpdate).orNull()!! }

            val countryAfterReUpdate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertThat(
                countryAfterReUpdate.getStringValue(InsightAttribute.CountryName.attributeId),
                equalTo("Germany")
            )
            assertThat(
                countryAfterReUpdate.getStringValue(InsightAttribute.CountryShortName.attributeId),
                equalTo("DE")
            )
        }
        println("### END object_testUpdate")
    }

    @Test
    fun testGetObjectsWithoutChildren() {
        println("### START object_testGetObjectsWithoutChildren")
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Abstract.id, withChildren = false).orNull()!!
        }
        assertThat(objectResponse.totalFilterCount, equalTo(0))

        val objects = objectResponse.objects
        assertTrue(objects.isEmpty())

        println("### END testGetObjectsWithoutChildren")
    }

    @Test
    fun testGetObjectsWithChildren() {
        println("### START object_testGetObjectsWithChildren")
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Abstract.id, withChildren = true).orNull()!!
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
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 1,
                perPage = 2
            ).orNull()!!
        }
        assertTrue(allINSIGHTOBJECTList.totalFilterCount == 2)
        val allObjects = allINSIGHTOBJECTList.objects
        assertThat(allObjects.size, equalTo(2))
        assertThat(allObjects[0].id, equalTo(InsightObjectId(94)))
        assertThat(allObjects[1].id, equalTo(InsightObjectId(95)))

        // results 1 and 2
        val allExplINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 1,
                perPage = 5
            ).orNull()!!
        }
        assertTrue(allExplINSIGHTOBJECTList.totalFilterCount == 2)
        val allExplObjects = allExplINSIGHTOBJECTList.objects
        assertThat(allExplObjects.size, equalTo(2))
        assertThat(allExplObjects[0].id, equalTo(InsightObjectId(94)))
        assertThat(allExplObjects[1].id, equalTo(InsightObjectId(95)))

        // result 1
        val firstINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 1,
                perPage = 1
            ).orNull()!!
        }
        assertTrue(firstINSIGHTOBJECTList.totalFilterCount == 2)
        val firstObjects = firstINSIGHTOBJECTList.objects
        assertThat(firstObjects.size, equalTo(1))
        assertThat(firstObjects[0].id, equalTo(InsightObjectId(94)))

        // result 2
        val secondINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 2,
                perPage = 1
            ).orNull()!!
        }
        assertTrue(secondINSIGHTOBJECTList.totalFilterCount == 2)
        val secondObjects = secondINSIGHTOBJECTList.objects
        assertThat(secondObjects.size, equalTo(1))
        assertThat(secondObjects[0].id, equalTo(InsightObjectId(95)))

        // page doesn't exist
        val emptyINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 3,
                perPage = 2
            ).orNull()!!
        }
        assertTrue(firstINSIGHTOBJECTList.totalFilterCount == 2)
        val emptyObjects = emptyINSIGHTOBJECTList.objects
        assertTrue(emptyObjects.isEmpty())

        println("### END testGetObjectsWithChildrenPaginated")
    }

    @Test
    fun testObjectCount() = runBlocking {
        println("### START object_testGetObjectsWithChildrenPaginated")
        val count = insightObjectOperator.getObjectCount("objectType = Many").orFail()
        assertThat(count, equalTo(55))
    }

}
