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

import com.squareup.kotlinpoet.*
import graphecule.client.model.ClassInfo
import graphecule.client.model.FieldInfo
import graphecule.client.model.GraphModel
import graphecule.client.model.TypeKind
import graphecule.common.HttpRequestAdapter
import java.io.File

/**
 * Builder for inner Request.Builder
 */
internal class GraphRequestBuilderClassBuilder(
    parentPackageName: String,
    outputLocation: File,
    classInfoToBuild: ClassInfo,
    graphModel: GraphModel,
    private val classBuilder: TypeSpec.Builder,
    private val outerClassBuilder: TypeSpec.Builder
) : AbstractClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel) {


    override fun build() {
        writeInnerGraphRequestBuilder()
    }

    /**
     * Builds the inner Builder and its fetch/invoke methods
     */
    private fun writeInnerGraphRequestBuilder() {
        val messageSuffix = if (classInfoToBuild == graphModel.mutationClass) "Use any of the \"invoke\" methods to specify which operations needs to be performed, and "
        else "Use any of the \"fetch\" methods to specify which parameters needs to be fetched, and "

        var companionBuilder: TypeSpec.Builder? = null
        if (classInfoToBuild == graphModel.mutationClass) {
            companionBuilder = TypeSpec.companionObjectBuilder()
        }

        val requestBuilder = TypeSpec.classBuilder("Builder")
            .addKdoc("Builder class to build [${classInfoToBuild.name}.${classInfoToBuild.name}$innerRequestClassNameSuffix].\n\n")
            .addKdoc(messageSuffix)
            .addKdoc(" use [build] method to build an instance of [${classInfoToBuild.name}.${classInfoToBuild.name}$innerRequestClassNameSuffix]")
            .addModifiers(KModifier.PUBLIC)
            .addProperty(
                PropertySpec
                    .builder("fieldsRequested", StringBuilder::class)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("StringBuilder(\" { __typename \")")
                    .build()
            )

        val message2 = if (classInfoToBuild == graphModel.mutationClass) "All operations that marked to be performed by calling the \"invoke\" methods\n"
        else "All fields that marked to be fetched by calling the \"fetch\" methods\n"

        val message3 = if (classInfoToBuild == graphModel.mutationClass) "performed"
        else "invoked"

        requestBuilder.addFunction(
            FunSpec.builder("build")
                .addKdoc("Returns an instance of [${classInfoToBuild.name}.${classInfoToBuild.name}$innerRequestClassNameSuffix].\n\n")
                .addKdoc(message2)
                .addKdoc(" on this [Builder] instance can be $message3 using the returned [${classInfoToBuild.name}.${classInfoToBuild.name}$innerRequestClassNameSuffix]")
                .returns(ClassName("", "${classInfoToBuild.name}$innerRequestClassNameSuffix"))
                .addStatement("fieldsRequested.append(\" } \")")
                .addStatement("val requestString = fieldsRequested.toString()")
                .addStatement("fieldsRequested.clear()")
                .addStatement("return·${classInfoToBuild.name}$innerRequestClassNameSuffix(requestString)").build()
        )

        addFetchMethods(requestBuilder, companionBuilder)

        classBuilder.addType(requestBuilder.build())

        if (companionBuilder != null) {
            outerClassBuilder.addType(companionBuilder.build())
        }
    }

    /**
     * Add all the fetch methods
     */
    private fun addFetchMethods(classBuilder: TypeSpec.Builder, companionBuilder: TypeSpec.Builder?) {
        val methodName = if (classInfoToBuild == graphModel.mutationClass) "invoke"
        else "fetch"

        classInfoToBuild.fields?.forEach { fieldInfo ->
            val fetchName = fieldInfo.name
            var fieldEffectiveType = fieldInfo.fieldType
            if (fieldEffectiveType.kind == TypeKind.LIST) {
                fieldEffectiveType = fieldInfo.fieldType.subType!!
            }

            if (fieldEffectiveType.kind != TypeKind.UNION) {
                addFetchMethod(
                    classBuilder, companionBuilder, "$methodName${fetchName.capitalize()}",
                    fieldEffectiveType.name, fieldInfo, false
                )
            }
            if (fieldEffectiveType.kind in arrayOf(TypeKind.UNION, TypeKind.INTERFACE)) {
                graphModel.classMap[fieldEffectiveType.name]?.childClasses?.forEach {
                    addFetchMethod(
                        classBuilder, companionBuilder, "$methodName${fetchName.capitalize()}As$it",
                        it, fieldInfo, true
                    )
                }
            }
        }
    }

    /**
     * Adds the fetch method for the given name
     */
    private fun addFetchMethod(classBuilder: TypeSpec.Builder, companionBuilder: TypeSpec.Builder?, fetchName: String, fetchAs: String, fieldInfo: FieldInfo, isExtensionFetch: Boolean) {
        val fetchBuilder = FunSpec
            .builder(fetchName)
            .addKdoc("Returns a [Builder] that can build a [${classInfoToBuild.name}$innerRequestClassNameSuffix] that fetches the field [${fieldInfo.name}] as [$fetchAs]\n")
            .returns(ClassName("", "Builder"))

        val companionCallParamList = if (companionBuilder != null) StringBuilder() else null
        val companionFetchBuilder = if (companionBuilder != null) FunSpec.builder(fetchName).addModifiers(KModifier.SUSPEND) else null
        companionFetchBuilder?.addKdoc("Invoke the mutation operation \"${fieldInfo.name}\"\n")?.returns(getType(fieldInfo.fieldType))
        var fieldEffectiveType = fieldInfo.fieldType.kind
        if (fieldEffectiveType == TypeKind.LIST) {
            fieldEffectiveType = fieldInfo.fieldType.subType!!.kind
        }
        var requestBuilderAdded = false
        if (fieldEffectiveType in arrayOf(TypeKind.OBJECT, TypeKind.INTERFACE, TypeKind.UNION)) {
            requestBuilderAdded = true
            fetchBuilder.addParameter("request", ClassName("", "${getPackageNameForClass(fetchAs)}.${fetchAs}.${fetchAs}$innerRequestClassNameSuffix"))
            fetchBuilder.addKdoc(
                "@param request [$fetchAs.$fetchAs$innerRequestClassNameSuffix] that specifies which fields should be fetched from [%T]. \n",
                ClassName(getPackageNameForClass(fetchAs), fetchAs)
            )
            companionFetchBuilder?.addParameter("request", ClassName("", "${getPackageNameForClass(fetchAs)}.${fetchAs}.${fetchAs}$innerRequestClassNameSuffix"))
                ?.addKdoc(
                    "@param request [$fetchAs.$fetchAs$innerRequestClassNameSuffix] that specifies which fields should be fetched from the result [%T]. \n",
                    ClassName(getPackageNameForClass(fetchAs), fetchAs)
                )
            companionCallParamList?.append("request")
        }


        fieldInfo.fieldArgs?.forEachIndexed { index, argInfo ->
            if (index == 0) {
                fetchBuilder.addStatement("val argsRequested = StringBuilder()")
            }

            if (!companionCallParamList.isNullOrEmpty()) {
                companionCallParamList.append(", ")
            }
            companionCallParamList?.append(argInfo.name)
            val argBuilder = ParameterSpec.builder(argInfo.name, getType(argInfo.fieldType))
            val companionArgBuilder = if (companionFetchBuilder != null) ParameterSpec.builder(argInfo.name, getType(argInfo.fieldType)) else null

            if (argInfo.fieldType.isNullable) {
                fetchBuilder.beginControlFlow("if (${escapeIfKeyword(argInfo.name)} != null )")
            }
            fetchBuilder.addStatement("argsRequested.append(\"${argInfo.name}·:·\")")

            if (!argInfo.defaultValue.isNullOrEmpty()) {
                when (argInfo.fieldType.kind) {
                    TypeKind.STRING, TypeKind.ID -> {
                        argBuilder.defaultValue("%S", argInfo.defaultValue)
                        companionArgBuilder?.defaultValue("%S", argInfo.defaultValue)
                    }
                    TypeKind.INT, TypeKind.FLOAT,
                    TypeKind.BOOLEAN -> {
                        argBuilder.defaultValue(argInfo.defaultValue!!)
                        companionArgBuilder?.defaultValue(argInfo.defaultValue!!)
                    }
                    TypeKind.ENUM -> {
                        argBuilder.defaultValue("${argInfo.fieldType.name}.${argInfo.defaultValue?.toUpperCase()?.removeQuotes()}")
                        companionArgBuilder?.defaultValue("${argInfo.fieldType.name}.${argInfo.defaultValue?.toUpperCase()?.removeQuotes()}")
                    }
                    else -> {
                        //println("Ignoring default for arg ${argInfo.name} ${argInfo.defaultValue}")
                    }
                }
            } else if (argInfo.fieldType.isNullable) {
                argBuilder.defaultValue("null")
                companionArgBuilder?.defaultValue("null")
            }


            when (argInfo.fieldType.kind) {
                TypeKind.STRING, TypeKind.ID -> {
                    fetchBuilder.addStatement("argsRequested.append(\"\\\"\${${escapeIfKeyword(argInfo.name)}.toString()}\\\"\")·")
                }
                else -> {
                    fetchBuilder.addStatement("argsRequested.append(${escapeIfKeyword(argInfo.name)}.toString())")
                }
            }

            fetchBuilder.addStatement("argsRequested.append(\" \")")

            if (argInfo.fieldType.isNullable) {
                fetchBuilder.endControlFlow()
            }

            fetchBuilder.addParameter(argBuilder.build())
            companionFetchBuilder?.addParameter(companionArgBuilder!!.build())

            if (!argInfo.description.isNullOrEmpty()) {
                fetchBuilder.addKdoc("@param ${argInfo.name} %S\n", argInfo.description)
                companionFetchBuilder?.addKdoc("@param ${argInfo.name} %S\n", argInfo.description)
            }
        }

        if (!fieldInfo.fieldArgs.isNullOrEmpty()) {
            fetchBuilder.beginControlFlow("val argString = if (argsRequested.isNotEmpty())")
            fetchBuilder.addStatement("\" ( \${argsRequested.toString()} ) \"")
            fetchBuilder.endControlFlow()
            fetchBuilder.beginControlFlow("else")
            fetchBuilder.addStatement("\"\"")
            fetchBuilder.endControlFlow()
            fetchBuilder.addStatement("fieldsRequested.append(\"·${fieldInfo.name}·\$argString\")")
        } else {
            fetchBuilder.addStatement("fieldsRequested.append(\"·${fieldInfo.name}·\")")
        }

        if (requestBuilderAdded) {
            if (isExtensionFetch) {
                fetchBuilder.addStatement("fieldsRequested.append(\"·{·...·on·$fetchAs·\")")
            }
            fetchBuilder.addStatement("fieldsRequested.append(request.requestString)")
            if (isExtensionFetch) {
                fetchBuilder.addStatement("fieldsRequested.append(\" } \")")
            }
        }

        fetchBuilder.addStatement("return this")
        classBuilder.addFunction(fetchBuilder.build())
        companionBuilder?.let {
            companionFetchBuilder?.addParameter(
                ParameterSpec.builder("httpRequestAdapter", HttpRequestAdapter::class.asTypeName().copy(true))
                    .defaultValue("null")
                    .build()
            )
                ?.addStatement("return·${classInfoToBuild.name}$innerRequestClassNameSuffix.Builder().$fetchName(${companionCallParamList?.toString()}).build().invoke(httpRequestAdapter).${fieldInfo.name}")

            it.addFunction(companionFetchBuilder!!.build())
        }
    }
}