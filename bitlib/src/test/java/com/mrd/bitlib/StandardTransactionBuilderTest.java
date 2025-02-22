/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mrd.bitlib;

import com.google.common.collect.ImmutableList;
import com.mrd.bitlib.crypto.BitcoinSigner;
import com.mrd.bitlib.crypto.IPrivateKeyRing;
import com.mrd.bitlib.crypto.IPublicKeyRing;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.ScriptOutputP2PKH;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.megiontechnologies.Bitcoins.SATOSHIS_PER_BITCOIN;
import static com.mrd.bitlib.TransactionUtils.MINIMUM_OUTPUT_VALUE;
import static com.mrd.bitlib.model.NetworkParameters.testNetwork;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StandardTransactionBuilderTest {
    private static NetworkParameters network = testNetwork;
    private StandardTransactionBuilder testme;

    private static final int COUNT = 9;
    private static final InMemoryPrivateKey[] PRIVATE_KEYS = new InMemoryPrivateKey[COUNT];
    private static final PublicKey PUBLIC_KEYS[] = new PublicKey[COUNT];
    private static final BitcoinAddress ADDRS[] = new BitcoinAddress[COUNT];
    private static final UnspentTransactionOutput UTXOS[][] = new UnspentTransactionOutput[COUNT][2];

    static {
        for (int i = 0; i < COUNT; i++) {
            PRIVATE_KEYS[i] = getPrivKey("1" + i);
            PUBLIC_KEYS[i] = PRIVATE_KEYS[i].getPublicKey();
            // their addresses and 2 UTXOs each,
            ADDRS[i] = PUBLIC_KEYS[i].toAddress(network, AddressType.P2PKH);
            // with values 1/3, 3/5, 7/9 and 15/17.
            UTXOS[i][0] = getUtxo(ADDRS[i], (long) Math.pow(2, 1 + i) - 1 + MINIMUM_OUTPUT_VALUE);
            UTXOS[i][1] = getUtxo(ADDRS[i], (long) Math.pow(2, 1 + i) + 1 + MINIMUM_OUTPUT_VALUE);
        }
    }

    private static final IPrivateKeyRing PRIVATE_KEY_RING = new IPrivateKeyRing() {
        @Override
        public BitcoinSigner findSignerByPublicKey(PublicKey publicKey) {
            int i = Arrays.asList(PUBLIC_KEYS).lastIndexOf(publicKey);
            if (i >= 0) {
                return PRIVATE_KEYS[i];
            }
            return null;
        }
    };

    private static final IPublicKeyRing KEY_RING = new IPublicKeyRing() {
        @Override
        public PublicKey findPublicKeyByAddress(BitcoinAddress address) {
            for (int i = 0; i < COUNT; i++) {
                if (ADDRS[i].equals(address)) {
                    return PUBLIC_KEYS[i];
                }
            }
            return null;
        }
    };

    @Before
    public void setUp() throws Exception {
        testme = new StandardTransactionBuilder(network);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyList() throws Exception {
        testRichest(ImmutableList.<UnspentTransactionOutput>of(), ADDRS[0]);
    }

    @Test
    public void testSingleList() throws Exception {
        for (int i = 0; i < COUNT; i++) {
            System.out.println(ADDRS[i].toString());
            testRichest(ImmutableList.of(UTXOS[i][0]), ADDRS[i]);
        }
    }

    @Test
    public void testList() throws Exception {
        for (int i = 1; i < COUNT; i++) {
            List<UnspentTransactionOutput> utxos = new ArrayList<>(2 * i + 1);
            for (int j = 0; j < i; j++) {
                utxos.add(UTXOS[j][0]);
                utxos.add(UTXOS[j][1]);
            }
            utxos.add(UTXOS[i][0]);
            testRichest(utxos, ADDRS[i - 1]);
            Collections.reverse(utxos);
            testRichest(utxos, ADDRS[i - 1]);
            utxos.add(UTXOS[i][1]);
            testRichest(utxos, ADDRS[i]);
            Collections.reverse(utxos);
            testRichest(utxos, ADDRS[i]);
        }
    }

    private void testRichest(Collection<UnspentTransactionOutput> utxos, BitcoinAddress winner) {
        BitcoinAddress address = testme.getRichest(utxos, network);
        assertEquals(winner, address);
    }

    @Test
    public void testCreateUnsignedTransactionWithoutChange() throws Exception {
        int feeExpected = (int) new FeeEstimatorBuilder().setLegacyInputs(1)
                .setLegacyOutputs(1)
                .setMinerFeePerKb(200000)
                .createFeeEstimator()
                .estimateFee();

        System.out.println(feeExpected);//38400
        long utxoAvailable = 2 * SATOSHIS_PER_BITCOIN + feeExpected + MINIMUM_OUTPUT_VALUE - 10;
        // UTXOs worth utxoAvailable satoshis, should result in 1 in 1 out.
        // MINIMUM_OUTPUT_VALUE - 10 satoshis will be
        // left out because it is inferior of the MINIMUM_OUTPUT_VALUE.
        Collection<UnspentTransactionOutput> inventory = ImmutableList.of(
                getUtxo(ADDRS[0], utxoAvailable)
        );
        testme.addOutput(ADDRS[2], 2 * SATOSHIS_PER_BITCOIN);
        UnsignedTransaction tx = testme.createUnsignedTransaction(inventory, ADDRS[3], KEY_RING,
                network, 200000);
        UnspentTransactionOutput[] inputs = tx.getFundingOutputs();
        assertEquals(1, inputs.length);
        assertEquals(utxoAvailable, inputs[0].value);

        TransactionOutput[] outputs = tx.getOutputs();
        assertEquals(1, outputs.length);
        assertTrue(tx.calculateFee() < feeExpected + MINIMUM_OUTPUT_VALUE);
        assertTrue(tx.calculateFee() > feeExpected);
        assertEquals(ADDRS[2], outputs[0].script.getAddress(network));
    }

    @Test
    public void testCreateUnsignedTransactionWithChange() throws Exception {
        // UTXOs worth 10BTC, spending 1BTC should result in 1 in 2 out, spending 1 and 9-fee
        Collection<UnspentTransactionOutput> inventory = ImmutableList.of(
                getUtxo(ADDRS[0], 10 * SATOSHIS_PER_BITCOIN)
        );
        testme.addOutput(ADDRS[1], SATOSHIS_PER_BITCOIN);
        int feeExpected = (int) new FeeEstimatorBuilder().setLegacyInputs(1)
                .setLegacyOutputs(2)
                .setMinerFeePerKb(200000)
                .createFeeEstimator()
                .estimateFee();

        UnsignedTransaction tx = testme.createUnsignedTransaction(inventory, ADDRS[2], KEY_RING,
                network, 200000); // miner fees to use = 200 satoshis per bytes.
        UnspentTransactionOutput[] inputs = tx.getFundingOutputs();
        assertEquals(1, inputs.length);
        TransactionOutput[] outputs = tx.getOutputs();
        assertEquals(2, outputs.length);
        assertEquals(feeExpected, tx.calculateFee());
        assertEquals(10 * SATOSHIS_PER_BITCOIN, outputs[0].value + outputs[1].value + tx.calculateFee());
    }

    @Test
    public void testCreateUnsignedTransactionMinToFee() throws Exception {
        // UTXOs worth 2MIN + 1 + 3, spending MIN should result in just one output
        Collection<UnspentTransactionOutput> inventory = ImmutableList.of(
                UTXOS[0][0], UTXOS[0][1]
        );
        testme.addOutput(ADDRS[1], MINIMUM_OUTPUT_VALUE);
        UnsignedTransaction tx = testme.createUnsignedTransaction(inventory, ADDRS[2], KEY_RING, network, 1000);
        UnspentTransactionOutput[] inputs = tx.getFundingOutputs();
        assertEquals(2, inputs.length);
        TransactionOutput[] outputs = tx.getOutputs();
        assertEquals(1, outputs.length);
        assertEquals(ADDRS[1], outputs[0].script.getAddress(network));
    }


    private static UnspentTransactionOutput getUtxo(BitcoinAddress address, long value) {
        return new UnspentTransactionOutput(new OutPoint(Sha256Hash.ZERO_HASH, 0), 0, value, new ScriptOutputP2PKH(address.getTypeSpecificBytes()));
    }

    /**
     * Helper to get defined public keys
     *
     * @param s one byte hex values as string representation. "00" - "ff"
     */
    private static InMemoryPrivateKey getPrivKey(String s) {
        return new InMemoryPrivateKey(HexUtils.toBytes(s + "00000000000000000000000000000000000000000000000000000000000000"), true);
    }

    // timing out after 50 * 10 ms. 50 is the signature count, to average a bit,
    // 10ms is what it may take at max in the test per sig.
    @Test(timeout = 500)
    @Ignore("This is not really a requirement but was meant to show the supperior performance of bitcoinJ")
    public void generateSignaturesBitlib() throws Exception {
        // bitlib is slow to sign. 6ms per signature. figure out how to replace that with bitcoinJ and whether that is faster.
        List<SigningRequest> requests = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            requests.add(new SigningRequest(PUBLIC_KEYS[i % COUNT], HashUtils.sha256(("bla" + i).getBytes())));
        }
        StandardTransactionBuilder.generateSignatures(requests.toArray(new SigningRequest[]{}), PRIVATE_KEY_RING);
    }
}
