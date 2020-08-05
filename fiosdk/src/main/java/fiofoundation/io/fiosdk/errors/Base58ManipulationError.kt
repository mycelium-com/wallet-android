package fiofoundation.io.fiosdk.errors

import fiofoundation.io.fiosdk.errors.FIOError

class Base58ManipulationError: FIOError {
    constructor():super()
    constructor(message: String):super(message)
    constructor(exception: Exception):super(exception)
    constructor(message: String,exception: Exception):super(message,exception)
}