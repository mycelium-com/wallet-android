Beta channel
============
In order to receive updates quicker than others, you need to do two things:

to be eligible for testing you need to join the g+ group at

https://plus.google.com/communities/102264813364583686576

after joining, you need to explicitly enable beta versions for the software at

https://play.google.com/apps/testing/com.mycelium.wallet

Building
========

To build everything from source, simply checkout the source and build using gradle
on the build system you need

 * JDK 1.7

Then you need to use the Android SDK manager to install the following components:

 * Android SDK version 19 with *ANDROID_HOME* variable pointing to the directory
 * SDK Platform Tools 19.0.1
 * Android SDK build Tools 19.0.1
 * Android 4.4.2 (API 19)
 * appcompat v7 rev. 19.0.1 (Android Support Library + Android Support Repository)

On the console write

    git clone https://github.com/mycelium-com/wallet.git
    cd wallet

Linux/mac

    ./gradlew build

Windows: 

    gradlew.bat build

 - voilà, look into wallet/public/mbw/build/apk to see the generated apk. 
   there are versions for prodnet and testnet.

alternatively you can install the latest version from the Play store at

https://play.google.com/store/apps/details?id=com.mycelium.wallet

If you cannot access the Play store you can obtain the apk directly from https://mycelium.com/

Features 
========

With the Mycelium Bitcoin Wallet you can send and receive Bitcoins using your mobile phone.

 - 100% control over your private keys, they never leave your device unless you export them
 - No block chain download, install and run in seconds
 - Ultra fast connection to the Bitcoin network through our super nodes
 - Watch-only addresses & private key import for secure cold-storage integration
 - Export generated keys directly to your SD card to print them securely with no intermediary computer
 - Secure your wallet with a PIN
 - Compatible with other bitcoin services through bitcoin: uri handling

Please not that this is a beta release - while we make sure to adhere to the highest standards of software craftsmanship we can not exclude that the software is bug-free. Please make sure you have backups of your private keys and do not use this for more than you are willing to lose.

This application's source is published at https://github.com/mycelium-com/wallet
We need your feedback. If you have a suggestion or a bug to report open an issue at https://github.com/mycelium-com/wallet

More features:
 - Sources available for review:  https://github.com/mycelium-com/wallet
 - Multiple private keys
 - Multiple Bitcoin denominations: BTC, mBTC, and uBTC
 - View your balance in multiple fiat currencies: USD, AUD, CAD, CHF, CNY, DKK, EUR, GBP, HKD, JPY, NZD, PLN, RUB, SEK, SGD, THB
 - Send and receive by specifying an amount in Fiat and switch between fiat and BTC while entering the amount
 - Address book for commonly used addresses
 - Transaction history with detailed transaction details.
 - Import private keys using SIPA and mini private key format (Casascius private keys) from QR-codes or clipboard
 - Export private keys as QR-codes, on clipboard, and directly to external SD card.
 - Share your bitcoin address using Twitter, Facebook, email and more.
 - Integrated QR-code scanner
 - Client side load balancing between two 100% redundant server nodes located in different datacenters.
 - Sign Messages using your private keys (compatible with bitcoin-qt)

Authors
=======
 - Jan Møller
 - Andreas Petersson

Credits
=======
Thanks to Jethro for tirelessly testing the app during beta development.

Thanks to our numerous volunteer translators who provide high-quality translations in many languages. Your name should be listed here, please contact me so i know you want to be included.

Thanks to Johannes Zweng for his testing and providing pull requests for fixes.
Thanks to all beta testers to provide early feedback.
