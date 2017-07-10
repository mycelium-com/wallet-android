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

package com.mrd.bitlib;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.mrd.bitlib.crypto.BitcoinSigner;
import com.mrd.bitlib.crypto.IPrivateKeyRing;
import com.mrd.bitlib.crypto.IPublicKeyRing;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.util.*;

import static com.mrd.bitlib.TransactionUtils.MINIMUM_OUTPUT_VALUE;

public class StandardTransactionBuilder {
   // hash size 32 + output index size 4 + script length 1 + max. script size for compressed keys 107 + sequence number 4
   // also see https://github.com/bitcoin/bitcoin/blob/master/src/primitives/transaction.h#L190
   public static final int MAX_INPUT_SIZE = 32 + 4 + 1 + 107 + 4;
   // output value 8B + script length 1B + script 25B (always)
   private static final int OUTPUT_SIZE = 8 + 1 + 25;

   private NetworkParameters _network;
   private List<TransactionOutput> _outputs;

   public static class InsufficientFundsException extends Exception {
      //todo consider refactoring this into a composite return value instead of an exception. it is not really "exceptional"
      private static final long serialVersionUID = 1L;

      public long sending;
      public long fee;

      public InsufficientFundsException(long sending, long fee) {
         super("Insufficient funds to send " + sending + " satoshis with fee " + fee);
         this.sending = sending;
         this.fee = fee;
      }
   }

   public static class OutputTooSmallException extends Exception {
      //todo consider refactoring this into a composite return value instead of an exception. it is not really "exceptional"
      private static final long serialVersionUID = 1L;

      public long value;

      public OutputTooSmallException(long value) {
         super("An output was added with a value of " + value
             + " satoshis, which is smaller than the minimum accepted by the Bitcoin network");
      }
   }

   public static class UnableToBuildTransactionException extends Exception {
      private static final long serialVersionUID = 1L;

      public UnableToBuildTransactionException(String msg) {
         super(msg);
      }
   }

   public static class SigningRequest implements Serializable {
      private static final long serialVersionUID = 1L;

      // The public part of the key we will sign with
      public PublicKey publicKey;

      // The data to make a signature on. For transactions this is the
      // transaction hash
      public Sha256Hash toSign;

      public SigningRequest(PublicKey publicKey, Sha256Hash toSign) {
         this.publicKey = publicKey;
         this.toSign = toSign;
      }
   }

   public static class UnsignedTransaction implements Serializable {
      private static final long serialVersionUID = 1L;
      public static final int NO_SEQUENCE = -1;

      private TransactionOutput[] _outputs;
      private UnspentTransactionOutput[] _funding;
      private SigningRequest[] _signingRequests;
      private NetworkParameters _network;

      public TransactionOutput[] getOutputs() {
         return _outputs;
      }

      public UnspentTransactionOutput[] getFundingOutputs() {
         return _funding;
      }

      public UnsignedTransaction(List<TransactionOutput> outputs, List<UnspentTransactionOutput> funding,
                                 IPublicKeyRing keyRing, NetworkParameters network) {
         _network = network;
         _outputs = outputs.toArray(new TransactionOutput[outputs.size()]);
         _funding = funding.toArray(new UnspentTransactionOutput[funding.size()]);
         _signingRequests = new SigningRequest[_funding.length];

         // Create empty input scripts pointing at the right out points
         TransactionInput[] inputs = new TransactionInput[_funding.length];
         for (int i = 0; i < _funding.length; i++) {
            inputs[i] = new TransactionInput(_funding[i].outPoint, ScriptInput.EMPTY, getDefaultSequenceNumber());
         }

         // Create transaction with valid outputs and empty inputs
         Transaction transaction = new Transaction(1, inputs, _outputs, getLockTime());

         for (int i = 0; i < _funding.length; i++) {
            UnspentTransactionOutput f = _funding[i];

            // Make sure that we only work on standard output scripts
            if (!(f.script instanceof ScriptOutputStandard)) {
               throw new RuntimeException("Unsupported script");
            }
            // Find the address of the funding
            byte[] addressBytes = ((ScriptOutputStandard) f.script).getAddressBytes();
            Address address = Address.fromStandardBytes(addressBytes, _network);

            // Find the key to sign with
            PublicKey publicKey = keyRing.findPublicKeyByAddress(address);
            if (publicKey == null) {
               // This should not happen as we only work on outputs that we have
               // keys for
               throw new RuntimeException("Public key not found");
            }

            // Set the input script to the funding output script
            inputs[i].script = ScriptInput.fromOutputScript(_funding[i].script);

            // Calculate the transaction hash that has to be signed
            Sha256Hash hash = hashTransaction(transaction);

            // Set the input to the empty script again
            inputs[i] = new TransactionInput(_funding[i].outPoint, ScriptInput.EMPTY);

            _signingRequests[i] = new SigningRequest(publicKey, hash);
         }
      }

