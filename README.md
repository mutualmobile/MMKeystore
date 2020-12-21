# AndroidKeyStore

[![Build Status](https://travis-ci.org/mutualmobile/MMKeystore.svg)](https://travis-ci.org/mutualmobile/MMKeystore)
[![Version](https://api.bintray.com/packages/mutualmobile/Android/androidkeystore/images/download.svg)](https://bintray.com/mutualmobile/Android/androidkeystore)

## Deprecated

This Library is now Deprecated and wont be updated anymore. Use [Android Native Keystore System](https://developer.android.com/reference/java/security/KeyStore) instead.


AndroidKeyStore is a library for Android apps that allows you to encrypt sensitive information, The encryption uses the Android KeyStore available to generate and uses RSA and AES keys for encryption operations.

## Getting Started

<p align="center"><img src="https://media.giphy.com/media/2WH71Azrx2ksWMGbsG/giphy.gif"></p>


## Adding androidkeystore to your project

Include the following dependencies in your app's build.gradle :

```
dependencies {
    implementation 'com.mutualmobile:androidkeystore:0.0.4'
}
```

## How to use

1. Get an instance of KeystoreCryptoFactory

``` java
val factory = KeystoreCryptoFactory.get(context)
```

2. Now carry out the encryption/decryption

**NOTE:** Since the Encryption/Decryption is Async, the following tasks needs to be done on a background thread

``` java
  //Generate Key Aliases
  factory.create_key_if_not_available(EMAIL)
  factory.create_key_if_not_available(PASSWORD)

  //Encrypt the string
  val encryptedEmail = factory.encrypt(EMAIL, mEmail)
  val encryptedPassword = factory.encrypt(PASSWORD, mPassword)

  //Decrypt the string
  factory.decrypt(EMAIL, encryptedEmail)
  factory.decrypt(PASSWORD, encryptedPassword)
```

## Special Thanks

Realm - The popular mobile database that runs directly inside phones, tablets or wearables. This library is cloned from (https://github.com/realm/realm-android-user-store)

License
-------

    Copyright 2018 Mutual Mobile

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
