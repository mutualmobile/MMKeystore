/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mutualmobile.androidkeystore.android.crypto.misc

/**
 * Base64 helper methods.
 */
object Base64 {
  fun to(bytes: ByteArray): String {
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
  }

  fun from(base64: String): ByteArray {
    return android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
  }

}