      public SigningRequest[] getSignatureInfo() {
         return _signingRequests;
      }

      /**
       * @return fee in satoshis
       */
      public long calculateFee() {
         long in = 0, out = 0;
         for (UnspentTransactionOutput funding : _funding) {
            in += funding.value;
         }
         for (TransactionOutput output : _outputs) {
            out += output.value;
         }
         return in - out;
      }

      public int getLockTime() {
         return 0;
      }

      public int getDefaultSequenceNumber() {
         return NO_SEQUENCE;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         String fee = CoinUtil.valueString(calculateFee(), false);
         sb.append(String.format("Fee: %s", fee)).append('\n');
         int max = Math.max(_funding.length, _outputs.length);
         for (int i = 0; i < max; i++) {
            UnspentTransactionOutput in = i < _funding.length ? _funding[i] : null;
            TransactionOutput out = i < _outputs.length ? _outputs[i] : null;
            String line;
            if (in != null && out != null) {
               line = String.format("%36s %13s -> %36s %13s", getAddress(in.script, _network), getValue(in.value),
                   getAddress(out.script, _network), getValue(out.value));
            } else if (in != null) {
               line = String.format("%36s %13s    %36s %13s", getAddress(in.script, _network), getValue(in.value), "",
                   "");
            } else if (out != null) {
               line = String.format("%36s %13s    %36s %13s", "", "", getAddress(out.script, _network),
                   getValue(out.value));
            } else {
               line = "";
            }
            sb.append(line).append('\n');
         }
         return sb.toString();
      }

      private String getAddress(ScriptOutput script, NetworkParameters network) {
         Address address = script.getAddress(network);
         if (address == null) {
            return "Unknown";
         }
         return address.toString();
      }

      private String getValue(Long value) {
         return String.format("(%s)", CoinUtil.valueString(value, false));
      }

   }

   public StandardTransactionBuilder(NetworkParameters network) {
      _network = network;
      _outputs = new LinkedList<>();
   }

   public void addOutput(Address sendTo, long value) throws OutputTooSmallException {
      addOutput(createOutput(sendTo, value, _network));
   }

   public void addOutput(TransactionOutput output) throws OutputTooSmallException {
      if (output.value < MINIMUM_OUTPUT_VALUE) {
         throw new OutputTooSmallException(output.value);
      }
      _outputs.add(output);
   }

   public void addOutputs(OutputList outputs) throws OutputTooSmallException {
      for (TransactionOutput output : outputs) {
         if (output.value > 0) {
            addOutput(output);
         }
      }
   }

   public static TransactionOutput createOutput(Address sendTo, long value, NetworkParameters network) {
      ScriptOutput script;
      if (sendTo.isMultisig(network)) {
         script = new ScriptOutputP2SH(sendTo.getTypeSpecificBytes());
      } else {
         script = new ScriptOutputStandard(sendTo.getTypeSpecificBytes());
      }
      return new TransactionOutput(value, script);
   }

   public static List<byte[]> generateSignatures(SigningRequest[] requests, IPrivateKeyRing keyRing) {
      List<byte[]> signatures = new LinkedList<>();
      for (SigningRequest request : requests) {
         BitcoinSigner signer = keyRing.findSignerByPublicKey(request.publicKey);
         if (signer == null) {
            // This should not happen as we only work on outputs that we have
            // keys for
            throw new RuntimeException("Private key not found");
         }
         byte[] signature = signer.makeStandardBitcoinSignature(request.toSign);
         signatures.add(signature);
      }
      return signatures;
   }

