package fiofoundation.io.fiosdk.session.processors

import fiofoundation.io.fiosdk.interfaces.ISignatureProvider
import fiofoundation.io.fiosdk.interfaces.IABIProvider
import fiofoundation.io.fiosdk.interfaces.IFIONetworkProvider
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.models.fionetworkprovider.Transaction
import fiofoundation.io.fiosdk.models.fionetworkprovider.TransactionConfig
import fiofoundation.io.fiosdk.errors.ErrorConstants
import fiofoundation.io.fiosdk.errors.session.TransactionProcessorConstructorInputError
import fiofoundation.io.fiosdk.errors.fionetworkprovider.GetBlockError
import fiofoundation.io.fiosdk.errors.fionetworkprovider.GetInfoError
import fiofoundation.io.fiosdk.errors.session.TransactionPrepareRpcError
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.GetBlockRequest
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.GetBlockResponse
import fiofoundation.io.fiosdk.errors.session.TransactionPrepareError
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.GetInfoResponse
import fiofoundation.io.fiosdk.errors.session.TransactionPrepareInputError
import fiofoundation.io.fiosdk.formatters.DateFormatter
import java.math.BigInteger
import java.text.ParseException
import fiofoundation.io.fiosdk.utilities.Utils
import java.io.IOException
import fiofoundation.io.fiosdk.errors.session.TransactionSignError
import fiofoundation.io.fiosdk.errors.signatureprovider.SignatureProviderError
import fiofoundation.io.fiosdk.errors.session.TransactionGetSignatureError
import fiofoundation.io.fiosdk.errors.session.TransactionCreateSignatureRequestError
import fiofoundation.io.fiosdk.models.signatureprovider.FIOTransactionSignatureRequest
import fiofoundation.io.fiosdk.models.signatureprovider.FIOTransactionSignatureResponse
import fiofoundation.io.fiosdk.errors.session.TransactionGetSignatureDeserializationError
import fiofoundation.io.fiosdk.errors.serializationprovider.DeserializeTransactionError
import fiofoundation.io.fiosdk.errors.session.TransactionGetSignatureNotAllowModifyTransactionError
import fiofoundation.io.fiosdk.errors.session.TransactionGetSignatureSigningError

import fiofoundation.io.fiosdk.errors.fionetworkprovider.PushTransactionError
import fiofoundation.io.fiosdk.errors.session.TransactionPushTransactionError
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.PushTransactionRequest
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse
import fiofoundation.io.fiosdk.errors.session.TransactionCreateSignatureRequestRpcError
import fiofoundation.io.fiosdk.errors.session.TransactionCreateSignatureRequestEmptyAvailableKeyError
import fiofoundation.io.fiosdk.errors.session.TransactionCreateSignatureRequestKeyError
import fiofoundation.io.fiosdk.errors.signatureprovider.GetAvailableKeysError
import fiofoundation.io.fiosdk.errors.session.TransactionBroadCastError
import fiofoundation.io.fiosdk.errors.session.TransactionBroadCastEmptySignatureError
import fiofoundation.io.fiosdk.errors.session.TransactionSignAndBroadCastError

import fiofoundation.io.fiosdk.errors.session.TransactionSerializeError

import fiofoundation.io.fiosdk.errors.session.TransactionCreateSignatureRequestSerializationError
import fiofoundation.io.fiosdk.errors.serializationprovider.SerializeTransactionError

import fiofoundation.io.fiosdk.errors.serializationprovider.SerializeError
import fiofoundation.io.fiosdk.errors.session.TransactionCreateSignatureRequestAbiError
import fiofoundation.io.fiosdk.errors.abiprovider.GetAbiError

import fiofoundation.io.fiosdk.models.FIOName
import fiofoundation.io.fiosdk.models.fionetworkprovider.actions.IAction
import fiofoundation.io.fiosdk.models.serializationprovider.AbiFIOSerializationObject


