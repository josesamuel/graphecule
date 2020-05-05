/*
 * Copyright (C) 2020 Joseph Samuel
 *
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
 */
package graphecule.client.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import graphecule.client.asSafeString
import graphecule.client.asStringOrNull
import graphecule.client.isNotNull
import graphecule.client.logMessage
import graphecule.common.HttpRequestAdapter
import graphecule.common.RequestSender
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Builds the [GraphModel] of a given graphql
 *
 * @param apiHost The graphql api host url to parse
 * @param httpRequestAdapter [HttpRequestAdapter] if any to handle authentication or interception.
 */
internal class GraphModelBuilder(private val apiHost: String, private val httpRequestAdapter: HttpRequestAdapter? = null) {

    private val LOCK = Any()
    private val modelMap: MutableMap<String, ClassInfo> = ConcurrentHashMap<String, ClassInfo>()
    private val processedTypes = ConcurrentHashMap<String, ClassInfo>()
    private val pendingTypesToProcess = ConcurrentHashMap.newKeySet<String>()
    private val currentlyProcessingTypes = ConcurrentHashMap.newKeySet<String>()
    private val requestSender by lazy { RequestSender(apiHost, httpRequestAdapter) }

    /**
     * Builds and returns the [GraphModel]
     */
    suspend fun buildGraphModel(): GraphModel = withContext(Dispatchers.IO) {
        val schemaQuery = "{__schema {queryType {name} mutationType {name} }}"
        val response = requestSender.sendRequest("{\"query\" : \"$schemaQuery\"}")
        var queryInfo: ClassInfo? = null
        var mutationInfo: ClassInfo? = null
        var errorMessage = response ?: "Failed to get reply from server"
        if (response != null) {
            val resultJson = JsonParser.parseString(response) as JsonObject
            if (resultJson.has("errors")) {
                errorMessage = resultJson.get("errors").asSafeString()
            } else {
                val dataJson = resultJson.getAsJsonObject("data")
                if (dataJson != null) {
                    val schemaJsonObject = dataJson.getAsJsonObject("__schema")
                    if (schemaJsonObject != null) {
                        val queryTypeJsonObject = schemaJsonObject.getAsJsonObject("queryType")
                        if (queryTypeJsonObject != null) {
                            val queryTypeName = queryTypeJsonObject.get("name").asString
                            queryInfo = getClassInfo(queryTypeName)
                            addTypeToProcess(queryTypeName)
                        }
                        val mutationType = schemaJsonObject.get("mutationType")

                        if (mutationType.isNotNull()) {
                            val mutationTypeJsonObject = mutationType as JsonObject
                            val mutationTypeName = mutationTypeJsonObject.get("name").asString
                            mutationInfo = getClassInfo(mutationTypeName)
                            addTypeToProcess(mutationTypeName)
                        }
                    }
                }
            }
        }

        val maxThread = httpRequestAdapter?.maxParallelRequests ?: 8
        val asyncExecutor = Executors.newFixedThreadPool(maxThread)
        val asyncScope = asyncExecutor.asCoroutineDispatcher()
        val deferredList: MutableList<Deferred<ClassInfo>> = mutableListOf()

        do {
            var deferredLimit = 0
            while (pendingTypesToProcess.isNotEmpty() && deferredLimit <= maxThread) {
                synchronized(LOCK) {
                    val typeToProcess = pendingTypesToProcess.first()
                    pendingTypesToProcess.remove(typeToProcess)
                    currentlyProcessingTypes.add(typeToProcess)
                    deferredList.add(async(context = asyncScope) { processType(typeToProcess) })
                }
                deferredLimit++
            }
            val results = deferredList.awaitAll()
            currentlyProcessingTypes.clear()
            results.forEach {
                processedTypes[it.name] = it
            }
            deferredList.clear()
            if (httpRequestAdapter?.rateLimit ?: 0 > 0) {
                logMessage("Throttling for ${httpRequestAdapter?.rateLimit} ms")
                delay(httpRequestAdapter?.rateLimit!!)
            }
        } while (pendingTypesToProcess.isNotEmpty())

        asyncScope.close()
        asyncExecutor.shutdownNow()

        val classNameCount = mutableMapOf<String, Int>()
        processedTypes.keys.forEach {
            val nameLowerCase = it.toLowerCase()
            var currentCount = classNameCount.getOrPut(nameLowerCase) {
                0
            }
            currentCount++
            classNameCount[nameLowerCase] = currentCount
        }
        logMessage("Processing Complete")

        if (queryInfo != null) {
            GraphModel(apiHost, queryInfo, mutationInfo, processedTypes, classNameCount)
        } else {
            throw RuntimeException("Failed to get query class. $errorMessage")
        }
    }