   /**
    * Create an unsigned transaction and automatically calculate the miner fee.
    * <p>
    * If null is specified as the change address the 'richest' address that is part of the funding is selected as the
    * change address. This way the change always goes to the address contributing most, and the change will be less
    * than the contribution.
    *
    * @param inventory     The list of unspent transaction outputs that can be used as
    *                      funding
    * @param changeAddress The address to send any change to, can be null
    * @param keyRing       The public key ring matching the unspent outputs
    * @param network       The network we are working on
    * @param minerFeeToUse The miner fee in sat to pay for every kilobytes of transaction size
    * @return An unsigned transaction or null if not enough funds were available
    */
   public UnsignedTransaction createUnsignedTransaction(Collection<UnspentTransactionOutput> inventory,
                                                        Address changeAddress, IPublicKeyRing keyRing,
                                                        NetworkParameters network, long minerFeeToUse)
       throws InsufficientFundsException, UnableToBuildTransactionException {
      // Make a copy so we can mutate the list
      List<UnspentTransactionOutput> unspent = new LinkedList<>(inventory);
      CoinSelector coinSelector = new FifoCoinSelector(minerFeeToUse, unspent);
      long fee = coinSelector.getFee();
      long outputSum = coinSelector.getOutputSum();
      List<UnspentTransactionOutput> funding = pruneRedundantOutputs(coinSelector.getFundings(), fee + outputSum);
      boolean needChangeOutputInEstimation = needChangeOutputInEstimation(funding, outputSum, minerFeeToUse);

      // the number of inputs might have changed - recalculate the fee
      int outputsSizeInFeeEstimation = _outputs.size();
      if (needChangeOutputInEstimation) {
         outputsSizeInFeeEstimation += 1;
      }
      fee = estimateFee(funding.size(), outputsSizeInFeeEstimation, minerFeeToUse);

      long found = 0;
      for (UnspentTransactionOutput output : funding) {
         found += output.value;
      }
      // We have found all the funds we need
      long toSend = fee + outputSum;

      if (changeAddress == null) {
         // If no change address is specified, get the richest address from the
         // funding set
         changeAddress = getRichest(funding, network);
      }

      // We have our funding, calculate change
      long change = found - toSend;

      // Get a copy of all outputs
      LinkedList<TransactionOutput> outputs = new LinkedList<>(_outputs);
      if(change >= MINIMUM_OUTPUT_VALUE) {
         TransactionOutput changeOutput = createOutput(changeAddress, change, _network);
         // Select a random position for our change so it is harder to analyze our addresses in the block chain.
         // It is OK to use the weak java Random class for this purpose.
         int position = new Random().nextInt(outputs.size() + 1);
         outputs.add(position, changeOutput);
      }

      UnsignedTransaction unsignedTransaction = new UnsignedTransaction(outputs, funding, keyRing, network);

      // check if we have a reasonable Fee or throw an error otherwise
      int estimateTransactionSize = estimateTransactionSize(unsignedTransaction.getFundingOutputs().length,
          unsignedTransaction.getOutputs().length);
      long calculatedFee = unsignedTransaction.calculateFee();
      float estimatedFeePerKb = (long) ((float) calculatedFee / ((float) estimateTransactionSize / 1000));

      // set a limit of MAX_MINER_FEE_PER_KB as absolute limit - it is very likely a bug in the fee estimator or transaction composer
      if (estimatedFeePerKb > Transaction.MAX_MINER_FEE_PER_KB) {
         throw new UnableToBuildTransactionException(
             String.format(Locale.getDefault(),
                 "Unreasonable high transaction fee of %s sat/1000Byte on a %d Bytes tx. Fee: %d sat, Suggested fee: %d sat",
                 estimatedFeePerKb, estimateTransactionSize, calculatedFee, minerFeeToUse)
         );
      }

      return unsignedTransaction;
   }

