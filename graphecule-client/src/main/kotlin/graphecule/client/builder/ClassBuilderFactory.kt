package graphecule.client.builder

import graphecule.client.model.ClassInfo
import graphecule.client.model.GraphModel
import graphecule.client.model.TypeKind
import java.io.File


/**
 * Returns an appropriate [FileBuilder] that can build the given [classInfoToBuild]
 */
internal fun getClassBuilder(
    parentPackageName: String,
    outputLocation: File,
    classInfoToBuild: ClassInfo,
    graphModel: GraphModel
): FileBuilder =
    when (classInfoToBuild.typeKind) {
        TypeKind.ENUM -> EnumClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel)
        TypeKind.INPUT_OBJECT -> InputClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel)
        else -> GraphClassBuilder(parentPackageName, outputLocation, classInfoToBuild, graphModel)
    }