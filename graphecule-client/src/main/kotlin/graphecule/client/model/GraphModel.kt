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


/**
 * Represents the graph model
 *
 * @param apiHost The Graphql api host address
 * @param queryClass [ClassInfo] fpr the query class
 * @param mutationClass [ClassInfo] for the mutation class if any
 * @param classMap Map of all the classes discovered
 * @param classNameCounts Map of count of each classes in a case sensitive manner
 */
internal data class GraphModel(
    val apiHost: String,
    val queryClass: ClassInfo,
    val mutationClass: ClassInfo?,
    val classMap: Map<String, ClassInfo>,
    val classNameCounts: Map<String, Int>
)


/**
 * Represent a type
 */
internal enum class TypeKind {
    INT,
    FLOAT,
    STRING,
    BOOLEAN,
    ID,
    LIST,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT_OBJECT
}

/**
 * Represents a field type
 *
 * @param name Name of the field type
 * @param kind The [TypeKind] of the field
 * @param isNullable Whether this field is nullable
 * @param subType Subtype of the field if any (for list types)
 */
internal data class FieldType(
    val name: String,
    val kind: TypeKind,
    val isNullable: Boolean = true,
    var subType: FieldType? = null
)

/**
 * Represent information of a field
 *
 * @param name Name of the field
 * @param description Description if any
 * @param fieldType [FieldType] type information
 * @param defaultValue Default value if any
 * @param fieldArgs Set of arguments for this field query
 */
internal data class FieldInfo(
    val name: String,
    val description: String?,
    val fieldType: FieldType,
    var defaultValue: String? = null,
    var fieldArgs: Set<FieldInfo>? = null
)

/**
 * Represents an enum value
 */
internal data class EnumValue(
    val name: String,
    val description: String?
)

/**
 * Represents info about a discovered class type
 *
 * @param name Name of the class
 * @param typeKind [TypeKind] of this class
 * @param description Description if any
 * @param childClasses Set of child class names if any
 * @param fields Set of [FieldInfo] for fields of this class
 * @param enumValues set of [EnumValue] if this is a enum type
 * @param queryArgs Set of query args if any
 */
internal data class ClassInfo(
    val name: String,
    var typeKind: TypeKind = TypeKind.OBJECT,
    var description: String? = null,
    var childClasses: MutableSet<String>? = null,
    var fields: MutableSet<FieldInfo>? = null,
    var enumValues: MutableSet<EnumValue>? = null,
    var queryArgs: MutableSet<FieldInfo>? = null
) {
    var parentClasses: MutableSet<String>? = null
        private set

    @Synchronized
    fun addParent(parent: String) {
        if (parentClasses == null) {
            parentClasses = mutableSetOf()
        }
        parentClasses!!.add(parent)
    }
}