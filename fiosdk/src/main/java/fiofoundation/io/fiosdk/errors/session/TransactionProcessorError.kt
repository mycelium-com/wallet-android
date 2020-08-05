package fiofoundation.io.fiosdk.errors.session

import fiofoundation.io.fiosdk.errors.FIOError

open class TransactionProcessorError: FIOError {
    constructor():super()
    constructor(message: String):super(message)
    constructor(exception: Exception):super(exception)
    constructor(message: String,exception: Exception):super(message,exception)
}