package fiofoundation.io.fiosdk.session

import fiofoundation.io.fiosdk.interfaces.ISignatureProvider
import fiofoundation.io.fiosdk.interfaces.IABIProvider
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.errors.session.TransactionProcessorConstructorInputError
import fiofoundation.io.fiosdk.interfaces.IFIONetworkProvider
import fiofoundation.io.fiosdk.models.fionetworkprovider.Transaction
import fiofoundation.io.fiosdk.session.processors.TransactionProcessor


class TransactionSession(val serializationProvider: ISerializationProvider,
                         val fioNetworkProvider: IFIONetworkProvider,
                         val abiProvider: IABIProvider,
                         val signatureProvider: ISignatureProvider) {

    fun getTransactionProcessor(): TransactionProcessor
    {
        return TransactionProcessor(
            this.serializationProvider, this.fioNetworkProvider,
            this.abiProvider, this.signatureProvider
        )
    }

    @Throws(TransactionProcessorConstructorInputError::class)
    fun getTransactionProcessor(transaction: Transaction): TransactionProcessor
    {
        return TransactionProcessor(
            this.serializationProvider, this.fioNetworkProvider,
            this.abiProvider, this.signatureProvider, transaction
        )
    }

}