package com.mycelium.modularizationtools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.gson.GsonBuilder
import com.mycelium.modularizationtools.model.Module
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class CommunicationManager private constructor(val context: Context, val modularizationApiVersion: Int) {
    private val trustedPackages = HashMap<String, PackageMetaData>()
    private val sessionFilename = "sessions.json"
    private val LOG_TAG: String? = this::class.java.canonicalName
    val pairedModules = HashSet<Module>()

    init {
        Log.d(LOG_TAG, "Initializing for package ${context.applicationContext.packageName}")
        loadTrustedPackages()
        loadSessions()
    }

    private fun saveSessions() {
        // TODO: this can probably be done nicer, with delayed writing to avoid writing to disk too often
        Log.d(LOG_TAG, "saveSessions()")
        Thread {
            val gson = GsonBuilder().create()
            try {
                synchronized(this@CommunicationManager) {
                    context.openFileOutput(sessionFilename, Context.MODE_PRIVATE).use {
                        it.write(gson.toJson(trustedPackages.values).toByteArray())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.d(LOG_TAG, "new thread saving sessions finished.")
        }.start()
    }

    private fun loadTrustedPackages() {
        val readerDev = InputStreamReader(context.resources.assets.open("trusted_packages.json"))
        val gson = GsonBuilder().create()
        val trustConfiguration = gson.fromJson(readerDev, TrustConfiguration::class.java)
        Log.d(LOG_TAG, "Loading trust database of latest package version…")
        for (pmd in trustConfiguration.packages) {
            Log.d(LOG_TAG, "Trusting ${pmd.name} with sig ${pmd.signature}.")
            trustedPackages.put(pmd.name, pmd)
        }
    }

    private fun loadSessions() {
        Log.d(LOG_TAG, "Restoring sessions for permitted packages…")
        val file = File(context.filesDir.absolutePath + File.separator + sessionFilename)
        if(file.exists()) {
            context.openFileInput(sessionFilename).use { fileInputStream ->
                InputStreamReader(fileInputStream).use {readerUser ->
                    val gson = GsonBuilder().create()
                    val trustedPackagesArray = gson.fromJson(readerUser, emptyArray<PackageMetaData>().javaClass) ?: emptyArray<PackageMetaData>()
                    for (pmd in trustedPackagesArray) {
                        trustedPackages[pmd.name]?.key = pmd.key
                    }
                }
            }
        } else {
            saveSessions()
        }
    }

    /**
     * @param key session key of "another" package
     * @param packageName package name, usually of another app
     * @throws SecurityException if the key is 0 or the package is not in the set of allowed third
     * party packages or the package is not signed by the right key
     */
    fun pair(packageName: String, key: Long, version: Int) {
        if (key == 0L) {
            //Disallowing 0 as it might be the default value of a Long.
            //By preventing pairing of 0L to anything, searching for 0L's paired package will also
            //lead to adequate SecurityExceptions.
            throw SecurityException("Key 0 does not look like a random key.")
        }
        val trustedPackage = trustedPackages[packageName]
                ?: throw SecurityException("Package $packageName generally not trusted.")
        if(modularizationApiVersion != version) {
            throw SecurityException("Version conflict detected!|$modularizationApiVersion")
        }
        val signingPubKeyHash = getSigningPubKeyHash(packageName)
        if (trustedPackage.signature != signingPubKeyHash) {
            throw SecurityException("Signature $signingPubKeyHash can't be verified for" +
                    " package $packageName.")
        }

        trustedPackages[packageName]!!.key = key
        saveSessions()
    }

    /**
     * @return if the pairing was successful or not
     */
    fun requestPair(packageName: String): Boolean {
        Log.d(LOG_TAG, "requestPair")
        var success = false
        val startTimeMillis = System.currentTimeMillis()
        val cr = context.contentResolver
        // reuse the key we already have. This avoids mismatches if both sides might initiate the communication.
        val packageMetaData = trustedPackages[packageName]
                ?: throw SecurityException("Unknown package name $packageName")
        val key = packageMetaData.key ?: Random().nextLong()
        val keyVersionSelectionArgs = arrayOf(key.toString(), modularizationApiVersion.toString())
        cr.query(Uri.parse("content://$packageName.PairingProvider"), null, null, keyVersionSelectionArgs, null)
                .use { cursor ->
                    cursor ?: return false // if the other module is not returning a proper Cursor, pairing fails here
                    pair(packageName, key, modularizationApiVersion)
                    cursor.moveToFirst()
                    pairedModules.add(Module(packageName
                            , cursor.getString(cursor.getColumnIndex("name"))
                            , cursor.getString(cursor.getColumnIndex("description"))))
                    success = true
                }
        Log.d(LOG_TAG, "It took ${System.currentTimeMillis()-startTimeMillis}ms to ${if(success) "" else "not "} pair with $packageName.")
        return success
    }

    /**
     * Check if another app can be trusted
     *
     * @param packageName the package name of an app
     * @throws SecurityException if the packageName is not in the list of trusted apps or if the
     * installed app of that name was not signed by the expected trusted party
     */
    fun checkSignature(packageName: String) {
        val signingPubKeyHash = getSigningPubKeyHash(packageName)
        val pmd = trustedPackages[packageName] ?: throw SecurityException(
                "Untrusted package $packageName.\n" +
                "To add this package and signature to the list of trusted packages, add this line:\n" +
                "  {\"name\": \"$packageName\", \"signature\": \"$signingPubKeyHash\"},\n" +
                "to your trusted_packages.json.")
        if(pmd.signature != signingPubKeyHash) {
            throw SecurityException("The currently installed package $packageName has the" +
                    " unexpected signature $signingPubKeyHash instead of ${pmd.signature}")
        }
    }

    /**
     * @param key the session key a package should be associated to.
     * @return the package name
     * @throws SecurityException if no package is associated or the associated package was
     * reinstalled using an untrusted signature
     */
    fun getPackageName(key: Long): String {
        for (pn in trustedPackages.keys) {
            if (trustedPackages[pn]!!.key == key) {
                checkSignature(pn)
                return pn
            }
        }
        throw SecurityException("Unknown session key $key")
    }

    fun getKey(packageName: String) = trustedPackages[packageName]!!.key

    private fun getSigningPubKeyHash(packageName: String): String {
        try {
            // Lint warns here about an exploit and we think this one is handled correctly but we are hesitant to suppress the warning for sake of future awareness.
            val signatures = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            var signingPubKeyHash: String? = null
            // A package can have more than one signature. Check them all.
            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signature.toByteArray())
                val base64Digest = Base64.encodeToString(digest, Base64.DEFAULT or Base64.NO_WRAP)
                if (signingPubKeyHash != null && base64Digest != signingPubKeyHash) {
                    // According to
                    // http://www.androidcentral.com/fake-id-and-android-security-updated
                    // fake signatures should be a non-issue. If that can be confirmed, change the
                    // code to instead of looping over signatures, just take signatures[0].
                    throw SecurityException("Suspect fake signature detected for package $packageName.")
                }
                signingPubKeyHash = base64Digest
            }
            if (signingPubKeyHash == null) {
                throw SecurityException("No Package Signature found for $packageName.")
            }
            return signingPubKeyHash
        } catch (e: PackageManager.NameNotFoundException) {
            throw SecurityException("Signature check failed for package $packageName!", e)
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityException("Signature check failed for package $packageName!", e)
        }
    }

    fun send(receivingPackage: String, intent: Intent) {
        if(!trustedPackages.containsKey(receivingPackage)) {
            Log.w(LOG_TAG, "Can't send to not trusted package $receivingPackage")
            return
        }

        val serviceIntent = intent.clone() as Intent
        serviceIntent.putExtra("key", getKey(receivingPackage))
        serviceIntent.component = ComponentName(receivingPackage, MessageReceiver::class.qualifiedName)
        try {
            context.startService(serviceIntent)
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "", e) // often throw after update mbw application with exception "process is bad"
        }

    }

    companion object {
        private var INSTANCE: CommunicationManager? = null

        @Synchronized
        @JvmStatic
        fun init(context: Context, spvApiVersion: Int) {
            if (INSTANCE == null) {
                INSTANCE = CommunicationManager(context, spvApiVersion)
            }
        }

        @Synchronized
        @JvmStatic
        fun getInstance(): CommunicationManager {
            return INSTANCE
                    ?: throw Error("Call init first, ideally from your Application's onCreate()")
        }
    }
}

/**
 * Security meta data of a package.
 *
 * @property name the full name of a package.
 * @property signature the base64 encoded sha256 hash
 * of the [android.content.pm.PackageInfo.signature][signature]
 * @property key the "session ID" the [name][target package] and us
 */
private data class PackageMetaData(
        val name: String,
        val signature: String,
        var key: Long? = null)

private data class TrustConfiguration(
        val packages: Array<PackageMetaData>)