   private boolean needChangeOutputInEstimation(List<UnspentTransactionOutput> funding,
                                                long outputSum, long minerFeeToUse) {
      long fee = estimateFee(funding.size(), _outputs.size(), minerFeeToUse);

      long found = 0;
      for (UnspentTransactionOutput output : funding) {
         found += output.value;
      }
      // We have found all the funds we need
      long toSend = fee + outputSum;

      // We have our funding, calculate change
      long change = found - toSend;

      if (change >= MINIMUM_OUTPUT_VALUE) {
         // We need to add a change output in the estimation.
         return true;
      } else {
         // The change output would be smaller (or zero) than what the network would accept.
         // In this case we leave it be as a small increased miner fee.
         return false;
      }
   }


   /**
    * Greedy picks the biggest UTXOs until the outputSum is met.
    * @param funding UTXO list in any order
    * @param outputSum amount to spend
    * @return shuffled list of UTXOs
    */
   private List<UnspentTransactionOutput> pruneRedundantOutputs(List<UnspentTransactionOutput> funding, long outputSum) {
      List<UnspentTransactionOutput> largestToSmallest = Ordering.natural().reverse().onResultOf(new Function<UnspentTransactionOutput, Comparable>() {
         @Override
         public Comparable apply(UnspentTransactionOutput input) {
            return input.value;
         }
      }).sortedCopy(funding);

      long target = 0;
      for (int i = 0; i < largestToSmallest.size(); i++) {
         UnspentTransactionOutput output = largestToSmallest.get(i);
         target += output.value;
         if (target >= outputSum) {

            List<UnspentTransactionOutput> ret = largestToSmallest.subList(0, i + 1);
            Collections.shuffle(ret);
            return ret;
         }
      }
      return largestToSmallest;
   }

   @VisibleForTesting
   Address getRichest(Collection<UnspentTransactionOutput> unspent, final NetworkParameters network) {
      Preconditions.checkArgument(!unspent.isEmpty());
      Function<UnspentTransactionOutput, Address> txout2Address = new Function<UnspentTransactionOutput, Address>() {
         @Override
         public Address apply(UnspentTransactionOutput input) {
            return input.script.getAddress(network);
         }
      };
      Multimap<Address, UnspentTransactionOutput> index = Multimaps.index(unspent, txout2Address);
      Address ret = getRichest(index);
      return Preconditions.checkNotNull(ret);
   }

   private Address getRichest(Multimap<Address, UnspentTransactionOutput> index) {
      Address ret = null;
      long maxSum = 0;
      for (Address address : index.keys()) {
         Collection<UnspentTransactionOutput> unspentTransactionOutputs = index.get(address);
         long newSum = sum(unspentTransactionOutputs);
         if (newSum > maxSum) {
            ret = address;
            maxSum = newSum;
         }
      }
      return ret;
   }

   private long sum(Iterable<UnspentTransactionOutput> outputs) {
      long sum = 0;
      for (UnspentTransactionOutput output : outputs) {
         sum += output.value;
      }
      return sum;
   }

   public static Transaction finalizeTransaction(UnsignedTransaction unsigned, List<byte[]> signatures) {
      // Create finalized transaction inputs
      TransactionInput[] inputs = new TransactionInput[unsigned._funding.length];
      for (int i = 0; i < unsigned._funding.length; i++) {
         // Create script from signature and public key
         ScriptInputStandard script = new ScriptInputStandard(signatures.get(i),
             unsigned._signingRequests[i].publicKey.getPublicKeyBytes());
         inputs[i] = new TransactionInput(unsigned._funding[i].outPoint, script, unsigned.getDefaultSequenceNumber());
      }

      // Create transaction with valid outputs and empty inputs
      return new Transaction(1, inputs, unsigned._outputs, unsigned.getLockTime());
   }

