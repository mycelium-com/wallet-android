package fiofoundation.io.fiosdk.errors.formatters

import fiofoundation.io.fiosdk.errors.FIOError

class FIOFormatterError: FIOError {
    constructor():super()
    constructor(message: String):super(message)
    constructor(exception: Exception):super(exception)
    constructor(message: String,exception: Exception):super(message,exception)
}