package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.GsonBuilder
import java.io.Serializable

open class FIOResponse: Serializable {
    fun toJson(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(this,this.javaClass)
    }
}