package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.models.fionetworkprovider.ObtDataRecord

class GetObtDataResponse: FIOResponse()
{
    @field:SerializedName("obt_data_records") var records: ArrayList<ObtDataRecord>
    @field:SerializedName("more") var more: Int=0

    init {
        records = arrayListOf()
    }
}