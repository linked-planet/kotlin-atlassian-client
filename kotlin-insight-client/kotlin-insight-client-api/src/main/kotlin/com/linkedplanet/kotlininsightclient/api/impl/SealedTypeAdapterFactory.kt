/*-
 * #%L
 * kotlin-insight-client-api
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
package com.linkedplanet.kotlininsightclient.api.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlin.reflect.KClass

/**
 * Creates TypeAdaptors for sealed classes.
 * Especially for deserialization Gson needs to know which class to instantiate.
 *
 * @param baseType The parent sealed class
 * @param typeFieldName the additional field inside the json that contains the name of the type
 * @param jsonNameForType Choose a different name for the type einside the json, other than the name of the class itself (defaults to simplename)
 */
class SealedTypeAdapterFactory<T : Any> private constructor(
    private val baseType: KClass<T>,
    private val typeFieldName: String,
    jsonNameForType: (KClass<out T>) -> Pair<String, KClass<out T>>
) : TypeAdapterFactory {

    private val subclasses = baseType.sealedSubclasses
    private val nameToSubclass: Map<String, KClass<out T>> = subclasses.associate(jsonNameForType)

    init {
        if (!baseType.isSealed) throw IllegalArgumentException("$baseType is not a sealed class")
    }

    override fun <R : Any> create(gson: Gson, type: TypeToken<R>?): TypeAdapter<R>? {
        if (type == null || subclasses.isEmpty() || subclasses.none { type.rawType.isAssignableFrom(it.java) }) return null
        val elementTypeAdapter = gson.getAdapter(JsonElement::class.java)
        val subclassToDelegate: Map<KClass<*>, TypeAdapter<*>> = subclasses.associateWith {
            gson.getDelegateAdapter(this, TypeToken.get(it.java))
        }

        return object : TypeAdapter<R>() {

            override fun write(writer: JsonWriter, value: R) {
                val srcType = value::class
                val label = srcType.simpleName!!

                @Suppress("UNCHECKED_CAST") val delegate = subclassToDelegate[srcType] as TypeAdapter<R>
                val jsonObject = delegate.toJsonTree(value).asJsonObject

                val clone = JsonObject()
                if (!jsonObject.has(typeFieldName)) {
                    clone.add(typeFieldName, JsonPrimitive(label))
                }
                jsonObject.entrySet().forEach {
                    clone.add(it.key, it.value)
                }
                elementTypeAdapter.write(writer, clone)
            }

            override fun read(reader: JsonReader): R {
                val element = elementTypeAdapter.read(reader)
                val labelElement = element.asJsonObject.remove(typeFieldName) ?: throw JsonParseException(
                    "cannot deserialize $baseType because it does not define a field named $typeFieldName"
                )
                val name = labelElement.asString
                val subclass = nameToSubclass[name] ?: throw JsonParseException("cannot find $name subclass of $baseType")
                @Suppress("UNCHECKED_CAST")
                return (subclass.objectInstance as? R) ?: (subclassToDelegate[subclass]!!.fromJsonTree(element) as R)
            }

        }
    }

    companion object {
        fun <T : Any> of(
            clz: KClass<T>,
            typeFieldName: String = "type",
            jsonNameForType: (KClass<out T>) -> Pair<String, KClass<out T>> = { it.simpleName!! to it }
        ) = SealedTypeAdapterFactory(clz, typeFieldName, jsonNameForType)
    }
}