package fiofoundation.io.fiosdk.models

import java.util.Objects

class FIOName (var accountName: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        val fioName = other as FIOName?

        return accountName == fioName!!.accountName
    }

    override fun hashCode(): Int {
        return Objects.hash(accountName)
    }
}