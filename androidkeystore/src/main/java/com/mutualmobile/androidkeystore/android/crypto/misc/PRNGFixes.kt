package com.mutualmobile.androidkeystore.android.crypto.misc

import android.os.Build
import android.os.Process
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

// Based on http://android-developers.blogspot.jp/2013/08/some-securerandom-thoughts.html
object PRNGFixes {

  private val BUILD_FINGERPRINT_AND_DEVICE_SERIAL = buildFingerprintAndDeviceSerial

  private// We're using the Reflection API because Build.SERIAL is only available
  // since API Level 9 (Gingerbread, Android 2.3).
  val deviceSerialNumber: String?
    get() {
      try {
        return Build::class.java.getField("SERIAL").get(null) as String
      } catch (ignored: Exception) {
        return null
      }

    }

  private val buildFingerprintAndDeviceSerial: ByteArray
    get() {
      val result = StringBuilder()
      val fingerprint = Build.FINGERPRINT
      if (fingerprint != null) {
        result.append(fingerprint)
      }
      val serial = deviceSerialNumber
      if (serial != null) {
        result.append(serial)
      }
      try {
        return result.toString().toByteArray(charset("UTF-8"))
      } catch (e: UnsupportedEncodingException) {
        throw RuntimeException("UTF-8 encoding not supported")
      }

    }

  fun apply() {
    applyOpenSSLFix()
  }

  @Throws(SecurityException::class)
  fun applyOpenSSLFix() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
      // No need to apply the fix
      return
    }

    try {
      // Mix in the device- and invocation-specific seed.
      Class.forName("org.apache.harmony.xnet.provider.jsse.NativeCrypto")
          .getMethod("RAND_seed", ByteArray::class.java)
          .invoke(null, generateSeed())

      // Mix output of Linux PRNG into OpenSSL's PRNG
      val bytesRead = Class
          .forName(
              "org.apache.harmony.xnet.provider.jsse.NativeCrypto")
          .getMethod("RAND_load_file", String::class.java, Long::class.javaPrimitiveType)
          .invoke(null, "/dev/urandom", 1024) as Int
      if (bytesRead != 1024) {
        throw IOException(
            "Unexpected number of bytes read from Linux PRNG: $bytesRead")
      }
    } catch (e: Exception) {
      throw SecurityException("Failed to seed OpenSSL PRNG", e)
    }

  }

  private fun generateSeed(): ByteArray {
    try {
      val seedBuffer = ByteArrayOutputStream()
      val seedBufferOut = DataOutputStream(seedBuffer)
      seedBufferOut.writeLong(System.currentTimeMillis())
      seedBufferOut.writeLong(System.nanoTime())
      seedBufferOut.writeInt(Process.myPid())
      seedBufferOut.writeInt(Process.myUid())
      seedBufferOut.write(
          BUILD_FINGERPRINT_AND_DEVICE_SERIAL)
      seedBufferOut.close()
      return seedBuffer.toByteArray()
    } catch (e: IOException) {
      throw SecurityException("Failed to generate seed", e)
    }

  }
}
