package fiofoundation.io.fiosdk.models.fionetworkprovider

import com.google.gson.annotations.SerializedName

class SentFIORequestContent: FIORequestContent()
{
    @field:SerializedName("status") var status:String = ""
}