package fiofoundation.io.fiosdk

import fiofoundation.io.fiosdk.enums.FioDomainVisiblity
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.errors.fionetworkprovider.*
import fiofoundation.io.fiosdk.errors.formatters.FIOFormatterError
import fiofoundation.io.fiosdk.errors.serializationprovider.DeserializeTransactionError
import fiofoundation.io.fiosdk.errors.session.TransactionBroadCastError
import fiofoundation.io.fiosdk.errors.session.TransactionPrepareError
import fiofoundation.io.fiosdk.errors.session.TransactionSignError
import fiofoundation.io.fiosdk.errors.signatureprovider.ImportKeyError
import fiofoundation.io.fiosdk.formatters.FIOFormatter
import fiofoundation.io.fiosdk.implementations.ABIProvider
import fiofoundation.io.fiosdk.implementations.FIONetworkProvider
import fiofoundation.io.fiosdk.implementations.SoftKeySignatureProvider
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.interfaces.ISignatureProvider
import fiofoundation.io.fiosdk.models.TokenPublicAddress
import fiofoundation.io.fiosdk.models.Validator
import fiofoundation.io.fiosdk.models.fionetworkprovider.*
import fiofoundation.io.fiosdk.models.fionetworkprovider.actions.*
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.*
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.*
import fiofoundation.io.fiosdk.session.processors.*
import fiofoundation.io.fiosdk.utilities.PrivateKeyUtils

import java.math.BigInteger

/**
 * Kotlin SDK for FIO Foundation API
 *
 * @param privateKey the fio private key of the client sending requests to FIO API.
 * @param publicKey the fio public key of the client sending requests to FIO API.
 * @param technologyPartnerId FIO Address of the wallet which generates this transaction.  Set to empty if not known.
 * @param serializationProvider the serialization provider used for abi serialization and deserialization.
 * @param signatureProvider the signature provider used to sign block chain transactions.
 * @param networkBaseUrl the url to the FIO API.
 */
