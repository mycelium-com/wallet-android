package fiofoundation.io.fiosdk.errors.serializationprovider

import fiofoundation.io.fiosdk.errors.FIOError

open class SerializationProviderError: FIOError{
    constructor():super()
    constructor(message: String):super(message)
    constructor(exception: Exception):super(exception)
    constructor(message: String,exception: Exception):super(message,exception)
}