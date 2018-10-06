/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

package com.mycelium.wallet.extsig.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.mrd.bitlib.SigningRequest;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.crypto.BipDerivationType;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionInput;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.HDAccountExternalSignature;
import com.mycelium.wapi.wallet.bip44.ExternalSignatureProvider;
import com.satoshilabs.trezor.ExternalSignatureDevice;
import com.satoshilabs.trezor.ExtSigDeviceConnectionException;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorMessage.SignTx;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxRequest;
import com.satoshilabs.trezor.protobuf.TrezorType;
import com.squareup.otto.Bus;

import org.bitcoinj.core.ScriptException;
import org.bitcoinj.script.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static com.mycelium.wallet.Constants.TAG;
import static org.bitcoinj.core.NetworkParameters.ID_MAINNET;
import static org.bitcoinj.core.NetworkParameters.ID_TESTNET;

public abstract class ExternalSignatureDeviceManager extends AbstractAccountScanManager implements ExternalSignatureProvider {
   protected final int PRIME_DERIVATION_FLAG = 0x80000000;

   private ExternalSignatureDevice externalSignatureDevice = null;
   private TrezorMessage.Features features;

   protected final LinkedBlockingQueue<String> pinMatrixEntry = new LinkedBlockingQueue<String>(1);

   public static class OnButtonRequest {
   }

   public static class OnPinMatrixRequest {
   }

   public ExternalSignatureDeviceManager(Context context, NetworkParameters network, Bus eventBus) {
      super(context, network, eventBus);
   }

   abstract protected ExternalSignatureDevice createDevice();

   private ExternalSignatureDevice getSignatureDevice() {
      if (externalSignatureDevice == null) {
         externalSignatureDevice = createDevice();
      }
      return externalSignatureDevice;
   }

   public String getLabelOrDefault() {
      if (features != null && !features.getLabel().isEmpty()) {
         return features.getLabel();
      }
      return externalSignatureDevice.getDefaultAccountName();
   }

   public boolean isMostRecentVersion() {
      if (features != null) {
         return !externalSignatureDevice.getMostRecentFirmwareVersion().isNewerThan(
               features.getMajorVersion(),
               features.getMinorVersion(),
               features.getPatchVersion()
         );
      } else {
         // we dont know...
         return true;
      }
   }

   public boolean hasExternalConfigurationTool(){
      return getSignatureDevice().getDeviceConfiguratorAppName() != null;
   }

