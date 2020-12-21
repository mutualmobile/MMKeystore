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

package com.mutualmobile.androidkeystore.android.crypto

import android.os.Build
import com.mutualmobile.androidkeystore.android.crypto.ciper.CipherJB
import com.mutualmobile.androidkeystore.android.crypto.ciper.CipherLegacy
import com.mutualmobile.androidkeystore.android.crypto.ciper.CipherMM
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException

/**
 * Return an appropriate [Cipher] given the version of Android.
 * Ex: on API 23 OpenSSL is replaced by BoringSSL.
 */
@Deprecated("Use Android Keystore instead")
object CipherFactory {

  private val IS_JB43 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
  private val IS_MM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
  private val IS_GINGERBREAD = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD

  @Throws(NoSuchPaddingException::class, NoSuchAlgorithmException::class,
      NoSuchProviderException::class)
  fun get(): Cipher {
    return if (IS_MM) {
      CipherMM.get()
    } else if (IS_JB43) {
      CipherJB.get()
    } else if (IS_GINGERBREAD) {
      CipherLegacy.get()
    } else {
      throw IllegalArgumentException("Not supported yet")
    }
  }
}