    private fun sendRequest(queryString: String): String? =
        runBlocking {
            requestSender.sendRequest(queryString)
        }


    /**
     * Process a given type to get all the information about it
     */
    private fun processType(typeName: String): ClassInfo {
        val classInfo: ClassInfo = getClassInfo(typeName)
        if (!processedTypes.containsKey(typeName)) {
            logMessage("Processing $typeName")
            val queryBody = getTypeQuery(typeName)
            val queryString = "{\"query\" : \"$queryBody\"}"
            val result = sendRequest(queryString)

            if (result != null) {
                val resultJson = JsonParser.parseString(result) as? JsonObject ?: return classInfo.also {
                    println("Parse error, returning default for  $typeName $result")
                }
                if (resultJson.has("errors")) {
                    println("Failed to get type $typeName : ${resultJson.get("errors")}")
                } else {
                    val dataJson = resultJson.getAsJsonObject("data")
                    if (dataJson != null) {
                        val typeJsonObject = dataJson.getAsJsonObject("__type")
                        if (typeJsonObject != null) {
                            classInfo.typeKind = getClassTypeKind(typeJsonObject)
                            classInfo.description = typeJsonObject.get("description").asStringOrNull()
                            getTypeConnections(typeJsonObject, "interfaces")?.forEach {
                                classInfo.addParent(it)
                            }
                            classInfo.childClasses = getTypeConnections(typeJsonObject, "possibleTypes", classInfo.childClasses)
                            classInfo.childClasses?.forEach {
                                val childConnection = getClassInfo(it)
                                childConnection.addParent(typeName)
                            }
                            updateEnumValues(typeJsonObject, classInfo)
                            classInfo.fields = getFields(typeJsonObject, "fields")
                            classInfo.queryArgs = getFields(typeJsonObject, "inputFields")
                        }
                    } else {
                        println("Failed to process type $typeName : $result. \nSpecify custom httpRequestAdapter to throttle requests\n\n")
                    }
                }
            }

            modelMap[typeName] = classInfo
        }

        return classInfo
    }

    /**
     * Returns type information
     */
    private fun getClassTypeKind(typeKindJson: JsonObject): TypeKind {
        return when (val kind = typeKindJson.get("kind").asString) {
            "OBJECT" -> TypeKind.OBJECT
            "INTERFACE" -> TypeKind.INTERFACE
            "UNION" -> TypeKind.UNION
            "ENUM" -> TypeKind.ENUM
            "INPUT_OBJECT" -> TypeKind.INPUT_OBJECT
            else -> {
                println("Unexpected type for class $kind")
                TypeKind.OBJECT
            }
        }
    }

    /**
     * Adds a type to be processed
     */
    private fun addTypeToProcess(typeName: String) {
        synchronized(LOCK) {
            if (!processedTypes.containsKey(typeName)
                && !pendingTypesToProcess.contains(typeName)
                && !currentlyProcessingTypes.contains(typeName)
            ) {
                pendingTypesToProcess.add(typeName)
            }
        }
    }

