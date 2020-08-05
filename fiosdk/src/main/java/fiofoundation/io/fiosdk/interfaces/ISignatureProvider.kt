package fiofoundation.io.fiosdk.interfaces

import fiofoundation.io.fiosdk.errors.signatureprovider.GetAvailableKeysError
import fiofoundation.io.fiosdk.errors.signatureprovider.SignTransactionError

import fiofoundation.io.fiosdk.models.signatureprovider.FIOTransactionSignatureRequest
import fiofoundation.io.fiosdk.models.signatureprovider.FIOTransactionSignatureResponse

interface ISignatureProvider {

    @Throws(GetAvailableKeysError::class)
    fun getAvailableKeys(): List<String>

    @Throws(SignTransactionError::class)
    fun signTransaction(fioTransactionSignatureRequest: FIOTransactionSignatureRequest): FIOTransactionSignatureResponse
}