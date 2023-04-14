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

import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

interface InsightObjectOperatorTest {
    val insightObjectOperator: InsightObjectOperator

    @Test
    fun testObjectListWithFlatReference() {
        println("### START object_testObjectListWithFlatReference")
        val companies = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Company.id).orNull()!!.objects
        }
        assertTrue(companies.size == 2)

        val firstCompany = companies.firstOrNull { it.id == 1 }
        assertNotNull(firstCompany)
        assertEquals(1, firstCompany!!.id)
        assertEquals("IT-1", firstCompany.objectKey)
        assertEquals("Test GmbH", firstCompany.label)

        // Name
        assertNotNull(firstCompany.getAttributeByName(InsightAttribute.CompanyName.attributeName))
        assertEquals(
            InsightAttribute.CompanyName.attributeId,
            firstCompany.getAttributeIdByName(InsightAttribute.CompanyName.attributeName)
        )
        assertEquals("Test GmbH", firstCompany.getStringValue(InsightAttribute.CompanyName.attributeId))

        // Country
        assertNotNull(firstCompany.getAttributeByName(InsightAttribute.CompanyCountry.attributeName))
        assertEquals(
            InsightAttribute.CompanyCountry.attributeId,
            firstCompany.getAttributeIdByName(InsightAttribute.CompanyCountry.attributeName)
        )
        assertEquals(
            "Germany",
            firstCompany.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName
        )
        assertFalse(firstCompany.attachmentsExist)

        val secondCompany = companies.firstOrNull { it.id == 2 }
        assertNotNull(secondCompany)
        assertEquals(2, secondCompany!!.id)
        assertEquals("IT-2", secondCompany.objectKey)
        assertEquals("Test AG", secondCompany.label)

        // Name
        assertNotNull(secondCompany.getAttributeByName(InsightAttribute.CompanyName.attributeName))
        assertEquals(
            InsightAttribute.CompanyName.attributeId,
            secondCompany.getAttributeIdByName(InsightAttribute.CompanyName.attributeName)
        )
        assertEquals("Test AG", secondCompany.getStringValue(InsightAttribute.CompanyName.attributeId))

        // Country
        assertNotNull(secondCompany.getAttributeByName(InsightAttribute.CompanyCountry.attributeName))
        assertEquals(
            InsightAttribute.CompanyCountry.attributeId,
            secondCompany.getAttributeIdByName(InsightAttribute.CompanyCountry.attributeName)
        )
        assertEquals(
            "Germany",
            secondCompany.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName
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
        assertTrue(companies!!.size == 2)

        val company = companies.firstOrNull { it.id == 1 }
        assertNotNull(company)

        val country = runBlocking {
            insightObjectOperator.getObjectById(
                company!!.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectId
            ).orNull()!!
        }
        assertTrue(country.getStringValue(InsightAttribute.CountryName.attributeId) == "Germany")
        assertTrue(country.getStringValue(InsightAttribute.CountryShortName.attributeId) == "DE")
        println("### END object_testObjectListWithResolvedReference")
    }

    @Test
    fun testObjectById() {
        println("### START object_testObjectById")
        val company = runBlocking {
            insightObjectOperator.getObjectById(1).orNull()!!
        }
        assertEquals(1, company.id)
        assertEquals("IT-1", company.objectKey)
        assertEquals("Test GmbH", company.label)
        assertEquals("Test GmbH", company.getStringValue(InsightAttribute.CompanyName.attributeId))
        assertEquals(
            "Germany",
            company.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName
        )
        assertEquals("Company", company.objectTypeName)
        assertEquals(1, company.objectTypeId)

        assertFalse(company.attachmentsExist)
        println("### END object_testObjectById")
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

        assertTrue(idList == listOf(35, 36, 37))
        assertTrue(nameList == listOf("Object1", "Object2", "Object3"))
        assertTrue(firstNameList == listOf("F1", "F2", "F3"))
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
        assertTrue(results2.size == 2)
        assertTrue(results2.contains("A"))
        assertTrue(results2.contains("B"))
        obj2.removeValue(InsightAttribute.TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateObject(obj2).orNull() }

        val obj3 = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results3 = obj3.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertTrue(results3.size == 1)
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
            val countryBeforeCreate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "England").orNull()
            assertNull(countryBeforeCreate)

            val companyBeforeCreate =
                insightObjectOperator.getObjectByName(InsightObject.Company.id, "MyTestCompany GmbH").orNull()
            assertNull(companyBeforeCreate)

            // Create and check direct result
            val country1 = insightObjectOperator.createObject(InsightObject.Country.id) {
                it.setValue(InsightAttribute.CountryName.attributeId, "England")
                it.setValue(InsightAttribute.CountryShortName.attributeId, "GB")
            }.orNull()!!

            val company1 = insightObjectOperator.createObject(InsightObject.Company.id) {
                it.setValue(InsightAttribute.CompanyName.attributeId, "MyTestCompany GmbH")
                it.setSingleReference(InsightAttribute.CompanyCountry.attributeId, country1.id)
            }.orNull()!!

            assertTrue(country1.id > 0)
            assertTrue(country1.getStringValue(InsightAttribute.CountryKey.attributeId)!!.isNotBlank())
            assertTrue(company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectId > 0)
            assertTrue(company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectKey.isNotBlank())

            // Check England does exist
            val countryReference = company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!
            val countryAfterCreate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "England").orNull()!!
            val companyAfterCreate =
                insightObjectOperator.getObjectByName(InsightObject.Company.id, "MyTestCompany GmbH").orNull()!!
            assertTrue(countryAfterCreate.id == countryReference.objectId)
            assertEquals(
                countryReference.objectKey,
                countryAfterCreate.getStringValue(InsightAttribute.CountryKey.attributeId)
            )
            assertEquals(
                countryReference.objectName,
                countryAfterCreate.getStringValue(InsightAttribute.CountryName.attributeId)
            )
            assertTrue(companyAfterCreate.id == company1.id)

            // Check Delete
            insightObjectOperator.deleteObject(countryReference.objectId)
            insightObjectOperator.deleteObject(company1.id)
            val companyAfterDelete = insightObjectOperator.getObjectById(countryReference.objectId).orNull()
            val countryAfterDelete = insightObjectOperator.getObjectById(country1.id).orNull()
            assertTrue(companyAfterDelete == null)
            assertTrue(countryAfterDelete == null)
        }
        println("### END object_testCreateAndDelete")
    }

    @Test
    fun testFilter() {
        println("### START object_testFilter")
        runBlocking {
            val countries =
                insightObjectOperator.getObjectsByIQL(InsightObject.Country.id, false, "\"ShortName\"=\"DE\"")
                    .orNull()!!.objects
            assertTrue(countries.size == 1)
            assertEquals("DE", countries.first().getStringValue(InsightAttribute.CountryShortName.attributeId))
            assertEquals("Germany", countries.first().getStringValue(InsightAttribute.CountryName.attributeId))
        }
        println("### END object_testFilter")
    }

    @Test
    fun testUpdate() {
        println("### START object_testUpdate")
        runBlocking {
            var country = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertEquals("Germany", country.getStringValue(InsightAttribute.CountryName.attributeId))
            assertEquals("DE", country.getStringValue(InsightAttribute.CountryShortName.attributeId))
            country.setValue(InsightAttribute.CountryShortName.attributeId, "ED")
            country = runBlocking { insightObjectOperator.updateObject(country).orNull()!! }

            val country2 = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertEquals("Germany", country2.getStringValue(InsightAttribute.CountryName.attributeId))
            assertEquals("ED", country2.getStringValue(InsightAttribute.CountryShortName.attributeId))

            var countryAfterUpdate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertEquals("Germany", countryAfterUpdate.getStringValue(InsightAttribute.CountryName.attributeId))
            assertEquals("ED", countryAfterUpdate.getStringValue(InsightAttribute.CountryShortName.attributeId))
            countryAfterUpdate.setValue(InsightAttribute.CountryShortName.attributeId, "DE")
            countryAfterUpdate = runBlocking { insightObjectOperator.updateObject(countryAfterUpdate).orNull()!! }

            val countryAfterReUpdate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertEquals("Germany", countryAfterReUpdate.getStringValue(InsightAttribute.CountryName.attributeId))
            assertEquals("DE", countryAfterReUpdate.getStringValue(InsightAttribute.CountryShortName.attributeId))
        }
        println("### END object_testUpdate")
    }

    @Test
    fun testGetObjectsWithoutChildren() {
        println("### START object_testGetObjectsWithoutChildren")
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Abstract.id, withChildren = false).orNull()!!
        }
        assertEquals(0, objectResponse.totalFilterCount)

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
        assertEquals(2, objectResponse.totalFilterCount)

        val objects = objectResponse.objects
        assertEquals(2, objects.size)

        val firstObj = objects.first()
        assertEquals(94, firstObj.id)

        val secondObj = objects[1]
        assertEquals(95, secondObj.id)

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
        assertEquals(2, allObjects.size)
        assertEquals(94, allObjects[0].id)
        assertEquals(95, allObjects[1].id)

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
        assertEquals(2, allExplObjects.size)
        assertEquals(94, allExplObjects[0].id)
        assertEquals(95, allExplObjects[1].id)

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
        assertEquals(1, firstObjects.size)
        assertEquals(94, firstObjects[0].id)

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
        assertEquals(1, secondObjects.size)
        assertEquals(95, secondObjects[0].id)

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
}
