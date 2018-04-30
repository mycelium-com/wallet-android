package com.mycelium.modularizationtools

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * This ContentProvider serves to authenticate one app against another.
 * The calling package is known to ContentProviders, which is why this is a ContentProvider.
 * This package contains a list of all accepted packages and signatures, against which a test is run during the query call.
 *
 */
open class PairingProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    /**
     * Heavy hack ahead! Sorry!
     *
     * Query gets abused as the pair function. A calling package updates the sessionKey with us.
     *
     * This will fail if no [selectionArgs] were provided or the [selectionArgs] cannot be converted to
     * valid sessionKey and version or the callingPackage is unknown or the callingPackage was signed by an
     * unexpected key.
     */
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val sessionKey = selectionArgs!![0].toLong()
        val version = selectionArgs[1].toInt()
        CommunicationManager.getInstance().pair(callingPackage, sessionKey, version)
        val cursor = MatrixCursor(arrayOf("name", "shortName", "description"))
        cursor.addRow(arrayOf(context.getString(R.string.module_name), context.getString(R.string.module_short_name),context.getString(R.string.module_description)))
        return cursor
    }

    override fun getType(uri: Uri): String? = throw UnsupportedOperationException("not implemented")

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("not implemented")

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int = throw UnsupportedOperationException("not implemented")

    override fun update(uri: Uri, values: ContentValues?, where: String?, whereArgs: Array<String>?): Int = throw UnsupportedOperationException("not implemented")

    companion object {
        private val LOG_TAG = PairingProvider::class.java.canonicalName
    }
}
