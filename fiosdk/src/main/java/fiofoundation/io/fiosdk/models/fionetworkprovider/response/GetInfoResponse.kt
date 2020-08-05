package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class GetInfoResponse: FIOResponse() {

    @SerializedName("head_block_num")
    val headBlockNumber: BigInteger? = null

    @SerializedName("last_irreversible_block_num")
    val lastIrreversibleBlockNumber: Long = 0

    @SerializedName("server_version")
    val serverVersion: String = ""

    @SerializedName("chain_id")
    val chainId: String? = null

    @SerializedName("head_block_id")
    val headBlockId: String = ""

    @SerializedName("head_block_time")
    val headBlockTime: String = ""

    @SerializedName("head_block_producer")
    val headBlockProducer: String = ""

    @SerializedName("virtual_block_cpu_limit")
    val virtualBlockCpuLimit: BigInteger? = null

    @SerializedName("virtual_block_net_limit")
    val virtualBlockNetLimit: BigInteger? = null

    @SerializedName("block_cpu_limit")
    private val blockCpuLimit: BigInteger? = null

    @SerializedName("block_net_limit")
    private val blockNetLimit: BigInteger? = null

    @SerializedName("server_version_string")
    val serverVersionString: String = ""
}