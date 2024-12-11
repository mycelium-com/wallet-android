package com.mrd.bitlib.model

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.signature.TaprootCommonSignatureMessageBuilder
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


    data class TestCase(
        val privateKey: InMemoryPrivateKey,
        val merkleRoot: String,
        val hashType: Int,
        val intermediary: Intermediary,
        val witness: String,
    )

    data class Intermediary(
        val internalPubkey: String,
        val tweak: String,
        val tweakedPrivkey: String,
        val sigMsg: String,
        val sigHash: String,
    )

    val testVectors = mapOf(
        0 to TestCase(
            InMemoryPrivateKey(HexUtils.toBytes("6b973d88838f27366ed61c9ad6367663045cb456e28335c109e30717ae0c6baa")),
            merkleRoot = "",
            hashType = 3,
            Intermediary(
                internalPubkey = "d6889cb081036e0faefa3a35157ad71086b123b2b144b649798b494c300a961d",
                tweak = "b86e7be8f39bab32a6f2c0443abbc210f0edac0e2c53d501b36b64437d9c6c70",
                tweakedPrivkey = "2405b971772ad26915c8dcdf10f238753a9b837e5f8e6a86fd7c0cce5b7296d9",
                sigMsg = "0003020000000065cd1de3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde623ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e2118959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957e0000000000d0418f0e9a36245b9a50ec87f8bf5be5bcae434337b87139c3a5b1f56e33cba0",
                sigHash = "2514a6272f85cfa0f45eb907fcb0d121b808ed37c6ea160a5a9046ed5526d555",
            ),
            witness = "ed7c1647cb97379e76892be0cacff57ec4a7102aa24296ca39af7541246d8ff14d38958d4cc1e2e478e4d4a764bbfd835b16d4e314b72937b29833060b87276c03"
        ),
        1 to TestCase(
            InMemoryPrivateKey(HexUtils.toBytes("1e4da49f6aaf4e5cd175fe08a32bb5cb4863d963921255f33d3bc31e1343907f")),
            merkleRoot = "5b75adecf53548f3ec6ad7d78383bf84cc57b55a3127c72b9a2481752dd88b21",
            hashType = 0x83,
            Intermediary(
                internalPubkey = "187791b6f712a8ea41c8ecdd0ee77fab3e85263b37e1ec18a3651926b3a6cf27",
                tweak = "cbd8679ba636c1110ea247542cfbd964131a6be84f873f7f3b62a777528ed001",
                tweakedPrivkey = "ea260c3b10e60f6de018455cd0278f2f5b7e454be1999572789e6a9565d26080",
                sigMsg = "0083020000000065cd1d00d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd9900000000808f891b00000000225120147c9c57132f6e7ecddba9800bb0c4449251c92a1e60371ee77557b6620f3ea3ffffffffffcef8fb4ca7efc5433f591ecfc57391811ce1e186a3793024def5c884cba51d",
                sigHash = "325a644af47e8a5a2591cda0ab0723978537318f10e6a63d4eed783b96a71a4d",
            ),
            witness = "052aedffc554b41f52b521071793a6b88d6dbca9dba94cf34c83696de0c1ec35ca9c5ed4ab28059bd606a4f3a657eec0bb96661d42921b5f50a95ad33675b54f83"
        ),
        3 to TestCase(
            InMemoryPrivateKey(HexUtils.toBytes("d3c7af07da2d54f7a7735d3d0fc4f0a73164db638b2f2f7c43f711f6d4aa7e64")),
            merkleRoot = "c525714a7f49c28aedbbba78c005931a81c234b2f6c99a73e4d06082adc8bf2b",
            hashType = 1,
            Intermediary(
                internalPubkey = "93478e9488f956df2396be2ce6c5cced75f900dfa18e7dabd2428aae78451820",
                tweak = "6af9e28dbf9d6aaf027696e2598a5b3d056f5fd2355a7fd5a37a0e5008132d30",
                tweakedPrivkey = "97323385e57015b75b0339a549c56a948eb961555973f0951f555ae6039ef00d",
                sigMsg = "0001020000000065cd1de3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde623ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e2118959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957ea2e6dab7c1f0dcd297c8d61647fd17d821541ea69c3cc37dcbad7f90d4eb4bc50003000000",
                sigHash = "bf013ea93474aa67815b1b6cc441d23b64fa310911d991e713cd34c7f5d46669",
            ),
            witness = "ff45f742a876139946a149ab4d9185574b98dc919d2eb6754f8abaa59d18b025637a3aa043b91817739554f4ed2026cf8022dbd83e351ce1fabc272841d2510a01"
        ),
        4 to TestCase(
            InMemoryPrivateKey(HexUtils.toBytes("f36bb07a11e469ce941d16b63b11b9b9120a84d9d87cff2c84a8d4affb438f4e")),
            merkleRoot = "ccbd66c6f7e8fdab47b3a486f59d28262be857f30d4773f2d5ea47f7761ce0e2",
            hashType = 0,
            Intermediary(
                internalPubkey = "e0dfe2300b0dd746a3f8674dfd4525623639042569d829c7f0eed9602d263e6f",
                tweak = "b57bfa183d28eeb6ad688ddaabb265b4a41fbf68e5fed2c72c74de70d5a786f4",
                tweakedPrivkey = "a8e7aa924f0d58854185a490e6c41f6efb7b675c0f3331b7f14b549400b4d501",
                sigMsg = "0000020000000065cd1de3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde623ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e2118959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957ea2e6dab7c1f0dcd297c8d61647fd17d821541ea69c3cc37dcbad7f90d4eb4bc50004000000",
                sigHash = "4f900a0bae3f1446fd48490c2958b5a023228f01661cda3496a11da502a7f7ef",
            ),
            witness = "b4010dd48a617db09926f729e79c33ae0b4e94b79f04a1ae93ede6315eb3669de185a17d2b0ac9ee09fd4c64b678a0b61a0a86fa888a273c8511be83bfd6810f"
        ),
        6 to TestCase(
            InMemoryPrivateKey(HexUtils.toBytes("415cfe9c15d9cea27d8104d5517c06e9de48e2f986b695e4f5ffebf230e725d8")),
            merkleRoot = "2f6b2c5397b6d68ca18e09a3f05161668ffe93a988582d55c6f07bd5b3329def",
            hashType = 2,
            Intermediary(
                internalPubkey = "55adf4e8967fbd2e29f20ac896e60c3b0f1d5b0efa9d34941b5958c7b0a0312d",
                tweak = "6579138e7976dc13b6a92f7bfd5a2fc7684f5ea42419d43368301470f3b74ed9",
                tweakedPrivkey = "241c14f2639d0d7139282aa6abde28dd8a067baa9d633e4e7230287ec2d02901",
                sigMsg = "0002020000000065cd1de3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde623ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e2118959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957e0006000000",
                sigHash = "15f25c298eb5cdc7eb1d638dd2d45c97c4c59dcaec6679cfc16ad84f30876b85"
            ),
            witness = "a3785919a2ce3c4ce26f298c3d51619bc474ae24014bcdd31328cd8cfbab2eff3395fa0a16fe5f486d12f22a9cedded5ae74feb4bbe5351346508c5405bcfee002"
        ),
        7 to TestCase(
            InMemoryPrivateKey(HexUtils.toBytes("c7b0e81f0a9a0b0499e112279d718cca98e79a12e2f137c72ae5b213aad0d103")),
            merkleRoot = "6c2dc106ab816b73f9d07e3cd1ef2c8c1256f519748e0813e4edd2405d277bef",
            hashType = 130,
            Intermediary(
                internalPubkey = "ee4fe085983462a184015d1f782d6a5f8b9c2b60130aff050ce221ecf3786592",
                tweak = "9e0517edc8259bb3359255400b23ca9507f2a91cd1e4250ba068b4eafceba4a9",
                tweakedPrivkey = "65b6000cd2bfa6b7cf736767a8955760e62b6649058cbc970b7c0871d786346b",
                sigMsg = "0082020000000065cd1d00e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf00000000804c8b2000000000225120712447206d7a5238acc7ff53fbe94a3b64539ad291c7cdbc490b7577e4b17df5ffffffff",
                sigHash = "cd292de50313804dabe4685e83f923d2969577191a3e1d2882220dca88cbeb10",
            ),
            witness = "ea0c6ba90763c2d3a296ad82ba45881abb4f426b3f87af162dd24d5109edc1cdd11915095ba47c3a9963dc1e6c432939872bc49212fe34c632cd3ab9fed429c482"
        ),
        8 to TestCase(
            InMemoryPrivateKey(HexUtils.toBytes("77863416be0d0665e517e1c375fd6f75839544eca553675ef7fdf4949518ebaa")),
            merkleRoot = "ab179431c28d3b68fb798957faf5497d69c883c6fb1e1cd9f81483d87bac90cc",
            hashType = 129,
            Intermediary(
                internalPubkey = "f9f400803e683727b14f463836e1e78e1c64417638aa066919291a225f0e8dd8",
                tweak = "639f0281b7ac49e742cd25b7f188657626da1ad169209078e2761cefd91fd65e",
                tweakedPrivkey = "ec18ce6af99f43815db543f47b8af5ff5df3b2cb7315c955aa4a86e8143d2bf5",
                sigMsg = "0081020000000065cd1da2e6dab7c1f0dcd297c8d61647fd17d821541ea69c3cc37dcbad7f90d4eb4bc500a778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af101000000002b0c230000000022512077e30a5522dd9f894c3f8b8bd4c4b2cf82ca7da8a3ea6a239655c39c050ab220ffffffff",
                sigHash = "cccb739eca6c13a8a89e6e5cd317ffe55669bbda23f2fd37b0f18755e008edd2",
            ),
            witness = "bbc9584a11074e83bc8c6759ec55401f0ae7b03ef290c3139814f545b58a9f8127258000874f44bc46db7646322107d4d86aec8e73b8719a61fff761d75b5dd981"
        ),
    )

    private val inputData = listOf(
        TransactionOutput(
            420000000,
            ScriptOutput.fromScriptBytes("512053a1f6e454df1aa2776a2814a721372d6258050de330b3c6d10ee8f4e0dda343".toBytes())
        ),
        TransactionOutput(
            462000000,
            ScriptOutput.fromScriptBytes("5120147c9c57132f6e7ecddba9800bb0c4449251c92a1e60371ee77557b6620f3ea3".toBytes())
        ),
        TransactionOutput(
            294000000,
            ScriptOutput.fromScriptBytes("76a914751e76e8199196d454941c45d1b3a323f1433bd688ac".toBytes())
        ),
        TransactionOutput(
            504000000,
            ScriptOutput.fromScriptBytes("5120e4d810fd50586274face62b8a807eb9719cef49c04177cc6b76a9a4251d5450e".toBytes())
        ),
        TransactionOutput(
            630000000,
            ScriptOutput.fromScriptBytes("512091b64d5324723a985170e4dc5a0f84c041804f2cd12660fa5dec09fc21783605".toBytes())
        ),
        TransactionOutput(
            378000000,
            ScriptOutput.fromScriptBytes("00147dd65592d0ab2fe0d0257d571abf032cd9db93dc".toBytes())
        ),
        TransactionOutput(
            672000000,
            ScriptOutput.fromScriptBytes("512075169f4001aa68f15bbed28b218df1d0a62cbbcf1188c6665110c293c907b831".toBytes())
        ),
        TransactionOutput(
            546000000,
            ScriptOutput.fromScriptBytes("5120712447206d7a5238acc7ff53fbe94a3b64539ad291c7cdbc490b7577e4b17df5".toBytes())
        ),
        TransactionOutput(
            588000000,
            ScriptOutput.fromScriptBytes("512077e30a5522dd9f894c3f8b8bd4c4b2cf82ca7da8a3ea6a239655c39c050ab220".toBytes())
        )
    )

    lateinit var utxos: Array<UnspentTransactionOutput>

    @Before
    fun setUp() {
        val rawUnsignedTx =
            HexUtils.toBytes("02000000097de20cbff686da83a54981d2b9bab3586f4ca7e48f57f5b55963115f3b334e9c010000000000000000d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd990000000000fffffffff8e1f583384333689228c5d28eac13366be082dc57441760d957275419a418420000000000fffffffff0689180aa63b30cb162a73c6d2a38b7eeda2a83ece74310fda0843ad604853b0100000000feffffffaa5202bdf6d8ccd2ee0f0202afbbb7461d9264a25e5bfd3c5a52ee1239e0ba6c0000000000feffffff956149bdc66faa968eb2be2d2faa29718acbfe3941215893a2a3446d32acd050000000000000000000e664b9773b88c09c32cb70a2a3e4da0ced63b7ba3b22f848531bbb1d5d5f4c94010000000000000000e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf0000000000ffffffffa778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af10100000000ffffffff0200ca9a3b000000001976a91406afd46bcdfd22ef94ac122aa11f241244a37ecc88ac807840cb0000000020ac9a87f5594be208f8532db38cff670c450ed2fea8fcdefcc9a663f78bab962b0065cd1d")
        tx = BitcoinTransaction.fromBytes(rawUnsignedTx)
        tx.inputs.forEachIndexed { i, ti ->
            setValue(ti, inputData[i].value)
            ti.script = ScriptInput.fromScriptBytes(inputData[i].script.scriptBytes)
        }
        utxos = tx.inputs.mapIndexed { i, input ->
            UnspentTransactionOutput(input.outPoint, -1, inputData[i].value, inputData[i].script)
        }.toTypedArray()
    }

    @Test
    fun test() {
        testVectors.forEach { i, vector ->
            Assert.assertEquals(
                "check publicKey $i",
                vector.intermediary.internalPubkey,
                HexUtils.toHex(vector.privateKey.publicKey.pubKeyCompressed.cutStartByteArray(32))
            )

            val tweak = TaprootUtils.tweak(vector.privateKey.publicKey, vector.merkleRoot.toBytes())
            Assert.assertEquals(
                "check tweak $i",
                vector.intermediary.tweak,
                tweak.toHex()
            )

            val tweakedPrivkey =
                TaprootUtils.tweakedPrivateKey(vector.privateKey.privateKeyBytes, tweak)
            Assert.assertEquals(
                "check tweakedPrivKey $i",
                vector.intermediary.tweakedPrivkey,
                tweakedPrivkey.toHex()
            )

            val builder = TaprootCommonSignatureMessageBuilder(tx, utxos, i, 2)
            builder.hashType = vector.hashType

            val writer = ByteWriter(1024)
            builder.build(writer)
            val signMsg = writer.toBytes()
            Assert.assertEquals(
                "test signMsg $i",
                vector.intermediary.sigMsg,
                HexUtils.toHex(HexUtils.toBytes("00") + signMsg)
            )

            val sigHash = TaprootUtils.sigHash(signMsg)
            Assert.assertEquals(
                vector.intermediary.sigHash,
                sigHash.toHex()
            )
            val witness = vector.privateKey.makeSchnorrBitcoinSignature(
                sigHash.bytes,
                vector.merkleRoot.toBytes(),
                auxRand
            ) + if (vector.hashType != 0) byteArrayOf(vector.hashType.toByte()) else ByteArray(0)
            Assert.assertEquals(
                "test witness $i",
                vector.witness,
                witness.toHex()
            )
            tx.inputs[i].witness = InputWitness(1).apply {
                setStack(0, witness)
            }
        }
//        Assert.assertEquals(
//            "test signed tx",
//            "020000000001097de20cbff686da83a54981d2b9bab3586f4ca7e48f57f5b55963115f3b334e9c010000000000000000d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd990000000000fffffffff8e1f583384333689228c5d28eac13366be082dc57441760d957275419a41842000000006b4830450221008f3b8f8f0537c420654d2283673a761b7ee2ea3c130753103e08ce79201cf32a022079e7ab904a1980ef1c5890b648c8783f4d10103dd62f740d13daa79e298d50c201210279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798fffffffff0689180aa63b30cb162a73c6d2a38b7eeda2a83ece74310fda0843ad604853b0100000000feffffffaa5202bdf6d8ccd2ee0f0202afbbb7461d9264a25e5bfd3c5a52ee1239e0ba6c0000000000feffffff956149bdc66faa968eb2be2d2faa29718acbfe3941215893a2a3446d32acd050000000000000000000e664b9773b88c09c32cb70a2a3e4da0ced63b7ba3b22f848531bbb1d5d5f4c94010000000000000000e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf0000000000ffffffffa778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af10100000000ffffffff0200ca9a3b000000001976a91406afd46bcdfd22ef94ac122aa11f241244a37ecc88ac807840cb0000000020ac9a87f5594be208f8532db38cff670c450ed2fea8fcdefcc9a663f78bab962b0141ed7c1647cb97379e76892be0cacff57ec4a7102aa24296ca39af7541246d8ff14d38958d4cc1e2e478e4d4a764bbfd835b16d4e314b72937b29833060b87276c030141052aedffc554b41f52b521071793a6b88d6dbca9dba94cf34c83696de0c1ec35ca9c5ed4ab28059bd606a4f3a657eec0bb96661d42921b5f50a95ad33675b54f83000141ff45f742a876139946a149ab4d9185574b98dc919d2eb6754f8abaa59d18b025637a3aa043b91817739554f4ed2026cf8022dbd83e351ce1fabc272841d2510a010140b4010dd48a617db09926f729e79c33ae0b4e94b79f04a1ae93ede6315eb3669de185a17d2b0ac9ee09fd4c64b678a0b61a0a86fa888a273c8511be83bfd6810f0247304402202b795e4de72646d76eab3f0ab27dfa30b810e856ff3a46c9a702df53bb0d8cc302203ccc4d822edab5f35caddb10af1be93583526ccfbade4b4ead350781e2f8adcd012102f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f90141a3785919a2ce3c4ce26f298c3d51619bc474ae24014bcdd31328cd8cfbab2eff3395fa0a16fe5f486d12f22a9cedded5ae74feb4bbe5351346508c5405bcfee0020141ea0c6ba90763c2d3a296ad82ba45881abb4f426b3f87af162dd24d5109edc1cdd11915095ba47c3a9963dc1e6c432939872bc49212fe34c632cd3ab9fed429c4820141bbc9584a11074e83bc8c6759ec55401f0ae7b03ef290c3139814f545b58a9f8127258000874f44bc46db7646322107d4d86aec8e73b8719a61fff761d75b5dd9810065cd1d",
//            tx.toBytes(true).toHex()
//        )
//        val stb = StandardTransactionBuilder(NetworkParameters.productionNetwork)
//        tx.inputs.forEach {
//            stb.addOutput(TransactionOutput())
//        }
//        tx.outputs.forEach {
//            stb.addOutput(it)
//        }

    }
    //02000000 00 01 09 7de20cbff686da83a54981d2b9bab3586f4ca7e48f57f5b55963115f3b334e9c 01000000 00 00000000 d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd99 00000000 00 ffffffff f8e1f583384333689228c5d28eac13366be082dc57441760d957275419a41842 00000000 6b 4830450221008f3b8f8f0537c420654d2283673a761b7ee2ea3c130753103e08ce79201cf32a022079e7ab904a1980ef1c5890b648c8783f4d10103dd62f740d13daa79e298d50c201210279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798 ffffffff f0689180aa63b30cb162a73c6d2a38b7eeda2a83ece74310fda0843ad604853b 01000000 00 feffffff aa5202bdf6d8ccd2ee0f0202afbbb7461d9264a25e5bfd3c5a52ee1239e0ba6c 00000000 00 feffffff 956149bdc66faa968eb2be2d2faa29718acbfe3941215893a2a3446d32acd050 00000000 00 00000000e664b9773b88c09c32cb70a2a3e4da0ced63b7ba3b22f848531bbb1d5d5f4c94010000000000000000                                             e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf 0000000000 ffffffff a778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af1 01000000 00 ffffffff 0200ca9a3b000000001976a91406afd46bcdfd22ef94ac122aa11f241244a37e cc88ac807840cb0000000020ac9a87f5594be208f8532db38cff670c450ed2fea8fcdefcc9a663f78bab962b0141ed7c1647cb97379e76892be0cacff57ec4a7102aa24296ca39af7541246d8ff14d38958d4cc1e2e478e4d4a764bbfd835b16d4e314b72937b29833060b87276c030141052aedffc554b41f52b521071793a6b88d6dbca9dba94cf34c83696de0c1ec35ca9c5ed4ab28059bd606a4f3a657eec0bb96661d42921b5f50a95ad33675b54f83000141ff45f742a876139946a149ab4d9185574b98dc919d2eb6754f8abaa59d18b025637a3aa043b91817739554f4ed2026cf8022dbd83e351ce1fabc272841d2510a010140b4010dd48a617db09926f729e79c33ae0b4e94b79f04a1ae93ede6315eb3669de185a17d2b0ac9ee09fd4c64b678a0b61a0a86fa888a273c8511be83bfd6810f0247304402202b795e4de72646d76eab3f0ab27dfa30b810e856ff3a46c9a702df53bb0d8cc302203ccc4d822edab5f35caddb10af1be93583526ccfbade4b4ead350781e2f8adcd012102f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f90141a3785919a2ce3c4ce26f298c3d51619bc474ae24014bcdd31328cd8cfbab2eff3395fa0a16fe5f486d12f22a9cedded5ae74feb4bbe5351346508c5405bcfee0020141ea0c6ba90763c2d3a296ad82ba45881abb4f426b3f87af162dd24d5109edc1cdd11915095ba47c3a9963dc1e6c432939872bc49212fe34c632cd3ab9fed429c4820141bbc9584a11074e83bc8c6759ec55401f0ae7b03ef290c3139814f545b58a9f8127258000874f44bc46db7646322107d4d86aec8e73b8719a61fff761d75b5dd9810065cd1d
    //02000000 00 01 09 7de20cbff686da83a54981d2b9bab3586f4ca7e48f57f5b55963115f3b334e9c 01000000 00 00000000 d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd99 00000000 00 ffffffff f8e1f583384333689228c5d28eac13366be082dc57441760d957275419a41842 00000000 1976a914751e76e8199196d454941c45d1b3a323f1433bd688ac                                                                                                                                                                      ffffffff f0689180aa63b30cb162a73c6d2a38b7eeda2a83ece74310fda0843ad604853b 01000000 00 feffffff aa5202bdf6d8ccd2ee0f0202afbbb7461d9264a25e5bfd3c5a52ee1239e0ba6c 00000000 00 feffffff 956149bdc66faa968eb2be2d2faa29718acbfe3941215893a2a3446d32acd050 00000000 16 00147dd65592d0ab2fe0d0257d571abf032cd9db93dc00000000e664b9773b88c09c32cb70a2a3e4da0ced63b7ba3b22f848531bbb1d5d5f4c94010000000000000000 e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf 0000000000 ffffffff a778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af1 01000000 00 ffffffff 0200ca9a3b000000001976a91406afd46bcdfd22ef94ac122aa11f241244a37e cc88ac807840cb0000000020ac9a87f5594be208f8532db38cff670c450ed2fea8fcdefcc9a663f78bab962b0141ed7c1647cb97379e76892be0cacff57ec4a7102aa24296ca39af7541246d8ff14d38958d4cc1e2e478e4d4a764bbfd835b16d4e314b72937b29833060b87276c030141052aedffc554b41f52b521071793a6b88d6dbca9dba94cf34c83696de0c1ec35ca9c5ed4ab28059bd606a4f3a657eec0bb96661d42921b5f50a95ad33675b54f83000141ff45f742a876139946a149ab4d9185574b98dc919d2eb6754f8abaa59d18b025637a3aa043b91817739554f4ed2026cf8022dbd83e351ce1fabc272841d2510a010140b4010dd48a617db09926f729e79c33ae0b4e94b79f04a1ae93ede6315eb3669de185a17d2b0ac9ee09fd4c64b678a0b61a0a86fa888a273c8511be83bfd6810f000141a3785919a2ce3c4ce26f298c3d51619bc474ae24014bcdd31328cd8cfbab2eff3395fa0a16fe5f486d12f22a9cedded5ae74feb4bbe5351346508c5405bcfee0020141ea0c6ba90763c2d3a296ad82ba45881abb4f426b3f87af162dd24d5109edc1cdd11915095ba47c3a9963dc1e6c432939872bc49212fe34c632cd3ab9fed429c4820141bbc9584a11074e83bc8c6759ec55401f0ae7b03ef290c3139814f545b58a9f8127258000874f44bc46db7646322107d4d86aec8e73b8719a61fff761d75b5dd9810065cd1d

    fun ByteArray.toHex(): String = HexUtils.toHex(this)
    fun String.toBytes(): ByteArray = HexUtils.toBytes(this)

    @Test
    fun test0() {
        val privateKey = testVectors[0]!!.privateKey
        val merkleRoot = testVectors[0]!!.merkleRoot.toBytes()


        Assert.assertEquals(
            "d6889cb081036e0faefa3a35157ad71086b123b2b144b649798b494c300a961d",
            HexUtils.toHex(privateKey.publicKey.pubKeyCompressed.cutStartByteArray(32))
        )

        val tweak = TaprootUtils.tweak(privateKey.publicKey, merkleRoot)
        Assert.assertEquals(
            "b86e7be8f39bab32a6f2c0443abbc210f0edac0e2c53d501b36b64437d9c6c70",
            tweak.toHex()
        )

        Assert.assertEquals(
            "2405b971772ad26915c8dcdf10f238753a9b837e5f8e6a86fd7c0cce5b7296d9",
            HexUtils.toHex(
                TaprootUtils.tweakedPrivateKey(
                    HexUtils.toBytes("6b973d88838f27366ed61c9ad6367663045cb456e28335c109e30717ae0c6baa"),
                    tweak
                )
            )
        )

        Assert.assertEquals(
            "2405b971772ad26915c8dcdf10f238753a9b837e5f8e6a86fd7c0cce5b7296d9",
            HexUtils.toHex(TaprootUtils.tweakedPrivateKey(privateKey.privateKeyBytes, tweak))
        )


        val builder = TaprootCommonSignatureMessageBuilder(tx, utxos, 0, 2)
        builder.hashType = Script.SIGHASH_SINGLE
        Assert.assertEquals(
            "test sequence hash",
            "18959c7221ab5ce9e26c3cd67b22c24f8baa54bac281d8e6b05e400e6c3a957e",
            (sequenceHash().invoke(builder) as Sha256Hash).toHex()
        )

        Assert.assertEquals(
            "test prev output hash",
            "e3b33bb4ef3a52ad1fffb555c0d82828eb22737036eaeb02a235d82b909c4c3f",
            (prevOutputsHash().invoke(builder) as Sha256Hash).toHex()
        )

        Assert.assertEquals(
            "test output hash",
            "a2e6dab7c1f0dcd297c8d61647fd17d821541ea69c3cc37dcbad7f90d4eb4bc5",
            (outputsHash().invoke(builder) as Sha256Hash).toHex()
        )

        Assert.assertEquals(
            "test inputAmountsHash",
            "58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde6",
            (inputAmountsHash().invoke(builder) as Sha256Hash).toHex()
        )

        Assert.assertEquals(
            "test scriptPubKeysHash",
            "23ad0f61ad2bca5ba6a7693f50fce988e17c3780bf2b1e720cfbb38fbdd52e21",
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

        Assert.assertEquals(
            "ed7c1647cb97379e76892be0cacff57ec4a7102aa24296ca39af7541246d8ff14d38958d4cc1e2e478e4d4a764bbfd835b16d4e314b72937b29833060b87276c",
            HexUtils.toHex(
                privateKey.makeSchnorrBitcoinSignature(sigHash.bytes, ByteArray(0), auxRand)
            )
        )
    }

    @Test
    fun testAmounts() {
        val builder = TaprootCommonSignatureMessageBuilder(tx, utxos, 0, 2)
        Assert.assertEquals(
            "58a6964a4f5f8f0b642ded0a8a553be7622a719da71d1f5befcefcdee8e0fde6",
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

        val builder = TaprootCommonSignatureMessageBuilder(tx, utxos, 0, 2)

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

        Assert.assertEquals(
            "6af9e28dbf9d6aaf027696e2598a5b3d056f5fd2355a7fd5a37a0e5008132d30",
            TaprootUtils.tweak(privateKey.publicKey, merkleRoot).toHex()
        )


        val builder = TaprootCommonSignatureMessageBuilder(tx, utxos, 3, 2)
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

        val builder = TaprootCommonSignatureMessageBuilder(unsignedTransaction, utxos, 0, 2)

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