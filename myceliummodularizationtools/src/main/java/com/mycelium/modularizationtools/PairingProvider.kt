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
    var communicationManager: CommunicationManager? = null

    override fun onCreate(): Boolean {
        communicationManager = CommunicationManager.getInstance(context)
        return true
    }

    /**
     * Heavy hack ahead! Sorry!
     *
     * Query gets abused as the pair function. A calling package updates the session ID with us.
     *
     * This will fail if no selection String was provided or the selection cannot be converted to a
     * Long other than 0 or the callingPackage is unknown or the callingPackage was signed by an
     * unexpected key.
     */
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val key = selection!!.toLong()
        communicationManager!!.pair(key, callingPackage)
        val cursor = MatrixCursor(arrayOf("name", "description"))
        cursor.addRow(arrayOf(context.getString(R.string.module_name), context.getString(R.string.module_description)))
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
