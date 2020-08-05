package fiofoundation.io.fiosdk.implementations

import fiofoundation.io.fiosdk.interfaces.ISignatureProvider
import fiofoundation.io.fiosdk.errors.signatureprovider.GetAvailableKeysError
import fiofoundation.io.fiosdk.errors.PEMProcessorError
import fiofoundation.io.fiosdk.enums.AlgorithmEmployed
import fiofoundation.io.fiosdk.models.PEMProcessor
import fiofoundation.io.fiosdk.errors.signatureprovider.SignTransactionError
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bitcoinj.core.Sha256Hash
import org.bouncycastle.util.encoders.Hex
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.errors.formatters.FIOFormatterError
import fiofoundation.io.fiosdk.errors.formatters.FIOFormatterSignatureIsNotCanonicalError
import fiofoundation.io.fiosdk.errors.signatureprovider.ImportKeyError
import fiofoundation.io.fiosdk.errors.signatureprovider.SoftKeySignatureErrorConstants
import fiofoundation.io.fiosdk.formatters.FIOFormatter
import fiofoundation.io.fiosdk.models.signatureprovider.FIOTransactionSignatureRequest
import fiofoundation.io.fiosdk.models.signatureprovider.FIOTransactionSignatureResponse
import java.math.BigInteger


class SoftKeySignatureProvider: ISignatureProvider
{

    private val keys = LinkedHashSet<String>()

    private val MAX_NOT_CANONICAL_RE_SIGN = 100

    private val R_INDEX = 0

    private val S_INDEX = 1

    private val BIG_INTEGER_POSITIVE = 1

    private val USING_K1_LEGACY_FORMAT = true

    private val USING_K1_NON_LEGACY_FORMAT = false

    private val DEFAULT_WHETHER_USING_K1_LEGACY_FORMAT = USING_K1_LEGACY_FORMAT

    @Throws(ImportKeyError::class)
    fun importKey(privateKey: String) {
        if (privateKey.isEmpty()) {
            throw ImportKeyError(SoftKeySignatureErrorConstants.IMPORT_KEY_INPUT_EMPTY_ERROR)
        }

        val privateKeyPem: String

        try {
            privateKeyPem = FIOFormatter.convertFIOPrivateKeyToPEMFormat(privateKey)
        } catch (fioFormatterError: FIOFormatterError) {
            throw ImportKeyError(
                String.format(
                    SoftKeySignatureErrorConstants.IMPORT_KEY_CONVERT_TO_PEM_ERROR,
                    privateKey
                ), fioFormatterError
            )
        }

        if (privateKeyPem.isEmpty()) {
            throw ImportKeyError(SoftKeySignatureErrorConstants.CONVERT_TO_PEM_EMPTY_ERROR)
        }

        this.keys.add(privateKeyPem)
    }

