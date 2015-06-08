Beta channel
============
In order to receive updates quicker than others, you need to do two things:

to be eligible for testing you need to join the g+ group at

https://plus.google.com/communities/102264813364583686576

after joining, you need to explicitly enable beta versions for the software at

https://play.google.com/apps/testing/com.mycelium.wallet

As Beta Testers, please make sure you have a recent **backup of the masterseed** and all the **private keys** inside Mycelium. Beta testers will experience a lot of the bugs that are introduced. So far this was never necessary but we don't explicitly give this guarantee.

Building
========

To build everything from source, simply checkout the source and build using gradle
on the build system you need

 * JDK 1.7

Then you need to use the Android SDK manager to install the following components:

 * ANDROID_HOME environment variable pointing to the directory where the sdk is installed
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

On the console write

    git clone https://github.com/mycelium-com/wallet.git
    cd wallet

Linux/mac

    ./gradlew build

Windows: 

    gradlew.bat build

 - voila, look into wallet/public/mbw/build/apk to see the generated apk. 
   there are versions for prodnet and testnet.

alternatively you can install the latest version from the Play store at

https://play.google.com/store/apps/details?id=com.mycelium.wallet

If you cannot access the Play store you can obtain the apk directly from https://mycelium.com/bitcoinwallet

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
 - Compatible with other bitcoin services through *bitcoin:* uri handling
 

Please note that bitcoin is still experimental and this app comes with no warranty - while we make sure to adhere to the highest standards of software craftsmanship we can not exclude that the software contains bugs. Please make sure you have backups of your private keys and do not use this for more than you are willing to lose.

This application's source is published at https://github.com/mycelium-com/wallet
We need your feedback. If you have a suggestion or a bug to report open an issue at https://github.com/mycelium-com/wallet/issues

More features:
 - Sources available for review:  https://github.com/mycelium-com/wallet
 - Multiple HD accounts, privat keys, external xPub or xPriv accounts
 - Multiple Bitcoin denominations: BTC, mBTC, bits and uBTC
 - View your balance in multiple fiat currencies: USD, AUD, CAD, CHF, CNY, DKK, EUR, GBP, HKD, JPY, NZD, PLN, RUB, SEK, SGD, THB, and many more
 - Send and receive by specifying an amount in Fiat and switch between fiat and BTC while entering the amount
 - Address book for commonly used addresses
 - Transaction history with detailed transaction details, including local stored comments
 - Import private keys using SIPA and mini private key format (Casascius private keys) from QR-codes or clipboard
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

Credits
=======
Thanks to Jethro for tirelessly testing the app during beta development.

Thanks to our numerous volunteer translators who provide high-quality translations in many languages. Your name should be listed here, please contact me so i know you want to be included.

Thanks to Johannes Zweng for his testing and providing pull requests for fixes.
Thanks to all beta testers to provide early feedback.
