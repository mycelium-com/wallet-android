package fiofoundation.io.fiosdk.models.signatureprovider

import fiofoundation.io.fiosdk.errors.signatureprovider.SignatureProviderError

class FIOTransactionSignatureResponse (
    val serializedTransaction:String?,
    val signatures: List<String>,
    val error: SignatureProviderError?)