   private UnspentTransactionOutput extractOldest(Collection<UnspentTransactionOutput> unspent) {
      // find the "oldest" output
      int minHeight = Integer.MAX_VALUE;
      UnspentTransactionOutput oldest = null;
      for (UnspentTransactionOutput output : unspent) {
         if (!(output.script instanceof ScriptOutputStandard)) {
            // only look for standard scripts
            continue;
         }

         // Unconfirmed outputs have height = -1 -> change this to Int.MAX-1, so that we
         // choose them as the last possible option
         int height = output.height > 0 ? output.height : Integer.MAX_VALUE - 1;

         if (height < minHeight) {
            minHeight = height;
            oldest = output;
         }
      }
      if (oldest == null) {
         // There were no outputs
         return null;
      }
      unspent.remove(oldest);
      return oldest;
   }

   private long outputSum() {
      long sum = 0;
      for (TransactionOutput output : _outputs) {
         sum += output.value;
      }
      return sum;
   }

   private static Sha256Hash hashTransaction(Transaction t) {
      ByteWriter writer = new ByteWriter(1024);
      t.toByteWriter(writer);
      // We also have to write a hash type.
      int hashType = 1;
      writer.putIntLE(hashType);
      // Note that this is NOT reversed to ensure it will be signed
      // correctly. If it were to be printed out
      // however then we would expect that it is IS reversed.
      return HashUtils.doubleSha256(writer.toBytes());
   }

   /**
    * Estimate the size of a transaction by taking the number of inputs and outputs into account. This allows us to
    * give a good estimate of the final transaction size, and determine whether out fee size is large enough.
    *
    * @param inputs  the number of inputs of the transaction
    * @param outputs the number of outputs of a transaction
    * @return The estimated transaction size in bytes
    */
   public static int estimateTransactionSize(int inputs, int outputs) {
      int estimate = 0;
      estimate += 4; // Version info
      estimate += CompactInt.toBytes(inputs).length; // num input encoding. Usually 1. >253 inputs -> 3
      estimate += MAX_INPUT_SIZE * inputs;
      estimate += CompactInt.toBytes(outputs).length; // num output encoding. Usually 1. >253 outputs -> 3
      estimate += OUTPUT_SIZE * outputs;
      estimate += 4; // nLockTime
      return estimate;
   }

   /**
    * Returns the estimate needed fee in satoshis for a default P2PKH transaction with a certain number
    * of inputs and outputs and the specified per-kB-fee
    *
    * @param inputs  number of inputs
    * @param outputs number of outputs
    * @param minerFeePerKb miner fee in satoshis per kB
    **/
   public static long estimateFee(int inputs, int outputs, long minerFeePerKb) {
      // fee is based on the size of the transaction, we have to pay for
      // every 1000 bytes
      float txSizeKb = (float) (estimateTransactionSize(inputs, outputs) / 1000.0); //in kilobytes
      return (long) (txSizeKb * minerFeePerKb);
   }

   private interface CoinSelector {
      List<UnspentTransactionOutput> getFundings();
      long getFee();
      long getOutputSum();
   }

   private class FifoCoinSelector implements CoinSelector {
      private List<UnspentTransactionOutput> allFunding;
      private long feeSat;
      private long outputSum;

      public FifoCoinSelector(long feeSatPerKb, List<UnspentTransactionOutput> unspent)
          throws InsufficientFundsException {
         // Find the funding for this transaction
         allFunding = new LinkedList<>();
         feeSat = estimateFee(unspent.size(), 1, feeSatPerKb);
         outputSum = outputSum();
         long foundSat = 0;
         while (foundSat < feeSat + outputSum) {
            UnspentTransactionOutput unspentTransactionOutput = extractOldest(unspent);
            if (unspentTransactionOutput == null) {
               // We do not have enough funds
               throw new InsufficientFundsException(outputSum, feeSat);
            }
            foundSat += unspentTransactionOutput.value;
            allFunding.add(unspentTransactionOutput);
            feeSat = estimateFee(allFunding.size(),
                needChangeOutputInEstimation(allFunding, outputSum, feeSatPerKb)
                    ? _outputs.size() + 1
                    : _outputs.size(), feeSatPerKb);
         }
      }

      @Override
      public List<UnspentTransactionOutput> getFundings() {
         return allFunding;
      }

      @Override
      public long getFee() {
         return feeSat;
      }

      @Override
      public long getOutputSum() {
         return outputSum;
      }
   }
}
