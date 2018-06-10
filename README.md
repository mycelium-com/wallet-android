Beta channel
============

Beta testers receive updates quicker than other users.  To receive beta updates, you need to do two things:

1. Join [the G+ group](https://plus.google.com/communities/102264813364583686576)
so you are eligible for testing
2. Explicitly enable beta versions of the software in
[Google play](https://play.google.com/apps/testing/com.mycelium.wallet)

Beta Testers should make sure they have a recent **backup of the masterseed** and
all **private keys** inside Mycelium. Beta testers will experience many bugs as their testing.
So far, restoring the wallet from the masterseed has not been necessary, however, we offer no guarantees about the future.

Building
========

To build everything from source, checkout the source and build using gradle.
On the build system you need:

 * JDK 1.7

You also need to use the Android SDK manager to install the following components:

 * `ANDROID_HOME` environment variable pointing to the directory where the SDK is installed
 * Android SDK Tools 22.6.4
 * SDK Platform Tools 19.0.2
 * Android SDK build Tools 19.1.0
 * Android 4.4.2 (API 19) (at least SDK Platform Rev. 3)
 * Android Extras:
    * Android Support Repository rev 5
    * Android Support Library rev 19.1
    * Google Play services for Froyo rev 12
    * Google Play services rev 17
    * Google Repository rev 8


The project layout is designed to be used with a recent version of Android Studio (currently 1.1.0)

#### Build commands

1. At the console prompt type the following to checkout the source:

    git clone https://github.com/mycelium-com/wallet-android.git
    cd wallet-android
    git submodule update --init --recursive

2. To build, type the command appropriate for your system:

    On Linux/Mac type the following to build the source:

        ./gradlew clean test mbw::asProdRel mbw::asBtRel

    On Windows type the following to build the source:

        gradlew.bat clean test mbw::asProdRel mbw::asBtRel

3. Voila, look into `mbw/build/outputs/apk/` to see the generated APK. 
   There are versions for both prodnet and testnet.

Alternatively you can install the latest version from the [Play Store](https://play.google.com/store/apps/details?id=com.mycelium.wallet).

If you cannot access the Play store, you can obtain the APK directly from https://mycelium.com/bitcoinwallet

Deterministic builds
====================

To validate the Mycelium image you obtain from Google Play Store, you can rebuild the Mycelium wallet yourself using
Docker and compare both images following these steps:
 
1. Create your own Docker image

        $ docker build . --tag mycelium-wallet .

  Check that this step succeeds by listing the available docker images:

        $ docker images 

2. Build Mycelium using Docker

        $ docker run --rm -v $(pwd):/project -w /project mycelium-wallet ./gradlew mbw::clean mbw::assembleProdnetRelease

  After this step succeeds, the mycelium unsigned APK is in `mbw/builds/outputs/apk`.
  You may need to create a `gradle.properties` file and set
  
        org.gradle.jvmargs = -Xmx5120m

3. Retrieve Google Play Mycelium APK from your phone
  Gets package path:

        $ adb shell pm path com.mycelium.wallet
        package:/data/app/com.mycelium.wallet-1/base.apk

  Retrieve file:

        $ adb pull /data/app/com.mycelium.wallet-1/base.apk mycelium-signed.apk

4. Compare signed APK with unsigned locally built APK using Signal apkdiff

        $ wget https://raw.githubusercontent.com/WhisperSystems/Signal-Android/master/apkdiff/apkdiff.py
        python apkdiff.py mycelium-signed.apk mbw/build/outputs/apk/.....your-prodnet.apk
        
Note: You might have to `sudo chown -R $USER:$USER .` as the docker user might create files that you have no access to under your normal user.

This work is based on WhisperSystems Signal reproducible builds:

* https://whispersystems.org/blog/reproducible-android/
* https://github.com/WhisperSystems/Signal-Android/wiki/Reproducible-Builds


Features
========

With the Mycelium Bitcoin Wallet you can send and receive Bitcoins using your mobile phone.

 - HD enabled - manage multiple accounts and never reuse addresses ([Bip32](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki)/[Bip44](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki) compatible)
 - Masterseed based - make one backup and be safe for ever. ([Bip39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki))
 - 100% control over your private keys, they never leave your device unless you export them
 - No block chain download - install and run in seconds
 - Ultra fast connection to the Bitcoin network through our super nodes
 - For enhanced privacy and availability you can connect to our super nodes via a tor-hidden service ( *.onion* address)
 - Watch-only addresses (single or xPub) & private key (single or xPriv) import for secure cold-storage integration
 - Directly spend from paper wallets (single key, xPriv or master seed)
 - Trezor enabled - directly spend from trezor-secured accounts.
 - [Mycelium Entropy](https://mycelium.com/entropy) compatible Shamir-Secret-Shared 2-out-of-3 keys spending
 - Secure your wallet with a PIN
 - Compatible with other bitcoin services through the `bitcoin:` URI scheme
 

Please note that Bitcoin is still experimental and this app comes with no warranty. While we make sure to adhere to the highest standards of software craftsmanship, we cannot guarantee that the software does not contain bugs. Please make sure you have backups of your private keys and do not use this software to manage more Bitcoins than you are willing to lose.

This application's source is published at https://github.com/mycelium-com/wallet
We need your feedback. If you have a suggestion or a bug to report [create an issue](https://github.com/mycelium-com/wallet/issues).

More features:
 - Source code is [available for review](https://github.com/mycelium-com/wallet-android)
 - Multiple HD accounts, private keys, external xPub or xPriv accounts
 - Multiple Bitcoin denominations: BTC, mBTC, bits and uBTC
 - View your balance in multiple fiat currencies: USD, AUD, CAD, CHF, CNY, DKK, EUR, GBP, HKD, JPY, NZD, PLN, RUB, SEK, SGD, THB, and many more
 - Send and receive by specifying an amount in fiat and switch between fiat and BTC while entering the amount
 - Address book for commonly used addresses
 - Transaction history with detailed information and local stored comments
 - Import private keys using SIPA (the ones beginning with a 5) and mini private key format (Casascius private keys) from QR-codes or clipboard
 - Export private-, xPub- or xPriv-keys as QR-codes, on clipboard or share with other applications
 - Share your bitcoin address using Twitter, Facebook, email and more.
 - Integrated QR-code scanner
 - Client side load balancing between three 100% redundant server nodes located in different data centers.
 - Sign Messages using your private keys (compatible with bitcoin-qt)

Authors
=======
 - Jan Møller
 - Andreas Petersson
 - Daniel Weigl
 - Jan Dreske
 - Dmitry Murashchik
 - Constantin Vennekel
 - Leo Wandersleb
 - Daniel Krawisz
 - Jerome Rousselot
 - Elvis Kurtnebiev
 - Sergey Dolgopolov

Credits
=======
Thanks to all collaborators who provided us with code or helped us with integrations!
Just to name a few:

 - Nicolas Bacca from Ledger
 - Sipa, Marek and others from Trezor
 - Jani and Aleš from Cashila
 - Kalle Rosenbaum, Bip120/121
 - David and Alex from Glidera
 - [Wiz](https://twitter.com/wiz) for helping us with KeepKey
 - Tom Bitton and Asa Zaidman from Simplex
 - (if you think you should be mentioned here, just notify us)

Thanks to Jethro for tirelessly testing the app during beta development.

Thanks to our numerous volunteer translators who provide high-quality translations in many languages. Your name should be listed here, please contact me so I know you want to be included.

Thanks to Johannes Zweng for his testing and providing pull requests for fixes.

Thanks to all beta testers to provide early feedback.
