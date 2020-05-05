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

import com.google.gson.annotations.SerializedName
import com.squareup.kotlinpoet.*
import graphecule.client.model.ClassInfo
import graphecule.client.model.GraphModel
import java.io.File

/**
 * Builder for an enum class type
 */
internal class EnumClassBuilder(
    parentPackageName: String,
    outputLocation: File,
    classInfoToBuild: ClassInfo,
    graphModel: GraphModel
) : AbstractClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel) {

    override fun build() {
        buildEnum()
    }

    /**
     * Builds the enum
     */
    private fun buildEnum() {
        val classBuilder = TypeSpec
            .enumBuilder(classInfoToBuild.name)
            .addModifiers(KModifier.PUBLIC)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("itemName", String::class)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("itemName", String::class)
                    .initializer("itemName")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addFunction(
                FunSpec
                    .builder("toString")
                    .addModifiers(KModifier.PUBLIC)
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(String::class)
                    .addStatement("return itemName")
                    .build()
            )

        val description = classInfoToBuild.description
        if (description?.isNotBlank() == true) {
            classBuilder.addKdoc(description)
        }

        addEnumTypes(classBuilder)
        writeClass(classBuilder)
    }

    /**
     * Adds the enum constants
     */
    private fun addEnumTypes(classBuilder: TypeSpec.Builder) {
        classInfoToBuild.enumValues?.forEach {
            val enumItemBuilder = TypeSpec
                .anonymousClassBuilder()
                .addAnnotation(
                    AnnotationSpec
                        .builder(SerializedName::class)
                        .addMember("\"${it.name}\"")
                        .build()
                )
                .addSuperclassConstructorParameter("%S", it.name)
            val itemDescription = it.description
            if (itemDescription?.isNotEmpty() == true) {
                enumItemBuilder.addKdoc(itemDescription)
            }
            classBuilder.addEnumConstant(it.name.toUpperCase(), enumItemBuilder.build())
        }
    }
}