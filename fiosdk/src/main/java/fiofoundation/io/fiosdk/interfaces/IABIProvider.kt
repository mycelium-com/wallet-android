package fiofoundation.io.fiosdk.interfaces

import fiofoundation.io.fiosdk.errors.abiprovider.GetAbiError
import fiofoundation.io.fiosdk.models.FIOName


interface IABIProvider {

    @Throws(GetAbiError::class)
    fun getAbis(chainId: String,accounts: List<FIOName>): Map<String, String>


    @Throws(GetAbiError::class)
    fun getAbi(chainId: String, account:FIOName): String
}