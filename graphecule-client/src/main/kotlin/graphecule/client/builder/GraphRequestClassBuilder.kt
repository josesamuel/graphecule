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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphecule.client.model.ClassInfo
import graphecule.client.model.GraphModel
import java.io.File

/**
 * Builder for inner Request class
 */
internal class GraphRequestClassBuilder(
    parentPackageName: String,
    outputLocation: File,
    classInfoToBuild: ClassInfo,
    graphModel: GraphModel,
    private val classBuilder: TypeSpec.Builder
) : AbstractClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel) {

    override fun build() {
        writeInnerGraphRequest()
    }

    /**
     * Build the inner Request class
     */
    private fun writeInnerGraphRequest() {

        val messageSuffix = if (classInfoToBuild == graphModel.mutationClass) " from the result of invoking a mutation operation"
        else "in a query."
        val message2 = when (classInfoToBuild) {
            graphModel.mutationClass -> "\n\nCall [invoke] to perform the operations"
            graphModel.queryClass -> "\n\nCall [sendRequest] to perform the operations"
            else -> ""
        }

        val requestBuilder = TypeSpec.classBuilder("${classInfoToBuild.name}$innerRequestClassNameSuffix")
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Specifies which parameters of [${classInfoToBuild.name}] needs to be fetched $messageSuffix.\n\n")
            .addKdoc("Use [${classInfoToBuild.name}.${classInfoToBuild.name}$innerRequestClassNameSuffix.Builder] to build an instance of this.\n")
            .addKdoc(message2)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("requestString", String::class).build()
            )
            .addProperty(
                PropertySpec
                    .builder("requestString", String::class)
                    .initializer("requestString")
                    .build()
            )

        GraphRequestBuilderClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel, requestBuilder, classBuilder)
            .build()
        if (classInfoToBuild == graphModel.queryClass || classInfoToBuild == graphModel.mutationClass) {
            GraphRequestSourceClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel, requestBuilder)
                .build()
        }

        classBuilder.addType(requestBuilder.build())
    }
}