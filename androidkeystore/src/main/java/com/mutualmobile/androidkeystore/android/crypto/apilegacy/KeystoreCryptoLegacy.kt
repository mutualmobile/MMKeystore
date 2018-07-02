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

package com.mutualmobile.androidkeystore.android.crypto.apilegacy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.LocalSocket
import android.net.LocalSocketAddress
import com.mutualmobile.androidkeystore.android.crypto.CipherFactory
import com.mutualmobile.androidkeystore.android.crypto.KeystoreCrypto
import com.mutualmobile.androidkeystore.android.crypto.apilegacy.KeystoreCryptoLegacy.State.UNLOCKED
import com.mutualmobile.androidkeystore.android.crypto.misc.Base64
import com.mutualmobile.androidkeystore.android.crypto.misc.PRNGFixes
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.GeneralSecurityException
import java.security.KeyStoreException
import java.security.SecureRandom
import java.util.ArrayList
import java.util.Enumeration
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Implements [KeystoreCrypto] methods for API 9 to 18 (pre Android KeyStore public API).
 */
class KeystoreCryptoLegacy @Throws(KeyStoreException::class)
constructor(private val context: Context) :
    KeystoreCrypto {
  private var mError = NO_ERROR
  private val random = SecureRandom()

  // States
  private enum class State {
    UNLOCKED, LOCKED, UNINITIALIZED
  }

  init {
    PRNGFixes.apply()
  }

  @Throws(KeyStoreException::class)
  override fun encrypt(alias: String, plainText: String): String {
    try {
      val cipher = CipherFactory.get()

      val iv = generateIv(cipher.blockSize)
      val ivParams = IvParameterSpec(iv)

      val key = SecretKeySpec(get(alias), "AES")

      cipher.init(Cipher.ENCRYPT_MODE, key, ivParams)
      val cipherText = cipher.doFinal(plainText.toByteArray(charset("UTF-8")))

      return String.format("%s%s%s", Base64.to(iv),
          DELIMITER,
          Base64.to(cipherText))
    } catch (e: GeneralSecurityException) {
      throw KeyStoreException(e)
    } catch (e: UnsupportedEncodingException) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  override fun getAliases(): Enumeration<String>? {
    return null
  }

  @Throws(KeyStoreException::class)
  override fun deleteEntry(alias: String) {

  }

  @Throws(KeyStoreException::class)
  override fun decrypt(alias: String, cipherText: String): String? {
    val keyBytes = get(alias) ?: return null
    val key = SecretKeySpec(keyBytes, "AES")

    try {
      val fields = cipherText.split(
          DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      if (fields.size != 2) {
        throw IllegalArgumentException("Invalid encrypted text format")
      }

      val iv = Base64.from(fields[0])
      val cipherBytes = Base64.from(fields[1])
      val cipher = CipherFactory.get()
      val ivParams = IvParameterSpec(iv)
      cipher.init(Cipher.DECRYPT_MODE, key, ivParams)
      val plaintext = cipher.doFinal(cipherBytes)
      return String(plaintext, charset("UTF-8"))
    } catch (e: GeneralSecurityException) {
      throw KeyStoreException(e)
    } catch (e: UnsupportedEncodingException) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  override fun is_keystore_unlocked(): Boolean {
    return state() == UNLOCKED
  }

  @Throws(KeyStoreException::class)
  override fun unlock_keystore() {
    try {
      val intent = Intent(
          UNLOCK_ACTION)
      intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  override fun create_key_if_not_available(alias: String) {
    if (get(alias) == null) {
      try {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(
            KEY_LENGTH)
        val key = kg.generateKey()

        val success = put(
            getBytes(
                alias), key.encoded)
        if (!success) {
          throw KeyStoreException("Keystore error")
        }
      } catch (e: Exception) {
        throw KeyStoreException(e)
      }

    }
  }

  @Throws(KeyStoreException::class)
  private fun state(): State {
    execute('t'.toString().toInt())
    when (mError) {
      NO_ERROR -> return UNLOCKED
      LOCKED -> return State.LOCKED
      UNINITIALIZED -> return State.UNINITIALIZED
      else -> throw KeyStoreException("" + mError)
    }
  }

  private operator fun get(key: ByteArray): ByteArray? {
    val values = execute('g'.toString().toInt(), key)
    return if (values == null || values.isEmpty()) null else values[0]
  }

  private operator fun get(key: String): ByteArray? {
    return get(
        getBytes(
            key))
  }

  private fun put(key: ByteArray, value: ByteArray): Boolean {
    execute('i'.toString().toInt(), key, value)
    return mError == NO_ERROR
  }

  private fun execute(code: Int, vararg parameters: ByteArray): ArrayList<ByteArray>? {
    var code = code
    mError = PROTOCOL_ERROR

    for (parameter in parameters) {
      if (parameter == null || parameter.size > 65535) {
        return null
      }
    }

    val socket = LocalSocket()
    try {
      socket.connect(
          sAddress)

      val out = socket.outputStream
      out.write(code)
      for (parameter in parameters) {
        out.write(parameter.size shr 8)
        out.write(parameter.size)
        out.write(parameter)
      }
      out.flush()
      socket.shutdownOutput()

      val `in` = socket.inputStream
      code = `in`.read()
      if (code != NO_ERROR) {
        if (code != -1) {
          mError = code
        }
        return null
      }

      val values = ArrayList<ByteArray>()
      while (true) {
        var i: Int
        var j: Int
        i = `in`.read()
        if (i == -1) {
          break
        }
        j = `in`.read()
        if (j == -1) {
          return null
        }
        val value = ByteArray(i shl 8 or j)
        i = 0
        while (i < value.size) {
          j = `in`.read(value, i, value.size - i)
          if (j == -1) {
            return null
          }
          i += j
        }
        values.add(value)
      }
      mError = NO_ERROR
      return values
    } catch (e: IOException) {
      e.printStackTrace()
    } finally {
      try {
        socket.close()
      } catch (e: IOException) {
      }

    }
    return null
  }

  private fun generateIv(length: Int): ByteArray {
    val b = ByteArray(length)
    random.nextBytes(b)
    return b
  }

  companion object {

    private val UNLOCK_ACTION = "android.credentials.UNLOCK"

    // ResponseCodes
    private val NO_ERROR = 1
    private val LOCKED = 2
    private val UNINITIALIZED = 3
    private val PROTOCOL_ERROR = 5

    private val sAddress = LocalSocketAddress(
        "keystore", LocalSocketAddress.Namespace.RESERVED)
    private val KEY_LENGTH = 256
    private val DELIMITER = "]"

    private fun getBytes(string: String): ByteArray {
      try {
        return string.toByteArray(charset("UTF-8"))
      } catch (e: UnsupportedEncodingException) {
        throw RuntimeException(e)
      }

    }
  }
}
