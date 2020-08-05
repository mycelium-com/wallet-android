package fiofoundation.io.fiosdk.implementations

import fiofoundation.io.fiosdk.errors.ErrorConstants
import fiofoundation.io.fiosdk.errors.abiprovider.GetAbiError
import fiofoundation.io.fiosdk.errors.fionetworkprovider.GetFeeError
import fiofoundation.io.fiosdk.formatters.ByteFormatter
import fiofoundation.io.fiosdk.interfaces.IABIProvider
import fiofoundation.io.fiosdk.interfaces.IFIONetworkProvider
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.GetRawAbiRequest
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.models.FIOName
import java.util.concurrent.ConcurrentHashMap


class ABIProvider(private val fioNetworkProvider:IFIONetworkProvider,private val serializationProvider: ISerializationProvider): IABIProvider
{
    private val abiCache:ConcurrentHashMap<String,String> = ConcurrentHashMap()

    @Throws(GetAbiError::class)
    override fun getAbis(chainId: String, accounts: List<FIOName>): Map<String, String>
    {
        val returnAbis:HashMap<String,String> = HashMap()
        val uniqAccounts = ArrayList(HashSet(accounts))

        for (account in uniqAccounts) {
            val abiJsonString = getAbi(chainId, account)
            returnAbis.put(account.accountName, abiJsonString)
        }

        return returnAbis
    }

    @Throws(GetAbiError::class)
    override fun getAbi(chainId: String, account: FIOName): String
    {
        var abiJsonString: String?
        val cacheKey = chainId + account.accountName

        abiJsonString = this.abiCache[cacheKey]
        if (!abiJsonString.isNullOrEmpty())
            return abiJsonString

        val getRawAbiRequest = GetRawAbiRequest(account.accountName)

        try
        {
            val getRawAbiResponse = try{this.fioNetworkProvider.getRawAbi(getRawAbiRequest)} catch(e: GetFeeError){throw GetAbiError(ErrorConstants.NO_RESPONSE_RETRIEVING_ABI)}

            val abi = getRawAbiResponse.abi

            if (abi.isNullOrEmpty()) {
                throw GetAbiError(ErrorConstants.MISSING_ABI_FROM_RESPONSE)
            }

            val abiByteFormatter = ByteFormatter.createFromBase64(abi)

            val calculatedHash = abiByteFormatter.sha256().toHex().toLowerCase()
            val verificationHash = getRawAbiResponse.abiHash.toLowerCase()

            if (calculatedHash != verificationHash) {
                throw GetAbiError(ErrorConstants.CALCULATED_HASH_NOT_EQUAL_RETURNED)
            }

            if (account.accountName != getRawAbiResponse.accountName) {
                throw GetAbiError(ErrorConstants.REQUESTED_ACCCOUNT_NOT_EQUAL_RETURNED)
            }

            abiJsonString = this.serializationProvider.deserializeAbi(abiByteFormatter.toHex())

            if (abiJsonString.isEmpty()) {
                throw GetAbiError(ErrorConstants.NO_ABI_FOUND)
            }

            this.abiCache[cacheKey] = abiJsonString

        } catch (ex: Exception)
        {
            throw GetAbiError(ErrorConstants.ERROR_RETRIEVING_ABI, ex)
        }

        return abiJsonString
    }
}