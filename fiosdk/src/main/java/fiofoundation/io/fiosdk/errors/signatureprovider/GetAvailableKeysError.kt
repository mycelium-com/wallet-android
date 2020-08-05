package fiofoundation.io.fiosdk.errors.signatureprovider

class GetAvailableKeysError: SignatureProviderError {
    constructor():super()
    constructor(message: String):super(message)
    constructor(exception: Exception):super(exception)
    constructor(message: String,exception: Exception):super(message,exception)
}