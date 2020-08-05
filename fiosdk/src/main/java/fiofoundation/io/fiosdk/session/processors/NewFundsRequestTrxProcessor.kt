package fiofoundation.io.fiosdk.session.processors

import fiofoundation.io.fiosdk.errors.ErrorConstants
import fiofoundation.io.fiosdk.errors.fionetworkprovider.PushTransactionError
import fiofoundation.io.fiosdk.errors.session.TransactionPushTransactionError
import fiofoundation.io.fiosdk.interfaces.IABIProvider
import fiofoundation.io.fiosdk.interfaces.IFIONetworkProvider
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.interfaces.ISignatureProvider
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.PushTransactionRequest
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse

class NewFundsRequestTrxProcessor(serializationProvider: ISerializationProvider,
                                     fioNetworkProvider: IFIONetworkProvider,
                                     abiProvider: IABIProvider,
                                     signatureProvider: ISignatureProvider
) : TransactionProcessor(serializationProvider,fioNetworkProvider,abiProvider,signatureProvider)
{
    @Throws(TransactionPushTransactionError::class)
    override fun pushTransaction(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse {
        try
        {
            return fioNetworkProvider.requestNewFunds(pushTransactionRequest)
        }
        catch (pushTransactionError: PushTransactionError)
        {
            throw TransactionPushTransactionError(
                ErrorConstants.TRANSACTION_PROCESSOR_RPC_PUSH_TRANSACTION,
                pushTransactionError)
        }
    }


}