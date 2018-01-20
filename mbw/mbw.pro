#obfuscation creates more problems than it solves
-dontobfuscate

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
#ignore xzing version trickery
-dontwarn com.google.zxing.**
-dontwarn java.lang.management.**
-dontwarn javax.naming.**
-dontwarn okio.**

#spongycastle/coinapult
-keep class org.spongycastle.**
-dontwarn org.spongycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.spongycastle.x509.util.LDAPStoreHelper

# ignore unknown class problems related to bitcoinj in mbwlib code as we exclude the package but re build it from mbw itself
-dontwarn org.bitcoinj.**
-dontwarn org.bitcoin.**

-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

-dontwarn android.support.**
-dontwarn org.apache.xmlrpc.**

-optimizationpasses 6

#When not preverifing in a case-insensitive filing system, such as Windows. Because this tool unpacks your processed jars, you should then use:
-dontusemixedcaseclassnames

#Specifies not to ignore non-public library classes. As of version 4.5, this is the default setting
-dontskipnonpubliclibraryclasses

#Preverification is irrelevant for the dex compiler and the Dalvik VM, so we can switch it off with the -dontpreverify option.
-dontpreverify

#Specifies to write out some more information during processing. If the program terminates with an exception, this option will print out the entire stack trace, instead of just the exception message.
-verbose

