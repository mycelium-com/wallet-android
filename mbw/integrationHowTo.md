Mycelium Integration Howto

This document describes how to add an external service buy component to Mycelium wallet, using an external web view and/or activities.

Getting started
---------------

git clone  gitlab@gitlab.mycelium.com:mycelium/mycelium-com-wallet.git
cd android-public/mbw
gradle assembleDevprodDebug
adb install -r build/outputs/apk/mbw-devprod-debug-2.8.4build28406.apk


Instructions
------------
cd src/main/java/com/mycelium/wallet/activity/main
cp SimplexFragment.java BTCVendorFragment.java # and adapt file contents to new provider. Update link to fragment xml in inflater.inflate call.
cd ../../../../../../../res/layout
cp main_si_fragment.xml main_<twolettercodefornewvendor>_fragment.xml # and adapt file contents to new provider.
vi glidera_buy_sell.xml   # this file is the buy sell menu. Copy from Simplex start to Simplex stop and adapt contents.
cd ../values
vi strings.xml # and create new strings as required from main_<vendor>_fragment.xml
cd ../../java/com/mycelium/wallet/persistence/
vi MetadataStorage.java
    # create <VENDOR>_IS_ENABLED static final field
    # create getter and setters setVendorIsEnabled and getVendorIsEnabled
cd ../../../../../res/drawable
cp /home/user/somepath/vendor.png .  # import a 100px by 100px square logo for new vendor
cd ../xml
vi preferences.xml     # In External Services list, add a CheckboxPreference with name enable<Vendor>Button
cd ../../src/main/java/com/mycelium/wallet/activity/settings
vi SettingsActivity.java      # add onClick<Vendor>Enable OnPreferencesListener, and CheckboxPreference enable<Vendor>Button in  // externalServices

Build and test:
gradle assembleDevProdDebug
adb install -r build/outputs/apk/mbw-devprod-debug-2.8.4build28406.apk






How to edit ads
---------------
Requires
- 1 png file
- marketing text
- 1 referral URL

Add the png to main/src/res/drawable with e.g. wget http://mypartner.com/logo.png
Edit src/res/values/strings.xml and add a value with id ad_buy_partner with the text and a html anchor link to partner referral URL. Add optional longer ad_info_partner text.
Edit AdFragment.java, update ads probabilities and field loading.
