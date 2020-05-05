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
package graphecule.client.builder

import com.google.gson.GsonBuilder
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import graphecule.client.model.ClassInfo
import graphecule.client.model.GraphModel
import graphecule.common.RequestSender
import graphecule.common.HttpRequestAdapter
import graphecule.common.gson.RuntimeTypeAdapterFactory
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Adds extras for the source query/mutation classes
 */
internal class GraphRequestSourceClassBuilder(
    parentPackageName: String,
    outputLocation: File,
    classInfoToBuild: ClassInfo,
    graphModel: GraphModel,
    private val classBuilder: TypeSpec.Builder
) : AbstractClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel) {


    override fun build() {
        writeSourceClassExtras()
    }

    /**
     * Adds the source extra methods
     */
    private fun writeSourceClassExtras() {
        classBuilder.addProperty(
            PropertySpec
                .builder("apiHost", String::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("\"${graphModel.apiHost}\"")
                .build()
        ).addProperty(
            PropertySpec
                .builder("errorMessages", List::class.parameterizedBy(String::class).copy(nullable = true))
                .mutable(true)
                .addModifiers(KModifier.PUBLIC)
                .initializer("null")
                .setter(FunSpec.setterBuilder().addModifiers(KModifier.PRIVATE).build())
                .build()
        )

        val sendMessageMethodName = if (classInfoToBuild == graphModel.mutationClass) "invoke"
        else "sendRequest"

        val sendMessageOperationName = if (classInfoToBuild == graphModel.mutationClass) " mutation ${classInfoToBuild.name} "
        else ""

        val sendReqBuilder = FunSpec
            .builder(sendMessageMethodName)
            .addModifiers(KModifier.SUSPEND)
            .returns(ClassName("", classInfoToBuild.name))
            .addKdoc("Sends this [${classInfoToBuild.name}$innerRequestClassNameSuffix], returns [${classInfoToBuild.name}] if the request is successful.\n\n")
            .addKdoc("May throw [RuntimeException] if request failed.\n")
            .addKdoc("Any error messages returned by server can be obtained from [errorMessages]\n\n")
            .addKdoc("@param httpRequestAdapter Optionally specify [HttpRequestAdapter] to handle authentication or request interception")
            .addParameter(
                ParameterSpec
                    .builder("httpRequestAdapter", HttpRequestAdapter::class.asTypeName().copy(true))
                    .defaultValue("null")
                    .build()
            )
            .beginControlFlow(
                "return kotlinx.coroutines.withContext(%T.IO)",
                Dispatchers::class
            )

            .addStatement("val req = requestString.replace(\"\\\"\", \"\\\\\\\"\").replace(\"\\n\", \"\\\\n\")")
            .addStatement("val·queryString·=·\"{\\\"query\\\"·:·\\\"$sendMessageOperationName\$req\\\"}\"")
            .addStatement("val requestSender = %T(apiHost, httpRequestAdapter)", RequestSender::class)
            .addStatement("val response = requestSender.sendRequest(queryString)")
            .addStatement(
                "val resultJson = %T.parseString(response) as %T",
                JsonParser::class,
                JsonObject::class
            )
            .beginControlFlow("if (resultJson.has(\"errors\"))")
            .addStatement("val errors = resultJson.getAsJsonArray(\"errors\")")
            .addStatement("val messages = mutableListOf<String>()")
            .beginControlFlow("errors?.forEach")
            .addStatement("val msg = (it as %T).get(\"message\")", JsonObject::class)
            .beginControlFlow("if (msg !is %T)", JsonNull::class)
            .addStatement("messages.add(msg.asString)")
            .endControlFlow()
            .endControlFlow()
            .addStatement("errorMessages = messages")
            .endControlFlow()

            .beginControlFlow("if (resultJson.has(\"data\"))")
            .addStatement("val gsonBuilder = %T()", GsonBuilder::class)

        graphModel.classMap.values.forEachIndexed { index, classInfo ->
            if (classInfo.childClasses?.isNotEmpty() == true) {
                sendReqBuilder.addStatement("val runtimeAdapter$index = %T.of(%T::class.java, \"__typename\")",
                    RuntimeTypeAdapterFactory::class,
                    ClassName(getPackageNameForClass(classInfo.name), "${classInfo.name}$wrapperInterfaceSuffix")
                )

                classInfo.childClasses?.forEach {
                    sendReqBuilder.addStatement("runtimeAdapter$index.registerSubtype(%T::class.java)", ClassName(getPackageNameForClass(it), it)
                    )
                }

                sendReqBuilder.addStatement("gsonBuilder.registerTypeAdapterFactory(runtimeAdapter$index)")
            }
        }


        sendReqBuilder
            .addStatement("gsonBuilder.create().fromJson(resultJson.getAsJsonObject(\"data\").toString(), ${classInfoToBuild.name}::class.java)")
            .endControlFlow()
            .beginControlFlow("else")
            .addStatement("val errorMessage = \"Failed to get response, check request.errorMessages for more details\"")
            .addStatement("throw %T(errorMessage)", RuntimeException::class)
            .endControlFlow()
            .endControlFlow()
            .build()

        classBuilder.addFunction(sendReqBuilder.build())
    }

}