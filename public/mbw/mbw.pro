-dontobfuscate
#creates more problems than it solves

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
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }
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
#keep classes used for deserializing json
-keepclasseswithmembers class com.coinapult.** {
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
-keep class com.mycelium.wallet.external.cashila.api.** { *; }
-keep class com.mycelium.wallet.external.glidera.api.** { *; }
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

# This is to solve when show error with internalization
-keep class org.ocpsoft.prettytime.i18n.**

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