package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking

class ColuFeeProvider(testnet: Boolean, wapi: Wapi, feeBacking: FeeEstimationsBacking) :
        BtcFeeProvider(testnet, wapi, feeBacking)