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

import com.google.gson.JsonElement
import com.google.gson.JsonNull

fun JsonElement?.asSafeString(): String =
    if (this == null || this is JsonNull) {
        ""
    } else {
        this.asString
    }

fun JsonElement?.asStringOrNull(): String? =
    if (this == null || this is JsonNull) {
        null
    } else {
        this.asString
    }

fun JsonElement?.isNotNull(): Boolean =
    !(this == null || this is JsonNull)