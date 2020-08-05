package fiofoundation.io.fiosdk.models.signatureprovider

class FIOTransactionSignatureRequest(var serializedTransaction: String?,var signingPublicKeys: List<String>?,var chainId: String?,var abis: List<BinaryAbi>?,var isModifiable: Boolean=false)