    /**
     * Returns the field type
     */
    private fun getFieldType(typeKindJson: JsonObject, isNullable: Boolean = true): FieldType {
        return when (val kind = typeKindJson.get("kind").asString) {
            "OBJECT", "INTERFACE", "UNION", "ENUM", "INPUT_OBJECT" -> {
                val name = typeKindJson.get("name").asString
                addTypeToProcess(name)
                FieldType(name, TypeKind.valueOf(kind), isNullable)
            }

            "NON_NULL" -> return getFieldType(typeKindJson.getAsJsonObject("ofType"), false)
            "LIST" -> {
                val listType = FieldType("", TypeKind.LIST, isNullable)
                listType.subType = getFieldType(typeKindJson.getAsJsonObject("ofType"))
                listType
            }
            else -> {
                val name = typeKindJson.get("name").asString.toUpperCase()
                try {
                    FieldType(name, TypeKind.valueOf(name), isNullable)
                } catch (exception: Exception) {
                    FieldType(name, TypeKind.STRING, isNullable)
                }
            }
        }
    }

    /**
     * Returns the connection of this given type, either parent or child relations
     */
    private fun getTypeConnections(typeKindJson: JsonObject, collectionName: String, currentConnections: MutableSet<String>? = null): MutableSet<String>? {
        var connections: MutableSet<String>? = currentConnections
        val connectionJson = typeKindJson.get(collectionName)
        if (connectionJson.isNotNull()) {
            if (connections == null) {
                connections = mutableSetOf()
            }
            val possibleTypes = connectionJson as JsonArray
            possibleTypes.forEach {
                (it as JsonObject).get("name")?.asStringOrNull()?.let { connectionName:String ->
                    addTypeToProcess(connectionName)
                    connections.add(connectionName)
                }
            }
        }
        return connections
    }

    /**
     * Update the enum values
     */
    private fun updateEnumValues(typeKindJson: JsonObject, classInfo: ClassInfo) {
        val enumJson = typeKindJson.get("enumValues")
        if (enumJson.isNotNull()) {
            val possibleTypes = enumJson as JsonArray
            if (classInfo.enumValues == null) {
                classInfo.enumValues = mutableSetOf()
            }
            possibleTypes.forEach {
                val enumJsonObject = it as JsonObject
                classInfo.enumValues!!.add(
                    EnumValue(
                        enumJsonObject.get("name").asString, enumJsonObject.get("description").asStringOrNull()
                    )
                )
            }
        }
    }

    /**
     * Returns the fields if any
     */
    private fun getFields(typeKindJson: JsonObject, jsonName: String): MutableSet<FieldInfo>? {
        var fields: MutableSet<FieldInfo>? = null
        val fieldsJson = typeKindJson.get(jsonName)
        if (fieldsJson.isNotNull()) {
            val possibleTypes = fieldsJson as JsonArray
            fields = mutableSetOf()
            possibleTypes.forEach {
                val fieldJson = it as JsonObject
                val fieldName = fieldJson.get("name").asString
                val fieldDescription = fieldJson.get("description").asStringOrNull()
                val fieldDefault = fieldJson.get("defaultValue").asStringOrNull()
                val fieldType = getFieldType(fieldJson.getAsJsonObject("type"))
                val fieldInfo = FieldInfo(fieldName, fieldDescription, fieldType, fieldDefault)
                if (fieldJson.has("args")) {
                    fieldInfo.fieldArgs = getFields(fieldJson, "args")
                }
                fields.add(fieldInfo)
            }
        }
        return fields
    }

    /**
     * Return the discovered [ClassInfo] of the given name, adding it if needed.
     */
    @Synchronized
    private fun getClassInfo(className: String): ClassInfo = modelMap.getOrPut(className) {
        ClassInfo(className)
    }

    /**
     * Returns the query for the given type name
     */
    private fun getTypeQuery(typeName: String) = """
        {
          __type(name:"$typeName"){
            name
            kind
            description
            interfaces {
              name
            }
            
            inputFields {
              name
              description
              defaultValue
              type {
                ...typeInfo
              }
            }
                        
            fields {
              name
              description
              args {
                name
                defaultValue
                description
                type {
                    ...typeInfo
                }
              }      
              type {
                ...typeInfo
              }
            }
            possibleTypes {
              name
            }
            enumValues {
              name
              description
            }
            ofType {
              name
            }
          }
        }
        fragment typeInfo on __Type {
            name
            kind
            ofType {
              name
              kind
              ofType {
                name
                kind
                ofType {
                    name
                    kind
                }            
              }
            }
        }
        """.trimIndent().replace("\"", "\\\"").replace("\n", "\\n")
}