open class TransactionProcessor(val serializationProvider: ISerializationProvider,
                                val fioNetworkProvider: IFIONetworkProvider,
                           val abiProvider: IABIProvider,
                                val signatureProvider: ISignatureProvider)
{

    @Throws(TransactionProcessorConstructorInputError::class)
    constructor(serializationProvider: ISerializationProvider,
                fioNetworkProvider: IFIONetworkProvider,
                abiProvider: IABIProvider,
                signatureProvider: ISignatureProvider,transaction: Transaction)
            :this(serializationProvider,fioNetworkProvider,abiProvider,signatureProvider){
        if (transaction.actions.isEmpty()) {
            throw TransactionProcessorConstructorInputError(
                    ErrorConstants.TRANSACTION_PROCESSOR_ACTIONS_EMPTY_ERROR_MSG)
        }

        this.transaction = transaction
    }

    var transaction: Transaction? = null

    var originalTransaction: Transaction? = null

    var signatures = ArrayList<String>()

    var serializedTransaction: String? = null

    var availableKeys: List<String>? = null

    var requiredKeys: List<String>? = null

    var transactionConfig = TransactionConfig()

    var chainId: String? = null

    var isTransactionModificationAllowed: Boolean = false

    private fun finishPreparing(preparingTransaction: Transaction)
    {
        this.transaction = preparingTransaction

        if (!this.serializedTransaction.isNullOrEmpty()) {
            this.serializedTransaction = ""
        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun getDeepClone(): Transaction?
    {
        return if (this.transaction != null) Utils.clone(this.transaction!!) else null

    }

    @Throws(TransactionGetSignatureError::class)
    private fun getSignature(fioTransactionSignatureRequest: FIOTransactionSignatureRequest): FIOTransactionSignatureResponse
    {
        val fioTransactionSignatureResponse: FIOTransactionSignatureResponse

        try
        {
            fioTransactionSignatureResponse = this.signatureProvider
                .signTransaction(fioTransactionSignatureRequest)

            if (fioTransactionSignatureResponse.error != null) {
                throw fioTransactionSignatureResponse.error
            }

        }
        catch (signatureProviderError: SignatureProviderError)
        {
            throw TransactionGetSignatureSigningError(
                ErrorConstants.TRANSACTION_PROCESSOR_SIGN_TRANSACTION_ERROR,
                signatureProviderError)
        }

        if (fioTransactionSignatureResponse.serializedTransaction.isNullOrEmpty()) {
            throw TransactionGetSignatureSigningError(
                ErrorConstants.TRANSACTION_PROCESSOR_SIGN_TRANSACTION_TRANS_EMPTY_ERROR
            )
        }

        if (fioTransactionSignatureResponse.signatures.isEmpty()) {
            throw TransactionGetSignatureSigningError(
                ErrorConstants.TRANSACTION_PROCESSOR_SIGN_TRANSACTION_SIGN_EMPTY_ERROR)
        }

        this.originalTransaction = this.transaction

        if (this.serializedTransaction != null && !this.serializedTransaction
                .equals(fioTransactionSignatureResponse.serializedTransaction))
        {
            // Throw error if an unmodifiable transaction is modified
            if (!this.isTransactionModificationAllowed)
            {
                throw TransactionGetSignatureNotAllowModifyTransactionError(
                    ErrorConstants.TRANSACTION_IS_NOT_ALLOWED_TOBE_MODIFIED)
            }

            val transactionJSON: String?
            try
            {
                transactionJSON = this.serializationProvider
                    .deserializeTransaction(fioTransactionSignatureResponse.serializedTransaction)


                if (transactionJSON.isEmpty())
                {
                    throw DeserializeTransactionError(
                        ErrorConstants.TRANSACTION_PROCESSOR_GET_SIGN_DESERIALIZE_TRANS_EMPTY_ERROR)
                }
            }
            catch (deserializeTransactionError: DeserializeTransactionError)
            {
                throw TransactionGetSignatureDeserializationError(
                    ErrorConstants.TRANSACTION_PROCESSOR_GET_SIGN_DESERIALIZE_TRANS_ERROR,
                    deserializeTransactionError)
            }

            this.transaction =
                Utils.getGson(DateFormatter.BACKEND_DATE_PATTERN).fromJson(transactionJSON, Transaction::class.java)
        }

        this.signatures = ArrayList()
        this.signatures.addAll(fioTransactionSignatureResponse.signatures)
        this.serializedTransaction = fioTransactionSignatureResponse.serializedTransaction

        return fioTransactionSignatureResponse
    }

    @Throws(TransactionPushTransactionError::class)
    open fun pushTransaction(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse {
        try
        {
            return this.fioNetworkProvider.pushTransaction(pushTransactionRequest)
        }
        catch (pushTransactionError: PushTransactionError)
        {
            throw TransactionPushTransactionError(
                ErrorConstants.TRANSACTION_PROCESSOR_RPC_PUSH_TRANSACTION,
                pushTransactionError)
        }

    }

    @Throws(TransactionCreateSignatureRequestError::class)
    private fun createSignatureRequest(): FIOTransactionSignatureRequest
    {
        if (this.transaction == null)
        {
            throw TransactionCreateSignatureRequestError(
                ErrorConstants.TRANSACTION_PROCESSOR_TRANSACTION_HAS_TO_BE_INITIALIZED)
        }

        if (this.transaction?.actions!!.isEmpty())
        {
            throw TransactionCreateSignatureRequestError(
                ErrorConstants.TRANSACTION_PROCESSOR_ACTIONS_EMPTY_ERROR_MSG)
        }

        // Cache the serialized version of transaction in the TransactionProcessor
        this.serializedTransaction =  this.serializeTransaction()

        val fioTransactionSignatureRequest = FIOTransactionSignatureRequest(
            this.serializedTransaction, null,
            this.chainId, null, this.isTransactionModificationAllowed)

        if (this.requiredKeys != null && this.requiredKeys?.isNotEmpty()!!)
        {
            fioTransactionSignatureRequest.signingPublicKeys = this.requiredKeys
            return fioTransactionSignatureRequest
        }

        if (this.availableKeys == null || this.availableKeys?.isEmpty()!!)
        {
            try
            {
                this.availableKeys = this.signatureProvider.getAvailableKeys()

            }
            catch (getAvailableKeysError: GetAvailableKeysError)
            {
                throw TransactionCreateSignatureRequestKeyError(
                    ErrorConstants.TRANSACTION_PROCESSOR_GET_AVAILABLE_KEY_ERROR,
                    getAvailableKeysError)
            }

            if (this.availableKeys?.isEmpty()!!)
            {
                throw TransactionCreateSignatureRequestEmptyAvailableKeyError(
                    ErrorConstants.TRANSACTION_PROCESSOR_GET_AVAILABLE_KEY_EMPTY)
            }
        }

        fioTransactionSignatureRequest.signingPublicKeys = this.availableKeys

        return fioTransactionSignatureRequest
    }

    @Throws(TransactionCreateSignatureRequestError::class)
    private fun serializeAction(action: IAction,
                                chainId: String,
                                abiProvider: IABIProvider): AbiFIOSerializationObject
    {
        val actionAbiJSON: String

        try
        {
            actionAbiJSON = abiProvider.getAbi(chainId, FIOName(action.account))
        }
        catch (getAbiError: GetAbiError)
        {
            throw TransactionCreateSignatureRequestAbiError(
                String.format(
                    ErrorConstants.TRANSACTION_PROCESSOR_GET_ABI_ERROR, action.account),
                getAbiError)
        }

        val actionAbifioSerializationObject = AbiFIOSerializationObject(action.account,
            action.name, null, actionAbiJSON)

        actionAbifioSerializationObject.hex = ""

        actionAbifioSerializationObject.json = action.data

        try {
            this.serializationProvider.serialize(actionAbifioSerializationObject)
            if (actionAbifioSerializationObject.hex.isEmpty())
            {
                throw TransactionCreateSignatureRequestSerializationError(
                    ErrorConstants.TRANSACTION_PROCESSOR_SERIALIZE_ACTION_WORKED_BUT_EMPTY_RESULT
                )
            }
        }
        catch (serializeError: SerializeError)
        {
            throw TransactionCreateSignatureRequestSerializationError(
                String.format(
                    ErrorConstants.TRANSACTION_PROCESSOR_SERIALIZE_ACTION_ERROR,
                    action.account), serializeError)
        }
        catch (serializeError: TransactionCreateSignatureRequestSerializationError)
        {
            throw TransactionCreateSignatureRequestSerializationError(
                String.format(
                    ErrorConstants.TRANSACTION_PROCESSOR_SERIALIZE_ACTION_ERROR,
                    action.account), serializeError)
        }

        return actionAbifioSerializationObject
    }

    @Throws(TransactionCreateSignatureRequestError::class)
    private fun serializeTransaction(): String
    {
        val clonedTransaction: Transaction?

        try
        {
            clonedTransaction = this.getDeepClone()
        }
        catch (e: IOException)
        {
            throw TransactionCreateSignatureRequestError(
                ErrorConstants.TRANSACTION_PROCESSOR_PREPARE_CLONE_ERROR, e)
        }
        catch (e: ClassNotFoundException)
        {
            throw TransactionCreateSignatureRequestError(
                ErrorConstants.TRANSACTION_PROCESSOR_PREPARE_CLONE_CLASS_NOT_FOUND, e)
        }

        if (clonedTransaction == null)
        {
            throw TransactionCreateSignatureRequestError(
                ErrorConstants.TRANSACTION_PROCESSOR_PREPARE_CLONE_ERROR)
        }


        if (this.chainId == null || this.chainId!!.isEmpty())
        {
            try
            {
                val getInfoResponse = this.fioNetworkProvider.getInfo()
                this.chainId = getInfoResponse.chainId
            }
            catch (getInfoError: GetInfoError)
            {
                throw TransactionCreateSignatureRequestRpcError(
                    ErrorConstants.TRANSACTION_PROCESSOR_RPC_GET_INFO, getInfoError)
            }
        }

        // Serialize each action of Transaction's actions
        for (action in clonedTransaction.actions) {
            val actionAbiFioSerializationObject = this.serializeAction(action,
                this.chainId!!, this.abiProvider)

            action.data = actionAbiFioSerializationObject.hex
        }

        if (clonedTransaction.contextFreeActions!=null && clonedTransaction.contextFreeActions!!.isNotEmpty()) {
            for (contextFreeAction in clonedTransaction.contextFreeActions!!)
            {
                val actionAbiEosSerializationObject = this.serializeAction(contextFreeAction,
                        this.chainId!!, this.abiProvider)

                contextFreeAction.data = actionAbiEosSerializationObject.hex
            }
        }

        this.transaction = clonedTransaction

        // Serialize transaction
        val _serializedTransaction: String?

        try
        {
            val clonedTransactionToJSON = Utils.getGson(DateFormatter.BACKEND_DATE_PATTERN).toJson(clonedTransaction)

            _serializedTransaction = this.serializationProvider
                .serializeTransaction(clonedTransactionToJSON)

            if (_serializedTransaction.isEmpty())
            {
                throw TransactionCreateSignatureRequestSerializationError(
                    ErrorConstants.TRANSACTION_PROCESSOR_SERIALIZE_TRANSACTION_WORKED_BUT_EMPTY_RESULT
                )
            }

        }
        catch (serializeTransactionError: SerializeTransactionError)
        {
            throw TransactionCreateSignatureRequestSerializationError(
                ErrorConstants.TRANSACTION_PROCESSOR_SERIALIZE_TRANSACTION_ERROR,
                serializeTransactionError)
        }

        return _serializedTransaction
    }

    //public methods

    @Throws(TransactionPrepareError::class)
    fun prepare(actions: ArrayList<IAction>, contextFreeActions: ArrayList<IAction>)
    {
        if (actions.isEmpty()) {
            throw TransactionPrepareInputError(ErrorConstants.TRANSACTION_PROCESSOR_ACTIONS_EMPTY_ERROR_MSG)
        }

        val preparingTransaction = Transaction(
            "", BigInteger.ZERO, BigInteger.ZERO,
            BigInteger.ZERO, BigInteger.ZERO,
            BigInteger.ZERO, contextFreeActions, actions, ArrayList())

        val getInfoResponse: GetInfoResponse

        try
        {
            getInfoResponse = this.fioNetworkProvider.getInfo()
        }
        catch (getInfoRpcError: GetInfoError) {
            throw TransactionPrepareRpcError(
                ErrorConstants.TRANSACTION_PROCESSOR_RPC_GET_INFO,
                getInfoRpcError
            )
        }


        if (this.chainId.isNullOrEmpty())
        {
            this.chainId = getInfoResponse.chainId ?: throw TransactionPrepareError(ErrorConstants.TRANSACTION_PROCESSOR_PREPARE_CHAINID_RPC_EMPTY)

            if (getInfoResponse.chainId.isNullOrEmpty()) {
                throw TransactionPrepareError(ErrorConstants.TRANSACTION_PROCESSOR_PREPARE_CHAINID_RPC_EMPTY)
            }

            this.chainId = getInfoResponse.chainId
        }
        else if (!getInfoResponse.chainId.isNullOrEmpty() && getInfoResponse.chainId != chainId)
        {
            // Throw error if both are not empty but one does not match with another
            throw TransactionPrepareError(
                String.format(ErrorConstants.TRANSACTION_PROCESSOR_PREPARE_CHAINID_NOT_MATCH,
                    this.chainId,
                    getInfoResponse.chainId)
            )
        }

        if (preparingTransaction.expiration.isEmpty())
        {
            val strHeadBlockTime = getInfoResponse.headBlockTime

            val headBlockTime: Long

            try
            {
                headBlockTime = DateFormatter.convertBackendTimeToMilli(strHeadBlockTime)
            }
            catch (e: ParseException)
            {
                throw TransactionPrepareError(
                    ErrorConstants.TRANSACTION_PROCESSOR_HEAD_BLOCK_TIME_PARSE_ERROR, e)
            }

            val expiresSeconds = this.transactionConfig.expiresSeconds

            val expirationTimeInMilliseconds = headBlockTime + expiresSeconds * 1000

            preparingTransaction.expiration = DateFormatter.convertMilliSecondToBackendTimeString(expirationTimeInMilliseconds)
        }

        val getBlockResponse: GetBlockResponse
        try
        {
            getBlockResponse = this.fioNetworkProvider
                .getBlock(GetBlockRequest(getInfoResponse.lastIrreversibleBlockNumber.toString()))
        }
        catch (getBlockRpcError: GetBlockError)
        {
            throw TransactionPrepareRpcError(
                ErrorConstants.TRANSACTION_PROCESSOR_PREPARE_RPC_GET_BLOCK, getBlockRpcError)
        }

        // Restrict the refBlockNum to 32 bit unsigned value
        val refBlockNum = getBlockResponse.blockNumber?.and(BigInteger.valueOf(0xffff))
        val refBlockPrefix = getBlockResponse.refBlockPrefix

        preparingTransaction.refBlockNum = refBlockNum
        preparingTransaction.refBlockPrefix = refBlockPrefix

        this.finishPreparing(preparingTransaction)
    }

    @Throws(TransactionPrepareError::class)
    fun prepare(actions: ArrayList<IAction>)
    {
        this.prepare(actions,ArrayList())
    }

    @Throws(TransactionSignError::class)
    fun sign(): Boolean
    {
        val fioTransactionSignatureRequest: FIOTransactionSignatureRequest

        try
        {
            fioTransactionSignatureRequest = this.createSignatureRequest()
        }
        catch (transactionCreateSignatureRequestError: TransactionCreateSignatureRequestError)
        {
            throw TransactionSignError(
                ErrorConstants.TRANSACTION_PROCESSOR_SIGN_CREATE_SIGN_REQUEST_ERROR,
                transactionCreateSignatureRequestError)
        }

        val fioTransactionSignatureResponse: FIOTransactionSignatureResponse
        try
        {
            fioTransactionSignatureResponse = this.getSignature(fioTransactionSignatureRequest)
            if (fioTransactionSignatureResponse.error != null) {
                throw fioTransactionSignatureResponse.error
            }
        }
        catch (transactionGetSignatureError: TransactionGetSignatureError) {
            throw TransactionSignError(transactionGetSignatureError)
        }
        catch (signatureProviderError: SignatureProviderError) {
            throw TransactionSignError(
                ErrorConstants.TRANSACTION_PROCESSOR_SIGN_SIGNATURE_RESPONSE_ERROR,
                signatureProviderError)
        }

        return true
    }

    @Throws(TransactionBroadCastError::class)
    fun broadcast(): PushTransactionResponse
    {
        if (this.serializedTransaction == null || this.serializedTransaction!!.isEmpty())
        {
            throw TransactionBroadCastError(
                ErrorConstants.TRANSACTION_PROCESSOR_BROADCAST_SERIALIZED_TRANSACTION_EMPTY)
        }

        if (this.signatures.isEmpty())
        {
            throw TransactionBroadCastEmptySignatureError(
                ErrorConstants.TRANSACTION_PROCESSOR_BROADCAST_SIGN_EMPTY)
        }

        val pushTransactionRequest = PushTransactionRequest(
            this.signatures, 0, "",
            this.serializedTransaction!!)

        try
        {
            return this.pushTransaction(pushTransactionRequest)
        }
        catch (transactionPushTransactionError: TransactionPushTransactionError)
        {
            throw TransactionBroadCastError(
                ErrorConstants.TRANSACTION_PROCESSOR_BROADCAST_TRANS_ERROR,
                transactionPushTransactionError)
        }

    }

    @Throws(TransactionSignAndBroadCastError::class)
    fun signAndBroadcast(): PushTransactionResponse
    {
        val fioTransactionSignatureRequest: FIOTransactionSignatureRequest

        try
        {
            fioTransactionSignatureRequest = this.createSignatureRequest()
        }
        catch (transactionCreateSignatureRequestError: TransactionCreateSignatureRequestError)
        {
            throw TransactionSignAndBroadCastError(transactionCreateSignatureRequestError)
        }

        try
        {
            this.getSignature(fioTransactionSignatureRequest)
        }
        catch (transactionGetSignatureError: TransactionGetSignatureError)
        {
            throw TransactionSignAndBroadCastError(transactionGetSignatureError)
        }

        if (this.serializedTransaction == null || this.serializedTransaction!!.isEmpty())
        {
            throw TransactionSignAndBroadCastError(
                ErrorConstants.TRANSACTION_PROCESSOR_SIGN_BROADCAST_SERIALIZED_TRANSACTION_EMPTY
            )
        }

        if (this.signatures.isEmpty())
        {
            throw TransactionSignAndBroadCastError(
                ErrorConstants.TRANSACTION_PROCESSOR_SIGN_BROADCAST_SIGN_EMPTY)
        }

        val pushTransactionRequest = PushTransactionRequest(
            this.signatures, 0, "",
            this.serializedTransaction!!)

        try
        {
            return this.pushTransaction(pushTransactionRequest)
        }
        catch (transactionPushTransactionError: TransactionPushTransactionError)
        {
            throw TransactionSignAndBroadCastError(transactionPushTransactionError)
        }

    }

    fun toJSON(): String
    {
        return Utils.getGson(DateFormatter.BACKEND_DATE_PATTERN).toJson(this.transaction)
    }

    @Throws(TransactionSerializeError::class)
    fun serialize(): String?
    {

        if (this.serializedTransaction != null && this.serializedTransaction!!.isNotEmpty())
        {
            return this.serializedTransaction!!
        }

        try
        {
            return this.serializeTransaction()
        }
        catch (transactionCreateSignatureRequestError: TransactionCreateSignatureRequestError)
        {
            throw TransactionSerializeError(
                ErrorConstants.TRANSACTION_PROCESSOR_SERIALIZE_ERROR,
                transactionCreateSignatureRequestError
            )
        }

    }
}