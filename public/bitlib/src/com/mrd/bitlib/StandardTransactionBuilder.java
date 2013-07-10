/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.bitlib;

import java.util.LinkedList;
import java.util.List;

import com.mrd.bitlib.crypto.BitcoinSigner;
import com.mrd.bitlib.crypto.PrivateKeyRing;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.crypto.PublicKeyRing;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.ScriptInput;
import com.mrd.bitlib.model.ScriptInputStandard;
import com.mrd.bitlib.model.ScriptOutput;
import com.mrd.bitlib.model.ScriptOutputMultisig;
import com.mrd.bitlib.model.ScriptOutputStandard;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionInput;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.HashUtils;

public class StandardTransactionBuilder {

   public static final long MINIMUM_MINER_FEE = 10000;
   public static final long MINIMUM_OUTPUT_VALUE = 5430;

   public static class InsufficientFundsException extends Exception {
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
      private static final long serialVersionUID = 1L;

      public long value;

      public OutputTooSmallException(long value) {
         super("An output was added with a value of " + value
               + " satoshis, which is smaller than the minimum accepted by the Bitcoin network");
      }

   }

   public static class SigningRequest {

      // The public part of the key we will sign with
      public PublicKey publicKey;

      // The data to make a signature on. For transactions this is the
      // transaction hash
      public byte[] toSign;

      public SigningRequest(PublicKey publicKey, byte[] toSign) {
         this.publicKey = publicKey;
         this.toSign = toSign;
      }

   }

   public static class UnsignedTransaction {
      private TransactionOutput[] _outputs;
      private UnspentTransactionOutput[] _funding;
      private SigningRequest[] _signingRequests;
      private NetworkParameters _network;

      private UnsignedTransaction(List<TransactionOutput> outputs, List<UnspentTransactionOutput> funding,
            PublicKeyRing keyRing, NetworkParameters network) {
         _network = network;
         _outputs = outputs.toArray(new TransactionOutput[] {});
         _funding = funding.toArray(new UnspentTransactionOutput[] {});
         _signingRequests = new SigningRequest[_funding.length];

         // Create empty input scripts pointing at the right out points
         TransactionInput[] inputs = new TransactionInput[_funding.length];
         for (int i = 0; i < _funding.length; i++) {
            inputs[i] = new TransactionInput(_funding[i].outPoint, ScriptInput.EMPTY);
         }

         // Create transaction with valid outputs and empty inputs
         Transaction transaction = new Transaction(1, inputs, _outputs, 0);

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
            byte[] hash = hashTransaction(transaction);

            // Set the input to the empty script again
            inputs[i] = new TransactionInput(_funding[i].outPoint, ScriptInput.EMPTY);

            _signingRequests[i] = new SigningRequest(publicKey, hash);

         }
      }

      public SigningRequest[] getSignatureInfo() {
         return _signingRequests;
      }

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

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         String fee = CoinUtil.valueString(calculateFee());
         sb.append(String.format("Fee: %s", fee)).append('\n');
         int max = Math.max(_funding.length, _outputs.length);
         for (int i = 0; i < max; i++) {
            UnspentTransactionOutput in = i < _funding.length ? _funding[i] : null;
            TransactionOutput out = i < _outputs.length ? _outputs[i] : null;
            String line;
            if (in != null && out != null) {
               line = String.format("%36s %13s -> %36s %13s", getAddress(in.script, _network), getValue(in.value),
                     getAddress(out.script, _network), getValue(out.value));
            } else if (in != null && out == null) {
               line = String.format("%36s %13s    %36s %13s", getAddress(in.script, _network), getValue(in.value), "",
                     "");
            } else if (in == null && out != null) {
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
         return String.format("(%s)", CoinUtil.valueString(value));
      }

   }

   private NetworkParameters _network;
   private List<TransactionOutput> _outputs;

   public StandardTransactionBuilder(NetworkParameters network) {
      _network = network;
      _outputs = new LinkedList<TransactionOutput>();
   }

   public void addOutput(Address sendTo, long value) throws OutputTooSmallException {
      if (value < MINIMUM_OUTPUT_VALUE) {
         throw new OutputTooSmallException(value);
      }
      _outputs.add(createOutput(sendTo, value));
   }

   private TransactionOutput createOutput(Address sendTo, long value) {
      ScriptOutput script;
      if (sendTo.isMultisig(_network)) {
         script = new ScriptOutputMultisig(sendTo.getTypeSpecificBytes());
      } else {
         script = new ScriptOutputStandard(sendTo.getTypeSpecificBytes());
      }
      TransactionOutput output = new TransactionOutput(value, script);
      return output;
   }

