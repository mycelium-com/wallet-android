/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrd.bitlib.model;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.primitives.UnsignedInteger;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.TransactionInput.TransactionInputParsingException;
import com.mrd.bitlib.model.TransactionOutput.TransactionOutputParsingException;
import com.mrd.bitlib.model.signature.TaprootCommonSignatureMessageBuilder;
import com.mrd.bitlib.model.signature.WitnessSignatureMessageBuilder;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Transaction represents a raw Bitcoin transaction. In other words, it contains only the information found in the
 * byte string representing a Bitcoin transaction. It contains no contextual information, such as the height
 * of the transaction in the block chain or the outputs that its inputs redeem.
 * <p>
 * Implements Serializable and is inserted directly in and out of the database. Therefore it cannot be changed
 * without messing with the database.
 */
public class BitcoinTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final long ONE_uBTC_IN_SATOSHIS = 100;
    private static final long ONE_mBTC_IN_SATOSHIS = 1000 * ONE_uBTC_IN_SATOSHIS;
    public static final long MAX_MINER_FEE_PER_KB = 200L * ONE_mBTC_IN_SATOSHIS; // 20000sat/B

    public int version;
    public final TransactionInput[] inputs;
    public final TransactionOutput[] outputs;
    public int lockTime;

    private Sha256Hash _hash;
    private Sha256Hash id;
