package fiofoundation.io.fiosdk.errors.abiprovider

import fiofoundation.io.fiosdk.errors.FIOError

open class AbiProviderError : FIOError
{
    constructor():super()
    constructor(message: String):super(message)
    constructor(exception: Exception):super(exception)
    constructor(message: String,exception: Exception):super(message,exception)
}