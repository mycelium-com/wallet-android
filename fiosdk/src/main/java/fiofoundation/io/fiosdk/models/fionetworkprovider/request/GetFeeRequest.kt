package fiofoundation.io.fiosdk.models.fionetworkprovider.request

import com.google.gson.annotations.SerializedName

class GetFeeRequest (@field:SerializedName("end_point") var endPoint: String,
                     @field:SerializedName("fio_address") var fioAddress: String) : FIORequest()