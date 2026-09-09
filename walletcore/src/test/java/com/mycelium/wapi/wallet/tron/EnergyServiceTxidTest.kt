package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.tron.energy.EnergyServiceManager.Companion.asTxid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The energy service partner reports the hashes of the transactions it broadcast for us and those
 * end up in the local history, where every txid is hex-decoded again on each read. Anything that
 * would not survive that round trip has to be rejected before it is stored.
 */
class EnergyServiceTxidTest {
    private val txid = "8c8272bfe21310ee77f1b5bc0a4fada8db2138e89fff1477e178855477308a6f"

    @Test
    fun acceptsAHash() {
        assertEquals(txid, asTxid(txid))
        assertEquals(txid, asTxid(" $txid "))
        assertEquals(txid, asTxid("0x$txid"))
        assertEquals(txid, asTxid(txid.uppercase()))
    }

    @Test
    fun rejectsWhatIsNotAHash() {
        assertNull(asTxid(null))
        assertNull(asTxid(""))
        assertNull(asTxid("pending"))
        assertNull(asTxid(txid.dropLast(1)))          // wrong length
        assertNull(asTxid(txid.dropLast(1) + "z"))    // not hex
    }
}