class FIOSDK(private var privateKey: String, var publicKey: String,var technologyPartnerId:String,
             var serializationProvider: ISerializationProvider,
             var signatureProvider: ISignatureProvider, private val networkBaseUrl:String)
{
    private var networkProvider:FIONetworkProvider = FIONetworkProvider(networkBaseUrl,"")

    private val abiProvider:ABIProvider = ABIProvider(networkProvider,this.serializationProvider)

    companion object Static {
        private var fioSdk: FIOSDK? = null

        private const val ISLEGACY_KEY_FORMAT = true

        /**
         * Create a FIO private key.
         *
         * @param mnemonic mnemonic used to generate a random unique private key.
         */
        @Throws(FIOFormatterError::class)
        fun createPrivateKey(mnemonic: String): String {
            return FIOFormatter.convertPEMFormattedPrivateKeyToFIOFormat(
                PrivateKeyUtils.createPEMFormattedPrivateKey(mnemonic)
            )
        }

        /**
         * Create a FIO public key.
         *
         * @param fioPrivateKey FIO private key.
         */
        @Throws(FIOFormatterError::class)
        fun derivedPublicKey(fioPrivateKey: String): String {
            return FIOFormatter.convertPEMFormattedPublicKeyToFIOFormat(
                PrivateKeyUtils.extractPEMFormattedPublicKey(
                    FIOFormatter.convertFIOPrivateKeyToPEMFormat(fioPrivateKey)
                ), ISLEGACY_KEY_FORMAT
            )
        }

        /**
         * Initialize a static instance of the FIO SDK.
         *
         * @param privateKey the fio private key of the client sending requests to FIO API.
         * @param publicKey the fio public key of the client sending requests to FIO API.
         * @param serializationProvider the serialization provider used for abi serialization and deserialization.
         * @param signatureProvider the signature provider used to sign block chain transactions.
         * @param networkBaseUrl the url to the FIO API.
         */
        fun getInstance(privateKey: String,publicKey: String,
                        serializationProvider: ISerializationProvider,
                        signatureProvider: ISignatureProvider,networkBaseUrl:String): FIOSDK
        {
             fioSdk = FIOSDK(privateKey,publicKey,"",serializationProvider,
                 signatureProvider,networkBaseUrl)

            return fioSdk!!
        }

        /**
         * Initialize a static instance of the FIO SDK.
         *
         * @param privateKey the fio private key of the client sending requests to FIO API.
         * @param publicKey the fio public key of the client sending requests to FIO API.
         * @param technologyPartnerId FIO Address of the wallet which generates this transaction.  Set to empty if not known.
         * @param serializationProvider the serialization provider used for abi serialization and deserialization.
         * @param signatureProvider the signature provider used to sign block chain transactions.
         * @param networkBaseUrl the url to the FIO API.
         */
        fun getInstance(privateKey: String,publicKey: String,technologyPartnerId: String,
                        serializationProvider: ISerializationProvider,
                        signatureProvider: ISignatureProvider,networkBaseUrl:String): FIOSDK
        {
            fioSdk = FIOSDK(privateKey,publicKey,technologyPartnerId,serializationProvider,
                signatureProvider,networkBaseUrl)

            return fioSdk!!
        }

        /**
         * Initialize a static instance of the FIO SDK using the default signature provider.
         *
         * @param privateKey the fio private key of the client sending requests to FIO API.
         * @param publicKey the fio public key of the client sending requests to FIO API.
         * @param networkBaseUrl the url to the FIO API.
         */
        fun getInstance(privateKey: String,publicKey: String,
                        serializationProvider: ISerializationProvider,
                        networkBaseUrl:String): FIOSDK {

            val signatureProvider = SoftKeySignatureProvider()

            if(privateKey!="")
                signatureProvider.importKey(privateKey)

            fioSdk = FIOSDK(
                privateKey,
                publicKey,
                "",
                serializationProvider,
                signatureProvider,
                networkBaseUrl
            )

            return fioSdk!!
        }

        /**
         * Initialize a static instance of the FIO SDK using the default signature provider.
         *
         * @param privateKey the fio private key of the client sending requests to FIO API.
         * @param publicKey the fio public key of the client sending requests to FIO API.
         * @param technologyPartnerId FIO Address of the wallet which generates this transaction.  Set to empty if not known.
         * @param networkBaseUrl the url to the FIO API.
         */
        fun getInstance(privateKey: String,publicKey: String,technologyPartnerId: String,
                        serializationProvider: ISerializationProvider,
                        networkBaseUrl:String): FIOSDK {

            val signatureProvider = SoftKeySignatureProvider()

            if(privateKey!="")
                signatureProvider.importKey(privateKey)

            fioSdk = FIOSDK(
                privateKey,
                publicKey,
                technologyPartnerId,
                serializationProvider,
                signatureProvider,
                networkBaseUrl
            )

            return fioSdk!!
        }

        /**
         * Return a previously initialized instance of the FIO SDK.
         */
        fun getInstance(): FIOSDK
        {
            return fioSdk!!
        }

        /**
         * Set the FIO SDK instance to null
         */
        fun destroyInstance()
        {
            fioSdk = null
        }
    }

    /**
     * @suppress
     */
    var mockServerBaseUrl:String=""
        set(value){
            if(value.isNotEmpty())
            {
                this.networkProvider = FIONetworkProvider(this.networkBaseUrl,value)
            }
            field = value
        }

    /**
     * @param privateKey set private key.  If using the default signature provider, a new instance
     * of the provider will be created automatically.
     * @throws [FIOError]
     * */
    @Throws(FIOError::class)
    open fun setPrivateKey(privateKey:String)
    {
        this.privateKey = privateKey

        //If the signature provider is the default provider, then import the private key
        try {
            if(this.signatureProvider is SoftKeySignatureProvider)
            {
                if(privateKey!="")
                    (this.signatureProvider as SoftKeySignatureProvider).importKey(privateKey)
                else
                    this.signatureProvider = SoftKeySignatureProvider()
            }
        }
        catch(e:ImportKeyError)
        {
            throw FIOError(e.message!!,e)
        }
        catch(ex:Exception)
        {
            throw FIOError(ex.message!!,ex)
        }

    }

    fun getPrivateKey():String
    {
        return this.privateKey
    }

    /**
     * @suppress
     */
    @Throws(FIOError::class)
    fun registerFioNameOnBehalfOfUser(fioName:String): RegisterFIONameForUserResponse
    {
        val request = RegisterFIONameForUserRequest(fioName, this.publicKey)

        return this.networkProvider.registerFioNameOnBehalfOfUser(request)
    }

    /**
     * @suppress
     */
    @Throws(FIOError::class)
    fun registerFioNameOnBehalfOfUser(fioName:String, ownerPublicKey:String): RegisterFIONameForUserResponse
    {
        try
        {
            val request = RegisterFIONameForUserRequest(fioName, ownerPublicKey)

            return this.networkProvider.registerFioNameOnBehalfOfUser(request)
        }
        catch(registerFIONameForUserError: RegisterFIONameForUserError)
        {
            throw FIOError(registerFIONameForUserError.message!!,registerFIONameForUserError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Registers a FIO Address on the FIO blockchain.
     *
     * @param fioAddress FIO Address to register.
     * @param ownerPublicKey Public key which will own the FIO Address after registration. Set to empty if same as sender.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioAddress(fioAddress:String ,ownerPublicKey:String, maxFee:BigInteger,
                           technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = RegisterFIOAddressTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateRegisterFioAddress(fioAddress,ownerPublicKey,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var registerFioAddressAction =
                    RegisterFIOAddressAction(
                        fioAddress,
                        ownerPublicKey,
                        wfa,
                        maxFee,
                        this.publicKey
                    )

                var actionList = ArrayList<RegisterFIOAddressAction>()
                actionList.add(registerFioAddressAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Registers a FIO Address on the FIO blockchain.
     *
     * @param fioAddress FIO Address to register.
     * @param ownerPublicKey Public key which will own the FIO Address after registration. Set to empty if same as sender.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @return
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioAddress(fioAddress:String, ownerPublicKey:String, maxFee:BigInteger): PushTransactionResponse
    {
        return registerFioAddress(fioAddress,ownerPublicKey,maxFee,this.technologyPartnerId)
    }

    /**
     * Registers a FIO Address on the FIO blockchain.  The owner will be the public key associated with the FIO SDK instance.
     *
     * @param fioAddress FIO Address to register.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioAddress(fioAddress:String, maxFee:BigInteger, technologyPartnerId:String): PushTransactionResponse
    {
        return registerFioAddress(fioAddress,"", maxFee, technologyPartnerId)
    }

    /**
     * Registers a FIO Address on the FIO blockchain.  The owner will be the public key associated with the FIO SDK instance.
     *
     * @param fioAddress FIO Address to register.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioAddress(fioAddress:String, maxFee:BigInteger): PushTransactionResponse
    {
        return registerFioAddress(fioAddress,"",maxFee)
    }

    /**
     * Registers a FIO Domain on the FIO blockchain.
     *
     * @param fioDomain FIO Domain to register.
     * @param ownerPublicKey Public key which will own the FIO Domain after registration. Set to empty if same as sender.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioDomain(fioDomain:String, ownerPublicKey:String, maxFee:BigInteger,
                          technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = RegisterFIODomainTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateRegisterFioDomain(fioDomain,ownerPublicKey,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var registerFioDomainAction = RegisterFIODomainAction(
                    fioDomain,
                    ownerPublicKey,
                    wfa,
                    maxFee,
                    this.publicKey
                )

                var actionList = ArrayList<RegisterFIODomainAction>()
                actionList.add(registerFioDomainAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Registers a FIO Domain on the FIO blockchain.
     *
     * @param fioDomain FIO Domain to register.
     * @param ownerPublicKey Public key which will own the FIO Domain after registration. Set to empty if same as sender.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioDomain(fioDomain:String, ownerPublicKey:String, maxFee:BigInteger): PushTransactionResponse
    {
        return registerFioDomain(fioDomain, ownerPublicKey, maxFee,this.technologyPartnerId)
    }

    /**
     * Registers a FIO Domain on the FIO blockchain.
     *
     * @param fioDomain FIO Domain to register. The owner will be the public key associated with the FIO SDK instance.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioDomain(fioDomain:String, maxFee:BigInteger,
                          technologyPartnerId:String): PushTransactionResponse
    {
        return registerFioDomain(fioDomain,"",maxFee,technologyPartnerId)
    }

    /**
     * Registers a FIO Domain on the FIO blockchain.
     *
     * @param fioDomain FIO Domain to register. The owner will be the public key associated with the FIO SDK instance.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun registerFioDomain(fioDomain:String, maxFee:BigInteger): PushTransactionResponse
    {
        return registerFioDomain(fioDomain,maxFee,this.technologyPartnerId)
    }

    /**
     * Renew a FIO Domain on the FIO blockchain.
     *
     * @param fioDomain FIO Domain to renew.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun renewFioDomain(fioDomain:String, maxFee:BigInteger,
                       technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = RegisterFIODomainTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateRenewFioDomain(fioDomain,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var renewFioDomainAction = RenewFIODomainAction(
                    fioDomain,
                    maxFee,
                    wfa,
                    this.publicKey
                )

                var actionList = ArrayList<RenewFIODomainAction>()
                actionList.add(renewFioDomainAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Renew a FIO Domain on the FIO blockchain.
     *
     * @param fioDomain FIO Domain to renew.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun renewFioDomain(fioDomain:String, maxFee:BigInteger): PushTransactionResponse
    {
        return renewFioDomain(fioDomain, maxFee,this.technologyPartnerId)
    }

    /**
     * Renew a FIO Address on the FIO blockchain.
     *
     * @param fioAddress FIO Address to renew.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun renewFioAddress(fioAddress:String, maxFee:BigInteger,
                        technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = RenewFIOAddressTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateRenewFioAddress(fioAddress,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var renewFioAddressAction =
                    RenewFIOAddressAction(
                        fioAddress,
                        maxFee,
                        wfa,
                        this.publicKey
                    )

                var actionList = ArrayList<RenewFIOAddressAction>()
                actionList.add(renewFioAddressAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Renew a FIO Address on the FIO blockchain.
     *
     * @param fioAddress FIO Address to renew.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by @ [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun renewFioAddress(fioAddress:String, maxFee:BigInteger): PushTransactionResponse
    {
        return renewFioAddress(fioAddress,maxFee,this.technologyPartnerId)
    }

    /**
     *
     * Transfers FIO tokens from public key associated with the FIO SDK instance to
     * the payeePublicKey.
     *
     * @param payeePublicKey FIO public Address of the one receiving the tokens.
     * @param amount Amount sent in SUFs.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun transferTokens(payeeFioPublicKey:String, amount:BigInteger, maxFee:BigInteger,
                       technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = TransTokensPubKeyTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateTransferPublicTokens(payeeFioPublicKey,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var transferTokensToPublickey = TransferTokensPubKeyAction(
                    payeeFioPublicKey,
                    amount,
                    maxFee,
                    wfa,
                    this.publicKey
                )

                var actionList = ArrayList<TransferTokensPubKeyAction>()
                actionList.add(transferTokensToPublickey)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     *
     * Transfers FIO tokens from public key associated with the FIO SDK instance to
     * the payeePublicKey.
     *
     * @param payeePublicKey FIO public Address of the one receiving the tokens.
     * @param amount Amount sent in SUFs.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun transferTokens(payeeFioPublicKey:String, amount:BigInteger, maxFee:BigInteger): PushTransactionResponse
    {
        return transferTokens(payeeFioPublicKey, amount, maxFee,this.technologyPartnerId)
    }

    /**
     * Retrieves balance of FIO tokens using the public key of the client
     * sending the request.
     * @return [GetFIOBalanceResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFioBalance(): GetFIOBalanceResponse
    {
        return this.getFioBalance(this.publicKey)
    }

    /**
     * Retrieves balance of FIO tokens
     *
     * @param fioPublicKey FIO public key.
     * @return [GetFIOBalanceResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFioBalance(fioPublicKey:String): GetFIOBalanceResponse
    {
        try
        {
            val request = GetFIOBalanceRequest(fioPublicKey)

            return this.networkProvider.getFIOBalance(request)
        }
        catch(fioBalanceError: GetFIOBalanceError)
        {
            throw FIOError(fioBalanceError.message!!,fioBalanceError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param payeeTokenPublicAddress Payee's public address where they want funds sent.
     * @param amount Amount requested.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Code of the token represented in amount requested.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId (optional) FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun requestFunds(payerFioAddress:String, payeeFioAddress:String,
                        payeeTokenPublicAddress:String, amount:Double, chainCode:String, tokenCode:String,
                        maxFee:BigInteger, technologyPartnerId:String=""): PushTransactionResponse
    {
        val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

        val fundsRequestContent = FundsRequestContent(payeeTokenPublicAddress,amount.toString(),chainCode,tokenCode)

        return this.requestNewFunds(payerFioAddress,payeeFioAddress,fundsRequestContent,maxFee,wfa)
    }

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param payeeTokenPublicAddress Payee's public address where they want funds sent.
     * @param amount Amount requested.
     * @param tokenCode Code of the token represented in amount requested.  The chain code will be set to this value as well.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun requestFunds(payerFioAddress:String, payeeFioAddress:String, payeeTokenPublicAddress:String,
                     amount:Double, tokenCode:String, maxFee:BigInteger): PushTransactionResponse
    {
        return requestFunds(payerFioAddress,payeeFioAddress,payeeTokenPublicAddress,amount,tokenCode,tokenCode,maxFee,this.technologyPartnerId)
    }

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param payeeTokenPublicAddress Payee's public address where they want funds sent.
     * @param amount Amount requested.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Code of the token represented in amount requested.
     * @param memo (optional)
     * @param hash (optional)
     * @param offlineUrl (optional)
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId (optional) FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun requestFunds(payerFioAddress:String, payeeFioAddress:String,
                        payeeTokenPublicAddress:String, amount:Double, chainCode:String, tokenCode:String,
                        memo: String?=null, hash: String?=null, offlineUrl:String?=null,
                        maxFee:BigInteger, technologyPartnerId:String=""): PushTransactionResponse
    {
        val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

        val fundsRequestContent = FundsRequestContent(payeeTokenPublicAddress,amount.toString(),chainCode,tokenCode,memo,hash,offlineUrl)

        return this.requestNewFunds(payerFioAddress,payeeFioAddress,fundsRequestContent,maxFee,wfa)
    }

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param payeeTokenPublicAddress Payee's public address where they want funds sent.
     * @param amount Amount requested.
     * @param tokenCode Code of the token represented in amount requested.  The chain code will be set to this value as well.
     * @param memo (optional)
     * @param hash (optional)
     * @param offlineUrl (optional)
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun requestFunds(payerFioAddress:String, payeeFioAddress:String, payeeTokenPublicAddress:String,
                     amount:Double, tokenCode:String, memo: String?=null, hash: String?=null,
                     offlineUrl:String?=null, maxFee:BigInteger): PushTransactionResponse
    {
        return requestFunds(payerFioAddress,payeeFioAddress,payeeTokenPublicAddress,amount,tokenCode,tokenCode,memo,hash,offlineUrl,maxFee,this.technologyPartnerId)
    }

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param payeeTokenPublicAddress Payee's public address where they want funds sent.
     * @param amount Amount requested.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Code of the token represented in amount requested.
     * @param memo
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId (optional) FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun requestFunds(payerFioAddress:String, payeeFioAddress:String,
                     payeeTokenPublicAddress:String, amount:Double, chainCode:String, tokenCode:String,
                     memo: String, maxFee:BigInteger, technologyPartnerId:String=""): PushTransactionResponse
    {
        val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

        val fundsRequestContent = FundsRequestContent(payeeTokenPublicAddress,amount.toString(),chainCode,tokenCode,memo)

        return this.requestNewFunds(payerFioAddress,payeeFioAddress,fundsRequestContent,maxFee,wfa)
    }

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param payeeTokenPublicAddress Payee's public address where they want funds sent.
     * @param amount Amount requested.
     * @param tokenCode Code of the token represented in amount requested.  The chain code will be set to this value as well.
     * @param memo
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun requestFunds(payerFioAddress:String, payeeFioAddress:String, payeeTokenPublicAddress:String,
                     amount:Double, tokenCode:String, memo: String, maxFee:BigInteger): PushTransactionResponse
    {
        return requestFunds(payerFioAddress,payeeFioAddress,payeeTokenPublicAddress,amount,tokenCode,tokenCode,memo,maxFee,this.technologyPartnerId)
    }

    /**
     * Reject funds request.
     *
     * @param fioRequestId Existing funds request Id
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun rejectFundsRequest(fioRequestId:BigInteger, maxFee: BigInteger, technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = RejectFundsRequestTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateRejectFundsRequest(fioRequestId,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var rejectFundsRequestAction = RejectFundsRequestAction(
                    fioRequestId,
                    maxFee,
                    wfa,
                    this.publicKey
                )

                var actionList = ArrayList<RejectFundsRequestAction>()
                actionList.add(rejectFundsRequestAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Reject funds request.
     *
     * @param fioRequestId Existing funds request Id
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun rejectFundsRequest(fioRequestId: BigInteger, maxFee: BigInteger): PushTransactionResponse
    {
        return rejectFundsRequest(fioRequestId,maxFee,this.technologyPartnerId)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param fioRequestId ID of funds request, if this Record Send transaction is in response to a previously received funds request.  Send empty if no FIO Request ID
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param payerTokenPublicAddress Public address on other blockchain of user sending funds.
     * @param payeeTokenPublicAddress Public address on other blockchain of user receiving funds.
     * @param amount Amount sent.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Code of the token represented in Amount requested, i.e. BTC.
     * @param obtId Other Blockchain Transaction ID (OBT ID), i.e Bitcoin transaction ID.
     * @param status Status of this OBT. Allowed statuses are: sent_to_blockchain.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @ExperimentalUnsignedTypes
    fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String, payeeFioAddress:String,
                   payerTokenPublicAddress: String, payeeTokenPublicAddress:String, amount:Double,
                      chainCode:String,tokenCode:String, status:String="sent_to_blockchain", obtId:String, maxFee:BigInteger,technologyPartnerId:String=""): PushTransactionResponse
    {
        val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

        val recordObtDataContent = RecordObtDataContent(payerTokenPublicAddress,payeeTokenPublicAddress,amount.toString(),
            chainCode,tokenCode,obtId,status)

        return this.recordObtData(fioRequestId,payerFioAddress,payeeFioAddress,recordObtDataContent,maxFee,wfa)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param fioRequestId ID of funds request, if this Record Send transaction is in response to a previously received funds request.  Send empty if no FIO Request ID
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param payerTokenPublicAddress Public address on other blockchain of user sending funds.
     * @param payeeTokenPublicAddress Public address on other blockchain of user receiving funds.
     * @param amount Amount sent.
     * @param tokenCode Code of the token represented in Amount requested, i.e. BTC.  The chain code will be set to this value as well.
     * @param obtId Other Blockchain Transaction ID (OBT ID), i.e Bitcoin transaction ID.
     * @param status Status of this OBT. Allowed statuses are: sent_to_blockchain.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @ExperimentalUnsignedTypes
    fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String, payeeFioAddress:String,
                      payerTokenPublicAddress: String, payeeTokenPublicAddress:String, amount:Double,
                      tokenCode:String, status:String="sent_to_blockchain", obtId:String, maxFee:BigInteger,technologyPartnerId:String=""): PushTransactionResponse
    {
        val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

        return recordObtData(fioRequestId,payerFioAddress,payeeFioAddress, payerTokenPublicAddress,
            payeeTokenPublicAddress,amount,tokenCode,status,obtId,maxFee,wfa)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param fioRequestId ID of funds request, if this Record Send transaction is in response to a previously received funds request.  Send empty if no FIO Request ID
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param payerTokenPublicAddress Public address on other blockchain of user sending funds.
     * @param payeeTokenPublicAddress Public address on other blockchain of user receiving funds.
     * @param amount Amount sent.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Code of the token represented in Amount requested, i.e. BTC.
     * @param obtId Other Blockchain Transaction ID (OBT ID), i.e Bitcoin transaction ID.
     * @param status Status of this OBT. Allowed statuses are: sent_to_blockchain.
     * @param memo
     * @param hash
     * @param offlineUrl
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @ExperimentalUnsignedTypes
    fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String, payeeFioAddress:String,
                   payerTokenPublicAddress: String, payeeTokenPublicAddress:String, amount:Double,
                      chainCode:String,tokenCode:String, status:String="sent_to_blockchain", obtId:String, maxFee:BigInteger,technologyPartnerId:String="",
                   memo:String?=null, hash:String?=null, offlineUrl:String?=null): PushTransactionResponse
    {
        val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

        val recordObtDataContent = RecordObtDataContent(payerTokenPublicAddress,payeeTokenPublicAddress,amount.toString(),
            chainCode,tokenCode,obtId,status,memo,hash,offlineUrl)

        return this.recordObtData(fioRequestId,payerFioAddress,payeeFioAddress,recordObtDataContent,maxFee,wfa)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param fioRequestId ID of funds request, if this Record Send transaction is in response to a previously received funds request.  Send empty if no FIO Request ID
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param payerTokenPublicAddress Public address on other blockchain of user sending funds.
     * @param payeeTokenPublicAddress Public address on other blockchain of user receiving funds.
     * @param amount Amount sent.
     * @param tokenCode Code of the token represented in Amount requested, i.e. BTC.  The chain code will be set to this value as well.
     * @param obtId Other Blockchain Transaction ID (OBT ID), i.e Bitcoin transaction ID.
     * @param status Status of this OBT. Allowed statuses are: sent_to_blockchain.
     * @param memo
     * @param hash
     * @param offlineUrl
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @ExperimentalUnsignedTypes
    fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String, payeeFioAddress:String,
                      payerTokenPublicAddress: String, payeeTokenPublicAddress:String, amount:Double,
                      tokenCode:String, status:String="sent_to_blockchain", obtId:String, maxFee:BigInteger,
                      memo:String?=null, hash:String?=null, offlineUrl:String?=null): PushTransactionResponse
    {
        return recordObtData(fioRequestId,payerFioAddress,payeeFioAddress,payerTokenPublicAddress,
            payeeTokenPublicAddress,amount,tokenCode,tokenCode,status,obtId,maxFee,this.technologyPartnerId,
            memo,hash,offlineUrl)
    }

    @ExperimentalUnsignedTypes
    fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String, payeeFioAddress:String,
                   payerTokenPublicAddress: String, payeeTokenPublicAddress:String, amount:Double,
                      chainCode:String,tokenCode:String, status:String="sent_to_blockchain",obtId:String,
                   maxFee:BigInteger): PushTransactionResponse
    {
        val recordObtDataContent = RecordObtDataContent(payerTokenPublicAddress,
            payeeTokenPublicAddress, amount.toString(),chainCode,tokenCode,obtId,status)

        return this.recordObtData(fioRequestId, payerFioAddress, payeeFioAddress, recordObtDataContent, maxFee,this.technologyPartnerId)
    }

    @ExperimentalUnsignedTypes
    fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String, payeeFioAddress:String,
                      payerTokenPublicAddress: String, payeeTokenPublicAddress:String, amount:Double,
                      tokenCode:String, status:String="sent_to_blockchain",obtId:String,
                      maxFee:BigInteger): PushTransactionResponse
    {
        return recordObtData(fioRequestId,payerFioAddress,payeeFioAddress,payerTokenPublicAddress,
            payeeTokenPublicAddress,amount,tokenCode,tokenCode,status,obtId,maxFee)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param payerTokenPublicAddress Public address on other blockchain of user sending funds.
     * @param payeeTokenPublicAddress Public address on other blockchain of user receiving funds.
     * @param amount Amount sent.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Code of the token represented in Amount requested, i.e. BTC.
     * @param status Status of this OBT. Allowed statuses are: sent_to_blockchain.
     * @param obtId Other Blockchain Transaction ID (OBT ID), i.e Bitcoin transaction ID.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @ExperimentalUnsignedTypes
    fun recordObtData(payerFioAddress:String, payeeFioAddress:String,
                      payerTokenPublicAddress: String, payeeTokenPublicAddress:String, amount:Double,
                      chainCode:String,tokenCode:String, status:String="sent_to_blockchain", obtId:String, maxFee:BigInteger,technologyPartnerId:String=""): PushTransactionResponse
    {
        val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

        val recordObtDataContent = RecordObtDataContent(payerTokenPublicAddress,payeeTokenPublicAddress,amount.toString(),
            chainCode,tokenCode,obtId,status)

        return this.recordObtData(BigInteger.ZERO,payerFioAddress,payeeFioAddress,recordObtDataContent,maxFee,wfa)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param payerTokenPublicAddress Public address on other blockchain of user sending funds.
     * @param payeeTokenPublicAddress Public address on other blockchain of user receiving funds.
     * @param amount Amount sent.
     * @param tokenCode Code of the token represented in Amount requested, i.e. BTC.  The chain code value will be to this as well.
     * @param status Status of this OBT. Allowed statuses are: sent_to_blockchain.
     * @param obtId Other Blockchain Transaction ID (OBT ID), i.e Bitcoin transaction ID.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @ExperimentalUnsignedTypes
    fun recordObtData(payerFioAddress:String, payeeFioAddress:String, payerTokenPublicAddress: String,
                      payeeTokenPublicAddress:String, amount:Double, tokenCode:String,
                      status:String="sent_to_blockchain", obtId:String, maxFee:BigInteger): PushTransactionResponse
    {
        return recordObtData(payerFioAddress,payeeFioAddress,payerTokenPublicAddress,payeeTokenPublicAddress,
            amount,tokenCode,tokenCode,status,obtId,maxFee,this.technologyPartnerId)
    }

    /**
     * Gets for any Obt Data sent using public key associated with the FIO SDK instance.
     *
     * @param limit Number of records to return. If omitted, all records will be returned.
     * @param offset First record from list to return. If omitted, 0 is assumed.
     *
     * @return [List<ObtDataRecord>]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getObtData(limit:Int?=null,offset:Int?=null): List<ObtDataRecord>
    {
        return this.getObtData(this.publicKey,limit,offset)
    }

    /**
     * Gets Obt Data, for a specific token, sent using public key associated with the FIO SDK instance.
     *
     * @param tokenCode The token code of the token whose obt data is desired.
     * @param limit Number of records to return. If omitted, all records will be returned.
     * @param offset First record from list to return. If omitted, 0 is assumed.
     *
     * @return [List<ObtDataRecord>]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getObtDataByTokenCode(tokenCode:String,limit:Int?=null,offset:Int?=null): List<ObtDataRecord>
    {
        val obtData = this.getObtData(this.publicKey)

        val tokenObtData = obtData.filter { obtRecord -> (obtRecord.deserializedContent!=null && obtRecord.deserializedContent!!.tokenCode == tokenCode) }

        if(limit!=null && offset!=null)
        {
            if (tokenObtData.size <= limit || offset<=0 || offset>tokenObtData.size)
                return tokenObtData

            var toIndex = offset + (limit - 1)

            if(toIndex<=tokenObtData.lastIndex)
                return tokenObtData.subList(offset,toIndex)
            else
                return tokenObtData.subList(offset,tokenObtData.lastIndex)
        }

        return tokenObtData
    }

    /**
     * Polls for any pending requests sent to public key associated with the FIO SDK instance.
     *
     * @param limit Number of request to return. If omitted, all requests will be returned.
     * @param offset First request from list to return. If omitted, 0 is assumed.
     *
     * @return [List<FIORequestContent>]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getPendingFioRequests(limit:Int?=null,offset:Int?=null): List<FIORequestContent>
    {
        return this.getPendingFioRequests(this.publicKey,limit,offset)
    }

    /**
     * Polls for any sent requests sent by public key associated with the FIO SDK instance.
     *
     * @param limit Number of request to return. If omitted, all requests will be returned.
     * @param offset First request from list to return. If omitted, 0 is assumed.
     *
     * @return [List<FIORequestContent>]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getSentFioRequests(limit:Int?=null,offset:Int?=null): List<FIORequestContent>
    {
        return this.getSentFioRequests(this.publicKey,limit,offset)
    }

    /**
     * Returns FIO Addresses and FIO Domains owned by this public key.
     *
     * @param fioPublicKey FIO public key of owner.
     * @return [GetFIONamesResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFioNames(fioPublicKey:String): GetFIONamesResponse
    {
        try
        {
            val request = GetFIONamesRequest(fioPublicKey)

            return this.networkProvider.getFIONames(request)
        }
        catch(getFioNamesError: GetFIONamesError)
        {
            throw FIOError(getFioNamesError.message!!,getFioNamesError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Returns FIO Addresses and FIO Domains owned by public key associated with the FIO SDK instance.
     *
     * @return [GetFIONamesResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFioNames(): GetFIONamesResponse
    {
        try
        {
            val request = GetFIONamesRequest(this.publicKey)

            return this.networkProvider.getFIONames(request)
        }
        catch(getFioNamesError: GetFIONamesError)
        {
            throw FIOError(getFioNamesError.message!!,getFioNamesError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Returns the FIO token public address for specified FIO Address.
     *
     * @param fioAddress FIO Address for which fio token public address is to be returned.
     * @return [GetPublicAddressResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFioPublicAddress(fioAddress:String): GetPublicAddressResponse
    {
        return getPublicAddress(fioAddress,"FIO","FIO")
    }

    /**
     * Returns a token public address for specified token code and FIO Address.
     *
     * @param fioAddress FIO Address for which the token public address is to be returned.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Token code for which public address is to be returned.
     * @return [GetPublicAddressResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getPublicAddress(fioAddress:String,chainCode:String, tokenCode:String): GetPublicAddressResponse
    {
        try
        {
            val request = GetPublicAddressRequest(fioAddress,chainCode,tokenCode)

            return this.networkProvider.getPublicAddress(request)
        }
        catch(getPublicAddressError: GetPublicAddressError)
        {
            throw FIOError(getPublicAddressError.message!!,getPublicAddressError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Checks if a FIO Address or FIO Domain is available for registration.
     *
     * @param fioName FIO Address or FIO Domain to check.
     * @return [FIONameAvailabilityCheckResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun isAvailable(fioName:String): FIONameAvailabilityCheckResponse
    {
        try
        {
            val request = FIONameAvailabilityCheckRequest(fioName)

            return this.networkProvider.isFIONameAvailable(request)
        }
        catch(fioNameAvailabilityCheckError: FIONameAvailabilityCheckError)
        {
            throw FIOError(fioNameAvailabilityCheckError.message!!,fioNameAvailabilityCheckError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Compute and return fee amount for specific call
     *
     * @param endPointName Name of API call end point, e.g. add_pub_address.
     * @param fioAddress FIO Address incurring the fee and owned by signer. Leave blank if not available.
     * @return [GetFeeResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFee(endPointName:FIOApiEndPoints.FeeEndPoint,fioAddress:String=""): GetFeeResponse
    {
        try
        {
            var request:GetFeeRequest?

            if(fioAddress!="" && !fioAddress.isFioAddress())
                throw FIOError("Invalid FIO Address")

            if(fioAddress!="" && fioAddress.isFioAddress())
                request = GetFeeRequest(endPointName.endpoint,fioAddress)
            else
                request = GetFeeRequest(endPointName.endpoint,"")

            return this.networkProvider.getFee(request)
        }
        catch(getFeeError: GetFeeError)
        {
            throw FIOError(getFeeError.message!!,getFeeError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Compute and return fee amount for New Funds Request
     *
     * @param payeeFioAddress The payee's FIO Address
     * @return [GetFeeResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFeeForNewFundsRequest(payeeFioAddress:String): GetFeeResponse
    {
        try
        {
            if(payeeFioAddress.isFioAddress()) {
                val request = GetFeeRequest(FIOApiEndPoints.new_funds_request, payeeFioAddress)

                return this.networkProvider.getFee(request)
            }
            else
                throw Exception("Invalid FIO Address")
        }
        catch(getFeeError: GetFeeError)
        {
            throw FIOError(getFeeError.message!!,getFeeError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Compute and return fee amount for Reject Funds Request
     * @param payerFioAddress The payer's FIO Address
     * @return [GetFeeResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFeeForRejectFundsRequest(payerFioAddress:String): GetFeeResponse
    {
        try
        {
            if(payerFioAddress.isFioAddress()) {
                val request = GetFeeRequest(FIOApiEndPoints.reject_funds_request, payerFioAddress)

                return this.networkProvider.getFee(request)
            }
            else
                throw Exception("Invalid FIO Address")
        }
        catch(getFeeError: GetFeeError)
        {
            throw FIOError(getFeeError.message!!,getFeeError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Compute and return fee amount for mapping a fio address to a block chain public address

     * @param fioAddress The FIO Address which will be mapped to public address.
     * @return [GetFeeResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFeeForAddPublicAddress(fioAddress:String): GetFeeResponse
    {
        try
        {
            if(fioAddress.isFioAddress()) {
                val request = GetFeeRequest(FIOApiEndPoints.add_public_address, fioAddress)

                return this.networkProvider.getFee(request)
            }
            else
                throw Exception("Invalid FIO Address")
        }
        catch(getFeeError: GetFeeError)
        {
            throw FIOError(getFeeError.message!!,getFeeError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Compute and return fee amount for recordObtData
     *
     * @param payerFioAddress The FIO Address of the payer whose transaction is being recorded by the recordObtData call
     * @return [GetFeeResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun getFeeForRecordObtData(payerFioAddress:String): GetFeeResponse
    {
        try
        {
            if(payerFioAddress.isFioAddress()) {
                val request = GetFeeRequest(FIOApiEndPoints.record_obt_data, payerFioAddress)

                return this.networkProvider.getFee(request)
            }
            else
                throw Exception("Invalid FIO Address")
        }
        catch(getFeeError: GetFeeError)
        {
            throw FIOError(getFeeError.message!!,getFeeError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Adds a public address of the specific blockchain type to the FIO Address.
     *
     * @param fioAddress FIO Address to add the public address to.
     * @param chainCode Blockchain code for blockchain hosting this token.
     * @param tokenCode Token code to be used with that public address.
     * @param tokenPublicAddress The public address to be added to the FIO Address for the specified token.
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId (optional) FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun addPublicAddress(fioAddress:String, chainCode:String, tokenCode:String, tokenPublicAddress:String,
                         maxFee:BigInteger, technologyPartnerId:String=""): PushTransactionResponse
    {
        var transactionProcessor = AddPublicAddressTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateAddPublicAddress(fioAddress,chainCode,tokenCode,tokenPublicAddress,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var addPublicAddressAction = AddPublicAddressAction(
                    fioAddress,
                    listOf(TokenPublicAddress(tokenPublicAddress,chainCode,tokenCode)),
                    maxFee,
                    wfa,
                    this.publicKey
                )

                var actionList = ArrayList<AddPublicAddressAction>()
                actionList.add(addPublicAddressAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Adds public addresses of specific blockchain types to the FIO Address.
     *
     * @param fioAddress FIO Address to add the public address to.
     * @param tokenPublicAddresses List of public token addresses to add [TokenPublicAddress].
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId (optional) FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun addPublicAddresses(fioAddress:String, tokenPublicAddresses:List<TokenPublicAddress>,
                         maxFee:BigInteger, technologyPartnerId:String=""): PushTransactionResponse
    {
        var transactionProcessor = AddPublicAddressTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateAddPublicAddresses(fioAddress,tokenPublicAddresses,technologyPartnerId)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var addPublicAddressAction = AddPublicAddressAction(
                    fioAddress,
                    tokenPublicAddresses,
                    maxFee,
                    wfa,
                    this.publicKey
                )

                var actionList = ArrayList<AddPublicAddressAction>()
                actionList.add(addPublicAddressAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * By default all FIO Domains are non-public, meaning only the owner can register FIO Addresses on that domain.
     * Setting them to public allows anyone to register a FIO Address on that domain.
     *
     * @param fioDomain FIO Domain to make public or private.  Default is private.
     * @param visibility [FioDomainVisiblity]
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId (optional) FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    fun setFioDomainVisibility(fioDomain:String, visibility:FioDomainVisiblity,
                         maxFee:BigInteger, technologyPartnerId:String=""): PushTransactionResponse
    {
        var transactionProcessor = SetFioDomainVisibilityTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateSetFioDomainVisibility(fioDomain,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                var setFioDomainVisibilityAction = SetFioDomainVisibilityAction(
                    fioDomain,
                    visibility,
                    maxFee,
                    wfa,
                    this.publicKey
                )


                var actionList = ArrayList<SetFioDomainVisibilityAction>()
                actionList.add(setFioDomainVisibilityAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Allows users to send their own content directly to FIO contracts
     *
     * @param account FIO account name
     * @param name FIO contract name
     * @param data JSON string of data to send
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    fun pushTransaction(account: String, name: String, data:String): PushTransactionResponse
    {
        var transactionProcessor = TransactionProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val auth = Authorization(this.publicKey, "active")
            val action = Action(account,name,auth,data)

            var actionList = ArrayList<Action>()
            actionList.add(action)

            @Suppress("UNCHECKED_CAST")
            transactionProcessor.prepare(actionList as ArrayList<IAction>)

            transactionProcessor.sign()

            return transactionProcessor.broadcast()
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    //Private Methods

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param fundsRequestContent [FundsRequestContent]
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    private fun requestNewFunds(payerFioAddress:String, payeeFioAddress:String,
                        fundsRequestContent: FundsRequestContent, maxFee:BigInteger,
                                technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = NewFundsRequestTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateNewFundsRequest(payerFioAddress,payeeFioAddress,fundsRequestContent,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                val payerPublicKey = this.getFioPublicAddress(payerFioAddress).publicAddress

                val encryptedContent = fundsRequestContent.serialize(this.privateKey,payerPublicKey,this.serializationProvider)

                var newFundsRequestAction = NewFundsRequestAction(
                    payerFioAddress,
                    payeeFioAddress,
                    encryptedContent,
                    maxFee,
                    wfa,
                    this.publicKey
                )


                var actionList = ArrayList<NewFundsRequestAction>()
                actionList.add(newFundsRequestAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    /**
     * Create a new funds request on the FIO chain.
     *
     * @param payerFioAddress FIO Address of the payer. This address will receive the request and will initiate payment.
     * @param payeeFioAddress FIO Address of the payee. This address is sending the request and will receive payment.
     * @param fundsRequestContent [FundsRequestContent]
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by [getFee] for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    private fun requestNewFunds(payerFioAddress:String, payeeFioAddress:String,
                        fundsRequestContent: FundsRequestContent, maxFee:BigInteger): PushTransactionResponse
    {
        return requestNewFunds(payerFioAddress, payeeFioAddress, fundsRequestContent, maxFee,this.technologyPartnerId)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param fioRequestId ID of funds request, if this Record Send transaction is in response to a previously received funds request.  Send empty if no FIO Request ID
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param recordObtDataContent [RecordObtDataContent]
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    private fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String, payeeFioAddress:String,
                           recordObtDataContent: RecordObtDataContent,
                           maxFee:BigInteger): PushTransactionResponse
    {
        return recordObtData(fioRequestId,payerFioAddress,payeeFioAddress,recordObtDataContent,maxFee,this.technologyPartnerId)
    }

    /**
     *
     * Records information on the FIO blockchain about a transaction that occurred on other blockchain, i.e. 1 BTC was sent on Bitcoin Blockchain, and both
     * sender and receiver have FIO Addresses. OBT stands for Other Blockchain Transaction
     *
     * @param fioRequestId ID of funds request, if this Record Send transaction is in response to a previously received funds request.  Send empty if no FIO Request ID
     * @param payerFioAddress FIO Address of the payer. This address initiated payment.
     * @param payeeFioAddress FIO Address of the payee. This address is receiving payment.
     * @param recordObtDataContent [RecordObtDataContent]
     * @param maxFee Maximum amount of SUFs the user is willing to pay for fee. Should be preceded by /get_fee for correct value.
     * @param technologyPartnerId FIO Address of the wallet which generates this transaction.
     * @return [PushTransactionResponse]
     *
     * @throws [FIOError]
     */
    @Throws(FIOError::class)
    @ExperimentalUnsignedTypes
    private fun recordObtData(fioRequestId: BigInteger, payerFioAddress:String,
                           payeeFioAddress:String, recordObtDataContent: RecordObtDataContent,
                           maxFee:BigInteger, technologyPartnerId:String): PushTransactionResponse
    {
        var transactionProcessor = RecordObtDataTrxProcessor(
            this.serializationProvider,
            this.networkProvider,
            this.abiProvider,
            this.signatureProvider
        )

        try
        {
            val wfa = if(technologyPartnerId.isEmpty()) this.technologyPartnerId else technologyPartnerId

            val validator = validateRecordObtDataRequest(fioRequestId,payerFioAddress,
                payeeFioAddress,recordObtDataContent,wfa)

            if(!validator.isValid)
                throw FIOError(validator.errorMessage!!)
            else
            {
                if(recordObtDataContent.status == "")
                    recordObtDataContent.status = "sent_to_blockchain"

                val payeeKey = this.getPublicAddress(payeeFioAddress,"FIO","FIO").publicAddress

                val encryptedContent = recordObtDataContent.serialize(this.privateKey,payeeKey,this.serializationProvider)

                var recordObtDataAction = RecordObtDataAction(
                    payerFioAddress,
                    payeeFioAddress,
                    encryptedContent,
                    fioRequestId,
                    maxFee,
                    wfa,
                    this.publicKey
                )


                var actionList = ArrayList<RecordObtDataAction>()
                actionList.add(recordObtDataAction)

                @Suppress("UNCHECKED_CAST")
                transactionProcessor.prepare(actionList as ArrayList<IAction>)

                transactionProcessor.sign()

                return transactionProcessor.broadcast()
            }
        }
        catch(fioError:FIOError)
        {
            throw fioError
        }
        catch(prepError: TransactionPrepareError)
        {
            throw FIOError(prepError.message!!,prepError)
        }
        catch(signError: TransactionSignError)
        {
            throw FIOError(signError.message!!,signError)
        }
        catch(broadcastError: TransactionBroadCastError)
        {
            throw FIOError(broadcastError.message!!,broadcastError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    @Throws(FIOError::class)
    private fun getObtData(requesteeFioPublicKey:String,limit:Int?=null,offset:Int?=null): List<ObtDataRecord>
    {
        try
        {
            val request = GetObtDataRequest(requesteeFioPublicKey,limit,offset)
            val response = this.networkProvider.getObtData(request)

            for (item in response.records)
            {
                try
                {
                    item.deserializedContent = RecordObtDataContent.deserialize(this.privateKey,item.payeeFioPublicKey,this.serializationProvider,item.content)
                }
                catch(deserializationError: DeserializeTransactionError)
                {
                    //eat this error.  We do not want this error to stop the process.
                }
            }

            return response.records
        }
        catch(getObtDataRequestError: GetObtDataRequestError)
        {
            throw FIOError(getObtDataRequestError.message!!,getObtDataRequestError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    @Throws(FIOError::class)
    private fun getPendingFioRequests(requesteeFioPublicKey:String,limit:Int?=null,offset:Int?=null): List<FIORequestContent>
    {
        try
        {
            val request = GetPendingFIORequestsRequest(requesteeFioPublicKey,limit,offset)
            val response = this.networkProvider.getPendingFIORequests(request)

            for (item in response.requests)
            {
                try
                {
                    item.deserializedContent = FundsRequestContent.deserialize(this.privateKey,item.payeeFioPublicKey,this.serializationProvider,item.content)
                }
                catch(deserializationError: DeserializeTransactionError)
                {
                    //eat this error.  We do not want this error to stop the process.
                }
            }

            return response.requests
        }
        catch(getPendingFIORequestsError: GetPendingFIORequestsError)
        {
            throw FIOError(getPendingFIORequestsError.message!!,getPendingFIORequestsError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    @Throws(FIOError::class)
    private fun getSentFioRequests(senderFioPublicKey:String,limit:Int?=null,offset:Int?=null): List<FIORequestContent>
    {
        try
        {
            val request = GetSentFIORequestsRequest(senderFioPublicKey,limit,offset)
            val response = this.networkProvider.getSentFIORequests(request)

            for (item in response.requests)
            {
                try
                {
                    item.deserializedContent = FundsRequestContent.deserialize(this.privateKey,item.payerFioPublicKey,this.serializationProvider,item.content)
                }
                catch(deserializationError: DeserializeTransactionError)
                {
                    //eat this error.  We do not want this error to stop the process.
                }
            }

            return response.requests
        }
        catch(getSentFIORequestsError: GetSentFIORequestsError)
        {
            throw FIOError(getSentFIORequestsError.message!!,getSentFIORequestsError)
        }
        catch(e:Exception)
        {
            throw FIOError(e.message!!,e)
        }
    }

    private fun validateNewFundsRequest(payerFioAddress:String, payeeFioAddress:String,
                                        fundsRequestContent: FundsRequestContent,technologyPartnerId:String): Validator
    {
        var isValid = (payerFioAddress.isFioAddress() && payeeFioAddress.isFioAddress()
                && fundsRequestContent.tokenCode.isTokenCode())

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        return Validator(isValid,if(!isValid) "Invalid New Funds Request" else "")
    }

    private fun validateAddPublicAddress(fioAddress:String, chainCode:String, tokenCode:String,
                                         tokenPublicAddress:String,
                                         technologyPartnerId:String=""): Validator
    {
        var isValid = (fioAddress.isFioAddress()
                && tokenPublicAddress.isNativeBlockChainPublicAddress()
                && tokenCode.isTokenCode() && chainCode.isTokenCode())

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        return Validator(isValid,if(!isValid) "Invalid AddPublicAddress Request" else "")
    }

    private fun validateAddPublicAddresses(fioAddress:String,
                                           tokenPublicAddresses:List<TokenPublicAddress>,
                                           technologyPartnerId:String=""): Validator
    {
        var isValid = true

        for(address in tokenPublicAddresses)
        {
            isValid = isValid && this.validateAddPublicAddress(fioAddress,address.chainCode,address.tokenCode,address.publicAddress,technologyPartnerId).isValid
        }

        return Validator(isValid,if(!isValid) "Invalid AddPublicAddress Request" else "")
    }

    private fun validateRegisterFioAddress(fioAddress:String ,ownerPublicKey:String,
                                           technologyPartnerId:String): Validator
    {
        var isValid = fioAddress.isFioAddress()

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        if(ownerPublicKey.isNotEmpty())
            isValid = isValid && ownerPublicKey.isFioPublicKey()

        return Validator(isValid,if(!isValid) "Invalid Register FIO Address Request" else "")
    }

    private fun validateRegisterFioDomain(fioDomain:String ,ownerPublicKey:String,
                                          technologyPartnerId:String): Validator
    {
        var isValid = fioDomain.isFioDomain()

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        if(ownerPublicKey.isNotEmpty())
            isValid = isValid && ownerPublicKey.isFioPublicKey()

        return Validator(isValid,if(!isValid) "Invalid Register FIO Domain Request" else "")
    }

    private fun validateRenewFioAddress(fioAddress:String, technologyPartnerId:String): Validator
    {
        try {
            return this.validateRegisterFioAddress(fioAddress,"",technologyPartnerId)
        }
        catch(e:Exception)
        {
            throw FIOError("Invalid Renew FIO Address Request")
        }
    }

    private fun validateRenewFioDomain(fioDomain:String, technologyPartnerId:String): Validator
    {
        try {
            return this.validateRegisterFioDomain(fioDomain,"",technologyPartnerId)
        }
        catch(e:Exception)
        {
            throw FIOError("Invalid Renew FIO Domain Request")
        }
    }

    private fun validateSetFioDomainVisibility(fioDomain:String, technologyPartnerId:String): Validator
    {
        var isValid = fioDomain.isFioDomain()

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        return Validator(isValid,if(!isValid) "Invalid Set FIO Domain Visibility Request" else "")
    }

    private fun validateRejectFundsRequest(fioRequestId:BigInteger,technologyPartnerId:String): Validator
    {
        var isValid = fioRequestId > BigInteger.ZERO

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        return Validator(isValid,if(!isValid) "Invalid Reject Funds Request" else "")
    }

    private fun validateRecordObtDataRequest(fioRequestId: BigInteger, payerFioAddress:String,
                                          payeeFioAddress:String, recordObtDataContent: RecordObtDataContent,
                                             technologyPartnerId:String): Validator
    {
        var isValid = fioRequestId >= BigInteger.ZERO

        isValid = isValid && (payerFioAddress.isFioAddress() && payeeFioAddress.isFioAddress()
                && recordObtDataContent.tokenCode.isTokenCode())

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        return Validator(isValid,if(!isValid) "Invalid Send Record Request" else "")
    }

    private fun validateTransferPublicTokens(payeeFioPublicKey:String, technologyPartnerId:String=""): Validator
    {
        var isValid = payeeFioPublicKey.isFioPublicKey()

        if(technologyPartnerId.isNotEmpty())
            isValid = isValid && technologyPartnerId.isFioAddress()

        return Validator(isValid,if(!isValid) "Invalid Transfer Public Tokens Request" else "")
    }
}