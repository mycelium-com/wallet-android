package fiofoundation.io.fiosdk.models.fionetworkprovider.request

import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.models.fionetworkprovider.Transaction


class GetRequiredKeysRequest(
    @SerializedName("available_keys") var availableKeys: List<String>?,
    var transaction: Transaction?)