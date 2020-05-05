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
 * A ClassBuilder for classes that has fields that can be queried/set
 */
internal class GraphClassBuilder(
    parentPackageName: String,
    outputLocation: File,
    classInfoToBuild: ClassInfo,
    graphModel: GraphModel
) : AbstractClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel) {

    override fun build() {
        buildClass()
    }

    /**
     * Builds the class, adds fields, inner classes as needed
     */
    private fun buildClass() {
        val classBuilder = TypeSpec
            .classBuilder(classInfoToBuild.name)
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OPEN)
        val description = classInfoToBuild.description
        if (description?.isNotBlank() == true) {
            classBuilder.addKdoc(description)
        }

        if (classInfoToBuild == graphModel.mutationClass) {
            classBuilder.addKdoc("\n\nTo run multiple mutations together, build it using [${classInfoToBuild.name}$innerRequestClassNameSuffix]")
            classBuilder.addKdoc("\nTo invoke a single mutation, call directly any of the \"invoke\" methods")
        } else if (classInfoToBuild == graphModel.queryClass) {
            classBuilder.addKdoc("\n\nTo fetch the fields, build the request using [${classInfoToBuild.name}$innerRequestClassNameSuffix]")
        }

        addFields(classBuilder)

        if (!classInfoToBuild.childClasses.isNullOrEmpty()) {
            writeWrapperInterface()
            classBuilder.addSuperinterface(ClassName(getPackageNameForClass(classInfoToBuild.name), "${classInfoToBuild.name}$wrapperInterfaceSuffix"))
        }

        classInfoToBuild.parentClasses?.forEach {
            classBuilder.addSuperinterface(ClassName(getPackageNameForClass(it), "$it$wrapperInterfaceSuffix"))
        }

        GraphRequestClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel, classBuilder)
            .build()

        writeClass(classBuilder)
    }

    /**
     * Create wrapper interfaces as needed if this is extended by any classes
     */
    private fun writeWrapperInterface() {
        val packageName = getPackageNameForClass(classInfoToBuild.name)
        val fileSpecBuilder = FileSpec.builder(packageName, "${classInfoToBuild.name}$wrapperInterfaceSuffix")
        val classBuilder = TypeSpec
            .interfaceBuilder("${classInfoToBuild.name}$wrapperInterfaceSuffix")
            .addModifiers(KModifier.PUBLIC)

        val description = classInfoToBuild.description
        if (description?.isNotBlank() == true) {
            classBuilder.addKdoc(description)
        }
        classBuilder.addKdoc("\n\n")
        classBuilder.addKdoc("@see ${classInfoToBuild.name} \n")
        classInfoToBuild.childClasses?.forEach {
            classBuilder.addKdoc("@see $it \n")
        }
        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(outputLocation)
    }

    /**
     * Adds fields to the class
     */
    private fun addFields(classBuilder: TypeSpec.Builder) {
        classInfoToBuild.fields?.forEach { fieldInfo ->

            val propertyBuilder = PropertySpec
                .builder(fieldInfo.name, getType(fieldInfo.fieldType))
                .addModifiers(KModifier.PUBLIC)
                .mutable(true)
            if (fieldInfo.description?.isNotBlank() == true) {
                propertyBuilder.addKdoc(fieldInfo.description)
            }

            if (fieldInfo.fieldType.isNullable) {
                propertyBuilder.initializer("null")
            } else {
                when (fieldInfo.fieldType.kind) {
                    TypeKind.INT -> propertyBuilder.initializer("0")
                    TypeKind.FLOAT -> propertyBuilder.initializer("0.0")
                    TypeKind.BOOLEAN -> propertyBuilder.initializer("false")

                    TypeKind.STRING, TypeKind.ID, TypeKind.LIST,
                    TypeKind.OBJECT, TypeKind.INTERFACE,
                    TypeKind.UNION, TypeKind.ENUM,
                    TypeKind.INPUT_OBJECT -> {
                        propertyBuilder.addModifiers(KModifier.LATEINIT)
                    }
                }
            }
            classBuilder.addProperty(propertyBuilder.build())
        }
    }
}