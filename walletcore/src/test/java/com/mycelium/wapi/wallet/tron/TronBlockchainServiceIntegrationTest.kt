package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.tron.remote.TronBlockchainService
import com.mycelium.wapi.wallet.tron.remote.TronEndpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.math.BigInteger

/**
 * Live TronGrid mainnet checks (keyless, ~1 QPS). Run manually:
 * ./gradlew :walletcore:test --tests "*TronBlockchainServiceIntegrationTest*"
 */
@Ignore("network")
class TronBlockchainServiceIntegrationTest {
    private val service = TronBlockchainService(listOf(TronEndpoint("https://api.trongrid.io", System.getenv("TRONGRID_API_KEY"))))

    // USDT contract account: always exists, has TRX balance and a deep tx history
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"

    @Test
    fun blockRef() {
        val ref = service.getBlockRef()
        assertTrue(ref.number > 60_000_000)
        assertEquals(64, ref.blockIdHex.length)
        assertTrue(ref.timestampMs > 1_700_000_000_000)
    }

    @Test
    fun balanceAndResources() {
        val bal = service.getBalance(usdtContract)
        assertTrue(bal.exists)
        val res = service.getAccountResources(usdtContract)
        assertTrue(res.totalNetLimit > 0)
        assertTrue(service.getEnergyPrice() > 0)
    }

    @Test
    fun history() {
        val txs = service.getTransactions(usdtContract)
        assertTrue(txs.isNotEmpty())
        val tx = txs.first()
        assertEquals(64, tx.txid.length)
        assertTrue(tx.from.startsWith("T"))
        val info = service.getTransactionInfo(tx.txid)
        assertTrue(info.blockNumber > 0 || !info.isConfirmed)
    }

    /**
     * End-to-end check of the signing pipeline against a real node without funds: java-tron
     * validates TAPOS and the signature before executing the contract, so a well-formed tx from
     * an empty account is rejected with a contract validation error (no owner account / balance
     * not sufficient) — never with SIGERROR / TAPOS_ERROR / parse errors.
     */
    @Test
    fun broadcastPipeline() {
        val keyPair = org.web3j.crypto.Keys.createEcKeyPair()
        val owner = TronAddress(com.mycelium.wapi.wallet.tron.coins.TronMain, TronKeys.addressOf(keyPair))
        val to = TronAddress(com.mycelium.wapi.wallet.tron.coins.TronMain, "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        val raw = TronTransactionBuilder.buildTransfer(service.getBlockRef(), owner.getBytes(), to.getBytes(), 1)
        val signed = TronTransactionBuilder.encodeSigned(raw.rawData, TronKeys.sign(raw.txId, keyPair))
        val result = service.broadcast(signed)
        println("broadcast: " + result)
        assertTrue(!result.success)
        assertEquals("CONTRACT_VALIDATE_ERROR", result.code)
        assertTrue(result.message?.contains("account", true) == true || result.message?.contains("balance", true) == true)

        // TRC20 path: TriggerSmartContract with fee_limit
        val data = TronTransactionBuilder.trc20TransferData(to, BigInteger.ONE)
        val rawTrc = TronTransactionBuilder.buildTriggerSmartContract(service.getBlockRef(), owner.getBytes(),
            TronAddress(com.mycelium.wapi.wallet.tron.coins.TronMain, usdtContract).getBytes(), data, 15_000_000)
        val signedTrc = TronTransactionBuilder.encodeSigned(rawTrc.rawData, TronKeys.sign(rawTrc.txId, keyPair))
        val resultTrc = service.broadcast(signedTrc)
        println("broadcast trc20: " + resultTrc)
        assertTrue(!resultTrc.success)
        assertEquals("CONTRACT_VALIDATE_ERROR", resultTrc.code)
    }

    @Test
    fun trc20() {
        // Binance hot wallet holds USDT
        val holder = "TAUN6FwrnwwmaEqYcckffC7wYmbaS6cBiX"
        val balance = service.getTrc20Balance(holder, usdtContract)
        assertNotNull(balance)
        assertTrue(balance!! > BigInteger.ZERO)
        val transfers = service.getTrc20Transfers(holder, usdtContract)
        assertTrue(transfers.isNotEmpty())
        val info = service.getTrc20TokenInfo(usdtContract)
        assertEquals("USDT", info?.symbol)
        assertEquals(6, info?.decimals)
        val energy = service.estimateTrc20TransferEnergy(holder, usdtContract, "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC", BigInteger.valueOf(1_000_000))
        assertTrue(energy > 10_000)
    }
}
