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
import graphecule.client.model.GraphModel
import graphecule.client.model.TypeKind
import java.io.File

/**
 * Builder an InputClass type.
 * It wont have any inner Request/Builder classes, and will have constructor
 */
internal class InputClassBuilder(
    parentPackageName: String,
    outputLocation: File,
    classInfoToBuild: ClassInfo,
    graphModel: GraphModel
) : AbstractClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel) {

    override fun build() {
        buildInputClass()
    }

    /**
     * Builds the input class
     */
    private fun buildInputClass() {
        val classBuilder = TypeSpec
            .classBuilder(classInfoToBuild.name)
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OPEN)
        val description = classInfoToBuild.description
        if (description?.isNotBlank() == true) {
            classBuilder.addKdoc(description)
        }

        val constructorBuilder = FunSpec
            .constructorBuilder()
        val toStringBuilder = FunSpec
            .builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("val message = %T(\" { \")", StringBuilder::class)

        addConstructorParameters(classBuilder, constructorBuilder, toStringBuilder)
        classBuilder.primaryConstructor(constructorBuilder.build())

        toStringBuilder.addStatement("message.append(\" } \")")
        toStringBuilder.addStatement("return message.toString()")

        classBuilder.addFunction(toStringBuilder.build())
        writeClass(classBuilder)
    }

    /**
     * Adds the constructor parameters for this input class
     */
    private fun addConstructorParameters(
        classBuilder: TypeSpec.Builder,
        constructorBuilder: FunSpec.Builder,
        toStringBuilder: FunSpec.Builder
    ) {
        classInfoToBuild.queryArgs?.forEach { fieldInfo ->

            if (fieldInfo.fieldType.isNullable) {
                toStringBuilder.beginControlFlow("if (${escapeIfKeyword(fieldInfo.name)} != null) ")
            }
            when (fieldInfo.fieldType.kind) {
                TypeKind.STRING, TypeKind.ID -> {
                    toStringBuilder.addStatement("message.append(\"·${fieldInfo.name}·:·\\\"\${${escapeIfKeyword(fieldInfo.name)}.toString()}\\\"·\")")
                }
                else -> {
                    toStringBuilder.addStatement("message.append(\"·${fieldInfo.name}·:·\${${escapeIfKeyword(fieldInfo.name)}.toString()}·\")")
                }
            }
            if (fieldInfo.fieldType.isNullable) {
                toStringBuilder.endControlFlow()
            }

            val paramBuilder = ParameterSpec
                .builder(fieldInfo.name, getType(fieldInfo.fieldType))
                .addModifiers(KModifier.PUBLIC)

            if (fieldInfo.fieldType.isNullable) {
                paramBuilder.defaultValue("null")
            }

            val propertyBuilder = PropertySpec
                .builder(fieldInfo.name, getType(fieldInfo.fieldType))
                .addModifiers(KModifier.PUBLIC)
                .mutable(true)
                .initializer(fieldInfo.name)

            if (fieldInfo.description?.isNotBlank() == true) {
                propertyBuilder.addKdoc(fieldInfo.description)
            }

            constructorBuilder.addParameter(paramBuilder.build())
            classBuilder.addProperty(propertyBuilder.build())
        }
    }
}