//    private Sha256Hash _unmalleableHash;

    // cache for some getters that need to do some work and might get called often
    private transient Boolean _rbfAble = null;
    private transient int _txSize = -1;

    public static BitcoinTransaction fromUnsignedTransaction(UnsignedTransaction unsignedTransaction) {
        UnspentTransactionOutput[] fundingOutputs = unsignedTransaction.getFundingOutputs();
        TransactionInput[] inputs = new TransactionInput[fundingOutputs.length];
        for (int idx = 0; idx < fundingOutputs.length; idx++) {
            UnspentTransactionOutput u = fundingOutputs[idx];
            ScriptInput script;
            if (unsignedTransaction.isSegWitOutput(idx)) {
                byte[] segWitScriptBytes = unsignedTransaction.getInputs()[idx].script.getScriptBytes();
                try {
                    script = ScriptInput.fromScriptBytes(segWitScriptBytes);
                } catch (Script.ScriptParsingException e) {
                    //Should never happen
                    throw new Error("Parsing segWitScriptBytes failed");
                }
            } else {
                script = new ScriptInput(u.script.getScriptBytes());
            }
            inputs[idx] = new TransactionInput(u.outPoint, script, unsignedTransaction.getDefaultSequenceNumber(), u.value);
        }
        return new BitcoinTransaction(1, inputs, unsignedTransaction.getOutputs(), unsignedTransaction.getLockTime());
    }

    public static BitcoinTransaction fromBytes(byte[] transaction) throws TransactionParsingException {
        return fromByteReader(new ByteReader(transaction));
    }

    public static BitcoinTransaction fromByteReader(ByteReader reader) throws TransactionParsingException {
        return fromByteReader(reader, null);
    }

    // use this builder if you already know the resulting transaction hash to speed up computation
    public static BitcoinTransaction fromByteReader(ByteReader reader, Sha256Hash knownTransactionHash)
            throws TransactionParsingException {
        int size = reader.available();
        try {
            int version = reader.getIntLE();
            boolean useSegwit = false;
            byte marker = peekByte(reader);
            if (marker == 0) {
                //segwit possible
                reader.get();
                byte flag = peekByte(reader);
                if (flag == 1) {
                    //it's segwit
                    reader.get();
                    useSegwit = true;
                } else {
                    throw new TransactionParsingException("Unable to parse segwit transaction. Flag must be 0x01");
                }
            }

            TransactionInput[] inputs = parseTransactionInputs(reader);
            TransactionOutput[] outputs = parseTransactionOutputs(reader);

            if (useSegwit) {
                parseWitness(reader, inputs);
            }

            int lockTime = reader.getIntLE();
            return new BitcoinTransaction(version, inputs, outputs, lockTime, size, knownTransactionHash);
        } catch (InsufficientBytesException e) {
            throw new TransactionParsingException(e.getMessage());
        }
    }

    private static void parseWitness(ByteReader reader, TransactionInput[] inputs) throws InsufficientBytesException {
        for (TransactionInput input : inputs) {
            long stackSize = reader.getCompactInt();
            InputWitness witness = new InputWitness((int) stackSize);
            input.setWitness(witness);
            for (int y = 0; y < stackSize; y++) {
                long pushSize = reader.getCompactInt();
                byte[] push = reader.getBytes((int) pushSize);
                witness.setStack(y, push);
            }
        }
    }

    private static TransactionOutput[] parseTransactionOutputs(ByteReader reader) throws InsufficientBytesException, TransactionParsingException {
        int numOutputs = (int) reader.getCompactInt();
        TransactionOutput[] outputs = new TransactionOutput[numOutputs];
        for (int i = 0; i < numOutputs; i++) {
            try {
                outputs[i] = TransactionOutput.fromByteReader(reader);
            } catch (TransactionOutputParsingException e) {
                throw new TransactionParsingException("Unable to parse transaction output at index " + i + ": "
                        + e.getMessage());
            }
        }
        return outputs;
    }

    private static TransactionInput[] parseTransactionInputs(ByteReader reader) throws InsufficientBytesException, TransactionParsingException {
        int numInputs = (int) reader.getCompactInt();
        TransactionInput[] inputs = new TransactionInput[numInputs];
        for (int i = 0; i < numInputs; i++) {
            try {
                inputs[i] = TransactionInput.fromByteReader(reader);
            } catch (TransactionInputParsingException e) {
                throw new TransactionParsingException("Unable to parse transaction input at index " + i + ": "
                        + e.getMessage(), e);
            } catch (IllegalStateException e) {
                throw new TransactionParsingException("ISE - Unable to parse transaction input at index " + i + ": "
                        + e.getMessage(), e);
            }
        }
        return inputs;
    }

    private static byte peekByte(ByteReader reader) throws InsufficientBytesException {
        byte b = reader.get();
        reader.setPosition(reader.getPosition() - 1);
        return b;
    }

    public BitcoinTransaction copy() {
        try {
            return BitcoinTransaction.fromByteReader(new ByteReader(toBytes()));
        } catch (TransactionParsingException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    public byte[] toBytes() {
        return toBytes(true);
    }

    public byte[] toBytes(boolean asSegwit) {
        ByteWriter writer = new ByteWriter(1024);
        toByteWriter(writer, asSegwit);
        return writer.toBytes();
    }

    public int getTxRawSize() {
        if (_txSize == -1) {
            _txSize = toBytes().length;
        }
        return _txSize;
    }

    /**
     * This method serializes transaction according to <a href="https://github.com/bitcoin/bips/blob/master/bip-0144.mediawiki">BIP144</a>
     */
    public void toByteWriter(ByteWriter writer) {
        toByteWriter(writer, true);
    }

    /**
     * Same as {@link #toByteWriter(ByteWriter)}, but allows to enforce SegWit tx serialization to classic format.
     *
     * @param asSegwit if true tx would be serialized according bip144 standard.
     */
    public void toByteWriter(ByteWriter writer, boolean asSegwit) {
        System.out.println("!!!! toByteWriter" + super.hashCode());
        writer.putIntLE(version);
        boolean isSegwit = isSegwit();
        boolean isSegWitMode = asSegwit && (isSegwit);
        int segwitPart = 0;
        if (isSegWitMode) {
            segwitPart -= writer.length();
            writer.putCompactInt(0); //marker
            writer.putCompactInt(1); //flag
            segwitPart += writer.length();
        }
        writeInputs(writer);
        writeOutputs(writer);
        if (isSegWitMode) {
            segwitPart -= writer.length();
            writeWitness(writer);
            segwitPart += writer.length();
        }
        writer.putIntLE(lockTime);
        vSizeTotal = writer.length();
        if (asSegwit) {
            if (isSegwit) {
                vSizeBase = vSizeTotal - segwitPart;
            } else {
                vSizeBase = writer.length();
            }
        }
    }

    // cash size calculation for speed up working with txs
    private int vSizeBase = 0;
    private int vSizeTotal = 0;

    public int vsize() {
        if (vSizeBase == 0) {
            toBytes(true);
        }
        // vsize calculations are from https://github.com/bitcoin/bips/blob/master/bip-0141.mediawiki#transaction-size-calculations
        // ... + 3 ) / 4 deals with the int cast rounding down but us needing to round up.
        return (vSizeBase * 3 + vSizeTotal + 3) / 4;
    }

    private void writeWitness(ByteWriter writer) {
        for (TransactionInput input : inputs) {
            input.getWitness().toByteWriter(writer);
        }
    }

    private void writeInputs(ByteWriter writer) {
        writer.putCompactInt(inputs.length);
        for (TransactionInput input : inputs) {
            input.toByteWriter(writer);
        }
    }

    private void writeOutputs(ByteWriter writer) {
        writer.putCompactInt(outputs.length);
        for (TransactionOutput output : outputs) {
            output.toByteWriter(writer);
        }
    }

    public BitcoinTransaction(int version, TransactionInput[] inputs, TransactionOutput[] outputs, int lockTime) {
        this(version, inputs, outputs, lockTime, -1);
    }

    private BitcoinTransaction(int version, TransactionInput[] inputs, TransactionOutput[] outputs, int lockTime, int txSize) {
        this.version = version;
        this.inputs = inputs;
        this.outputs = outputs;
        this.lockTime = lockTime;
        this._txSize = txSize;
    }

    public BitcoinTransaction(BitcoinTransaction copyFrom) {
        this.version = copyFrom.version;
        this.inputs = copyFrom.inputs;
        this.outputs = copyFrom.outputs;
        this.lockTime = copyFrom.lockTime;
        this._txSize = copyFrom._txSize;
        this._hash = copyFrom._hash;
        this.id = copyFrom.id;
//        this._unmalleableHash = copyFrom._unmalleableHash;
    }

    // we already know the hash of this transaction, dont recompute it
    protected BitcoinTransaction(int version, TransactionInput[] inputs, TransactionOutput[] outputs, int lockTime,
                                 int txSize, Sha256Hash knownTransactionHash) {
        this(version, inputs, outputs, lockTime, txSize);
        this._hash = knownTransactionHash;
    }

    public Sha256Hash getId() {
        if (id == null) {
            ByteWriter writer = new ByteWriter(2000);
            toByteWriter(writer, false);
            id = HashUtils.doubleSha256(writer.toBytes()).reverse();
        }
        return id;
    }

    public Sha256Hash getHash() {
        if (_hash == null) {
            ByteWriter writer = new ByteWriter(2000);
            toByteWriter(writer);
            _hash = HashUtils.doubleSha256(writer.toBytes()).reverse();
        }
        return _hash;
    }

    public Sha256Hash getTxDigestHash(int i) {
        ByteWriter writer = new ByteWriter(1024);
        if (inputs[i].script instanceof ScriptInputP2WSH
                || inputs[i].script instanceof ScriptInputP2WPKH) {
            new WitnessSignatureMessageBuilder(this, i, version).build(writer);
        } else if (inputs[i].script instanceof ScriptInputP2TR) {
            throw new RuntimeException("T2TR use commonSignatureMessage");
        } else {
            toByteWriter(writer, false);
        }
        // We also have to write a hash type.
        int hashType = 1;
        writer.putIntLE(hashType);
        // Note that this is NOT reversed to ensure it will be signed
        // correctly. If it were to be printed out
        // however then we would expect that it is IS reversed.
        return HashUtils.doubleSha256(writer.toBytes());
    }

    public byte[] commonSignatureMessage(int i) {
        if (inputs[i].script instanceof ScriptInputP2TR) {
            ByteWriter writer = new ByteWriter(1024);
            new TaprootCommonSignatureMessageBuilder(this, i, version).build(writer);
            return writer.toBytes();
        } else {
            throw new RuntimeException("commonSignatureMessage only for P2TR, vin " + i);
        }
    }

    private Sha256Hash getPrevOutsHash() {
        ByteWriter writer = new ByteWriter(1024);
        for (TransactionInput input : inputs) {
            input.outPoint.hashPrev(writer);
        }
        return HashUtils.doubleSha256(writer.toBytes());
    }

    private Sha256Hash getOutputsHash() {
        ByteWriter writer = new ByteWriter(1024);
        for (TransactionOutput output : outputs) {
            writer.putLongLE(output.value);
            writer.put((byte) (output.script.getScriptBytes().length & 0xFF));
            writer.putBytes(output.script.getScriptBytes());
        }
        return HashUtils.doubleSha256(writer.toBytes());
    }

    private Sha256Hash getSequenceHash()  {
        ByteWriter writer = new ByteWriter(1024);
        for (TransactionInput input : inputs) {
            writer.putIntLE(input.sequence);
        }
        return HashUtils.doubleSha256(writer.toBytes());
    }

    /**
     * Returns the minimum nSequence number of all inputs
     * Can be used to detect transactions marked for Full-RBF and thus are very low trust while having 0 conf
     * Transactions with minSequenceNumber < MAX_INT-1 are eligible for full RBF
     * https://github.com/bitcoin/bitcoin/pull/6871#event-476297575
     *
     * @return the min nSequence of all inputs of that transaction
     */
    public UnsignedInteger getMinSequenceNumber() {
        UnsignedInteger minVal = UnsignedInteger.MAX_VALUE;
        for (TransactionInput input : inputs) {
            UnsignedInteger nSequence = UnsignedInteger.fromIntBits(input.sequence);
            if (nSequence.compareTo(minVal) < 0) {
                minVal = nSequence;
            }
        }
        return minVal;
    }

    /**
     * Returns true if this transaction is marked for RBF and thus can easily get replaced by a
     * conflicting transaction while it is still unconfirmed.
     *
     * @return true if any of its inputs has a nSequence < MAX_INT-1
     */
    public boolean isRbfAble() {
        if (_rbfAble == null) {
            _rbfAble = (getMinSequenceNumber().compareTo(UnsignedInteger.MAX_VALUE.minus(UnsignedInteger.ONE)) < 0);
        }
        return _rbfAble;
    }



    /**
     * @return true if transaction is SegWit, else false
     */
    public boolean isSegwit() {
        return Iterables.any(Arrays.asList(inputs), new Predicate<TransactionInput>() {
            @Override
            public boolean apply(@Nullable TransactionInput transactionInput) {
                return transactionInput != null && transactionInput.hasWitness();
            }
        });
    }

    /**
     * Calculate the unmalleable hash of this transaction. If the signature bytes
     * for an input cannot be determined the result is null
     */
//    public Sha256Hash getUnmalleableHash() {
//        if (_unmalleableHash == null) {
//            ByteWriter writer = new ByteWriter(2000);
//            for (TransactionInput i : inputs) {
//                byte[] bytes = i.getUnmalleableBytes();
//                if (bytes == null) {
//                    return null;
//                }
//                writer.putBytes(bytes);
//            }
//            _unmalleableHash = HashUtils.doubleSha256(writer.toBytes()).reverse();
//        }
//        return _unmalleableHash;
//    }

    @Override
    public String toString() {
        return String.valueOf(getId()) + " in: " + inputs.length + " out: " + outputs.length;
    }

    @Override
    public int hashCode() {
        return getHash().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof BitcoinTransaction)) {
            return false;
        }
        return getHash().equals(((BitcoinTransaction) other).getHash());
    }

    public boolean isCoinbase() {
        for (TransactionInput in : inputs) {
            if (in.script instanceof ScriptInputCoinbase) {
                return true;
            }
        }
        return false;
    }

    public static class TransactionParsingException extends Exception {
        private static final long serialVersionUID = 1L;

        public TransactionParsingException(String message) {
            super(message);
        }

        public TransactionParsingException(String message, Exception e) {
            super(message, e);
        }
    }
}
