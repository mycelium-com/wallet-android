package fiofoundation.io.fiosdk.utilities

import fiofoundation.io.fiosdk.formatters.FIOFormatter
import fiofoundation.io.fiosdk.models.PEMProcessor
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash

import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation

object PrivateKeyUtils {
    private const val BIP44_PURPOSE = 44
    private const val BIP44_COIN_TYPE = 235
    private const val BIP44_ACCOUNT = 0
    private const val BIP44_CHANGE = 0
    private const val BIP44_INDEX = 0

    fun createPEMFormattedPrivateKey(mnemonic: String): String {

        val wordList: List<String> = mnemonic.split(" ")

        val seed = MnemonicCode.toSeed(wordList,"")

        val pathList:ArrayList<ChildNumber> = ArrayList()
        pathList.add(ChildNumber(BIP44_PURPOSE, true))
        pathList.add(ChildNumber(BIP44_COIN_TYPE, true))
        pathList.add(ChildNumber(BIP44_ACCOUNT, true))
        pathList.add(ChildNumber(BIP44_CHANGE, false))

        val masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val rootHierarchy = DeterministicHierarchy(masterPrivateKey)

        val fioKey = rootHierarchy.deriveChild(pathList, false, true, ChildNumber(BIP44_INDEX, false))

        val privateKeyBytes = fioKey.privKeyBytes

        val wifBytes = ByteArray(37)
        wifBytes[0] = 0x80.toByte()

        privateKeyBytes.copyInto(wifBytes,1,if (privateKeyBytes.size > 32) 1 else 0,32)

        val hash = Sha256Hash.hashTwice(wifBytes, 0, 33)
        hash.copyInto(wifBytes,33,0,4)

        return FIOFormatter.convertFIOPrivateKeyToPEMFormat(Base58.encode(wifBytes))
    }

    fun extractPEMFormattedPublicKey(pemPrivateKey: String): String
    {
        val pemProcessor = PEMProcessor(pemPrivateKey)

        return pemProcessor.extractPEMPublicKeyFromPrivateKey(true)
    }

    fun base58Decode(encodedString: String): ByteArray
    {
        return Base58.decode(encodedString)
    }

}