#The -optimizations option disables some arithmetic simplifications that Dalvik 1.0 and 1.5 can't handle. Note that the Dalvik VM also can't handle aggressive overloading (of static fields).
#To understand or change this check http://proguard.sourceforge.net/index.html#/manual/optimizations.html
#added !code/allocation/variable to resolve build issue
-optimizations !field/removal/writeonly,!field/marking/private,!class/merging/*,!code/allocation/variable, !class/unboxing/enum

#To repackage classes on a single package
#-repackageclasses ''

#Uncomment if using annotations to keep them.
#-keepattributes *Annotation*

#Keep classes that are referenced on the AndroidManifest
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-keep public class com.google.zxing.client.android.common.executor.HoneycombAsyncTaskExecInterface

#To remove debug logs:
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

#To avoid changing names of methods invoked on layout's onClick.
# Uncomment and add specific method names if using onClick on layouts
#-keepclassmembers class * {
# public void onClickButton(android.view.View);
#}

#Maintain java native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

#To maintain custom components names that are used on layouts XML.
#Uncomment if having any problem with the approach below
#-keep public class custom.components.package.and.name.**

#To maintain custom components names that are used on layouts XML:
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# GMS related classes
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

#Maintain enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

#To keep parcelable classes (to serialize - deserialize objects to sent through Intents)
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

#Keep the R
-keepclassmembers class **.R$* {
    public static <fields>;
}

#keep classes used for deserializing json
-keepclasseswithmembers class com.mycelium.lt.location.** {
  <init>(...);
  *;
}

#keep classes used for deserializing json
-keepclasseswithmembers class com.mycelium.wapi.** {
  <init>(...);
  *;
}

#keep classes used for deserializing json for coinapult
-keepclasseswithmembers class com.coinapult.** {
  <init>(...);
  *;
}

#keep classes used for deserializing json for colu
-keepclasseswithmembers class com.mycelium.wallet.colu.** {
  <init>(...);
  *;
}

#keep classes used for deserializing json
-keepclasseswithmembers class com.mycelium.wallet.bitid.json.** {
  <init>(...);
  *;
}

#keep classes used for deserializing payment requests
-keepclasseswithmembers class org.bitcoin.protocols.** {
  <init>(...);
  *;
}

-keep public class com.mycelium.lt.api.** {
  <init>(...);
 }
-dontwarn com.fasterxml.jackson.**


# keep everything decorated with butterknife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewInjector { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# retrofit + API interfaces
-keep class retrofit.** { *; }
-keep class com.mycelium.wallet.external.glidera.api.** { *; }
-keep class com.mycelium.wallet.external.rmc.remote.** { *; }
-keepclassmembernames interface * {
    @retrofit.http.* <methods>;
}

#-dontwarn rx.**
-dontwarn retrofit.**

# keep everything in ledger/nordpol
-keep class nordpol.** { *; }

# keep RX-relevant stuff
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# google http client

-keepclassmembers class * {
   @com.google.api.client.util.Key <fields>;
}

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# This is to prevent proguard from removing translations for prettytime
-keep class org.ocpsoft.prettytime.i18n.**


# bitcoinj
-keep,includedescriptorclasses class org.bitcoinj.wallet.Protos$** { *; }
-keepclassmembers class org.bitcoinj.wallet.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-keep,includedescriptorclasses class org.bitcoin.protocols.payments.Protos$** { *; }
-keepclassmembers class org.bitcoin.protocols.payments.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-keep class org.bitcoinj.crypto.** { *; }
-dontwarn org.bitcoinj.store.WindowsMMapHack
-dontwarn org.bitcoinj.store.LevelDBBlockStore
-dontnote org.bitcoinj.crypto.DRMWorkaround
-dontnote org.bitcoinj.crypto.TrustStoreLoader$DefaultTrustStoreLoader
-dontnote com.subgraph.orchid.crypto.PRNGFixes
-dontwarn okio.DeflaterSink
-dontwarn okio.Okio
-dontnote com.squareup.okhttp.internal.Platform
-dontwarn org.bitcoinj.store.LevelDBFullPrunedBlockStore**

# slf4j
#Warning: com.mrd.bitlib.crypto.Bip38: can't find referenced method 'byte[] scrypt(byte[],byte[],int,int,int,int,com.lambdaworks.crypto.SCryptProgress)' in program class com.lambdaworks.crypto.SCrypt
#Warning: com.mrd.bitlib.crypto.MrdExport$V1$EncryptionParameters: can't find referenced method 'byte[] scrypt(byte[],byte[],int,int,int,int,com.lambdaworks.crypto.SCryptProgress)' in program class com.lambdaworks.crypto.SCrypt
-dontwarn org.slf4j.LoggerFactory
-dontwarn org.slf4j.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder



#rmc
-keep class com.mycelium.wallet.activity.rmc.json.** { *; }
-keep class com.mycelium.wallet.activity.rmc.model.** { *; }

#Warning: org.slf4j.LoggerFactory: can't find referenced class org.slf4j.impl.StaticLoggerBinder
#Warning: org.slf4j.LoggerFactory: can't find referenced class org.slf4j.impl.StaticLoggerBinder
#Warning: org.slf4j.LoggerFactory: can't find referenced class org.slf4j.impl.StaticLoggerBinder
#Warning: org.slf4j.LoggerFactory: can't find referenced class org.slf4j.impl.StaticLoggerBinder
#Warning: org.slf4j.LoggerFactory: can't find referenced class org.slf4j.impl.StaticLoggerBinder
#Warning: org.slf4j.MDC: can't find referenced class org.slf4j.impl.StaticMDCBinder
#Warning: org.slf4j.MDC: can't find referenced class org.slf4j.impl.StaticMDCBinder
#Warning: org.slf4j.MDC: can't find referenced class org.slf4j.impl.StaticMDCBinder
#Warning: org.slf4j.MarkerFactory: can't find referenced class org.slf4j.impl.StaticMarkerBinder
#Warning: org.slf4j.MarkerFactory: can't find referenced class org.slf4j.impl.StaticMarkerBinder
#Warning: org.slf4j.MarkerFactory: can't find referenced class org.slf4j.impl.StaticMarkerBinder
 
###### ADDITIONAL OPTIONS NOT USED NORMALLY

#To keep callback calls. Uncomment if using any
#http://proguard.sourceforge.net/index.html#/manual/examples.html#callback
#-keep class mypackage.MyCallbackClass {
#   void myCallbackMethod(java.lang.String);
#}

#Uncomment if using Serializable
#-keepclassmembers class * implements java.io.Serializable {
#    private static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    java.lang.Object writeReplace();
#    java.lang.Object readResolve();
#}

#for retrofit2:
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
-keep class com.mycelium.wallet.external.changelly.** { *; }
-keep class com.mycelium.wallet.exchange.** { *; }
