package com.mycelium.wallet.external.vip

import com.mycelium.wallet.external.DefaultJsonRpcRequest
import com.mycelium.wallet.external.vip.model.ActivateVipRequest
import com.mycelium.wallet.external.vip.model.ActivateVipResponse
import com.mycelium.wallet.external.vip.model.CheckVipResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface to describing VIP mycelium API for retrofit2 library and providing retrofit object intialization.
 */
interface VipAPI {
    @POST("activate")
    suspend fun activate(@Body body: ActivateVipRequest): ActivateVipResponse

    @POST("check")
    suspend fun check(
        @Body body: DefaultJsonRpcRequest = DefaultJsonRpcRequest(),
    ): CheckVipResponse
}
