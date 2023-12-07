/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */
package com.mycelium.wallet

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.common.base.Strings
import com.mycelium.wallet.databinding.EnterTrezorPinDialogBinding

class TrezorPinDialog(context: Context, hidden: Boolean) : PinDialog(context, hidden, true) {
    private var pinDisp: TextView? = null

    override fun loadLayout() {
        setContentView(EnterTrezorPinDialogBinding.inflate(layoutInflater).apply {
            numpadBinding = this.keyboard.numPad
        }.root)
    }

    override fun initPinPad() {
        pinDisp = findViewById<View>(R.id.pin_display) as TextView?
        findViewById<View>(R.id.pin_button0)!!.visibility = View.INVISIBLE

        // reorder the Buttons for the trezor PIN-entry (like a NUM-Pad)
        buttons = numpadBinding?.let {
            listOf(
                it.pinButton7, it.pinButton8, it.pinButton9, it.pinButton4, it.pinButton5,
                it.pinButton6, it.pinButton1, it.pinButton2, it.pinButton3
            )
        }.orEmpty()
        numpadBinding?.pinBack?.setText("OK")
        enteredPin = ""
        var cnt = 0
        for (b in buttons) {
            val akCnt = cnt
            b.setOnClickListener { addDigit((akCnt + 1).toString()) }
            b.text = "\u2022" // unicode "bullet"
            cnt++
        }
        numpadBinding?.pinBack?.setOnClickListener {
            acceptPin()
        }
        numpadBinding?.pinClr?.setOnClickListener {
            clearDigits()
            updatePinDisplay()
        }
    }

    override fun updatePinDisplay() {
        pinDisp!!.text =
            Strings.repeat("\u25CF  ", enteredPin.length) // Unicode Character 'BLACK CIRCLE'
        checkPin()
    }

    override fun checkPin() {
        if (enteredPin.length >= 9) {
            acceptPin()
        }
    }
}