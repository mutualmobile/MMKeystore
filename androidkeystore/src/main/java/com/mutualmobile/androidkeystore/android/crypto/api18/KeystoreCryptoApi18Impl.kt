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

package com.mutualmobile.androidkeystore.android.crypto.api18

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.security.KeyPairGeneratorSpec
import com.mutualmobile.androidkeystore.android.crypto.CipherFactory
import com.mutualmobile.androidkeystore.android.crypto.KeystoreCrypto
import com.mutualmobile.androidkeystore.android.crypto.misc.Base64
import com.mutualmobile.androidkeystore.android.crypto.misc.PRNGFixes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.reflect.InvocationTargetException
import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.SecureRandom
import java.security.PrivateKey
import java.security.UnrecoverableEntryException
import java.security.cert.CertificateException
import java.util.ArrayList
import java.util.Calendar
import java.util.Enumeration
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

/**
 * Implements [KeystoreCrypto] methods for API 18 (after the Android KeyStore public API).
 */
open class KeystoreCryptoApi18Impl @Throws(KeyStoreException::class)
constructor(protected var context: Context) :
    KeystoreCrypto {

  protected var keyStore: KeyStore

  init {
    PRNGFixes.apply()
    try {
      keyStore = KeyStore.getInstance(
          ANDROID_KEYSTORE)
      keyStore.load(null)
    } catch (e: KeyStoreException) {
      e.printStackTrace()
      throw KeyStoreException(e)
    } catch (e: CertificateException) {
      e.printStackTrace()
      throw KeyStoreException(e)
    } catch (e: NoSuchAlgorithmException) {
      e.printStackTrace()
      throw KeyStoreException(e)
    } catch (e: IOException) {
      e.printStackTrace()
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  override fun encrypt(alias: String, plainText: String): String {
    try {
      val key = generateAESKey()
      val encrypted = encryptedUsingAESKey(key, plainText)
      val encryptedKey = encryptAESKeyUsingRSA(alias, key)
      // append with AES enc with RSA
      return String.format("%s%s%s", Base64.to(encryptedKey),
          DELIMITER, Base64.to(encrypted))
    } catch (e: Exception) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  override fun getAliases(): Enumeration<String> {
    return keyStore.aliases()
  }

  @Throws(KeyStoreException::class)
  override fun deleteEntry(alias: String) {
    keyStore.deleteEntry(alias)
  }

  @Throws(KeyStoreException::class)
  override fun decrypt(alias: String, cipherText: String): String {
    try {
      val fields = cipherText.split(
          DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      if (fields.size != 2) {
        throw IllegalArgumentException("Invalid encrypted text format")
      }

      val aesEncWithRSA = Base64.from(fields[0])
      val encToken = Base64.from(fields[1])

      // decrypt AES using RSA
      val key = decryptAESKeyUsingRSA(alias, aesEncWithRSA)

      // decrypt Token using decrypted AES
      return decryptedUsingAESKey(key, encToken)
    } catch (e: Exception) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  override fun is_keystore_unlocked(): Boolean {
    try {
      val keyStoreClass = Class.forName("android.security.KeyStore")
      val getInstanceMethod = keyStoreClass.getMethod("getInstance")
      val invoke = getInstanceMethod.invoke(null)

      val isUnlockedMethod = keyStoreClass.getMethod("isUnlocked")
      return isUnlockedMethod.invoke(invoke) as Boolean
    } catch (e: ClassNotFoundException) {
      throw KeyStoreException(e)
    } catch (e: NoSuchMethodException) {
      throw KeyStoreException(e)
    } catch (e: IllegalAccessException) {
      throw KeyStoreException(e)
    } catch (e: InvocationTargetException) {
      throw KeyStoreException(e)
    }

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

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  @Throws(KeyStoreException::class)
  override fun create_key_if_not_available(toString: String) {
    try {
      if (!keyStore.containsAlias(toString)) {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        end.add(Calendar.YEAR, 10)

        val spec = KeyPairGeneratorSpec.Builder(context).setAlias(toString)
            .setSubject(X500Principal(
                X500_PRINCIPAL))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
        val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
        generator.initialize(spec)
        generator.generateKeyPair()
      }
    } catch (e: Exception) {
      throw KeyStoreException(e)
    }

  }

  @Throws(NoSuchAlgorithmException::class)
  private fun generateAESKey(): SecretKey {
    // Generate a 256-bit key
    val outputKeyLength = 256

    val secureRandom = SecureRandom()
    // Do *not* seed secureRandom! Automatically seeded from system entropy.
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(outputKeyLength, secureRandom)
    return keyGenerator.generateKey()
  }

  @Throws(KeyStoreException::class)
  private fun encryptedUsingAESKey(key: SecretKey, plainText: String): ByteArray {
    try {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      return cipher.doFinal(plainText.toByteArray(charset("UTF-8")))
    } catch (e: NoSuchAlgorithmException) {
      throw KeyStoreException(e)
    } catch (e: NoSuchPaddingException) {
      throw KeyStoreException(e)
    } catch (e: BadPaddingException) {
      throw KeyStoreException(e)
    } catch (e: UnsupportedEncodingException) {
      throw KeyStoreException(e)
    } catch (e: IllegalBlockSizeException) {
      throw KeyStoreException(e)
    } catch (e: InvalidKeyException) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  private fun decryptedUsingAESKey(key: SecretKey, cipherText: ByteArray): String {
    try {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.DECRYPT_MODE, key)
      val encrypted = cipher.doFinal(cipherText)
      return String(encrypted, charset("UTF-8"))
    } catch (e: NoSuchAlgorithmException) {
      throw KeyStoreException(e)
    } catch (e: NoSuchPaddingException) {
      throw KeyStoreException(e)
    } catch (e: BadPaddingException) {
      throw KeyStoreException(e)
    } catch (e: UnsupportedEncodingException) {
      throw KeyStoreException(e)
    } catch (e: IllegalBlockSizeException) {
      throw KeyStoreException(e)
    } catch (e: InvalidKeyException) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  private fun encryptAESKeyUsingRSA(alias: String, key: SecretKey): ByteArray {
    try {
      val privateKey = keyStore.getKey(alias, null) as PrivateKey?

      val publicKey = if (privateKey != null) {
        keyStore.getCertificate(alias).publicKey
      } else {
        null
      }

      val cipher = CipherFactory.get()
      cipher.init(Cipher.ENCRYPT_MODE, publicKey)

      val outputStream = ByteArrayOutputStream()
      val cipherOutputStream = CipherOutputStream(outputStream, cipher)
      cipherOutputStream.write(key.encoded)
      cipherOutputStream.close()

      return outputStream.toByteArray()
    } catch (e: NoSuchPaddingException) {
      throw KeyStoreException(e)
    } catch (e: NoSuchAlgorithmException) {
      throw KeyStoreException(e)
    } catch (e: NoSuchProviderException) {
      throw KeyStoreException(e)
    } catch (e: InvalidKeyException) {
      throw KeyStoreException(e)
    } catch (e: KeyStoreException) {
      throw KeyStoreException(e)
    } catch (e: UnrecoverableEntryException) {
      throw KeyStoreException(e)
    } catch (e: IOException) {
      throw KeyStoreException(e)
    }

  }

  @Throws(KeyStoreException::class)
  private fun decryptAESKeyUsingRSA(alias: String, aesEncKey: ByteArray): SecretKeySpec {
    try {
      val cipher = CipherFactory.get()
      val privateKey = keyStore.getKey(alias, null) as PrivateKey
      cipher.init(Cipher.DECRYPT_MODE, privateKey)

      val cipherInputStream = CipherInputStream(ByteArrayInputStream(aesEncKey), cipher)
      val values = ArrayList<Byte>()

      while (true) {
        val nextByte: Int = cipherInputStream.read()
        if (nextByte < 0) {
          break
        }
        values.add(nextByte.toByte())
      }

      val bytes = ByteArray(values.size)
      for (i in bytes.indices) {
        bytes[i] = values[i]
      }

      return SecretKeySpec(bytes, "AES")
    } catch (e: NoSuchPaddingException) {
      throw KeyStoreException(e)
    } catch (e: NoSuchAlgorithmException) {
      throw KeyStoreException(e)
    } catch (e: NoSuchProviderException) {
      throw KeyStoreException(e)
    } catch (e: UnsupportedEncodingException) {
      throw KeyStoreException(e)
    } catch (e: IOException) {
      throw KeyStoreException(e)
    } catch (e: InvalidKeyException) {
      throw KeyStoreException(e)
    } catch (e: UnrecoverableEntryException) {
      throw KeyStoreException(e)
    } catch (e: KeyStoreException) {
      throw KeyStoreException(e)
    }

  }

  companion object {
    private val DELIMITER = "]"
    val X500_PRINCIPAL = "CN=MM, O=Keystore"
    val ANDROID_KEYSTORE = "AndroidKeyStore"

    val UNLOCK_ACTION = "com.android.credentials.UNLOCK"
  }
}
