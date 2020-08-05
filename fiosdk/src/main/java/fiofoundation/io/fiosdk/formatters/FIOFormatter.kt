package fiofoundation.io.fiosdk.formatters

import com.google.common.base.Preconditions.checkArgument
import com.google.common.primitives.Bytes

import java.io.CharArrayReader
import java.math.BigInteger
import java.util.Arrays

import fiofoundation.io.fiosdk.models.PEMProcessor
import fiofoundation.io.fiosdk.enums.AlgorithmEmployed
import fiofoundation.io.fiosdk.errors.DerToPemConversionError
import fiofoundation.io.fiosdk.errors.ErrorConstants
import fiofoundation.io.fiosdk.errors.LowSVerificationError
import fiofoundation.io.fiosdk.errors.formatters.FIOFormatterError
import fiofoundation.io.fiosdk.errors.formatters.FIOFormatterSignatureIsNotCanonicalError
import fiofoundation.io.fiosdk.errors.Base58ManipulationError
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.asn1.x9.X9IntegerConverter
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECAlgorithms
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.FixedPointUtil
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader

class FIOFormatter {

    companion object Static {
        val PATTERN_STRING_FIO_PREFIX_EOS = "FIO"
        private val PATTERN_STRING_FIO_PREFIX_PUB_R1 = "PUB_R1_"
        private val PATTERN_STRING_FIO_PREFIX_PUB_K1 = "PUB_K1_"
        private val PATTERN_STRING_FIO_PREFIX_PVT_R1 = "PVT_R1_"
        private val PATTERN_STRING_FIO_PREFIX_SIG_R1 = "SIG_R1_"
        private val PATTERN_STRING_FIO_PREFIX_SIG_K1 = "SIG_K1_"

        private val PATTERN_STRING_PEM_PREFIX_PRIVATE_KEY_SECP256R1 = "30770201010420"
        private val PATTERN_STRING_PEM_PREFIX_PRIVATE_KEY_SECP256K1 = "302E0201010420"
        private val PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256R1_UNCOMPRESSED = "3059301306072a8648ce3d020106082a8648ce3d030107034200"
        private val PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256K1_UNCOMPRESSED = "3056301006072a8648ce3d020106052b8104000a034200"
        private val PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256R1_COMPRESSED = "3039301306072a8648ce3d020106082a8648ce3d030107032200"
        private val PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256K1_COMPRESSED = "3036301006072a8648ce3d020106052b8104000a032200"

        private val PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256K1 = "A00706052B8104000A"
        private val PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256R1 = "A00A06082A8648CE3D030107"

        private val PEM_HEADER_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----"
        private val PEM_FOOTER_PUBLIC_KEY = "-----END PUBLIC KEY-----"
        private val PEM_HEADER_PRIVATE_KEY = "-----BEGIN EC PRIVATE KEY-----"
        private val PEM_FOOTER_PRIVATE_KEY = "-----END EC PRIVATE KEY-----"
        private val PEM_HEADER_EC_PRIVATE_KEY = "EC PRIVATE KEY"
        private val PEM_HEADER_EC_PUBLIC_KEY = "PUBLIC KEY"

        private val SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX = "R1"
        private val SECP256K1_CHECKSUM_VALIDATION_SUFFIX = "K1"
        private val LEGACY_CHECKSUM_VALIDATION_SUFFIX = ""

        private val STANDARD_KEY_LENGTH = 32
        private val CHECKSUM_BYTES = 4
        private val FIRST_TWO_BYTES_OF_KEY = 4
        private val DATA_SEQUENCE_LENGTH_BYTE_POSITION = 2

        private val FIO_SECP256K1_HEADER_BYTE = 0x80

        private val UNCOMPRESSED_PUBLIC_KEY_BYTE_INDICATOR:Byte = 0x04
        private val COMPRESSED_PUBLIC_KEY_BYTE_INDICATOR_POSITIVE_Y:Byte = 0x02
        private val COMPRESSED_PUBLIC_KEY_BYTE_INDICATOR_NEGATIVE_Y:Byte = 0x03

        private val CHAIN_ID_LENGTH = 64

        private val MINIMUM_SIGNABLE_TRANSACTION_LENGTH = CHAIN_ID_LENGTH + 64 + 1

        private val VALUE_TO_ADD_TO_SIGNATURE_HEADER = 31
        private val EXPECTED_R_OR_S_LENGTH = 32
        private val NUMBER_OF_POSSIBLE_PUBLIC_KEYS = 4

        private val SECP256_R1 = "secp256r1"

        private val SECP256_K1 = "secp256k1"

        private val ecParamsR1:ECDomainParameters

        private val ecParamsK1:ECDomainParameters

        private val CURVE_PARAMS_R1 = CustomNamedCurves.getByName(SECP256_R1)

        private val CURVE_PARAMS_K1 = CustomNamedCurves.getByName(SECP256_K1)

        private val CURVE_R1:ECDomainParameters

        private val HALF_CURVE_ORDER_R1:BigInteger

        private val CURVE_K1:ECDomainParameters

        private val HALF_CURVE_ORDER_K1:BigInteger

        private enum class PEMObjectType constructor(private val string:String) {
            PUBLICKEY("PUBLIC KEY"),
            PRIVATEKEY("PRIVATE KEY"),
            SIGNATURE("SIGNATURE")
        }


        init
        {
            val paramsR1 = SECNamedCurves.getByName(SECP256_R1)
            val paramsK1 = SECNamedCurves.getByName(SECP256_K1)

            ecParamsR1 = ECDomainParameters(paramsR1.curve, paramsR1.g, paramsR1.n, paramsR1.h)
            ecParamsK1 = ECDomainParameters(paramsK1.curve, paramsK1.g, paramsK1.n, paramsK1.h)

            FixedPointUtil.precompute(CURVE_PARAMS_R1.g)
            CURVE_R1 = ECDomainParameters(CURVE_PARAMS_R1.curve, CURVE_PARAMS_R1.g, CURVE_PARAMS_R1.n, CURVE_PARAMS_R1.h)
            HALF_CURVE_ORDER_R1 = CURVE_PARAMS_R1.n.shiftRight(1)

            CURVE_K1 = ECDomainParameters(CURVE_PARAMS_K1.curve, CURVE_PARAMS_K1.g, CURVE_PARAMS_K1.n, CURVE_PARAMS_K1.h)
            HALF_CURVE_ORDER_K1 = CURVE_PARAMS_K1.n.shiftRight(1)
        }

        @Throws(FIOFormatterError::class)
        fun convertPEMFormattedPublicKeyToFIOFormat(publicKeyPEM:String, requireLegacyFormOfSecp256k1Key:Boolean): String
        {
            var fioFormattedPublicKey = publicKeyPEM
            var algorithmEmployed: AlgorithmEmployed
            var pemObject: PemObject? = null
            var type: String = ""

            try
            {
                CharArrayReader(fioFormattedPublicKey.toCharArray()).use {
                        reader-> PemReader(reader).use {
                        pemReader-> pemObject = pemReader.readPemObject()
                    type = pemObject!!.type
                }
                }
            }
            catch (e:Exception)
            {
                throw FIOFormatterError(ErrorConstants.INVALID_PEM_PRIVATE_KEY, e)
            }

            if (type.matches(("(?i:.*$PEM_HEADER_EC_PUBLIC_KEY.*)").toRegex()))
            {

                fioFormattedPublicKey = Hex.toHexString(pemObject!!.content)

                if (fioFormattedPublicKey.toUpperCase().contains(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256R1_UNCOMPRESSED.toUpperCase()))
                {
                    fioFormattedPublicKey = fioFormattedPublicKey.toUpperCase().replace(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256R1_UNCOMPRESSED.toUpperCase(),"")
                    algorithmEmployed = AlgorithmEmployed.SECP256R1
                }
                else if (fioFormattedPublicKey.toUpperCase().contains(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256R1_COMPRESSED.toUpperCase()))
                {
                    fioFormattedPublicKey = fioFormattedPublicKey.toUpperCase().replace(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256R1_COMPRESSED.toUpperCase(),"")
                    algorithmEmployed = AlgorithmEmployed.SECP256R1
                }
                else if (fioFormattedPublicKey.toUpperCase().contains(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256K1_UNCOMPRESSED.toUpperCase()))
                {
                    fioFormattedPublicKey = fioFormattedPublicKey.toUpperCase().replace(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256K1_UNCOMPRESSED.toUpperCase(),"")
                    algorithmEmployed = AlgorithmEmployed.SECP256K1
                }
                else if (fioFormattedPublicKey.toUpperCase().contains(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256K1_COMPRESSED.toUpperCase()))
                {
                    fioFormattedPublicKey = fioFormattedPublicKey.toUpperCase().replace(PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256K1_COMPRESSED.toUpperCase(),"")
                    algorithmEmployed = AlgorithmEmployed.SECP256K1
                }
                else
                {
                    throw FIOFormatterError(ErrorConstants.INVALID_DER_PRIVATE_KEY)
                }

                val eosFormattedPublicKeyBytes = Hex.decode(fioFormattedPublicKey)
                if (eosFormattedPublicKeyBytes[0] == UNCOMPRESSED_PUBLIC_KEY_BYTE_INDICATOR)
                {
                    try
                    {
                        fioFormattedPublicKey = Hex.toHexString(compressPublickey(Hex.decode(fioFormattedPublicKey), algorithmEmployed))
                    }
                    catch (e:Exception) {
                        throw FIOFormatterError(e)
                    }
                }

                try
                {
                    fioFormattedPublicKey = encodePublicKey(Hex.decode(fioFormattedPublicKey),
                        algorithmEmployed, requireLegacyFormOfSecp256k1Key)
                }
                catch (e: Base58ManipulationError)
                {
                    throw FIOFormatterError(e)
                }
            }
            else
            {
                throw FIOFormatterError(ErrorConstants.INVALID_PEM_PRIVATE_KEY)
            }

            return fioFormattedPublicKey
        }

        @Throws(FIOFormatterError::class)
        fun convertFIOPublicKeyToPEMFormat(publicKeyFIO: String): String {
            var pemFormattedPublickKey = publicKeyFIO
            val algorithmEmployed:AlgorithmEmployed
            val keyPrefix:String

            if (pemFormattedPublickKey.toUpperCase().contains(PATTERN_STRING_FIO_PREFIX_PUB_R1.toUpperCase()))
            {
                algorithmEmployed = AlgorithmEmployed.SECP256R1
                keyPrefix = PATTERN_STRING_FIO_PREFIX_PUB_R1
                pemFormattedPublickKey = pemFormattedPublickKey.replace(PATTERN_STRING_FIO_PREFIX_PUB_R1, "")
            }
            else if (pemFormattedPublickKey.toUpperCase().contains(PATTERN_STRING_FIO_PREFIX_PUB_K1.toUpperCase()))
            {
                algorithmEmployed = AlgorithmEmployed.SECP256K1
                keyPrefix = PATTERN_STRING_FIO_PREFIX_PUB_K1
                pemFormattedPublickKey = pemFormattedPublickKey.replace(PATTERN_STRING_FIO_PREFIX_PUB_K1, "")
            }
            else if (pemFormattedPublickKey.toUpperCase().contains(PATTERN_STRING_FIO_PREFIX_EOS.toUpperCase()))
            {
                algorithmEmployed = AlgorithmEmployed.SECP256K1
                keyPrefix = PATTERN_STRING_FIO_PREFIX_EOS
                pemFormattedPublickKey = pemFormattedPublickKey.replace(PATTERN_STRING_FIO_PREFIX_EOS, "")
            }
            else
            {
                throw FIOFormatterError(ErrorConstants.INVALID_EOS_PUBLIC_KEY)
            }

            val base58DecodedPublicKey: ByteArray
            try
            {
                base58DecodedPublicKey = decodePublicKey(pemFormattedPublickKey, keyPrefix)
            }
            catch (e:Exception) {
                throw FIOFormatterError(ErrorConstants.BASE58_DECODING_ERROR, e)
            }

            pemFormattedPublickKey = Hex.toHexString(base58DecodedPublicKey)

            if (base58DecodedPublicKey[0] == UNCOMPRESSED_PUBLIC_KEY_BYTE_INDICATOR)
            {
                try
                {
                    pemFormattedPublickKey = Hex.toHexString(compressPublickey(Hex.decode(pemFormattedPublickKey), algorithmEmployed))
                }
                catch (e:Exception) {
                    throw FIOFormatterError(e)
                }
            }

            when (algorithmEmployed) {
                AlgorithmEmployed.SECP256R1 -> pemFormattedPublickKey = PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256R1_COMPRESSED + pemFormattedPublickKey
                AlgorithmEmployed.SECP256K1 -> pemFormattedPublickKey = PATTERN_STRING_PEM_PREFIX_PUBLIC_KEY_SECP256K1_COMPRESSED + pemFormattedPublickKey
                else -> throw FIOFormatterError(ErrorConstants.UNSUPPORTED_ALGORITHM)
            }

            if (pemFormattedPublickKey.length > FIRST_TWO_BYTES_OF_KEY)
            {
                val i = (pemFormattedPublickKey.length - FIRST_TWO_BYTES_OF_KEY) / 2
                val correctedLength = Integer.toHexString(i)
                pemFormattedPublickKey = (pemFormattedPublickKey.substring(0, DATA_SEQUENCE_LENGTH_BYTE_POSITION) + correctedLength + pemFormattedPublickKey.substring(FIRST_TWO_BYTES_OF_KEY))
            }
            else
            {
                throw FIOFormatterError(ErrorConstants.INVALID_EOS_PUBLIC_KEY)
            }

            try
            {
                pemFormattedPublickKey = derToPEM(Hex.decode(pemFormattedPublickKey), PEMObjectType.PUBLICKEY)
            }
            catch (e:Exception) {
                throw FIOFormatterError(e)
            }

            return pemFormattedPublickKey
        }

        @Throws(FIOFormatterError::class)
        fun convertDERSignatureToFIOFormat(signatureDER:ByteArray, signableTransaction:ByteArray, publicKeyPEM:String):String {
            var eosFormattedSignature = ""

            try
            {
                ASN1InputStream(signatureDER).use { asn1InputStream->

                    val publicKey = PEMProcessor(publicKeyPEM)

                    val algorithmEmployed = publicKey.getAlgorithm()

                    val keyData = publicKey.getKeyData()
                    val sequence = asn1InputStream.readObject() as DLSequence
                    val r = (sequence.getObjectAt(0) as ASN1Integer).positiveValue
                    var s = (sequence.getObjectAt(1) as ASN1Integer).positiveValue

                    s = checkAndHandleLowS(s, algorithmEmployed)

                    var recoverId = getRecoveryId(r, s, Sha256Hash.of(signableTransaction), keyData, algorithmEmployed)

                    if (recoverId < 0) {
                        throw IllegalStateException(ErrorConstants.COULD_NOT_RECOVER_PUBLIC_KEY_FROM_SIG)
                    }

                    recoverId += VALUE_TO_ADD_TO_SIGNATURE_HEADER
                    val headerByte = (recoverId).toByte()

                    val decodedSignature = Bytes.concat(byteArrayOf(headerByte), org.bitcoinj.core.Utils.bigIntegerToBytes(r, EXPECTED_R_OR_S_LENGTH), org.bitcoinj.core.Utils.bigIntegerToBytes(s, EXPECTED_R_OR_S_LENGTH))
                    if ((algorithmEmployed == AlgorithmEmployed.SECP256K1 && !isCanonical(decodedSignature))) {
                        throw IllegalArgumentException(ErrorConstants.NON_CANONICAL_SIGNATURE)
                    }

                    val signatureWithCheckSum:ByteArray
                    val signaturePrefix:String
                    when (algorithmEmployed)
                    {
                        AlgorithmEmployed.SECP256R1 -> {
                            signatureWithCheckSum = addCheckSumToSignature(decodedSignature,
                                SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                            signaturePrefix = PATTERN_STRING_FIO_PREFIX_SIG_R1
                        }
                        AlgorithmEmployed.SECP256K1 -> {
                            signatureWithCheckSum = addCheckSumToSignature(decodedSignature, SECP256K1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                            signaturePrefix = PATTERN_STRING_FIO_PREFIX_SIG_K1
                        }

                        else -> throw FIOFormatterError(ErrorConstants.UNSUPPORTED_ALGORITHM)
                    }

                    eosFormattedSignature = signaturePrefix + Base58.encode(signatureWithCheckSum)

                }
            }
            catch (e:Exception)
            {
                throw FIOFormatterError(ErrorConstants.SIGNATURE_FORMATTING_ERROR, e)
            }

            return eosFormattedSignature
        }

        @Throws(FIOFormatterError::class)
        fun convertRawRandSofSignatureToFIOFormat(signatureR:String, signatureS: String, signableTransaction:ByteArray, publicKeyPEM:String):String {
            var fioFormattedSignature:String

            try
            {
                val publicKey = PEMProcessor(publicKeyPEM)
                val algorithmEmployed = publicKey.getAlgorithm()
                val keyData = publicKey.getKeyData()

                val r = BigInteger(signatureR)
                var s = BigInteger(signatureS)

                s = checkAndHandleLowS(s, algorithmEmployed)

                var recoverId = getRecoveryId(r, s, Sha256Hash.of(signableTransaction), keyData, algorithmEmployed)

                if (recoverId < 0)
                {
                    throw IllegalStateException(ErrorConstants.COULD_NOT_RECOVER_PUBLIC_KEY_FROM_SIG)
                }

                recoverId += VALUE_TO_ADD_TO_SIGNATURE_HEADER
                val headerByte = recoverId.toByte()

                val decodedSignature = Bytes.concat(byteArrayOf(headerByte), org.bitcoinj.core.Utils.bigIntegerToBytes(r, EXPECTED_R_OR_S_LENGTH), org.bitcoinj.core.Utils.bigIntegerToBytes(s, EXPECTED_R_OR_S_LENGTH))
                if (algorithmEmployed == AlgorithmEmployed.SECP256K1 && !isCanonical(decodedSignature))
                {
                    throw FIOFormatterSignatureIsNotCanonicalError(ErrorConstants.NON_CANONICAL_SIGNATURE)
                }

                val signatureWithCheckSum:ByteArray
                val signaturePrefix:String
                when (algorithmEmployed)
                {
                    AlgorithmEmployed.SECP256R1 -> {
                        signatureWithCheckSum = addCheckSumToSignature(decodedSignature, SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                        signaturePrefix = PATTERN_STRING_FIO_PREFIX_SIG_R1
                    }
                    AlgorithmEmployed.SECP256K1 -> {
                        signatureWithCheckSum = addCheckSumToSignature(decodedSignature, SECP256K1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                        signaturePrefix = PATTERN_STRING_FIO_PREFIX_SIG_K1
                    }
                    else -> throw FIOFormatterError(ErrorConstants.UNSUPPORTED_ALGORITHM)
                }

                fioFormattedSignature = signaturePrefix + Base58.encode(signatureWithCheckSum)

            }
            catch (e:Exception)
            {
                throw FIOFormatterError(ErrorConstants.SIGNATURE_FORMATTING_ERROR, e)
            }

            return fioFormattedSignature
        }

        @Throws(FIOFormatterError::class)
        fun convertPEMFormattedPrivateKeyToFIOFormat(privateKeyPEM: String): String {
            var fioFormattedPrivateKey = privateKeyPEM
            val algorithmEmployed:AlgorithmEmployed
            var pemObject:PemObject? = null
            var type = ""

            try
            {
                CharArrayReader(fioFormattedPrivateKey.toCharArray()).use { reader-> PemReader(reader).use { pemReader->
                        pemObject = pemReader.readPemObject()
                        type = pemObject!!.type
                    }
                }
            }
            catch (e:Exception) {
                throw FIOFormatterError(ErrorConstants.INVALID_PEM_PRIVATE_KEY, e)
            }

            if (type.matches(("(?i:.*$PEM_HEADER_EC_PRIVATE_KEY.*)").toRegex()))
            {

                //Get Base64 encoded private key from PEM object
                fioFormattedPrivateKey = Hex.toHexString(pemObject!!.content)

                //Determine algorithm used to generate key
                if (fioFormattedPrivateKey.matches(("(?i:.*$PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256R1.*)").toRegex()))
                {
                    algorithmEmployed = AlgorithmEmployed.SECP256R1
                }
                else if (fioFormattedPrivateKey.matches(("(?i:.*$PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256K1.*)").toRegex()))
                {
                    algorithmEmployed = AlgorithmEmployed.SECP256K1
                }
                else
                {
                    throw FIOFormatterError(ErrorConstants.INVALID_DER_PRIVATE_KEY)
                }

                //Strip away the DER header and footer
                when (algorithmEmployed)
                {
                    AlgorithmEmployed.SECP256R1 -> fioFormattedPrivateKey = fioFormattedPrivateKey.substring(PATTERN_STRING_PEM_PREFIX_PRIVATE_KEY_SECP256R1.length, fioFormattedPrivateKey.length - PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256R1.length)
                    AlgorithmEmployed.SECP256K1 -> fioFormattedPrivateKey = fioFormattedPrivateKey.substring(PATTERN_STRING_PEM_PREFIX_PRIVATE_KEY_SECP256K1.length, (fioFormattedPrivateKey.length - PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256K1.length))
                    else -> throw FIOFormatterError(ErrorConstants.UNSUPPORTED_ALGORITHM)
                }

                try
                {
                    fioFormattedPrivateKey = encodePrivateKey(Hex.decode(fioFormattedPrivateKey), algorithmEmployed)
                }
                catch (e: Base58ManipulationError) {
                    throw FIOFormatterError(e)
                }

                val builder = StringBuilder(fioFormattedPrivateKey)
                when (algorithmEmployed)
                {
                    AlgorithmEmployed.SECP256K1 -> {}
                    AlgorithmEmployed.SECP256R1 -> builder.insert(0, PATTERN_STRING_FIO_PREFIX_PVT_R1)
                    else -> {}
                }

                fioFormattedPrivateKey = builder.toString()
            }
            else
            {
                throw FIOFormatterError(ErrorConstants.INVALID_PEM_PRIVATE_KEY)
            }

            return fioFormattedPrivateKey
        }

        @Throws(FIOFormatterError::class)
        fun convertFIOPrivateKeyToPEMFormat(privateKeyEOS:String): String {
            var pemFormattedPrivateKey = privateKeyEOS
            val algorithmEmployed:AlgorithmEmployed

            if (pemFormattedPrivateKey.toUpperCase().contains(PATTERN_STRING_FIO_PREFIX_PVT_R1.toUpperCase()))
            {
                algorithmEmployed = AlgorithmEmployed.SECP256R1

                pemFormattedPrivateKey = pemFormattedPrivateKey.split((PATTERN_STRING_FIO_PREFIX_PVT_R1).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            }
            else
            {
                algorithmEmployed = AlgorithmEmployed.SECP256K1
            }

            val base58DecodedPrivateKey:ByteArray
            try
            {
                base58DecodedPrivateKey = decodePrivateKey(pemFormattedPrivateKey, algorithmEmployed)
            }
            catch (e:Exception) {
                throw FIOFormatterError(ErrorConstants.BASE58_DECODING_ERROR, e)
            }

            pemFormattedPrivateKey = Hex.toHexString(base58DecodedPrivateKey)

            when (algorithmEmployed) {
                AlgorithmEmployed.SECP256R1 -> pemFormattedPrivateKey = (PATTERN_STRING_PEM_PREFIX_PRIVATE_KEY_SECP256R1 + pemFormattedPrivateKey
                        + PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256R1)
                AlgorithmEmployed.SECP256K1 -> pemFormattedPrivateKey = (PATTERN_STRING_PEM_PREFIX_PRIVATE_KEY_SECP256K1 + pemFormattedPrivateKey
                        + PATTERN_STRING_PEM_SUFFIX_PRIVATE_KEY_SECP256K1)
                else -> throw FIOFormatterError(ErrorConstants.UNSUPPORTED_ALGORITHM)
            }

            if (pemFormattedPrivateKey.length > FIRST_TWO_BYTES_OF_KEY)
            {
                val i = (pemFormattedPrivateKey.length - FIRST_TWO_BYTES_OF_KEY) / 2
                val correctedLength = Integer.toHexString(i)
                pemFormattedPrivateKey = (pemFormattedPrivateKey.substring(0, DATA_SEQUENCE_LENGTH_BYTE_POSITION)
                        + correctedLength + pemFormattedPrivateKey.substring(FIRST_TWO_BYTES_OF_KEY))
            }
            else
            {
                throw FIOFormatterError(ErrorConstants.INVALID_EOS_PRIVATE_KEY)
            }

            try
            {
                pemFormattedPrivateKey = derToPEM(Hex.decode(pemFormattedPrivateKey), PEMObjectType.PRIVATEKEY)
            }
            catch (e:DerToPemConversionError) {
                throw FIOFormatterError(e)
            }

            return pemFormattedPrivateKey
        }

        @Throws(FIOFormatterError::class)
        fun extractSerializedTransactionFromSignable(eosTransaction:String): String {
            if (eosTransaction.isEmpty())
            {
                throw FIOFormatterError(ErrorConstants.EMPTY_INPUT_EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE)
            }

            if (eosTransaction.length <= MINIMUM_SIGNABLE_TRANSACTION_LENGTH)
            {
                throw FIOFormatterError(String.format(ErrorConstants.INVALID_INPUT_SIGNABLE_TRANS_LENGTH_EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE, MINIMUM_SIGNABLE_TRANSACTION_LENGTH))
            }

            if (!eosTransaction.endsWith(Hex.toHexString(ByteArray(32))))
            {
                throw FIOFormatterError(ErrorConstants.INVALID_INPUT_SIGNABLE_TRANS_EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE)
            }

            try
            {
                val cutChainId = eosTransaction.substring(CHAIN_ID_LENGTH)
                return cutChainId.substring(0, cutChainId.length - Hex.toHexString(ByteArray(32)).length)
            }
            catch (ex:Exception) {
                throw FIOFormatterError(ErrorConstants.EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE_ERROR, ex)
            }

        }

        @Throws(FIOFormatterError::class)
        fun prepareSerializedTransactionForSigning(serializedTransaction:String, chainId:String):String
        {
            if (serializedTransaction.isEmpty() || chainId.isEmpty())
            {
                throw FIOFormatterError(ErrorConstants.EMPTY_INPUT_PREPARE_SERIALIZIED_TRANS_FOR_SIGNING)
            }

            val signableTransaction = chainId + serializedTransaction + Hex.toHexString(ByteArray(32))
            if (signableTransaction.length <= MINIMUM_SIGNABLE_TRANSACTION_LENGTH)
            {
                throw FIOFormatterError(String.format(ErrorConstants.INVALID_INPUT_SIGNABLE_TRANS_LENGTH_EXTRACT_SERIALIZIED_TRANS_FROM_SIGNABLE, MINIMUM_SIGNABLE_TRANSACTION_LENGTH))
            }

            return signableTransaction
        }


        @Throws(DerToPemConversionError::class)
        private fun derToPEM(derEncodedByteArray:ByteArray, pemObjectType:PEMObjectType): String {
            val pemForm = StringBuilder()
            try
            {
                if (pemObjectType == PEMObjectType.PRIVATEKEY)
                {
                    pemForm.append(PEM_HEADER_PRIVATE_KEY)
                }
                else if (pemObjectType == PEMObjectType.PUBLICKEY)
                {
                    pemForm.append(PEM_HEADER_PUBLIC_KEY)
                }
                else
                {
                    throw DerToPemConversionError(ErrorConstants.DER_TO_PEM_CONVERSION)
                }

                pemForm.append("\n")

                val base64EncodedByteArray = String(Base64.encode(derEncodedByteArray))
                pemForm.append(base64EncodedByteArray)
                pemForm.append("\n")

                if (pemObjectType == PEMObjectType.PRIVATEKEY)
                {
                    pemForm.append(PEM_FOOTER_PRIVATE_KEY)
                }
                else if (pemObjectType == PEMObjectType.PUBLICKEY)
                {
                    pemForm.append(PEM_FOOTER_PUBLIC_KEY)
                }
                else
                {
                    throw DerToPemConversionError(ErrorConstants.DER_TO_PEM_CONVERSION)
                }
            }
            catch (e:Exception)
            {
                throw DerToPemConversionError(ErrorConstants.DER_TO_PEM_CONVERSION, e)
            }

            return pemForm.toString()
        }

        @Throws(Base58ManipulationError::class)
        fun decodePrivateKey(strKey:String, keyType: AlgorithmEmployed):ByteArray
        {
            if (strKey.isEmpty())
            {
                throw IllegalArgumentException(ErrorConstants.BASE58_EMPTY_KEY)
            }

            var decodedKey: ByteArray

            try
            {
                val base58Decoded = Base58.decode(strKey)
                val firstCheckSum = Arrays.copyOfRange(base58Decoded, base58Decoded.size - CHECKSUM_BYTES, base58Decoded.size)

                decodedKey = Arrays.copyOfRange(base58Decoded, 0, base58Decoded.size - CHECKSUM_BYTES)

                when (keyType)
                {
                    AlgorithmEmployed.SECP256R1 -> {
                        val secp256r1Suffix = SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray()
                        if (invalidRipeMD160CheckSum(decodedKey, firstCheckSum, secp256r1Suffix))
                        {
                            throw IllegalArgumentException(ErrorConstants.BASE58_INVALID_CHECKSUM)
                        }
                    }

                    AlgorithmEmployed.PRIME256V1 -> {
                        val prime256v1Suffix = SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray()
                        if (invalidRipeMD160CheckSum(decodedKey, firstCheckSum, prime256v1Suffix))
                        {
                            throw IllegalArgumentException(ErrorConstants.BASE58_INVALID_CHECKSUM)
                        }
                    }

                    AlgorithmEmployed.SECP256K1 -> if (invalidSha256x2CheckSum(decodedKey, firstCheckSum))
                    {
                        throw IllegalArgumentException(ErrorConstants.BASE58_INVALID_CHECKSUM)
                    }
                }

                if (decodedKey.size > STANDARD_KEY_LENGTH && keyType !== AlgorithmEmployed.SECP256R1)
                {
                    decodedKey = Arrays.copyOfRange(decodedKey, 1, decodedKey.size)
                    if ((decodedKey.size > STANDARD_KEY_LENGTH && decodedKey[STANDARD_KEY_LENGTH] == (1).toByte()))
                    {
                        decodedKey = Arrays.copyOfRange(decodedKey, 0, decodedKey.size - 1)
                    }
                }
            }
            catch (ex:Exception)
            {
                throw Base58ManipulationError(ErrorConstants.BASE58_DECODING_ERROR, ex)
            }

            return decodedKey
        }

        @Throws(Base58ManipulationError::class)
        fun encodePrivateKey(pemKey:ByteArray, keyType:AlgorithmEmployed): String
        {
            var localPemKey = pemKey
            val checkSum:ByteArray
            var base58Key: String

            when (keyType)
            {
                AlgorithmEmployed.SECP256R1 -> checkSum = extractCheckSumRIPEMD160(pemKey, SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                AlgorithmEmployed.PRIME256V1 -> checkSum = extractCheckSumRIPEMD160(pemKey, SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                AlgorithmEmployed.SECP256K1 -> {
                    localPemKey = Bytes.concat(byteArrayOf((FIO_SECP256K1_HEADER_BYTE).toByte()),
                        pemKey)
                    checkSum = extractCheckSumSha256x2(localPemKey)
                }
            }

            base58Key = Base58.encode(Bytes.concat(localPemKey, checkSum))

            return if (base58Key.isEmpty()) {
                throw Base58ManipulationError(ErrorConstants.BASE58_ENCODING_ERROR)
            } else {
                base58Key
            }
        }

        @Throws(Base58ManipulationError::class)
        fun encodePublicKey(pemKey:ByteArray, keyType:AlgorithmEmployed, isLegacy:Boolean):String
        {
            var base58Key:String
            if (pemKey.isEmpty())
            {
                throw IllegalArgumentException(ErrorConstants.PUBLIC_KEY_IS_EMPTY)
            }

            try
            {
                val checkSum:ByteArray
                when (keyType)
                {
                    AlgorithmEmployed.SECP256K1 -> if (isLegacy)
                    {
                        checkSum = extractCheckSumRIPEMD160(pemKey, LEGACY_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                    }
                    else
                    {
                        checkSum = extractCheckSumRIPEMD160(pemKey, SECP256K1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())
                    }

                    AlgorithmEmployed.SECP256R1 -> checkSum = extractCheckSumRIPEMD160(pemKey, SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray())

                    else -> throw Base58ManipulationError(ErrorConstants.UNSUPPORTED_ALGORITHM)
                }

                base58Key = Base58.encode(Bytes.concat(pemKey, checkSum))

                if (base58Key == "")
                {
                    throw Base58ManipulationError(ErrorConstants.BASE58_ENCODING_ERROR)
                }

            }
            catch (ex:Exception)
            {
                throw Base58ManipulationError(ErrorConstants.BASE58_ENCODING_ERROR, ex)
            }

            val builder = StringBuilder(base58Key)
            when (keyType)
            {
                AlgorithmEmployed.SECP256K1 -> if (isLegacy)
                {
                    builder.insert(0, PATTERN_STRING_FIO_PREFIX_EOS)
                }
                else
                {
                    builder.insert(0, PATTERN_STRING_FIO_PREFIX_PUB_K1)
                }
                AlgorithmEmployed.SECP256R1 -> builder.insert(0, PATTERN_STRING_FIO_PREFIX_PUB_R1)

                else -> {}
            }

            base58Key = builder.toString()

            return base58Key
        }

        @Throws(Base58ManipulationError::class)
        fun decodePublicKey(strKey:String, keyPrefix:String): ByteArray
        {
            if (strKey.isEmpty())
            {
                throw IllegalArgumentException("Input key to decode can't be empty.")
            }

            var decodedKey:ByteArray

            try
            {
                val base58Decoded = Base58.decode(strKey)

                val firstCheckSum = Arrays.copyOfRange(base58Decoded, base58Decoded.size - CHECKSUM_BYTES, base58Decoded.size)

                decodedKey = Arrays.copyOfRange(base58Decoded, 0, base58Decoded.size - CHECKSUM_BYTES)

                when (keyPrefix)
                {
                    PATTERN_STRING_FIO_PREFIX_PUB_R1 -> if (invalidRipeMD160CheckSum(decodedKey, firstCheckSum, SECP256R1_AND_PRIME256V1_CHECKSUM_VALIDATION_SUFFIX.toByteArray()))
                    {
                        throw IllegalArgumentException(ErrorConstants.BASE58_INVALID_CHECKSUM)
                    }

                    PATTERN_STRING_FIO_PREFIX_PUB_K1 -> if (invalidRipeMD160CheckSum(decodedKey, firstCheckSum, SECP256K1_CHECKSUM_VALIDATION_SUFFIX.toByteArray()))
                    {
                        throw IllegalArgumentException(ErrorConstants.BASE58_INVALID_CHECKSUM)
                    }

                    PATTERN_STRING_FIO_PREFIX_EOS -> if (invalidRipeMD160CheckSum(decodedKey, firstCheckSum, LEGACY_CHECKSUM_VALIDATION_SUFFIX.toByteArray()))
                    {
                        throw IllegalArgumentException(ErrorConstants.BASE58_INVALID_CHECKSUM)
                    }

                    else -> {}
                }

            }
            catch (ex:Exception)
            {
                throw Base58ManipulationError(ErrorConstants.BASE58_DECODING_ERROR, ex)
            }

            return decodedKey
        }

        @Throws(FIOFormatterError::class)
        fun decompressPublicKey(compressedPublicKey:ByteArray, algorithmEmployed:AlgorithmEmployed): ByteArray
        {
            return this.decompressPublickey(compressedPublicKey,algorithmEmployed)
        }

        private fun invalidRipeMD160CheckSum(inputKey:ByteArray, checkSumToValidate:ByteArray, keyTypeByteArray:ByteArray): Boolean
        {
            if (inputKey.isEmpty() || checkSumToValidate.isEmpty())
            {
                throw IllegalArgumentException(ErrorConstants.BASE58_EMPTY_CHECKSUM_OR_KEY_OR_KEY_TYPE)
            }

            val keyWithType = Bytes.concat(inputKey, keyTypeByteArray)
            val digestRIPEMD160 = digestRIPEMD160(keyWithType)
            val checkSumFromInputKey = Arrays.copyOfRange(digestRIPEMD160, 0, CHECKSUM_BYTES)

            return !Arrays.equals(checkSumToValidate, checkSumFromInputKey)
        }

        private fun invalidSha256x2CheckSum(inputKey:ByteArray, checkSumToValidate:ByteArray): Boolean
        {
            if (inputKey.isEmpty()|| checkSumToValidate.isEmpty())
            {
                throw IllegalArgumentException(ErrorConstants.BASE58_EMPTY_CHECKSUM_OR_KEY)
            }

            val sha256x2 = Sha256Hash.hashTwice(inputKey)
            val checkSumFromInputKey = Arrays.copyOfRange(sha256x2, 0, CHECKSUM_BYTES)

            return !Arrays.equals(checkSumToValidate, checkSumFromInputKey)
        }

        private fun digestRIPEMD160(input:ByteArray): ByteArray
        {
            val digest = RIPEMD160Digest()
            val output = ByteArray(digest.digestSize)

            digest.update(input, 0, input.size)
            digest.doFinal(output, 0)

            return output
        }

        private fun extractCheckSumRIPEMD160(pemKey:ByteArray, keyTypeByteArray:ByteArray?): ByteArray
        {
            var localPemKey = pemKey
            if (keyTypeByteArray != null)
            {
                localPemKey = Bytes.concat(pemKey, keyTypeByteArray)
            }

            val ripemd160Digest = digestRIPEMD160(localPemKey)

            return Arrays.copyOfRange(ripemd160Digest, 0, CHECKSUM_BYTES)
        }

        private fun extractCheckSumSha256x2(pemKey:ByteArray): ByteArray
        {
            val sha256x2 = Sha256Hash.hashTwice(pemKey)

            return Arrays.copyOfRange(sha256x2, 0, CHECKSUM_BYTES)
        }

        @Throws(FIOFormatterError::class)
        private fun decompressPublickey(compressedPublicKey:ByteArray, algorithmEmployed:AlgorithmEmployed): ByteArray
        {
            try
            {
                val parameterSpec = ECNamedCurveTable.getParameterSpec(algorithmEmployed.name)
                val ecPoint = parameterSpec.curve.decodePoint(compressedPublicKey)
                val x = ecPoint.xCoord.encoded
                var y = ecPoint.yCoord.encoded

                if (y.size > STANDARD_KEY_LENGTH)
                {
                    y = Arrays.copyOfRange(y, 1, y.size)
                }

                return Bytes.concat(byteArrayOf(UNCOMPRESSED_PUBLIC_KEY_BYTE_INDICATOR), x, y)
            }
            catch (e:Exception)
            {
                throw FIOFormatterError(ErrorConstants.PUBLIC_KEY_DECOMPRESSION_ERROR, e)
            }

        }

        @Throws(FIOFormatterError::class)
        private fun compressPublickey(compressedPublicKey:ByteArray, algorithmEmployed:AlgorithmEmployed): ByteArray
        {
            val compressionPrefix:Byte
            try
            {
                val parameterSpec = ECNamedCurveTable.getParameterSpec(algorithmEmployed.name)
                val ecPoint = parameterSpec.curve.decodePoint(compressedPublicKey)
                val x = ecPoint.xCoord.encoded
                val y = ecPoint.yCoord.encoded

                val bigIntegerY = BigInteger(Hex.toHexString(y), 16)
                val bigIntegerTwo = BigInteger.valueOf(2)
                val remainder = bigIntegerY.mod(bigIntegerTwo)

                if (remainder == BigInteger.ZERO)
                {
                    compressionPrefix = COMPRESSED_PUBLIC_KEY_BYTE_INDICATOR_POSITIVE_Y
                }
                else
                {
                    compressionPrefix = COMPRESSED_PUBLIC_KEY_BYTE_INDICATOR_NEGATIVE_Y
                }

                return Bytes.concat(byteArrayOf(compressionPrefix), x)
            }
            catch (e:Exception)
            {
                throw FIOFormatterError(ErrorConstants.PUBLIC_KEY_COMPRESSION_ERROR, e)
            }
        }

        @Throws(LowSVerificationError::class)
        private fun checkAndHandleLowS(s:BigInteger, keyType:AlgorithmEmployed): BigInteger
        {
            if (!isLowS(s, keyType))
            {
                when (keyType)
                {
                    AlgorithmEmployed.SECP256R1 -> return CURVE_R1.n.subtract(s)

                    else -> return CURVE_K1.n.subtract(s)
                }
            }

            return s
        }

        @Throws(LowSVerificationError::class)
        private fun isLowS(s:BigInteger, keyType:AlgorithmEmployed): Boolean
        {
            val compareResult:Int

            when (keyType)
            {
                AlgorithmEmployed.SECP256R1 -> compareResult = s.compareTo(HALF_CURVE_ORDER_R1)

                AlgorithmEmployed.SECP256K1 -> compareResult = s.compareTo(HALF_CURVE_ORDER_K1)

                else -> throw LowSVerificationError(ErrorConstants.UNSUPPORTED_ALGORITHM)
            }

            return compareResult == 0 || compareResult == -1
        }

        private fun addCheckSumToSignature(signature:ByteArray, keyTypeByteArray:ByteArray): ByteArray
        {
            val signatureWithKeyType = Bytes.concat(signature, keyTypeByteArray)
            val signatureRipemd160 = digestRIPEMD160(signatureWithKeyType)
            val checkSum = Arrays.copyOfRange(signatureRipemd160, 0, CHECKSUM_BYTES)

            return Bytes.concat(signature, checkSum)
        }

        private fun isCanonical(signature: ByteArray): Boolean
        {
            return ((signature[1].toInt() and (0x80).toByte().toInt()) == (0x00).toByte().toInt()
                    && !((signature[1].toInt() == (0x00).toByte().toInt() && ((signature[2].toInt() and (0x80).toByte().toInt()) == (0x00).toByte().toInt())))
                    && (signature[33].toInt() and (0x80).toByte().toInt()) == (0x00).toByte().toInt()
                    && !((signature[33].toInt() == (0x00).toByte().toInt() && (((signature[34].toInt() and (0x80).toByte().toInt()) == (0x00).toByte().toInt())))))
        }

        private fun getRecoveryId(r:BigInteger, s:BigInteger, sha256HashMessage:Sha256Hash, publicKey:ByteArray, keyType:AlgorithmEmployed): Int
        {
            for (i in 0 until NUMBER_OF_POSSIBLE_PUBLIC_KEYS)
            {
                val recoveredPublicKey = recoverPublicKeyFromSignature(i, r, s, sha256HashMessage, true, keyType)

                if (Arrays.equals(publicKey, recoveredPublicKey))
                {
                    return i
                }
            }

            return -1
        }

        private fun recoverPublicKeyFromSignature(recId:Int, r:BigInteger, s:BigInteger, message:Sha256Hash, compressed:Boolean, keyType:AlgorithmEmployed): ByteArray?
        {
            checkArgument(recId >= 0, "recId must be positive")
            checkArgument(r.signum() >= 0, "r must be positive")
            checkArgument(s.signum() >= 0, "s must be positive")


            val n:BigInteger
            val g:ECPoint
            val curve:ECCurve.Fp

            when (keyType)
            {
                AlgorithmEmployed.SECP256R1 -> {
                    n = ecParamsR1.n
                    g = ecParamsR1.g
                    curve = ecParamsR1.curve as ECCurve.Fp
                }

                else -> {
                    n = ecParamsK1.n
                    g = ecParamsK1.g
                    curve = ecParamsK1.curve as ECCurve.Fp
                }
            }

            val i = BigInteger.valueOf(recId.toLong() / 2)
            val x = r.add(i.multiply(n))

            val prime = curve.q

            if (x.compareTo(prime) >= 0)
            {
                return null
            }

            val R = decompressKey(x, (recId and 1) == 1, keyType)
            if (!R.multiply(n).isInfinity)
            {
                return null
            }

            val e = message.toBigInteger()

            val eInv = BigInteger.ZERO.subtract(e).mod(n)
            val rInv = r.modInverse(n)
            val srInv = rInv.multiply(s).mod(n)
            val eInvrInv = rInv.multiply(eInv).mod(n)
            val q = ECAlgorithms.sumOfTwoMultiplies(g, eInvrInv, R, srInv)

            return q.getEncoded(compressed)
        }

        private fun decompressKey(xBN:BigInteger, yBit:Boolean, keyType:AlgorithmEmployed): ECPoint
        {
            val curve:ECCurve.Fp

            when (keyType)
            {
                AlgorithmEmployed.SECP256R1 -> curve = ecParamsR1.curve as ECCurve.Fp

                else -> curve = ecParamsK1.curve as ECCurve.Fp
            }

            val x9 = X9IntegerConverter()
            val compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(curve))

            compEnc[0] = (if (yBit)
                COMPRESSED_PUBLIC_KEY_BYTE_INDICATOR_NEGATIVE_Y
            else
                COMPRESSED_PUBLIC_KEY_BYTE_INDICATOR_POSITIVE_Y).toByte()

            return curve.decodePoint(compEnc)
        }
    }
}