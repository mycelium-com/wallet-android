import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.R

fun AddressType.asStringRes() = when(this) {
    AddressType.P2PKH -> R.string.p2pkh
    AddressType.P2WPKH -> R.string.bech
    AddressType.P2SH_P2WPKH -> R.string.p2sh
}

fun AddressType.asShortStringRes() = when(this) {
    AddressType.P2PKH -> R.string.p2pkh_short
    AddressType.P2WPKH -> R.string.bech_short
    AddressType.P2SH_P2WPKH -> R.string.p2sh_short
}
