package com.mycelium.wallet.activity.util

import android.content.Intent
import android.net.Uri
import android.support.v4.app.FragmentManager
import com.google.common.base.Preconditions
import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.activity.StringHandlerActivity.*
import com.mycelium.wallet.activity.modern.adapter.SelectAssetDialog
import com.mycelium.wallet.bitid.BitIDSignRequest
import com.mycelium.wallet.content.ResultType
import com.mycelium.wallet.pop.PopRequest
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.WalletManager
import java.util.*


fun Intent.getPrivateKey(): InMemoryPrivateKey {
    checkType(ResultType.PRIVATE_KEY)
    val key = getSerializableExtra(RESULT_PRIVATE_KEY) as InMemoryPrivateKey
    Preconditions.checkNotNull(key)
    return key
}

fun Intent.getHdKeyNode(): HdKeyNode {
    checkType(ResultType.HD_NODE)
    val hdKeyNode = getSerializableExtra(RESULT_HD_NODE) as HdKeyNode
    Preconditions.checkNotNull(hdKeyNode)
    return hdKeyNode
}

fun Intent.getAddress(walletManager: WalletManager, fragmentManager: FragmentManager) {
    checkType(ResultType.ADDRESS_STRING)
    val address = getStringExtra(RESULT_ADDRESS_STRING_KEY)
    Preconditions.checkNotNull(address)
    val addresses = walletManager.parseAddress(address)
    val dialog = SelectAssetDialog.getInstance(addresses)
    dialog.show(fragmentManager, "dialog")
}

fun Intent.getAddress(): GenericAddress {
    checkType(ResultType.ADDRESS)
    val address = getSerializableExtra(RESULT_ADDRESS_KEY) as GenericAddress
    Preconditions.checkNotNull(address)
    return address
}

fun Intent.getAssetUri(): GenericAssetUri {
    checkType(ResultType.ASSET_URI)
    val uri = getSerializableExtra(RESULT_URI_KEY) as GenericAssetUri
    Preconditions.checkNotNull(uri)
    return uri
}

fun Intent.getUri(): Uri {
    checkType(ResultType.URI)
    val uri = getSerializableExtra(RESULT_URI_KEY) as Uri
    Preconditions.checkNotNull(uri)
    return uri
}

fun Intent.getShare(): BipSss.Share {
    checkType(ResultType.SHARE)
    val share = getSerializableExtra(RESULT_SHARE_KEY) as BipSss.Share
    Preconditions.checkNotNull(share)
    return share
}

fun Intent.getAccount(): UUID {
    checkType(ResultType.ACCOUNT)
    val account = getSerializableExtra(RESULT_ACCOUNT_KEY) as UUID
    Preconditions.checkNotNull(account)
    return account
}

fun Intent.getMasterSeed(): Bip39.MasterSeed {
    checkType(ResultType.MASTER_SEED)
    val result = getSerializableExtra(RESULT_MASTER_SEED_KEY) as Bip39.MasterSeed
    Preconditions.checkNotNull(result)
    return result
}

fun Intent.getPopRequest(): PopRequest {
    checkType(ResultType.POP_REQUEST)
    val result = getSerializableExtra(RESULT_POP_REQUEST) as PopRequest
    Preconditions.checkNotNull(result)
    return result
}

fun Intent.getBitIdRequest(): BitIDSignRequest {
    checkType(ResultType.BIT_ID_REQUEST)
    val result = getSerializableExtra(RESULT_BIT_ID_REQUEST) as BitIDSignRequest
    Preconditions.checkNotNull(result)
    return result
}

private fun Intent.checkType(type: ResultType) {
    Preconditions.checkState(type === getSerializableExtra(RESULT_TYPE_KEY))
}