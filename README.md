beta channel
============
to get the latest bleeding edge versions go to 

https://play.google.com/apps/testing/com.mycelium.wallet

to be eligible for testing you need to join the g+ group at 

https://plus.google.com/communities/102264813364583686576

building
========

to build everything from source, simply checkout the source and build using gradle
note that you need 

 * Android SDK version 19 with *ANDROID_HOME* variable pointing to it.
 * appcompat v7 rev. 19 (Android Support Library + Android Support Repository)


on the console write

    git clone https://github.com/mycelium-com/wallet.git
    cd wallet

linux/mac

    ./gradlew build

in windows: 

    gradlew.bat build

 - voila, look into wallet/public/mbw/build/apk to see the generated apk. 
   there are versions for prodnet and testnet.

alternatively you can install the latest version from the play store at

https://play.google.com/store/apps/details?id=com.mycelium.wallet

features 
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

Thanks to Jethro for tirelessly testing the app during beta development.
