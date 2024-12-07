package com.mrd.bitlib.model

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mrd.bitlib.util.TaprootUtils
import com.mrd.bitlib.util.cutStartByteArray
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class TaprootCommonSignatureMessageBuilderTest {
    lateinit var tx: BitcoinTransaction
    val auxRand =
        HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000")

    @Before
    fun setUp() {
        val rawUnsignedTx =
            HexUtils.toBytes("02000000097de20cbff686da83a54981d2b9bab3586f4ca7e48f57f5b55963115f3b334e9c010000000000000000d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd990000000000fffffffff8e1f583384333689228c5d28eac13366be082dc57441760d957275419a418420000000000fffffffff0689180aa63b30cb162a73c6d2a38b7eeda2a83ece74310fda0843ad604853b0100000000feffffffaa5202bdf6d8ccd2ee0f0202afbbb7461d9264a25e5bfd3c5a52ee1239e0ba6c0000000000feffffff956149bdc66faa968eb2be2d2faa29718acbfe3941215893a2a3446d32acd050000000000000000000e664b9773b88c09c32cb70a2a3e4da0ced63b7ba3b22f848531bbb1d5d5f4c94010000000000000000e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf0000000000ffffffffa778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af10100000000ffffffff0200ca9a3b000000001976a91406afd46bcdfd22ef94ac122aa11f241244a37ecc88ac807840cb0000000020ac9a87f5594be208f8532db38cff670c450ed2fea8fcdefcc9a663f78bab962b0065cd1d")
        tx = BitcoinTransaction.fromBytes(rawUnsignedTx)
    }

    @Test
    fun test1() {
        val privateKey =
            InMemoryPrivateKey(HexUtils.toBytes("6b973d88838f27366ed61c9ad6367663045cb456e28335c109e30717ae0c6baa"))
        val publicKey =
            HexUtils.toBytes("d6889cb081036e0faefa3a35157ad71086b123b2b144b649798b494c300a961d")

        Assert.assertEquals(
            HexUtils.toHex(publicKey),
            HexUtils.toHex(privateKey.publicKey.pubKeyCompressed.cutStartByteArray(32))
        )

        val hashAmounts = "58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde6"
        val hashOutputs = "a2e6dab7c1f0dcd297c8d61647fd17d821541ea69c3cc37dcbad7f90d4eb4bc5"
        val hashPrevouts = "e3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f"
        val hashScriptPubkeys = "23ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e21"
        val hashSequences = "18959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957e"


        val builder = TaprootCommonSignatureMessageBuilder(tx, 0, 2)
        setupAmount(builder)
        setupScriptPubKey(builder)
        builder.hashType = Script.SIGHASH_SINGLE
        // test sequence hash
        Assert.assertEquals(
            hashSequences,
            (sequenceHash().invoke(builder) as Sha256Hash).toHex()
        )

        // test prev output hash
        Assert.assertEquals(
            hashPrevouts,
            (prevOutputsHash().invoke(builder) as Sha256Hash).toHex()
        )

        // test output hash
        Assert.assertEquals(
            hashOutputs,
            (outputsHash().invoke(builder) as Sha256Hash).toHex()
        )

        Assert.assertEquals(
            hashAmounts,
            (inputAmountsHash().invoke(builder) as Sha256Hash).toHex()
        )

        Assert.assertEquals(
            hashScriptPubkeys,
            (scriptPubKeysHash().invoke(builder) as Sha256Hash).toHex()
        )

        val writer = ByteWriter(1024)
        builder.build(writer)
        val signMsg = writer.toBytes()

        Assert.assertEquals(
            "0003020000000065cd1de3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde623ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e2118959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957e0000000000d0418f0e9a36245b9a50ec87f8bf5be5bcae434337b87139c3a5b1f56e33cba0",
            HexUtils.toHex(HexUtils.toBytes("00") + signMsg)
        )

        val sigHash = TaprootUtils.sigHash(signMsg)
        Assert.assertEquals(
            "2514a6272f85cfa0f45eb907fcb0d121b808ed37c6ea160a5a9046ed5526d555",
            sigHash.toHex()
        )

//        Assert.assertEquals(
//            "ed7c1647cb97379e76892be0cacff57ec4a7102aa24296ca39af7541246d8ff14d38958d4cc1e2e478e4d4a764bbfd835b16d4e314b72937b29833060b87276c",
//            HexUtils.toHex(
//                privateKey.makeSchnorrBitcoinSignature(
//                    sigHash,
//                    ByteArray(0),
//                    HexUtils.toBytes("")
//                )
//            )
//        )

    }

    @Test
    fun testAmounts() {
        val hashAmounts = "58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde6"

        val builder = TaprootCommonSignatureMessageBuilder(tx, 0, 2)

        setupAmount(builder)

        Assert.assertEquals(
            hashAmounts,
            (inputAmountsHash().invoke(builder) as Sha256Hash).toHex()
        )
    }

    private fun setupAmount(builder: TaprootCommonSignatureMessageBuilder) {
        setValue(builder.inputs[0], 420000000)
        setValue(builder.inputs[1], 462000000)
        setValue(builder.inputs[2], 294000000)
        setValue(builder.inputs[3], 504000000)
        setValue(builder.inputs[4], 630000000)
        setValue(builder.inputs[5], 378000000)
        setValue(builder.inputs[6], 672000000)
        setValue(builder.inputs[7], 546000000)
        setValue(builder.inputs[8], 588000000)
    }

    private fun setupScriptPubKey(builder: TaprootCommonSignatureMessageBuilder) {
        builder.inputs[0].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("512053a1f6e454df1aa2776a2814a721372d6258050de330b3c6d10ee8f4e0dda343"))
        builder.inputs[1].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("5120147c9c57132f6e7ecddba9800bb0c4449251c92a1e60371ee77557b6620f3ea3"))
        builder.inputs[2].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("76a914751e76e8199196d454941c45d1b3a323f1433bd688ac"))
        builder.inputs[3].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("5120e4d810fd50586274face62b8a807eb9719cef49c04177cc6b76a9a4251d5450e"))
        builder.inputs[4].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("512091b64d5324723a985170e4dc5a0f84c041804f2cd12660fa5dec09fc21783605"))
        builder.inputs[5].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("00147dd65592d0ab2fe0d0257d571abf032cd9db93dc"))
        builder.inputs[6].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("512075169f4001aa68f15bbed28b218df1d0a62cbbcf1188c6665110c293c907b831"))
        builder.inputs[7].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("5120712447206d7a5238acc7ff53fbe94a3b64539ad291c7cdbc490b7577e4b17df5"))
        builder.inputs[8].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("512077e30a5522dd9f894c3f8b8bd4c4b2cf82ca7da8a3ea6a239655c39c050ab220"))
    }


    @Test
    fun testScriptPubkeys() {
        val hashScriptPubkeys = "23ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e21"

        val builder = TaprootCommonSignatureMessageBuilder(tx, 0, 2)
        setupAmount(builder)
        setupScriptPubKey(builder)

        Assert.assertEquals(
            hashScriptPubkeys,
            (scriptPubKeysHash().invoke(builder) as Sha256Hash).toHex()
        )
    }


    @Test
    fun test3() {
        val privateKey =
            InMemoryPrivateKey(HexUtils.toBytes("d3c7af07da2d54f7a7735d3d0fc4f0a73164db638b2f2f7c43f711f6d4aa7e64"))
        val merkleRoot =
            HexUtils.toBytes("c525714a7f49c28aedbbba78c005931a81c234b2f6c99a73e4d06082adc8bf2b")

        Assert.assertEquals(
            "93478e9488f956df2396be2ce6c5cced75f900dfa18e7dabd2428aae78451820",
            HexUtils.toHex(privateKey.publicKey.pubKeyCompressed.cutStartByteArray(32))
        )

        val builder = TaprootCommonSignatureMessageBuilder(tx, 3, 2)
        setupAmount(builder)
        setupScriptPubKey(builder)
        builder.hashType = Script.SIGHASH_ALL


        val writer = ByteWriter(1024)
        builder.build(writer)
        val signMsg = writer.toBytes()

        Assert.assertEquals(
            "0001020000000065cd1de3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde623ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e2118959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957ea2e6dab7c1f0dcd297c8d61647fd17d821541ea69c3cc37dcbad7f90d4eb4bc50003000000",
            HexUtils.toHex(HexUtils.toBytes("00") + signMsg)
        )

        val sigHash = TaprootUtils.sigHash(signMsg)
        Assert.assertEquals(
            "bf013ea93474aa67815b1b6cc441d23b64fa310911d991e713cd34c7f5d46669",
            sigHash.toHex()
        )
        Assert.assertEquals(
            "6af9e28dbf9d6aaf027696e2598a5b3d056f5fd2355a7fd5a37a0e5008132d30",
            TaprootUtils.tweak(privateKey.publicKey, merkleRoot).toHex()
        )
    }


    @Test
    fun test4() {
        val rawUnsignedTx =
            HexUtils.toBytes("02000000000101ec9016580d98a93909faf9d2f431e74f781b438d81372bb6aab4db67725c11a70000000000ffffffff0110270000000000001600144e44ca792ce545acba99d41304460dd1f53be3840000000000")
        val unsignedTransaction = BitcoinTransaction.fromBytes(rawUnsignedTx)

        val builder = TaprootCommonSignatureMessageBuilder(unsignedTransaction, 0, 2)

        setValue(builder.inputs[0], 20000)
        builder.inputs[0].script =
            ScriptInput.fromScriptBytes(HexUtils.toBytes("51200f0c8db753acbd17343a39c2f3f4e35e4be6da749f9e35137ab220e7b238a667"))

        val writer = ByteWriter(1024)
        builder.build(writer)
        val signMsg = writer.toBytes()

        Assert.assertEquals(
            "010200000000000000eaff979f4771d11a857e48550a28c4d3503cf2a966182c94010fd21d5b700700a" + "e9475d31b535bec000c9bfc7abc79b6a07db9eea2dd0e5066adddfb349bb53b4cd686f794463476c6fc24b4a43e0abc7b58a0ea78a998d2be39cdb73f8d9cc2" + "ad95131bc0b799c0b1af477fb14fcf26a6a9f76079e48bf090acb7e8367bfd0ec3a3f98ac2310126a614269e5715b0cabf38ce62232dd9ed8a878bdc0addea750000000000",
            HexUtils.toHex(signMsg),
        )

        val sighash = TaprootUtils.sigHash(signMsg)
        Assert.assertEquals(
            "a7b390196945d71549a2454f0185ece1b47c56873cf41789d78926852c355132",
            sighash.toHex(),
        )

        val privateKey =
            InMemoryPrivateKey(HexUtils.toBytes("55d7c5a9ce3d2b15a62434d01205f3e59077d51316f5c20628b3a4b8b2a76f4c"))


        Assert.assertEquals(
            "b693a0797b24bae12ed0516a2f5ba765618dca89b75e498ba5b745b71644362298a45ca39230d10a02ee6290a91cebf9839600f7e35158a447ea182ea0e022ae",
            HexUtils.toHex(
                privateKey.makeSchnorrBitcoinSignature(
                    sighash.bytes,
                    ByteArray(0),
                    auxRand
                )
            )
        )

    }


    private fun setValue(input: TransactionInput, value: Long) {
        val field = TransactionInput::class.java.getDeclaredField("value")
        field.isAccessible = true
        field.set(input, value)
    }

    private fun inputAmountsHash(): Method =
        TaprootCommonSignatureMessageBuilder::class.java.getDeclaredMethod(
            "inputAmountsHash"
        ).apply {
            isAccessible = true
        }

    private fun sequenceHash(): Method =
        TaprootCommonSignatureMessageBuilder::class.java.getDeclaredMethod(
            "sequenceHash"
        ).apply {
            isAccessible = true
        }

    private fun prevOutputsHash(): Method =
        TaprootCommonSignatureMessageBuilder::class.java.getDeclaredMethod(
            "prevOutputsHash"
        ).apply {
            isAccessible = true
        }

    private fun outputsHash(): Method =
        TaprootCommonSignatureMessageBuilder::class.java.getDeclaredMethod(
            "outputsHash"
        ).apply {
            isAccessible = true
        }

    private fun scriptPubKeysHash(): Method =
        TaprootCommonSignatureMessageBuilder::class.java.getDeclaredMethod(
            "scriptPubKeysHash"
        ).apply {
            isAccessible = true
        }
}