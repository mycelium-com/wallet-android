package com.mrd.bitlib.crypto

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.mrd.bitlib.util.HexUtils.toBytes

enum class HDKeyMagic(val isPrivate: Boolean, val isProdnet: Boolean, val typeToMagicMap: BiMap<BipDerivationType, ByteArray>) {
    PRODNET_PUBLIC(false, true,
            mapOf(BipDerivationType.BIP44 to toBytes("04 88 B2 1E"),    // xpub
                    BipDerivationType.BIP49 to toBytes("04 9d 7c b2")).toBiMap()),    // ypub
    //                    toBytes("04 b2 47 46") to BipDerivationType.BIP44)),  // zpub // TODO SegWit fix bech
    TESTNET_PUBLIC(false, false,
            mapOf(BipDerivationType.BIP44 to toBytes("04 35 87 CF"),    // tpub
                    BipDerivationType.BIP49 to toBytes("04 4a 52 62")).toBiMap()),    // upub
    //                    toBytes("04 5f 1c f6"))),  // vpub
    PRODNET_PRIVATE(true, true,
            mapOf(BipDerivationType.BIP44 to toBytes("04 88 AD E4"),    // xprv
                    BipDerivationType.BIP49 to toBytes("04 9d 78 78")).toBiMap()),    // yprv
    //                    toBytes("04 b2 43 0c"))),  // zprv
    TESTNET_PRIVATE(true, false,
            mapOf(BipDerivationType.BIP44 to toBytes("04 35 83 94"),    // tprv
                    BipDerivationType.BIP49 to toBytes("04 4a 4e 28")).toBiMap())    // uprv
//                    toBytes("04 5f 18 bc")))   // vprv;
}

fun <K, V> Map<K, V>.toBiMap() = HashBiMap.create(this)!!