    @Throws(SignTransactionError::class)
    override fun signTransaction(fioTransactionSignatureRequest: FIOTransactionSignatureRequest): FIOTransactionSignatureResponse {

        if (fioTransactionSignatureRequest.signingPublicKeys!!.isEmpty()) {
            throw SignTransactionError(SoftKeySignatureErrorConstants.SIGN_TRANS_EMPTY_KEY_LIST)

        }

        if (fioTransactionSignatureRequest.chainId!!.isEmpty()) {
            throw SignTransactionError(SoftKeySignatureErrorConstants.SIGN_TRANS_EMPTY_CHAIN_ID)
        }

        if (fioTransactionSignatureRequest.serializedTransaction!!.isEmpty()) {
            throw SignTransactionError(SoftKeySignatureErrorConstants.SIGN_TRANS_EMPTY_TRANSACTION)
        }

        val serializedTransaction = fioTransactionSignatureRequest.serializedTransaction

        // This is the un-hashed message which is used to recover public key
        val message: ByteArray

        // This is the hashed message which is signed.
        val hashedMessage: ByteArray

        try
        {
            message = Hex.decode(FIOFormatter.prepareSerializedTransactionForSigning(
                serializedTransaction!!,
                fioTransactionSignatureRequest.chainId!!).toUpperCase())

            hashedMessage = Sha256Hash.hash(message)
        }
        catch (fioFormatterError: FIOFormatterError) {
            throw SignTransactionError(String.format(SoftKeySignatureErrorConstants.SIGN_TRANS_PREPARE_SIGNABLE_TRANS_ERROR,
                    serializedTransaction
                ), fioFormatterError
            )
        }

        if (this.keys.isEmpty()) {
            throw SignTransactionError(SoftKeySignatureErrorConstants.SIGN_TRANS_NO_KEY_AVAILABLE)
        }

        val signatures = ArrayList<String>()

        // Getting public key and search for the corresponding private key
        for (inputPublicKey in fioTransactionSignatureRequest.signingPublicKeys!!) {

            var privateKeyBI = BigInteger.ZERO
            var curve: AlgorithmEmployed? = null

            try {
                // Search for corresponding private key
                for (key in keys) {
                    val availableKeyProcessor = PEMProcessor(key)
                    //Extract public key in PEM format from inner private key
                    val innerPublicKeyPEM =
                        availableKeyProcessor.extractPEMPublicKeyFromPrivateKey(DEFAULT_WHETHER_USING_K1_LEGACY_FORMAT)

                    // Convert input public key to PEM format for comparision
                    val inputPublicKeyPEM = FIOFormatter.convertFIOPublicKeyToPEMFormat(inputPublicKey)

                    if (innerPublicKeyPEM == inputPublicKeyPEM) {
                        privateKeyBI = BigInteger(BIG_INTEGER_POSITIVE, availableKeyProcessor.getKeyData())
                        curve = availableKeyProcessor.getAlgorithm()
                        break
                    }
                }
            }
            catch (error: FIOError)
            {
                throw SignTransactionError(
                    String.format(SoftKeySignatureErrorConstants.SIGN_TRANS_SEARCH_KEY_ERROR,
                        inputPublicKey
                    ), error
                )
            }

            // Throw error if found no private key with input public key

            if (privateKeyBI == BigInteger.ZERO || curve == null) {
                throw SignTransactionError(
                    String.format(SoftKeySignatureErrorConstants.SIGN_TRANS_KEY_NOT_FOUND,
                        inputPublicKey
                    )
                )
            }

            for (i in 0 until MAX_NOT_CANONICAL_RE_SIGN) {
                // Sign transaction
                // Use default constructor to have signature generated with secureRandom, otherwise it would generate same signature for same key all the time
                val signer = ECDSASigner()

                val domainParameters: ECDomainParameters
                try
                {
                    domainParameters = PEMProcessor.getCurveDomainParameters(curve)
                }
                catch (processorError: PEMProcessorError) {
                    throw SignTransactionError(
                        String.format(SoftKeySignatureErrorConstants.SIGN_TRANS_GET_CURVE_DOMAIN_ERROR,
                            curve.string
                        ), processorError
                    )
                }

                val parameters = ECPrivateKeyParameters(privateKeyBI, domainParameters)
                signer.init(true, parameters)

                val signatureComponents = signer.generateSignature(hashedMessage)

                try {
                    val signature = FIOFormatter.convertRawRandSofSignatureToFIOFormat(
                        signatureComponents[R_INDEX].toString(),
                        signatureComponents[S_INDEX].toString(),
                        message, FIOFormatter.convertFIOPublicKeyToPEMFormat(inputPublicKey)
                    )
                    // Format Signature
                    signatures.add(signature)
                    break
                } catch (eosFormatterError: FIOFormatterError) {
                    // In theory, Non-canonical error only happened with K1 key
                    if (eosFormatterError.cause is FIOFormatterSignatureIsNotCanonicalError && curve === AlgorithmEmployed.SECP256K1) {
                        // Try to sign again until MAX_NOT_CANONICAL_RE_SIGN is reached or get a canonical signature
                        continue
                    }

                    throw SignTransactionError(
                        SoftKeySignatureErrorConstants.SIGN_TRANS_FORMAT_SIGNATURE_ERROR,
                        eosFormatterError
                    )
                }

            }
        }

        return FIOTransactionSignatureResponse(serializedTransaction, signatures, null)
    }

    @Throws(GetAvailableKeysError::class)
    override fun getAvailableKeys(): List<String> {
        val availableKeys = ArrayList<String>()
        if (this.keys.isEmpty()) {
            return availableKeys
        }

        try {
            for (key in this.keys) {
                val processor = PEMProcessor(key)
                val curve = processor.getAlgorithm()

                when (curve) {
                    AlgorithmEmployed.SECP256R1 ->
                        // USING_K1_NON_LEGACY_FORMAT is being used here because its value does not matter to SECP256R1 key
                        availableKeys.add(processor.extractFIOPublicKeyFromPrivateKey(USING_K1_NON_LEGACY_FORMAT))

                    AlgorithmEmployed.SECP256K1 -> {
                        // Non legacy
                        //availableKeys.add(processor.extractFIOPublicKeyFromPrivateKey(USING_K1_NON_LEGACY_FORMAT))
                        // legacy
                        availableKeys.add(processor.extractFIOPublicKeyFromPrivateKey(USING_K1_LEGACY_FORMAT))
                    }

                    else -> throw GetAvailableKeysError(SoftKeySignatureErrorConstants.GET_KEYS_KEY_FORMAT_NOT_SUPPORTED)
                }
            }
        } catch (pemProcessorError: PEMProcessorError) {
            throw GetAvailableKeysError(SoftKeySignatureErrorConstants.CONVERT_TO_PEM_EMPTY_ERROR, pemProcessorError)
        }

        return availableKeys
    }
}