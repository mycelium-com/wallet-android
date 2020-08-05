package fiofoundation.io.fiosdk.models

import fiofoundation.io.fiosdk.enums.AlgorithmEmployed
import fiofoundation.io.fiosdk.errors.ErrorConstants
import fiofoundation.io.fiosdk.errors.Base58ManipulationError
import fiofoundation.io.fiosdk.errors.formatters.FIOFormatterError
import fiofoundation.io.fiosdk.formatters.FIOFormatter
import fiofoundation.io.fiosdk.errors.PEMProcessorError
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.sec.SECObjectIdentifiers
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.FixedPointCombMultiplier
import org.bouncycastle.math.ec.FixedPointUtil
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader

import java.io.CharArrayReader
import java.io.IOException
import java.io.Reader
import java.lang.Exception
import java.math.BigInteger

class PEMProcessor(var pemObjectString: String) {
    private var pemObject: PemObject? = null

    init{
        FixedPointUtil.precompute(CURVE_PARAMS_R1.g)

        CharArrayReader(pemObjectString.toCharArray()).use { reader ->
            PemReader(reader).use { pemReader ->
                this.pemObject = pemReader.readPemObject()
            }
        }

        if(this.pemObject == null) {
            throw PEMProcessorError(ErrorConstants.INVALID_PEM_OBJECT)
        }
    }

    companion object {

        private val PRIVATE_KEY_TYPE = "EC PRIVATE KEY"

        private val PRIVATE_KEY_START_INDEX = 2

        private val SECP256_R1 = "secp256r1"

        private val SECP256_K1 = "secp256k1"

        private val CURVE_PARAMS_R1:X9ECParameters = CustomNamedCurves.getByName(SECP256_R1)

        private val CURVE_PARAMS_K1:X9ECParameters = CustomNamedCurves.getByName(SECP256_K1)

        private val BIG_INTEGER_POSITIVE = 1

        private val CURVE_R1:ECDomainParameters = ECDomainParameters(CURVE_PARAMS_R1.curve, CURVE_PARAMS_R1.g, CURVE_PARAMS_R1.n, CURVE_PARAMS_R1.h)

        private val CURVE_K1:ECDomainParameters = ECDomainParameters(CURVE_PARAMS_K1.curve, CURVE_PARAMS_K1.g, CURVE_PARAMS_K1.n, CURVE_PARAMS_K1.h)

        @Throws(PEMProcessorError::class)
        fun getCurveDomainParameters(curve: AlgorithmEmployed): ECDomainParameters
        {
            when (curve)
            {
                AlgorithmEmployed.SECP256R1->{return CURVE_R1}
                AlgorithmEmployed.PRIME256V1->{return CURVE_R1}

                AlgorithmEmployed.SECP256K1->{return CURVE_K1}

                else->{
                    throw PEMProcessorError(ErrorConstants.UNSUPPORTED_ALGORITHM)
                }
            }
        }
    }




    fun getType(): String {
        return pemObject!!.type
    }

    fun getDERFormat(): String {
        return Hex.toHexString(pemObject!!.content)
    }

    @Throws(PEMProcessorError::class)
    fun getAlgorithm(): AlgorithmEmployed  {

        var pemObjectParsed: Any? = parsePEMObject()

        var oid: String
        if (pemObjectParsed is SubjectPublicKeyInfo) {

            oid = pemObjectParsed.algorithm.parameters.toString()

        } else if (pemObjectParsed is PEMKeyPair) {

            oid = pemObjectParsed.privateKeyInfo.privateKeyAlgorithm.parameters.toString()
        } else {
            throw PEMProcessorError(ErrorConstants.DER_TO_PEM_CONVERSION)
        }

        if (SECObjectIdentifiers.secp256r1.id == oid) {
            return AlgorithmEmployed.SECP256R1
        } else if (SECObjectIdentifiers.secp256k1.id == oid) {
            return AlgorithmEmployed.SECP256K1
        } else {
            throw PEMProcessorError(ErrorConstants.UNSUPPORTED_ALGORITHM + oid)
        }
    }

    @Throws(PEMProcessorError::class)
    fun getKeyData(): ByteArray {

        var pemObjectParsed: Any? = parsePEMObject()

        if (pemObjectParsed is SubjectPublicKeyInfo) {
            return pemObjectParsed.publicKeyData.bytes
        } else if (pemObjectParsed is PEMKeyPair) {
            var sequence: DLSequence? = null
            try
            {
                ASN1InputStream(Hex.decode(this.getDERFormat())).use { asn1InputStream ->
                    sequence = asn1InputStream.readObject() as DLSequence
                }
            }
            catch (e:IOException) {
                throw PEMProcessorError(e)
            }
            for (obj: Any in sequence!!) {
                if (obj is DEROctetString) {
                    var key: ByteArray
                    try
                    {
                        key = obj.encoded
                    } catch (e: IOException) {
                        throw PEMProcessorError(e)
                    }
                    return Arrays.copyOfRange(key, PRIVATE_KEY_START_INDEX, key.size)
                }
            }
            throw PEMProcessorError(ErrorConstants.KEY_DATA_NOT_FOUND)

        }
        else
        {
            throw PEMProcessorError(ErrorConstants.DER_TO_PEM_CONVERSION)
        }
    }

    @Throws(PEMProcessorError::class)
    fun extractFIOPublicKeyFromPrivateKey(isLegacy: Boolean): String {
        if (this.getType() != PRIVATE_KEY_TYPE) {
            throw PEMProcessorError(ErrorConstants.PUBLIC_KEY_COULD_NOT_BE_EXTRACTED_FROM_PRIVATE_KEY);
        }

        var keyCurve: AlgorithmEmployed = this.getAlgorithm()
        var privateKeyBI = BigInteger(BIG_INTEGER_POSITIVE, this.getKeyData())
        var n: BigInteger
        var g:ECPoint

        when (keyCurve)
        {
            AlgorithmEmployed.SECP256R1->{
                n = CURVE_R1.n
                g = CURVE_R1.g
            }
            else->{
                n = CURVE_K1.n
                g = CURVE_K1.g
            }

        }

        if (privateKeyBI.bitLength() > n.bitLength()) {
            privateKeyBI = privateKeyBI.mod(n)
        }

        var publicKeyByteArray: ByteArray = FixedPointCombMultiplier().multiply(g, privateKeyBI).getEncoded(true)

        try {
            return FIOFormatter.encodePublicKey(publicKeyByteArray, keyCurve, isLegacy)
        } catch (e:Base58ManipulationError) {
            throw PEMProcessorError(e)
        }
    }

    @Throws(PEMProcessorError::class)
    fun extractPEMPublicKeyFromPrivateKey(isLegacy: Boolean): String {
        try {
            return FIOFormatter.convertFIOPublicKeyToPEMFormat(extractFIOPublicKeyFromPrivateKey(isLegacy))
        } catch (e: FIOFormatterError) {
            throw PEMProcessorError(e)
        }
    }

    @Throws(PEMProcessorError::class)
    private fun parsePEMObject(): Any {

        try {
            var l_pemObject: Any? = null
            CharArrayReader(pemObjectString.toCharArray()).use { reader ->
                PEMParser(reader).use { pemParser ->

                    l_pemObject = pemParser.readObject()
                }
            }

            return l_pemObject!!
        }
        catch(e: Exception)
        {
            throw PEMProcessorError(ErrorConstants.ERROR_READING_PEM_OBJECT, e)
        }
    }
}