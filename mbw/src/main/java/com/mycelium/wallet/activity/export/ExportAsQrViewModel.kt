package com.mycelium.wallet.activity.export

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.ExportableAccount


class ExportAsQrViewModel(val context: Application) : AndroidViewModel(context) {
    private val mbwManager = MbwManager.getInstance(context)!!
    private lateinit var model: ExportAsQrModel
    private lateinit var accountData: ExportableAccount.Data
    private var isHdAccount = false
    private var privateDataSelected: MutableLiveData<Boolean> = MutableLiveData()  // whether user switched to private
    var showRedWarning: MutableLiveData<Boolean> = MutableLiveData()       // show warning instead of qr for private
    private var hasWarningAccepted = false

    fun init(accountData: ExportableAccount.Data, isHdAccount: Boolean) {
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }
        model = ExportAsQrModel(context, accountData)
        this.accountData = accountData
        this.isHdAccount = isHdAccount

        System.out.println("exportasmvvm oncreate")
        updateBools(false)
    }

    fun hasPrivateData(): Boolean = accountData.privateData.isPresent

    fun isHdAccount(): Boolean = isHdAccount

    fun updateBools(privateDataSelected: Boolean) {
        this.privateDataSelected.value = privateDataSelected
        if (privateDataSelected) {
            showRedWarning.value = !hasWarningAccepted
        } else {
            showRedWarning.value = false
        }
    }

    // must return false to bind with onLongClick in xml
    fun acceptWarning() : Boolean {
        hasWarningAccepted = true
        showRedWarning.value = false
        return false
    }

    /**
     * Returns private(DEFAULT)/public key as string
     */
    fun getData(): String {
        return when (privateDataSelected.value!!) {
            true -> accountData.privateData.get()
            false -> accountData.publicData.get()
        }
    }

    /**
     * Returns private/public key as string with spaces for readability
     */
    fun getReadableData(): String {
        val data = getData()

        // split the date in fragments with 8chars and a newline after three parts
        val builder = StringBuilder()
        var cnt = 0
        for (part in Utils.stringChopper(data, 8)) {
            cnt++
            builder.append(part)
            builder.append(if (cnt % 3 == 0) "\n" else " ")
        }

        return builder.toString()
    }

    fun exportDataToClipboard() {
        val data = getData()

        if (privateDataSelected.value!!) {
//            val builder = AlertDialog.Builder(context)
//            builder.setMessage(R.string.export_to_clipboard_warning).setCancelable(false)
//                    .setPositiveButton(R.string.yes) { dialog, id ->
            Utils.setClipboardString(data, context)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
//                        dialog.dismiss()
//                    }.setNegativeButton(R.string.no) { dialog, id -> }
//            val alertDialog = builder.create()
//            alertDialog.show()
        } else {
            Utils.setClipboardString(data, context)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareData() {
        val data = getData()

        if (privateDataSelected.value!!) {
//            val builder = AlertDialog.Builder(context)
//            builder.setMessage(R.string.export_share_warning).setCancelable(false)
//                    .setPositiveButton(R.string.yes) { dialog, id ->
            val s = Intent(Intent.ACTION_SEND)
            s.type = "text/plain"
            s.putExtra(Intent.EXTRA_SUBJECT, context.resources.getString(R.string.xpriv_title))
            s.putExtra(Intent.EXTRA_TEXT, data)
            context.startActivity(Intent.createChooser(s, context.resources.getString(R.string.share_xpriv)))
//                        dialog.dismiss()
//                    }.setNegativeButton(R.string.no) { dialog, id -> }
//            val alertDialog = builder.create()
//            alertDialog.show()
        } else {
            val s = Intent(Intent.ACTION_SEND)
            s.type = "text/plain"
            s.putExtra(Intent.EXTRA_SUBJECT, context.resources.getString(R.string.xpub_title))
            s.putExtra(Intent.EXTRA_TEXT, data)
            context.startActivity(Intent.createChooser(s, context.resources.getString(R.string.share_xpub)))
        }
    }

    override fun onCleared() = model.onCleared()
}