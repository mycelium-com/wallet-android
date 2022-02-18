package com.mycelium.wapi.content.fio

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.CryptoUriParser
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import com.mycelium.wapi.wallet.fio.coins.FIOTest


class FIOUriParser(override val network: NetworkParameters)
    : CryptoUriParser(network, "FIO", FIOMain, FIOTest)