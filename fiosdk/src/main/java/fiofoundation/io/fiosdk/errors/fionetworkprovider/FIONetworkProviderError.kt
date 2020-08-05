package fiofoundation.io.fiosdk.errors.fionetworkprovider

import fiofoundation.io.fiosdk.errors.FIOError

open class FIONetworkProviderError: FIOError {
    constructor():super()
    constructor(message: String): super(message)
    constructor(message: String, exception: Exception): super(message,exception)
    constructor(exception: Exception): super(exception)
}