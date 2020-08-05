package fiofoundation.io.fiosdk.errors

import com.google.gson.GsonBuilder

open class FIOError: Exception {
    constructor(): super()

    constructor(message: String): super(message)

    constructor(message: String, exception: Exception): super(message,exception)

    constructor(exception: Exception): super(exception)

    fun toJson(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(this,this.javaClass)
    }
}