package fiofoundation.io.androidfioserializationprovider

import fiofoundation.io.fiosdk.errors.serializationprovider.SerializationProviderError

class AbiFIOContextNullError : SerializationProviderError {
    constructor():super()
    constructor(message: String):super(message)
    constructor(exception: Exception):super(exception)
    constructor(message: String,exception: Exception):super(message,exception)
}