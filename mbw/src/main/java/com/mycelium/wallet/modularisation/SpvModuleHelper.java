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

package com.mycelium.wallet.modularisation;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.mycelium.spvmodule.TransactionFee;
import com.mycelium.spvmodule.providers.TransactionContract;

public class SpvModuleHelper {

    public static boolean isValidQrSendRequest(ContentResolver contentResolver, String spvModuleName, int accountIndex, String content) {

        // sample data:
        // dash:yVkBeQYpDh2mmKHJoph3drcMtRL1u8ueDB
        // bitcoin:mt9qaetTyTeaTzLGgcMHDVyW3o2DjwU6vm

        Uri uri = TransactionContract.ValidateQrCode.CONTENT_URI(spvModuleName);
        Cursor cursor = null;
        try {
            String selection = TransactionContract.ValidateQrCode.SELECTION_COMPLETE;
            String[] selectionArgs = new String[]{Integer.toString(accountIndex), content};
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                boolean isValidAddress = cursor.getInt(cursor.getColumnIndex(TransactionContract.ValidateQrCode.IS_VALID)) == 1;
                return isValidAddress;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    public static long calculateMaxSpendableAmount(ContentResolver contentResolver, String spvModuleName, int accountIndex, TransactionFee txFee, float txFeeFactor) {

        Uri uri = TransactionContract.CalculateMaxSpendable.CONTENT_URI(spvModuleName);
        Cursor cursor = null;
        try {
            String selection = TransactionContract.CalculateMaxSpendable.SELECTION_COMPLETE;
            String[] selectionArgs = new String[]{Integer.toString(accountIndex), txFee.name(), Float.toString(txFeeFactor)};
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                long maxSpendableAmount = cursor.getLong(cursor.getColumnIndex(TransactionContract.CalculateMaxSpendable.MAX_SPENDABLE));
                return maxSpendableAmount;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0L;
    }

    public static CheckSendAmountResult checkSendAmount(ContentResolver contentResolver, String spvModuleName, int accountIndex, TransactionFee txFee, float txFeeFactor, Long amountToSend) {

        Uri uri = TransactionContract.CheckSendAmount.CONTENT_URI(spvModuleName);
        Cursor cursor = null;
        try {
            String selection = TransactionContract.CheckSendAmount.SELECTION_COMPLETE;
            String[] selectionArgs = new String[]{Integer.toString(accountIndex), txFee.name(), Float.toString(txFeeFactor), Long.toString(amountToSend)};
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                String resultStr = cursor.getString(cursor.getColumnIndex(TransactionContract.CheckSendAmount.RESULT));
                TransactionContract.CheckSendAmount.Result result = TransactionContract.CheckSendAmount.Result.valueOf(resultStr);
                switch (result) {
                    case RESULT_OK: {
                        return CheckSendAmountResult.OK;
                    }
                    case RESULT_NOT_ENOUGH_FUNDS: {
                        return CheckSendAmountResult.NOT_ENOUGH_FUNDS;
                    }
                    case RESULT_INVALID: {
                        return CheckSendAmountResult.INVALID;
                    }
                    default: {
                        throw new IllegalStateException("Unsupported result " + result);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return CheckSendAmountResult.INVALID;
    }

    public enum CheckSendAmountResult {
        OK,
        NOT_ENOUGH_FUNDS,
        INVALID
    }
}
