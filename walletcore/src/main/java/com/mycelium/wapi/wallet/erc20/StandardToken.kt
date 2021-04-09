package com.mycelium.wapi.wallet.erc20

import io.reactivex.Flowable
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.Log
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.Contract
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

/**
 *
 * Auto generated code.
 *
 * **Do not modify!**
 *
 * Please use the [web3j command line tools](https://docs.web3j.io/command_line.html),
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * [codegen module](https://github.com/web3j/web3j/tree/master/codegen) to update.
 *
 *
 * Generated with web3j version 4.5.14.
 */
class StandardToken private constructor(contractAddress: String?, web3j: Web3j?, credentials: Credentials?,
                                        contractGasProvider: ContractGasProvider?)
    : Contract(BINARY, contractAddress, web3j, credentials, contractGasProvider) {

    fun getTransferEvents(transactionReceipt: TransactionReceipt?): List<TransferEventResponse> {
        val valueList = extractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt)
        val responses = ArrayList<TransferEventResponse>(valueList.size)
        for (eventValues in valueList) {
            val typedResponse = TransferEventResponse()
            typedResponse.log = eventValues.log
            typedResponse.from = eventValues.indexedValues[0].value as String
            typedResponse.to = eventValues.indexedValues[1].value as String
            typedResponse.value = eventValues.nonIndexedValues[0].value as BigInteger
            responses.add(typedResponse)
        }
        return responses
    }

    private fun transferEventFlowable(filter: EthFilter?): Flowable<TransferEventResponse> {
        return web3j.ethLogFlowable(filter).map { log: Log? ->
            val eventValues = extractEventParametersWithLog(TRANSFER_EVENT, log)
            val typedResponse = TransferEventResponse()
            typedResponse.log = eventValues.log
            typedResponse.from = eventValues.indexedValues[0].value as String
            typedResponse.to = eventValues.indexedValues[1].value as String
            typedResponse.value = eventValues.nonIndexedValues[0].value as BigInteger
            typedResponse
        }
    }

    fun transferEventFlowable(startBlock: DefaultBlockParameter?, endBlock: DefaultBlockParameter?): Flowable<TransferEventResponse> {
        val filter = EthFilter(startBlock, endBlock, getContractAddress())
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT))
        return transferEventFlowable(filter)
    }

    fun balanceOf(account: String?): RemoteCall<BigInteger> {
        val function = org.web3j.abi.datatypes.Function(FUNC_BALANCEOF,
                listOf(Address(account)),
                listOf(object : TypeReference<Uint256?>() {}) as List<TypeReference<*>>?)
        return executeRemoteCallSingleValueReturn(function, BigInteger::class.java)
    }

    fun transfer(recipient: String?, amount: BigInteger?): RemoteCall<TransactionReceipt> {
        val function = org.web3j.abi.datatypes.Function(
                FUNC_TRANSFER,
                listOf(Address(recipient),
                        Uint256(amount)), emptyList())
        return executeRemoteCallTransaction(function)
    }

    class TransferEventResponse {
        var from: String? = null
        var to: String? = null
        var value: BigInteger? = null
        var log: Log? = null
    }

    companion object {
        const val BINARY = "Bin file was not provided"
        const val FUNC_BALANCEOF = "balanceOf"
        const val FUNC_TRANSFER = "transfer"
        val TRANSFER_EVENT = Event("Transfer",
                listOf(object : TypeReference<Address?>(true) {}, object : TypeReference<Address?>(true) {}, object : TypeReference<Uint256?>() {}))

        fun load(contractAddress: String?, web3j: Web3j?, credentials: Credentials?, contractGasProvider: ContractGasProvider?): StandardToken {
            return StandardToken(contractAddress, web3j, credentials, contractGasProvider)
        }
    }
}