   public static List<byte[]> generateSignatures(SigningRequest[] requests, PrivateKeyRing keyRing) {
      List<byte[]> signatures = new LinkedList<byte[]>();
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
    * Create an unsigned transaction without specifying a fee. The fee is
    * automatically calculated to pass minimum relay and mining requirements.
    * 
    * @param unspent
    *           The list of unspent transaction outputs that can be used as
    *           funding
    * @param changeAddress
    *           The address to send any change to
    * @param keyRing
    *           The public key ring matching the unspent outputs
    * @param network
    *           The network we are working on
    * @return An unsigned transaction or null if not enough funds were available
    * @throws InsufficientFundsToPayFeeException
    * @throws InsufficientFundsException
    */
   public UnsignedTransaction createUnsignedTransaction(List<UnspentTransactionOutput> unspent, Address changeAddress,
         PublicKeyRing keyRing, NetworkParameters network) throws InsufficientFundsException {
      long fee = MINIMUM_MINER_FEE;
      while (true) {
         UnsignedTransaction unsigned;
         try {
            unsigned = createUnsignedTransaction(unspent, changeAddress, fee, keyRing, network);
         } catch (InsufficientFundsException e) {
            // We did not even have enough funds to pay the minimum fee
            throw e;
         }
         int txSize = estimateTransacrionSize(unsigned);
         // fee is based on the size of the transaction, we have to pay for
         // every 1000 bytes
         long requiredFee = (1 + (txSize / 1000)) * MINIMUM_MINER_FEE;
         if (fee >= requiredFee) {
            return unsigned;
         }
         // collect coins anew with an increased fee
         fee += MINIMUM_MINER_FEE;
      }
   }

   /**
    * Create an unsigned transaction with a specific miner fee. Note that
    * specifying a miner fee that is too low may result in hanging transactions
    * that never confirm.
    * 
    * @param unspent
    *           The list of unspent transaction outputs that can be used as
    *           funding
    * @param changeAddress
    *           The address to send any change to
    * @param fee
    *           The miner fee to pay. Specifying zero may result in hanging
    *           transactions.
    * @param keyRing
    *           The public key ring matching the unspent outputs
    * @param network
    *           The network we are working on
    * @return An unsigned transaction or null if not enough funds were available
    * @throws InsufficientFundsException
    */
   public UnsignedTransaction createUnsignedTransaction(List<UnspentTransactionOutput> unspent, Address changeAddress,
         long fee, PublicKeyRing keyRing, NetworkParameters network) throws InsufficientFundsException {
      // Make a copy so we can mutate the list
      unspent = new LinkedList<UnspentTransactionOutput>(unspent);
      List<UnspentTransactionOutput> funding = new LinkedList<UnspentTransactionOutput>();
      long outputSum = outputSum();
      long toSend = fee + outputSum;
      long found = 0;
      while (found < toSend) {
         UnspentTransactionOutput output = extractOldest(unspent);
         if (output == null) {
            // We do not have enough funds
            throw new InsufficientFundsException(outputSum, fee);
         }
         found += output.value;
         funding.add(output);
      }
      // We have our funding, calculate change
      long change = found - toSend;

      // Get a copy of all outputs
      List<TransactionOutput> outputs = new LinkedList<TransactionOutput>(_outputs);

      if (change > 0) {
         // We have more funds than needed, add an output to our change address
         if (change >= MINIMUM_OUTPUT_VALUE) {
            // But only if the change is larger than the minimum output accepted
            // by the network
            outputs.add(createOutput(changeAddress, change));
         } else {
            // The change output would be smaller than what the network would
            // accept. In this case we leave it be as a small increased miner
            // fee.
         }
      }

      return new UnsignedTransaction(outputs, funding, keyRing, network);
   }

   public static Transaction finalizeTransaction(UnsignedTransaction unsigned, List<byte[]> signatures) {
      // Create finalized transaction inputs
      TransactionInput[] inputs = new TransactionInput[unsigned._funding.length];
      for (int i = 0; i < unsigned._funding.length; i++) {
         // Create script from signature and public key
         ScriptInputStandard script = new ScriptInputStandard(signatures.get(i),
               unsigned._signingRequests[i].publicKey.getPublicKeyBytes());
         inputs[i] = new TransactionInput(unsigned._funding[i].outPoint, script);
      }

      // Create transaction with valid outputs and empty inputs
      Transaction transaction = new Transaction(1, inputs, unsigned._outputs, 0);
      return transaction;
   }

   private UnspentTransactionOutput extractOldest(List<UnspentTransactionOutput> unspent) {
      // find the "oldest" output
      int minHeight = Integer.MAX_VALUE;
      UnspentTransactionOutput oldest = null;
      for (UnspentTransactionOutput output : unspent) {
         if (!(output.script instanceof ScriptOutputStandard)) {
            // only look for standard scripts
            continue;
         }
         if (output.height < minHeight) {
            minHeight = output.height;
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

   private static byte[] hashTransaction(Transaction t) {
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
    * Estimate transaction size by clearing all input scripts and adding 140
    * bytes for each input. (The type of scripts we generate are 138-140 bytes
    * long). This allows us to give a good estimate of the final transaction
    * size, and determine whether out fee size is large enough.
    * 
    * @param unsigned
    *           The unsigned transaction to estimate the size of
    * @return The estimated transaction size
    */
   private static int estimateTransacrionSize(UnsignedTransaction unsigned) {
      // Create fake empty inputs
      TransactionInput[] inputs = new TransactionInput[unsigned._funding.length];
      for (int i = 0; i < inputs.length; i++) {
         inputs[i] = new TransactionInput(unsigned._funding[i].outPoint, ScriptInput.EMPTY);
      }

      // Create fake transaction
      Transaction t = new Transaction(1, inputs, unsigned._outputs, 0);
      int txSize = t.toBytes().length;

      // Add maximum size for each input
      txSize += 140 * t.inputs.length;

      return txSize;
   }

}
