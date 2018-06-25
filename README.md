# MMKeyStore

[![Build Status](https://travis-ci.org/mutualmobile/mmkeystore.svg)](https://travis-ci.org/mutualmobile/mmkeystore)
[![Version](https://api.bintray.com/packages/mutualmobile/Android/mmkeystore/images/download.svg)](https://bintray.com/mutualmobile/Android/mmkeystore)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.mutualmobile/mmkeystore/badge.svg)](http://www.javadoc.io/doc/com.mutualmobile/mmkeystore)

mmkeystore is a library for Android apps that allows you to encrypt sensitive information, The encryption uses the Android KeyStore available to generate and uses RSA and AES keys for encryption operations.

## Getting Started

<p align="center"><img src="https://giphy.com/gifs/2WH71Azrx2ksWMGbsG/embed"></p>


## Adding mmkeystore to your project

Include the following dependencies in your app's build.gradle :

```
dependencies {
    compile 'com.mutualmobile:mmkeystore:<version>'
}
```

See [version.txt](version.txt) for the latest version number.

In case you don't want to use the precompiled version, you can build the library yourself from source.

## How to use

1. Get an instance of SyncCryptoFactory

```
factory = SyncCryptoFactory.get(this)
```

2. Generate key aliases
NOTE: This has to be a unique key identifier plus do this in a Background Thread as the process is ASYNC!

```
 factory.create_key_if_not_available(EMAIL)
 factory.create_key_if_not_available(PASSWORD)
```

3. Encrypt the string!

```
val encryptedEmail = factory.encrypt(EMAIL, mEmail)
val encryptedPassword = factory.encrypt(PASSWORD, mPassword)
```

4. Decrypt using the encrypted string!

```
factory.decrypt(EMAIL, encryptedEmail)
factory.decrypt(PASSWORD, encryptedPassword)
```

## Special Thanks

Realm - The popular mobile database that runs directly inside phones, tablets or wearables. This library is cloned from (https://github.com/realm/realm-android-user-store)

## License

MMKeystore User Encryption is published under the Apache 2.0 license.

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