   public void openExternalConfigurationTool(final Context context, final String msg, final Runnable onClose) {
      // see if we know how to init that device
      final String packageName = getSignatureDevice().getDeviceConfiguratorAppName();
      if (packageName != null) {
         AlertDialog.Builder downloadDialog = new AlertDialog.Builder(context);
         downloadDialog.setTitle(R.string.ext_sig_configuration_dialog_title);
         downloadDialog.setMessage(msg);
         downloadDialog.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
               Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
               if (intent == null) {
                  intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
               }
               context.startActivity(intent);
               if (onClose!=null) {
                  onClose.run();
               }
            }
         });
         downloadDialog.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
               if (onClose!=null) {
                  onClose.run();
               }
            }
         });
         downloadDialog.show();
      }
   }

   @Override
   protected boolean onBeforeScan() {
      return initialize();
   }

   public boolean initialize() {
      // check if a trezor is attached and connect to it, otherwise loop and check periodically

      // wait until a device is connected
      while (!getSignatureDevice().isDevicePluggedIn()) {
         try {
            setState(Status.unableToScan, getCurrentAccountState());
            Thread.sleep(4000);
         } catch (InterruptedException e) {
            break;
         }
      }

      // set up the connection and afterwards send a Features-Request
      if (getSignatureDevice().connect(getContext())) {
         TrezorMessage.Initialize req = TrezorMessage.Initialize.newBuilder().build();
         Message resp = getSignatureDevice().send(req);
         if (resp != null && resp instanceof TrezorMessage.Features) {
            final TrezorMessage.Features f = (TrezorMessage.Features) resp;

            // remember the features
            getMainThreadHandler().post(new Runnable() {
               @Override
               public void run() {
                  features = f;
               }
            });

            return true;
         } else if (resp == null) {
            Log.e("trezor", "Got null-response from trezor");
         } else {
            Log.e("trezor", "Got wrong response from trezor " + resp.getClass().toString());
         }
      }
      return false;
   }

   // based on https://github.com/trezor/python-trezor/blob/a2a5b6a4601c6912166ef7f85f04fa1101c2afd4/trezorlib/client.py
   @Override
   public Transaction getSignedTransaction(UnsignedTransaction unsigned, HDAccountExternalSignature forAccount) {
      if (!initialize()) {
         return null;
      }

      setState(Status.readyToScan, getCurrentAccountState());

      // send initial signing-request
      SignTx signTx = SignTx.newBuilder()
            .setCoinName(getNetwork().getCoinName())
            .setInputsCount(unsigned.getFundingOutputs().length)
            .setOutputsCount(unsigned.getOutputs().length)
            .build();

      Message response;
      try {
         response = getSignatureDevice().send(signTx);
      } catch (ExtSigDeviceConnectionException ex) {
         postErrorMessage(ex.getMessage());
         return null;
      }

      SigningRequest[] signatureInfo = unsigned.getSigningRequests();

      ByteWriter signedTx = new ByteWriter(1024);

      while (true) {
         // check for common response and handle them
         try {
            response = filterMessages(response);
         } catch (ExtSigDeviceConnectionException ex) {
            postErrorMessage(ex.getMessage());
            return null;
         }

         if (response == null) {
            // Something went wrong while talking with trezor - get out of here
            return null;
         }

         if (!(response instanceof TxRequest)) {
            Log.e("trezor", "Trezor: Unexpected Response " + response.getClass().toString());
            return null;
         }

         TxRequest txRequest = (TxRequest) response;

         // response had a part of the signed tx - write it to our buffer
         if (txRequest.hasSerialized() && txRequest.getSerialized().hasSerializedTx()) {
            signedTx.putBytes(txRequest.getSerialized().getSerializedTx().toByteArray());
         }

         if (txRequest.getRequestType() == TrezorType.RequestType.TXFINISHED) {
            // We are done here...
            break;
         }

         // Device asked for more information, let's process it.
         TrezorType.TxRequestDetailsType txRequestDetailsType = txRequest.getDetails();
         Log.d("trezor", "RequestTyp: " + txRequest.getRequestType().toString());


         Transaction currentTx;
         if (txRequestDetailsType.hasTxHash()) {
            // trezor requested information about a related tx - get it from the account backing
            Sha256Hash requestHash = Sha256Hash.of(txRequestDetailsType.getTxHash().toByteArray());
            currentTx = TransactionEx.toTransaction(forAccount.getTransaction(requestHash));
         } else {
            // trezor requested information about the to-be-signed tx
            currentTx = Transaction.fromUnsignedTransaction(unsigned);
         }

         // Lets see, what trezor wants to know
         if (txRequest.getRequestType() == TrezorType.RequestType.TXMETA) {
            // Send transaction metadata

            TrezorType.TransactionType txType = TrezorType.TransactionType.newBuilder()
                  .setInputsCnt(currentTx.inputs.length)
                  .setOutputsCnt(currentTx.outputs.length)
                  .setVersion(currentTx.version)
                  .setLockTime(currentTx.lockTime)
                  .build();

            TrezorMessage.TxAck txAck = TrezorMessage.TxAck.newBuilder()
                  .setTx(txType)
                  .build();

            response = getSignatureDevice().send(txAck);

         } else if (txRequest.getRequestType() == TrezorType.RequestType.TXINPUT) {
            TransactionInput ak_input = currentTx.inputs[txRequestDetailsType.getRequestIndex()];


            ByteString prevHash = ByteString.copyFrom(ak_input.outPoint.txid.getBytes());
            ByteString scriptSig = ByteString.copyFrom(ak_input.script.getScriptBytes());
            TrezorType.TxInputType.Builder txInputBuilder = TrezorType.TxInputType.newBuilder()
                  .setPrevHash(prevHash)
                  .setPrevIndex(ak_input.outPoint.index)
                  .setSequence(ak_input.sequence)
                  .setScriptSig(scriptSig);

            // get the bip32 path for the address, so that trezor knows with what key to sign it
            // only for the unsigned txin
            if (!txRequestDetailsType.hasTxHash()) {
               SigningRequest signingRequest = signatureInfo[txRequestDetailsType.getRequestIndex()];
               boolean foundKey = false;
               for (BipDerivationType derivationType : new BipDerivationType[]{
                       BipDerivationType.BIP44,
                       BipDerivationType.BIP49,
                       BipDerivationType.BIP84
               }) {
                  Address toSignWith = signingRequest.getPublicKey().toAddress(getNetwork(), derivationType.getAddressType());
                  if (toSignWith != null) {
                     Optional<Integer[]> addId = forAccount.getAddressId(toSignWith);
                     if (addId.isPresent()) {
                        new InputAddressSetter(txInputBuilder).setAddressN((int) derivationType.getPurpose(), forAccount.getAccountIndex(), addId.get());
                        foundKey = true;
                        break;
                     }
                  }
               }
               if (!foundKey) {
                  Log.w("trezor", "no address found for signing InputIDX " + txRequestDetailsType.getRequestIndex());
               }
            }

            TrezorType.TxInputType txInput = txInputBuilder.build();

            TrezorType.TransactionType txType = TrezorType.TransactionType.newBuilder()
                  .addInputs(txInput)
                  .build();

            TrezorMessage.TxAck txAck = TrezorMessage.TxAck.newBuilder()
                  .setTx(txType)
                  .build();

            response = getSignatureDevice().send(txAck);

         } else if (txRequest.getRequestType() == TrezorType.RequestType.TXOUTPUT) {
            TransactionOutput ak_output = currentTx.outputs[txRequestDetailsType.getRequestIndex()];

            TrezorType.TransactionType txType;

            if (txRequestDetailsType.hasTxHash()) {
               // request has an hash -> requests data for an existing output
               ByteString scriptPubKey = ByteString.copyFrom(ak_output.script.getScriptBytes());
               TrezorType.TxOutputBinType txOutput = TrezorType.TxOutputBinType.newBuilder()
                     .setScriptPubkey(scriptPubKey)
                     .setAmount(ak_output.value)
                     .build();

               txType = TrezorType.TransactionType.newBuilder()
                     .addBinOutputs(txOutput)
                     .build();

            } else {
               // request has no hash -> trezor wants informations about the
               // outputs of the new tx
               Address address = ak_output.script.getAddress(getNetwork());
               TrezorType.TxOutputType.Builder txOutput = TrezorType.TxOutputType.newBuilder()
                     .setAmount(ak_output.value)
                     .setScriptType(mapScriptType(ak_output.script));

               Optional<Integer[]> addId = forAccount.getAddressId(address);
               BipDerivationType derivationType = BipDerivationType.Companion.getDerivationTypeByAddress(address);
               if (addId.isPresent() && addId.get()[0] == 1) {
                  // If it is one of our internal change addresses, add the HD-PathID
                  // so that trezor knows, this is the change txout and can calculate the value of the tx correctly
                  new OutputAddressSetter(txOutput).setAddressN((int) derivationType.getPurpose(), forAccount.getAccountIndex(), addId.get());
               } else {
                  // If it is regular address (non-change), set address instead of address_n
                  txOutput.setAddress(address.toString());
               }

               txType = TrezorType.TransactionType.newBuilder()
                     .addOutputs(txOutput.build())
                     .build();
            }

            TrezorMessage.TxAck txAck = TrezorMessage.TxAck.newBuilder()
                  .setTx(txType)
                  .build();

            response = getSignatureDevice().send(txAck);
            Log.w(TAG, "getSignedTransaction: C " + HexUtils.toHex(txAck.toByteArray()) + " -> " + response);
         }
      }

      Transaction ret;
      try {
         ret = Transaction.fromByteReader(new ByteReader(signedTx.toBytes()));
         checkSignedTransaction(unsigned, signedTx);
      } catch (Transaction.TransactionParsingException e) {
         postErrorMessage("Trezor TX not valid.");
         Log.e("trezor", "Trezor TX not valid " + e.getMessage(), e);
         return null;
      } catch (ScriptException e) {
         postErrorMessage("Probably wrong passphrase.");
         Log.e(TAG, "bitcoinJ doesn't like this transaction: ", e);
         return null;
      }
      return ret;
   }

   /**
    * At least Trezor and KeepKey have no way of knowing the input scripts as they see only the outpoints that they are asked to sign against. Therefore, they tend to sign with wrong keys, if using a passphrase that is not stored in the app. See https://github.com/mycelium-com/wallet/issues/169
    */
   private void checkSignedTransaction(UnsignedTransaction unsigned, ByteWriter signedTx) {
      org.bitcoinj.core.NetworkParameters networkParameters =  org.bitcoinj.core.NetworkParameters.fromID(getNetwork().isProdnet() ? ID_MAINNET : ID_TESTNET);
      org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(networkParameters, signedTx.toBytes());
      for (int i = 0; i < tx.getInputs().size(); i++) {
         org.bitcoinj.core.TransactionInput input = tx.getInput(i);
         org.bitcoinj.script.Script scriptSig = input.getScriptSig();

         String addressString = unsigned.getFundingOutputs()[i].script.getAddress(getNetwork()).toString();
         org.bitcoinj.script.Script outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(networkParameters, addressString));
         scriptSig.correctlySpends(tx, i, outputScript, org.bitcoinj.script.Script.ALL_VERIFY_FLAGS);
      }
   }

   private TrezorType.OutputScriptType mapScriptType(ScriptOutput script) {
      if (script instanceof ScriptOutputStandard) {
         return TrezorType.OutputScriptType.PAYTOADDRESS;
      } else if (script instanceof ScriptOutputP2SH) {
         return TrezorType.OutputScriptType.PAYTOSCRIPTHASH;
      } else {
         throw new RuntimeException("unknown script type");
      }
   }

   @Override
   public Optional<HdKeyNode> getAccountPubKeyNode(HdKeyPath keyPath, BipDerivationType derivationType) {
      TrezorMessage.GetPublicKey msgGetPubKey = TrezorMessage.GetPublicKey.newBuilder()
            .addAllAddressN(keyPath.getAddressN())
            .build();

      try {
         Message resp = filterMessages(getSignatureDevice().send(msgGetPubKey));
         if (resp instanceof TrezorMessage.PublicKey) {
            TrezorMessage.PublicKey pubKeyNode = (TrezorMessage.PublicKey) resp;
            PublicKey pubKey = new PublicKey(pubKeyNode.getNode().getPublicKey().toByteArray());
            HdKeyNode accountRootNode = new HdKeyNode(
                  pubKey,
                  pubKeyNode.getNode().getChainCode().toByteArray(),
                  3, 0,
                  keyPath.getLastIndex(),
                  derivationType
            );
            return Optional.of(accountRootNode);
         } else {
            return Optional.absent();
         }
      } catch (final ExtSigDeviceConnectionException ex) {
         postErrorMessage(ex.getMessage());
         return Optional.absent();
      }
   }

   @NonNull
   @Override
   public UUID createOnTheFlyAccount(@NonNull List<? extends HdKeyNode> accountRoots, @NonNull WalletManager walletManager, int accountIndex) {
      UUID account = null;
      for (HdKeyNode root:
           accountRoots) {
         if (walletManager.hasAccount(root.getUuid())) {
            // Account already exists
            account = root.getUuid();
         }
      }
      if (account == null) {
         account = walletManager.createExternalSignatureAccount(accountRoots, this, accountIndex);
      }
      return account;
   }

   public void enterPin(String pin) {
      pinMatrixEntry.clear();
      pinMatrixEntry.offer(pin);
   }

   private Message filterMessages(final Message msg) {
      if (msg instanceof TrezorMessage.ButtonRequest) {
         getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
               getEventBus().post(new OnButtonRequest());
            }
         });

         TrezorMessage.ButtonAck txButtonAck = TrezorMessage.ButtonAck.newBuilder()
               .build();
         return filterMessages(getSignatureDevice().send(txButtonAck));

      } else if (msg instanceof TrezorMessage.PinMatrixRequest) {
         getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
               getEventBus().post(new OnPinMatrixRequest());
            }
         });
         String pin;
         try {
            // wait for the user to enter the pin
            pin = pinMatrixEntry.take();
         } catch (InterruptedException e) {
            pin = "";
         }

         TrezorMessage.PinMatrixAck txPinAck = TrezorMessage.PinMatrixAck.newBuilder()
               .setPin(pin)
               .build();

         // send the Pin Response and (if everything is okay) get the response for the
         // previous requested action
         return filterMessages(getSignatureDevice().send(txPinAck));
      } else if (msg instanceof TrezorMessage.PassphraseRequest) {
         // get the user to enter a passphrase
         Optional<String> passphrase = waitForPassphrase();

         GeneratedMessage response;
         if (!passphrase.isPresent()) {
            // user has not provided a password - reset session on trezor and cancel
            response = TrezorMessage.ClearSession.newBuilder().build();
            getSignatureDevice().send(response);
            return null;
         } else {
            response = TrezorMessage.PassphraseAck.newBuilder()
                  .setPassphrase(passphrase.get())
                  .build();

            // send the Passphrase Response and get the response for the
            // previous requested action
            return filterMessages(getSignatureDevice().send(response));
         }
      } else if (msg instanceof TrezorMessage.Failure) {
         final TrezorMessage.Failure errMsg = (TrezorMessage.Failure) msg;
         if (postErrorMessage(errMsg.getMessage(), errMsg.getCode())) {
            return null;
         } else {
            throw new RuntimeException("Trezor error:" + errMsg.getCode().toString() + "; " + errMsg.getMessage());
         }
      }

      return msg;
   }

   public TrezorMessage.Features getFeatures() {
      return features;
   }

   private abstract class AddressSetter {
      public abstract void addAddressN(Integer addressPath);

      public void setAddressN(Integer purposeNumber, Integer accountNumber, Integer[] addId) {
         // build the full bip32 path
         Integer[] addressPath = new Integer[]{
                 purposeNumber | PRIME_DERIVATION_FLAG,
                 getNetwork().getBip44CoinType() | PRIME_DERIVATION_FLAG,
                 accountNumber | PRIME_DERIVATION_FLAG,
                 addId[0],
                 addId[1]};
         for (Integer b : addressPath) {
            this.addAddressN(b);
         }
      }
   }

   private class OutputAddressSetter extends AddressSetter {
      final private TrezorType.TxOutputType.Builder txOutput;

      private OutputAddressSetter(TrezorType.TxOutputType.Builder txOutput) {
         this.txOutput = txOutput;
      }

      @Override
      public void addAddressN(Integer addressPath) {
         txOutput.addAddressN(addressPath);
      }
   }

   private class InputAddressSetter extends AddressSetter {
      final private TrezorType.TxInputType.Builder txInput;

      private InputAddressSetter(TrezorType.TxInputType.Builder txInput) {
         this.txInput = txInput;
      }

      @Override
      public void addAddressN(Integer addressPath) {
         txInput.addAddressN(addressPath);
      }
   }
}
