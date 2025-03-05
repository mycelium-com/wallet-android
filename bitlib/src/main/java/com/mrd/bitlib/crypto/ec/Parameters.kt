/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrd.bitlib.crypto.ec

import com.mrd.bitlib.util.HexUtils
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

object Parameters {
    @JvmField
    val curve: Curve
    @JvmField
    val G: Point
    @JvmField
    val n: BigInteger
    @JvmField
    val h: BigInteger
    /**
     * The maximum number a signature can have in version 3 transactions
     */
    @JvmField
    val MAX_SIG_S: BigInteger

    // bouncy G test show bouncy multiply is faster
    @JvmField
    val _G: ECPoint
    @JvmField
    val _curve: ECCurve

    // test mode implementation for bouncy
    @JvmField
    val USE_BOUNCY = false

    val p = BigInteger(1,
            HexUtils.toBytes("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F"))

    private val Gx = "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798"
    private val Gy = "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8"

    init {
        val a = BigInteger.ZERO
        val b = BigInteger.valueOf(7)
        curve = Curve(p, a, b)
        G = curve.decodePoint(HexUtils.toBytes("04" + Gx + Gy))
        n = BigInteger(1, HexUtils.toBytes("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"))
        h = BigInteger.ONE
        MAX_SIG_S = BigInteger(1, HexUtils.toBytes("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0"))

        //bouncy
        _curve = ECNamedCurveTable.getParameterSpec("secp256k1").curve
        _G = _curve.createPoint(BigInteger(Gx, 16), BigInteger(Gy, 16))
    }
}
