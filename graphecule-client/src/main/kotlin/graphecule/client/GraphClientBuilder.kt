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
package graphecule.client

import graphecule.client.builder.getClassBuilder
import graphecule.client.model.GraphModelBuilder
import graphecule.common.HttpRequestAdapter
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.Executors

/**
 * Generates client classes for the model represented by the given graphql server.
 *
 * Query and mutation classes will be generated to the given package location,
 * all dependant model and input classes will be generated in subpackages under it.
 *
 * Queries can be performed from the generated Query class using its inner Request class.
 * Mutation operations can be performed from the generated Mutation class using its inner Request class or
 * direct invoke methods in it
 *
 * (Actual name of Query and Mutation classes will depend on the names given at graphql server).
 *
 * To handle any authentication, specify a [httpRequestAdapter]
 *
 * @param apiHost The graphql host url
 * @param outputLocation The output location. Default to current directory
 * @param packageName Base package name for the generated client classes. Default to "graph"
 * @param httpRequestAdapter Provide a [HttpRequestAdapter] to handle any authentication or interception
 */
class GraphClientBuilder(
    private val apiHost: String,
    private val outputLocation: String = ".",
    private val packageName: String = "graph",
    private val httpRequestAdapter: HttpRequestAdapter? = null
) {


    /**
     * Builds the client classes.
     */
    suspend fun buildGraphClient() = withContext(Dispatchers.IO) {
        val clientModel = GraphModelBuilder(apiHost, httpRequestAdapter).buildGraphModel()
        val outputFolder = File(outputLocation)
        var classCount = 0

        val asyncExecutor = Executors.newFixedThreadPool(8)
        val asyncScope = asyncExecutor.asCoroutineDispatcher()
        val deferredList: MutableList<Deferred<Unit>> = mutableListOf()

        clientModel.classMap.values.forEach {
            logMessage("Saving ${it.name}")
            deferredList.add(async(context = asyncScope) { getClassBuilder(packageName, outputFolder, it, clientModel).build() })
            classCount++
        }
        deferredList.awaitAll()
        asyncScope.close()
        asyncExecutor.shutdownNow()

        logMessage("")
        println()
        println("Created $classCount classes at ${outputFolder.absolutePath}")
        println("Perform query using class $packageName.${clientModel.queryClass.name}")
        if (clientModel.mutationClass != null) {
            println("Perform mutation operations using class $packageName.${clientModel.mutationClass.name}")
        }
    }

    companion object {
        @JvmStatic
        fun main(arguments: Array<String>) {
            if (arguments.size != 2 && arguments.size != 1) {
                println("")
                println("")
                println("Usage java -jar Graphecule.jar <GRAPHQL_API_HOST> <PROPERTY_FILE>")
                println("")
                println("GRAPHQL_API_HOST      : Provides the URL to the GraphQl Api Server")
                println("PROPERTY_FILE         : Optional. Provide any additional parameters as needed. ")
                println("                        If file is not specified, by default looks for 'graphecule.properties'")
                println("")
                println("")
                println("Additional properties that can be provided through the above property file:")
                println("")
                println("Optional: OutputLocation : Specifies where to generate the client code. Default current directory")
                println("OutputLocation=./out/")
                println("")
                println("")
                println("Optional: PackageName : Specifies under which package to generate the client code. Default 'graph'")
                println("PackageName=graphqlhub.graph.api")
                println("")
                println("")
                println("Optional: RateLimit : Specifies any throttling limit between graphql requests in ms. Default no limit")
                println("RateLimit=500")
                println("")
                println("")
                println("Optional: MaxParallelRequests : Specifies max parallel requests that can be send to graphql server. Default 8")
                println("MaxParallelRequests=4")
                println("")
                println("")
                println("#Any other key=value pairs will be added as http headers")
                println("")
                println("#eg: To add a http header for authorization")
                println("authorization=Bearer XYZ")
                println("")
                println("")
                println("eg: java -jar Graphecule.jar https://www.graphqlhub.com/graphql")
            } else {
                val apiHost = arguments[0]
                val propertyFileArgument = if (arguments.size == 2) arguments[2] else "graphecule.properties"
                val properties = Properties()
                val propertyFile = File(propertyFileArgument)
                try {
                    if (propertyFile.exists()) {
                        properties.load(FileInputStream(propertyFile))
                    }
                } catch (exception: Exception) {
                    println("Error while reading property file ${exception.message}")
                }

                val httpRequestHeaders = mutableMapOf<String, String>()
                var outputLocation: String? = null
                var packageName: String? = null
                var rateLimit: Long? = null
                var maxParallel: Int? = null

                properties.forEach { key, value ->
                    when (key.toString()) {
                        "OutputLocation" -> outputLocation = value?.toString()
                        "PackageName" -> packageName = value?.toString()
                        "RateLimit" -> rateLimit = value?.toString()?.toLongOrNull()
                        "MaxParallelRequests" -> maxParallel = value?.toString()?.toIntOrNull()
                        else -> {
                            httpRequestHeaders[key.toString()] = value?.toString() ?: ""
                        }
                    }
                }

                val httpRequestAdapter: HttpRequestAdapter? = if (rateLimit != null || httpRequestHeaders.isNotEmpty()) {
                    HttpRequestAdapter(httpRequestHeaders, rateLimit ?: 0, maxParallel ?: 8)
                } else {
                    null
                }

                runBlocking {
                    GraphClientBuilder(
                        apiHost,
                        outputLocation ?: ".",
                        packageName ?: "graph",
                        httpRequestAdapter
                    ).buildGraphClient()
                }

            }
        }
    }
}