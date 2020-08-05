package com.mycelium.wapi.wallet.coins.families;

import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public abstract class EOSBasedCryptoCurrency extends CryptoCurrency {
    {
        family = Families.EOS;
        isUtxosBased = true;
    }
}
