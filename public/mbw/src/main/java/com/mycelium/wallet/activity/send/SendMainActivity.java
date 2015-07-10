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

package com.mycelium.wallet.activity.send;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.GetAmountActivity;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.modern.AddressBookFragment;
import com.mycelium.wallet.activity.modern.GetFromAddressBookActivity;
import com.mycelium.wallet.bitid.ExternalService;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.paymentrequest.PaymentRequestException;
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler;
import com.mycelium.paymentrequest.PaymentRequestInformation;
import com.mycelium.wallet.external.cashila.activity.CashilaPaymentsActivity;
import com.mycelium.wallet.external.cashila.api.response.BillPay;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44AccountExternalSignature;
import com.squareup.otto.Subscribe;
import org.bitcoin.protocols.payments.PaymentACK;

import java.util.Arrays;
import java.util.UUID;

public class SendMainActivity extends Activity {

   private static final int GET_AMOUNT_RESULT_CODE = 1;
   private static final int SCAN_RESULT_CODE = 2;
   private static final int ADDRESS_BOOK_RESULT_CODE = 3;
   private static final int MANUAL_ENTRY_RESULT_CODE = 4;
   private static final int REQUEST_PICK_ACCOUNT = 5;
   protected static final int SIGN_TRANSACTION_REQUEST_CODE = 6;
   private static final int BROADCAST_REQUEST_CODE = 7;
   private static final int REQUEST_PAYMENT_HANDLER = 8;
   public static final String RAW_PAYMENT_REQUEST = "rawPaymentRequest";
   private BillPay _sepaPayment;
   public static final String AMOUNT_TO_SEND = "amountToSend";


   public static final String ACCOUNT = "account";
   public static final String IS_COLD_STORAGE = "isColdStorage";
   public static final String RECEIVING_ADDRESS = "receivingAddress";
   public static final String HD_KEY = "hdKey";
   public static final String TRANSACTION_LABEL = "transactionLabel";
   public static final String BITCOIN_URI = "bitcoinUri";
   public static final String FEE_LVL = "feeLvl";
   public static final String PAYMENT_FETCHED = "paymentFetched";
   private static final String PAYMENT_REQUEST_HANDLER_ID = "paymentRequestHandlerId";
   private static final String SIGNED_TRANSACTION = "signedTransaction";
   public static final String SEPA_PAYMENT = "sepaPayment";

   private enum TransactionStatus {
      MissingArguments, OutputTooSmall, InsufficientFunds, OK;


   }
   private MbwManager _mbwManager;

   private PaymentRequestHandler _paymentRequestHandler;
   private String _paymentRequestHandlerUuid;

   protected WalletAccount _account;
   private Double _oneBtcInFiat; // May be null
   private Long _amountToSend;
   private Address _receivingAddress;
   protected String _transactionLabel;
   private BitcoinUri _bitcoinUri;
   protected boolean _isColdStorage;
   private TransactionStatus _transactionStatus;
   protected UnsignedTransaction _unsigned;
   private Transaction _signedTransaction;
   private MinerFee _fee;
   private ProgressDialog _progress;
   private UUID _receivingAcc;
   private  boolean _xpubSyncing = false;
   private boolean _spendingUnconfirmed = false;
   private boolean _paymentFetched = false;

   public static Intent getIntent(Activity currentActivity, UUID account, boolean isColdStorage) {
      return getIntent(currentActivity, account, null, null, isColdStorage);
   }

   public static Intent getIntent(Activity currentActivity, UUID account,
                             Long amountToSend, Address receivingAddress, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, SendMainActivity.class);
      intent.putExtra(ACCOUNT, account);
      intent.putExtra(AMOUNT_TO_SEND, amountToSend);
      intent.putExtra(RECEIVING_ADDRESS, receivingAddress);
      intent.putExtra(IS_COLD_STORAGE, isColdStorage);
      return intent;
   }

   public static Intent getSepaIntent(Activity currentActivity, UUID account,
                                  BillPay sepaPayment, String txLabel, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, SendMainActivity.class);
      intent.putExtra(ACCOUNT, account);
      intent.putExtra(AMOUNT_TO_SEND, Bitcoins.nearestValue(sepaPayment.details.amountToDeposit).getLongValue());
      intent.putExtra(RECEIVING_ADDRESS, sepaPayment.details.address);
      intent.putExtra(TRANSACTION_LABEL, txLabel);
      intent.putExtra(SEPA_PAYMENT, sepaPayment);
      intent.putExtra(IS_COLD_STORAGE, isColdStorage);
      return intent;
   }

   public static Intent getIntent(Activity currentActivity, UUID account, HdKeyNode hdKey) {
      Intent intent = new Intent(currentActivity, SendMainActivity.class);
      intent.putExtra(ACCOUNT, account);
      intent.putExtra(HD_KEY, hdKey);
      intent.putExtra(IS_COLD_STORAGE, false);
      return intent;
   }

   public static Intent getIntent(Activity currentActivity, UUID account, BitcoinUri uri, boolean isColdStorage) {
      Intent intent = getIntent(currentActivity, account, uri.amount, uri.address, isColdStorage);
      intent.putExtra(TRANSACTION_LABEL, uri.label);
      intent.putExtra(BITCOIN_URI, uri);
      return intent;
   }

   public static Intent getIntent(Activity currentActivity, UUID account, byte[] rawPaymentRequest, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, SendMainActivity.class);
      intent.putExtra(ACCOUNT, account);
      intent.putExtra(IS_COLD_STORAGE, isColdStorage);
      intent.putExtra(RAW_PAYMENT_REQUEST, rawPaymentRequest);

      return intent;
   }

   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.send_main_activity);
      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra(ACCOUNT));

      // May be null
      _amountToSend = (Long) getIntent().getSerializableExtra(AMOUNT_TO_SEND);
      // May be null
      _receivingAddress = (Address) getIntent().getSerializableExtra(RECEIVING_ADDRESS);
      //May be null
      _transactionLabel = getIntent().getStringExtra(TRANSACTION_LABEL);
      //May be null
      _bitcoinUri = (BitcoinUri)getIntent().getSerializableExtra(BITCOIN_URI);

      _isColdStorage = getIntent().getBooleanExtra(IS_COLD_STORAGE, false);
      _account = _mbwManager.getWalletManager(_isColdStorage).getAccount(accountId);
      _fee = _mbwManager.getMinerFee();

      // Load saved state, overwriting amount and address
      if (savedInstanceState != null) {
         _amountToSend = (Long) savedInstanceState.getSerializable(AMOUNT_TO_SEND);
         _receivingAddress = (Address) savedInstanceState.getSerializable(RECEIVING_ADDRESS);
         _transactionLabel = savedInstanceState.getString(TRANSACTION_LABEL);
         _fee = MinerFee.fromString(savedInstanceState.getString(FEE_LVL));
         _bitcoinUri = (BitcoinUri) savedInstanceState.getSerializable(BITCOIN_URI);
         _paymentFetched = savedInstanceState.getBoolean(PAYMENT_FETCHED);
         _signedTransaction = (Transaction) savedInstanceState.getSerializable(SIGNED_TRANSACTION);

         // get the payment request handler from the BackgroundObject cache - if the application
         // has restarted since it was cached, the user gets queried again
         _paymentRequestHandlerUuid = savedInstanceState.getString(PAYMENT_REQUEST_HANDLER_ID);
         if (_paymentRequestHandlerUuid != null) {
            _paymentRequestHandler = (PaymentRequestHandler) _mbwManager.getBackgroundObjectsCache()
                  .getIfPresent(_paymentRequestHandlerUuid);
         }
      }

      //if we do not have a stored receiving address, and got a keynode, we need to figure out the address
      if (_receivingAddress == null) {
         HdKeyNode hdKey = (HdKeyNode) getIntent().getSerializableExtra(HD_KEY);
         if (hdKey != null) setReceivingAddressFromKeynode(hdKey);
      }

      //check whether the account can spend, if not, ask user to select one
      if (_account.canSpend()) {
         // See if we can create the transaction with what we have
         _transactionStatus = tryCreateUnsignedTransaction();
      } else {
         //we need the user to pick a spending account - the activity will then init sendmain correctly
         BitcoinUriWithAddress uri = new BitcoinUriWithAddress(_receivingAddress, _amountToSend, _transactionLabel);
         GetSpendingRecordActivity.callMeWithResult(this, uri, REQUEST_PICK_ACCOUNT);
         //no matter whether the user did successfully send or tapped back - we do not want to stay here with a wrong account selected
         finish();
      }

      // SEPA transfer for if cashila account is paired
      if (_mbwManager.isWalletPaired(ExternalService.CASHILA)) {
         findViewById(R.id.btSepaTransfer).setVisibility(View.VISIBLE);
         findViewById(R.id.btSepaTransfer).setOnClickListener(sepaClickListener);
      } else {
         findViewById(R.id.btSepaTransfer).setVisibility(View.GONE);
      }

      _sepaPayment = (BillPay) getIntent().getSerializableExtra(SEPA_PAYMENT);
      if (_sepaPayment != null){
         showSepaInfo(_sepaPayment);
      }

      // lets see if we got a raw Paymentreuest (probably by downloading a file with MIME application/bitcoin-paymentrequest)
      byte[] _rawPr = getIntent().getByteArrayExtra(RAW_PAYMENT_REQUEST);
      if (_rawPr != null && _paymentRequestHandler == null) {
         verifyPaymentRequest(_rawPr);
      }

      //lets check whether we got a payment request uri and need to fetch payment data
      if (_bitcoinUri != null && !Strings.isNullOrEmpty(_bitcoinUri.callbackURL) && _paymentRequestHandler == null) {
         verifyPaymentRequest(_bitcoinUri);
      }

      // Scan
      findViewById(R.id.btScan).setOnClickListener(scanClickListener);

      // Address Book
      findViewById(R.id.btAddressBook).setOnClickListener(addressBookClickListener);

      // Manual Entry
      findViewById(R.id.btManualEntry).setOnClickListener(manualEntryClickListener);

      // Clipboard
      findViewById(R.id.btClipboard).setOnClickListener(clipboardClickListener);

      // Enter Amount
      findViewById(R.id.btEnterAmount).setOnClickListener(amountClickListener);

      // Change miner fee
      findViewById(R.id.btFeeLvl).setOnClickListener(feeClickListener);

      // Send button
      findViewById(R.id.btSend).setOnClickListener(sendClickListener);

      // Amount Hint
      ((TextView) findViewById(R.id.tvAmount)).setHint(getResources().getString(R.string.amount_hint_denomination,
            _mbwManager.getBitcoinDenomination().toString()));

      //unconfirmed spending warning
      findViewById(R.id.tvUnconfirmedWarning).setOnClickListener(unconfirmedClickListener);

   }

   private void verifyPaymentRequest(BitcoinUri uri) {
      Intent intent = VerifyPaymentRequestActivity.getIntent(this, uri);
      startActivityForResult(intent, REQUEST_PAYMENT_HANDLER);
   }

   private void verifyPaymentRequest(byte[] rawPr) {
      Intent intent = VerifyPaymentRequestActivity.getIntent(this, rawPr);
      startActivityForResult(intent, REQUEST_PAYMENT_HANDLER);
   }

   private void showSepaInfo(BillPay sepaPayment) {
      // show the sepa information, instead of the Btc Address
      ViewGroup parent = (ViewGroup) findViewById(R.id.tvReceiver).getParent();
      findViewById(R.id.tvReceiver).setVisibility(View.GONE);
      View view = getLayoutInflater().inflate(R.layout.ext_cashila_sepa_info, parent, true);

      ((TextView)view.findViewById(R.id.tvName)).setText(sepaPayment.recipient.name);
      ((TextView)view.findViewById(R.id.tvSepaAmount)).setText(
            Utils.formatFiatValueAsString(sepaPayment.payment.amount) + " " + sepaPayment.payment.currency);

      ((TextView)view.findViewById(R.id.tvSepaFee)).setText(getResources().getString(R.string.cashila_fee,
            Utils.formatFiatValueAsString(sepaPayment.details.fee) + " " + sepaPayment.payment.currency));

      ((TextView)view.findViewById(R.id.tvIban)).setText(sepaPayment.payment.iban);
      ((TextView)view.findViewById(R.id.tvBic)).setText(sepaPayment.payment.bic);
      ((TextView)view.findViewById(R.id.tvBtcAddress)).setText(String.format("(%s)", sepaPayment.details.address.toString()));

      // hide the button to change the amount
      findViewById(R.id.btEnterAmount).setVisibility(View.GONE);
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable(AMOUNT_TO_SEND, _amountToSend);
      savedInstanceState.putSerializable(RECEIVING_ADDRESS, _receivingAddress);
      savedInstanceState.putString(TRANSACTION_LABEL, _transactionLabel);
      savedInstanceState.putString(FEE_LVL, _fee.tag);
      savedInstanceState.putBoolean(PAYMENT_FETCHED, _paymentFetched);
      savedInstanceState.putSerializable(BITCOIN_URI, _bitcoinUri);
      savedInstanceState.putSerializable(PAYMENT_REQUEST_HANDLER_ID, _paymentRequestHandlerUuid);
      savedInstanceState.putSerializable(SIGNED_TRANSACTION, _signedTransaction);
   }



   private OnClickListener scanClickListener = new OnClickListener() {
      @Override
      public void onClick(View arg0) {
         ScanActivity.callMe(SendMainActivity.this, SCAN_RESULT_CODE, StringHandleConfig.returnKeyOrAddressOrUriOrKeynode());
      }
   };

   private OnClickListener sepaClickListener = new OnClickListener() {
      @Override
      public void onClick(View arg0) {
         _mbwManager.getVersionManager().showFeatureWarningIfNeeded(SendMainActivity.this, Feature.CASHILA, true, new Runnable() {
            @Override
            public void run() {
               startActivity(CashilaPaymentsActivity.getIntent(SendMainActivity.this));
               finish();
            }
         });
      }
   };

   private OnClickListener addressBookClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         Intent intent = new Intent(SendMainActivity.this, GetFromAddressBookActivity.class);
         startActivityForResult(intent, ADDRESS_BOOK_RESULT_CODE);
      }
   };

   private OnClickListener manualEntryClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         Intent intent = new Intent(SendMainActivity.this, ManualAddressEntry.class);
         startActivityForResult(intent, MANUAL_ENTRY_RESULT_CODE);
      }
   };

   private OnClickListener clipboardClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         BitcoinUriWithAddress uri = getUriFromClipboard();
         if (uri != null) {
            Toast.makeText(SendMainActivity.this, getResources().getString(R.string.using_address_from_clipboard),
                  Toast.LENGTH_SHORT).show();
            _receivingAddress = uri.address;
            if (uri.amount != null) {
               _amountToSend = uri.amount;
            }
            _transactionStatus = tryCreateUnsignedTransaction();
            updateUi();
         }
      }
   };

   private OnClickListener amountClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         GetAmountActivity.callMe(SendMainActivity.this, GET_AMOUNT_RESULT_CODE, _account.getId(), _amountToSend, getFeePerKb().getLongValue(), _isColdStorage);
      }
   };

   private Bitcoins getFeePerKb() {
      return _fee.getFeePerKb(_mbwManager.getWalletManager(_isColdStorage).getLastFeeEstimations());
   }

   private OnClickListener sendClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         if (_isColdStorage || _account instanceof Bip44AccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            signTransaction();
         } else {
            _mbwManager.runPinProtectedFunction(SendMainActivity.this, pinProtectedSignAndSend);
         }
      }
   };

   private OnClickListener unconfirmedClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         new AlertDialog.Builder(SendMainActivity.this)
               .setTitle(getString(R.string.spending_unconfirmed_title))
               .setMessage(getString(R.string.spending_unconfirmed_description))
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                     // continue
                  }
               })
               .setIcon(android.R.drawable.ic_dialog_alert)
               .show();
      }
   };

   private OnClickListener feeClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         _fee = _fee.getNext();
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
         //warn user if minimum fee is selected
         if (_fee == MinerFee.ECONOMIC || _fee == MinerFee.LOWPRIO) {
            Toast.makeText(SendMainActivity.this, getString(R.string.toast_warning_low_fee), Toast.LENGTH_SHORT).show();
         }
      }
   };

   private TransactionStatus tryCreateUnsignedTransaction() {
      _unsigned = null;

      if (_paymentRequestHandler == null && (_amountToSend == null || _receivingAddress == null)) {
         return TransactionStatus.MissingArguments;
      }

      // Create the unsigned transaction
      try {
         if (_paymentRequestHandler == null || !_paymentRequestHandler.hasValidPaymentRequest()) {
            WalletAccount.Receiver receiver = new WalletAccount.Receiver(_receivingAddress, _amountToSend);
            _unsigned = _account.createUnsignedTransaction(Arrays.asList(receiver), getFeePerKb().getLongValue());
         checkSpendingUnconfirmed();
            return TransactionStatus.OK;
         } else {
            PaymentRequestInformation paymentRequestInformation = _paymentRequestHandler.getPaymentRequestInformation();
            OutputList outputs = paymentRequestInformation.getOutputs();

            // has the payment request an amount set?
            if (paymentRequestInformation.hasAmount()) {
               _amountToSend = paymentRequestInformation.getOutputs().getTotalAmount();
            } else {
               if (_amountToSend ==  null || _amountToSend == 0){
                  return TransactionStatus.MissingArguments;
               }

               // build new output list with user specified amount
               outputs = outputs.newOutputsWithTotalAmount(_amountToSend);
            }
            _unsigned = _account.createUnsignedTransaction(outputs, getFeePerKb().getLongValue());
            _receivingAddress = null;
            _transactionLabel = paymentRequestInformation.getPaymentDetails().memo;
            return TransactionStatus.OK;
         }
      } catch (InsufficientFundsException e) {
         Toast.makeText(this, getResources().getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
         return TransactionStatus.InsufficientFunds;
      } catch (OutputTooSmallException e1) {
         Toast.makeText(this, getResources().getString(R.string.amount_too_small), Toast.LENGTH_LONG).show();
         return TransactionStatus.OutputTooSmall;
      }
   }

   private void checkSpendingUnconfirmed() {
      for (UnspentTransactionOutput out : _unsigned.getFundingOutputs()) {
         Address address = out.script.getAddress(_mbwManager.getNetwork());
         if (out.height == -1 && _account.isOwnExternalAddress(address)) {
            // this is an unconfirmed output from an external address -> we want to warn the user
            // we allow unconfirmed spending of internal (=change addresses) without warning
            _spendingUnconfirmed = true;
            return;
         }
      }
      //no unconfirmed outputs are used as inputs, we are fine
      _spendingUnconfirmed  = false;
   }

   private void updateUi() {
      updateRecipient();
      updateAmount();

      // Enable/disable send button
      findViewById(R.id.btSend).setEnabled(_transactionStatus == TransactionStatus.OK);
      findViewById(R.id.root).invalidate();

   }

   private void updateRecipient() {
      boolean hasPaymentRequest = _paymentRequestHandler != null && _paymentRequestHandler.hasValidPaymentRequest();
      if (_receivingAddress == null && !hasPaymentRequest) {
         // Hide address, show "Enter"
         ((TextView) findViewById(R.id.tvRecipientTitle)).setText(R.string.enter_recipient_title);
         findViewById(R.id.llEnterRecipient).setVisibility(View.VISIBLE);
         findViewById(R.id.llRecipientAddress).setVisibility(View.GONE);
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
         return;
      }
      // Hide "Enter", show address
      ((TextView) findViewById(R.id.tvRecipientTitle)).setText(R.string.recipient_title);
      findViewById(R.id.llRecipientAddress).setVisibility(View.VISIBLE);
      findViewById(R.id.llEnterRecipient).setVisibility(View.GONE);

      // Set address label if applicable
      TextView receiverLabel = (TextView) findViewById(R.id.tvReceiverLabel);

      // See if the address is in the address book or one of our accounts
      String label = null;
      if (_receivingAddress!=null) {
         label = getAddressLabel(_receivingAddress);
      }

      if (label == null || label.length() == 0) {
         // Hide label
         receiverLabel.setVisibility(View.GONE);
      } else {
         // Show label
         receiverLabel.setText(label);
         receiverLabel.setVisibility(View.VISIBLE);
      }

      // Set Address
      if (_sepaPayment == null && !hasPaymentRequest) {
         String choppedAddress = _receivingAddress.toMultiLineString();
         ((TextView) findViewById(R.id.tvReceiver)).setText(choppedAddress);
      }

      if (hasPaymentRequest){
         PaymentRequestInformation paymentRequestInformation = _paymentRequestHandler.getPaymentRequestInformation();
         if (paymentRequestInformation.hasValidSignature()) {
            ((TextView) findViewById(R.id.tvReceiver)).setText(paymentRequestInformation.getPkiVerificationData().displayName);
         } else {
            ((TextView) findViewById(R.id.tvReceiver)).setText(getString(R.string.label_unverified_recipient));
         }

      }

      //Check the wallet manager to see whether its our own address, and whether we can spend from it
      WalletManager walletManager = _mbwManager.getWalletManager(false);
      if (_receivingAddress != null && walletManager.isMyAddress(_receivingAddress)) {
         TextView tvWarning = (TextView) findViewById(R.id.tvWarning);
         if (walletManager.hasPrivateKeyForAddress(_receivingAddress)) {
            // Show a warning as we are sending to one of our own addresses
            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText(R.string.my_own_address_warning);
            tvWarning.setTextColor(getResources().getColor(R.color.yellow));
         } else {
            // Show a warning as we are sending to one of our own addresses,
            // which is read-only
            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText(R.string.read_only_warning);
            tvWarning.setTextColor(getResources().getColor(R.color.red));
         }

      } else {
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
      }

      //if present, show transaction label
      if (_transactionLabel != null) {
         findViewById(R.id.tvTransactionLabelTitle).setVisibility(View.VISIBLE);
         findViewById(R.id.tvTransactionLabel).setVisibility(View.VISIBLE);
         ((TextView) findViewById(R.id.tvTransactionLabel)).setText(_transactionLabel);
      } else {
         findViewById(R.id.tvTransactionLabelTitle).setVisibility(View.GONE);
         findViewById(R.id.tvTransactionLabel).setVisibility(View.GONE);
      }
   }

   private String getAddressLabel(Address address) {
      Optional<UUID> accountId = _mbwManager.getWalletManager(false).getAccountByAddress(address);
      if (!accountId.isPresent()) {
         // We don't have it in our accounts, look in address book, returns empty string by default
         return _mbwManager.getMetadataStorage().getLabelByAddress(address);
      }
      // Get the name of the account
      return _mbwManager.getMetadataStorage().getLabelByAccount(accountId.get());
   }

   private void updateAmount() {
      // Update Amount
      if (_amountToSend == null) {
         // No amount to show
         ((TextView) findViewById(R.id.tvAmountTitle)).setText(R.string.enter_amount_title);
         ((TextView) findViewById(R.id.tvAmount)).setText("");
         findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
         findViewById(R.id.tvError).setVisibility(View.GONE);
      } else {
         ((TextView) findViewById(R.id.tvAmountTitle)).setText(R.string.amount_title);
         if (_transactionStatus == TransactionStatus.OutputTooSmall) {
            // Amount too small
            ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amountToSend));
            findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvError)).setText(R.string.amount_too_small_short);
            findViewById(R.id.tvError).setVisibility(View.VISIBLE);
         } else if (_transactionStatus == TransactionStatus.InsufficientFunds) {
            // Insufficient funds
            ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amountToSend));
            ((TextView) findViewById(R.id.tvError)).setText(R.string.insufficient_funds);
            findViewById(R.id.tvError).setVisibility(View.VISIBLE);
         } else {
            // Set Amount
            ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amountToSend));
            if (!_mbwManager.hasFiatCurrency() || _oneBtcInFiat == null) {
               findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
            } else {
               // Set approximate amount in fiat
               TextView tvAmountFiat = ((TextView) findViewById(R.id.tvAmountFiat));
               tvAmountFiat.setText(getFiatValue(_amountToSend, _oneBtcInFiat));
               tvAmountFiat.setVisibility(View.VISIBLE);
            }
            findViewById(R.id.tvError).setVisibility(View.GONE);
            //check if we need to warn the user about unconfirmed funds
            if (_spendingUnconfirmed) {
               findViewById(R.id.tvUnconfirmedWarning).setVisibility(View.VISIBLE);
            } else {
               findViewById(R.id.tvUnconfirmedWarning).setVisibility(View.GONE);
            }
         }
      }

      // Disable Amount button if we have a payment request with valid amount
      if (_paymentRequestHandler != null && _paymentRequestHandler.getPaymentRequestInformation().hasAmount()) {
         findViewById(R.id.btEnterAmount).setEnabled(false);
      }


      // Update Fee-Display
      TextView btFeeLvl = (Button) findViewById(R.id.btFeeLvl);
      if (_unsigned == null) {
         // Only show button for fee lvl, cannot calculate fee yet
         ((Button) findViewById(R.id.btFeeLvl)).setText(_fee.getMinerFeeName(this));
         findViewById(R.id.tvFeeValue).setVisibility(View.INVISIBLE);
      } else {
         // Show fee fully calculated
         btFeeLvl.setVisibility(View.VISIBLE);

         long fee = _unsigned.calculateFee();

         //show fee lvl on button - always show the fees in mBtc
         CoinUtil.Denomination feeDenomination = CoinUtil.Denomination.mBTC;
         String feeString = CoinUtil.valueString(fee, feeDenomination, true) +  " " + feeDenomination.getUnicodeName();
         ((Button) findViewById(R.id.btFeeLvl)).setText(_fee.getMinerFeeName(this));

         if (!_mbwManager.hasFiatCurrency() || _oneBtcInFiat == null) {
         } else {
            // Set approximate fee in fiat
            feeString += ", " + getFiatValue(fee, _oneBtcInFiat);
         }

         TextView tvFeeFiat = ((TextView) findViewById(R.id.tvFeeValue));
         tvFeeFiat.setVisibility(View.VISIBLE);
         tvFeeFiat.setText("(" + feeString + ")");

      }

   }

   private String getFiatValue(long satoshis, Double oneBtcInFiat) {
      String currency = _mbwManager.getFiatCurrency();
      String converted = Utils.getFiatValueAsString(satoshis, oneBtcInFiat);
      return getResources().getString(R.string.approximate_fiat_value, currency, converted);
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);

      // If we don't have a fresh exchange rate, now is a good time to request one, as we will need it in a minute
      _oneBtcInFiat = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      if (_oneBtcInFiat == null) {
         _mbwManager.getExchangeRateManager().requestRefresh();
      }

      findViewById(R.id.btClipboard).setEnabled(getUriFromClipboard() != null);
      findViewById(R.id.pbSend).setVisibility(View.GONE);

      updateUi();
      super.onResume();
   }

   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   final Runnable pinProtectedSignAndSend = new Runnable() {

      @Override
      public void run() {
         signTransaction();
      }
   };

   protected void signTransaction() {
      // if we have a payment request, check if it is expired
      if (_paymentRequestHandler != null){
         if (_paymentRequestHandler.getPaymentRequestInformation().isExpired()){
            Toast.makeText(SendMainActivity.this, getString(R.string.payment_request_not_sent_expired),
                  Toast.LENGTH_LONG).show();
            return;
         }
      }

      disableButtons();
      SignTransactionActivity.callMe(this, _account.getId(), _isColdStorage, _unsigned, SIGN_TRANSACTION_REQUEST_CODE);
   }

   protected void disableButtons() {
      findViewById(R.id.pbSend).setVisibility(View.VISIBLE);
      findViewById(R.id.btSend).setEnabled(false);
      findViewById(R.id.btAddressBook).setEnabled(false);
      findViewById(R.id.btManualEntry).setEnabled(false);
      findViewById(R.id.btClipboard).setEnabled(false);
      findViewById(R.id.btScan).setEnabled(false);
      findViewById(R.id.btEnterAmount).setEnabled(false);
   }


   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_RESULT_CODE) {
         if (resultCode != RESULT_OK) {
            if (intent != null) {
               String error = intent.getStringExtra(StringHandlerActivity.RESULT_ERROR);
               if (error != null) {
                  Toast.makeText(this, error, Toast.LENGTH_LONG).show();
               }
            }
         } else {
            StringHandlerActivity.ResultType type = (StringHandlerActivity.ResultType) intent.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY);
            if (type == StringHandlerActivity.ResultType.PRIVATE_KEY) {
               InMemoryPrivateKey key = StringHandlerActivity.getPrivateKey(intent);
               _receivingAddress = key.getPublicKey().toAddress(_mbwManager.getNetwork());
            } else if (type == StringHandlerActivity.ResultType.ADDRESS) {
               _receivingAddress = StringHandlerActivity.getAddress(intent);
            } else if (type == StringHandlerActivity.ResultType.URI_WITH_ADDRESS) {
               BitcoinUriWithAddress uri = StringHandlerActivity.getUriWithAddress(intent);
               if (uri.callbackURL != null) {
                  //we contact the merchant server instead of using the params
                  _bitcoinUri = uri;
                  _paymentFetched = false;
                  verifyPaymentRequest(_bitcoinUri);
                  return;
               }
               _receivingAddress = uri.address;
               _transactionLabel = uri.label;
               if (uri.amount != null) {
                  //we set the amount to the one contained in the qr code, even if another one was entered previously
                  if (_amountToSend != null && _amountToSend > 0) {
                     Toast.makeText(this, R.string.amount_changed, Toast.LENGTH_LONG).show();
                  }
                  _amountToSend = uri.amount;
               }
            } else if (type == StringHandlerActivity.ResultType.URI) {
               //todo: maybe merge with BitcoinUriWithAddress ?
               BitcoinUri uri = StringHandlerActivity.getUri(intent);
               if (uri.callbackURL != null) {
                  //we contact the merchant server instead of using the params
                  _bitcoinUri = uri;
                  _paymentFetched = false;
                  verifyPaymentRequest(_bitcoinUri);
                  return;
               }
            } else if (type == StringHandlerActivity.ResultType.HD_NODE) {
               setReceivingAddressFromKeynode(StringHandlerActivity.getHdKeyNode(intent));
            } else {
               throw new IllegalStateException("Unexpected result type from scan: " + type.toString());
            }

         }

         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
      } else if (requestCode == ADDRESS_BOOK_RESULT_CODE && resultCode == RESULT_OK) {
         // Get result from address chooser
         String s = Preconditions.checkNotNull(intent.getStringExtra(AddressBookFragment.ADDRESS_RESULT_NAME));
         String result = s.trim();
         // Is it really an address?
         Address address = Address.fromString(result, _mbwManager.getNetwork());
         if (address == null) {
            return;
         }
         _receivingAddress = address;

         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
      } else if (requestCode == MANUAL_ENTRY_RESULT_CODE && resultCode == RESULT_OK) {
         Address address = Preconditions.checkNotNull((Address) intent
               .getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME));
         _receivingAddress = address;

         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
      } else if (requestCode == GET_AMOUNT_RESULT_CODE && resultCode == RESULT_OK) {
         // Get result from address chooser
         _amountToSend = Preconditions.checkNotNull((Long) intent.getSerializableExtra("amount"));
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
      } else if (requestCode == SIGN_TRANSACTION_REQUEST_CODE){
         if (resultCode == RESULT_OK) {
            _signedTransaction = (Transaction) Preconditions.checkNotNull(intent.getSerializableExtra("signedTx"));

            // if we have a payment request with a payment_url, handle the send differently:
            if (_paymentRequestHandler != null
                  && _paymentRequestHandler.getPaymentRequestInformation().hasPaymentCallbackUrl()) {

               // check again if the payment request isn't expired, as signing might have taken some time
               // (e.g. with external signature provider)
               if (!_paymentRequestHandler.getPaymentRequestInformation().isExpired()) {
                  // first send signed tx directly to the Merchant, and broadcast
                  // it only if we get a ACK from him (in paymentRequestAck)
                  _paymentRequestHandler.sendResponse(_signedTransaction, _account.getReceivingAddress());
               } else {
                  Toast.makeText(SendMainActivity.this, getString(R.string.payment_request_not_sent_expired),
                        Toast.LENGTH_LONG).show();

               }
            } else {
               BroadcastTransactionActivity.callMe(this, _account.getId(), _isColdStorage, _signedTransaction, _transactionLabel, BROADCAST_REQUEST_CODE);
            }
         }
      } else if (requestCode == BROADCAST_REQUEST_CODE) {
         // return result from broadcast
         this.setResult(resultCode, intent);
         finish();
      } else if (requestCode == REQUEST_PAYMENT_HANDLER) {
         if (resultCode == RESULT_OK){
            _paymentRequestHandlerUuid = Preconditions.checkNotNull(intent.getStringExtra("REQUEST_PAYMENT_HANDLER_ID"));
            if (_paymentRequestHandlerUuid != null) {
               _paymentRequestHandler = (PaymentRequestHandler) _mbwManager.getBackgroundObjectsCache()
                     .getIfPresent(_paymentRequestHandlerUuid);
            } else {
               _paymentRequestHandler = null;
            }
            _transactionStatus = tryCreateUnsignedTransaction();
            updateUi();
         } else {
            // user canceled - also leave this activity
            setResult(RESULT_CANCELED);
            finish();
         }
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }



   private void setReceivingAddressFromKeynode(HdKeyNode hdKeyNode) {
      _progress = ProgressDialog.show(this, "", getString(R.string.retrieving_pubkey_address), true);
      _receivingAcc = _mbwManager.getWalletManager(true).createUnrelatedBip44Account(hdKeyNode);
      _xpubSyncing = true;
      _mbwManager.getWalletManager(true).startSynchronization();
   }


   private BitcoinUriWithAddress getUriFromClipboard() {
      String content = Utils.getClipboardString(SendMainActivity.this);
      if (content.length() == 0) {
         return null;
      }
      String string = content.trim();
      if (string.matches("[a-zA-Z0-9]*")) {
         // Raw format
         Address address = Address.fromString(string, _mbwManager.getNetwork());
         if (address == null) {
            return null;
         }
         return new BitcoinUriWithAddress(address, null, null);
      } else {
         Optional<BitcoinUriWithAddress> b = BitcoinUriWithAddress.parseWithAddress(string, _mbwManager.getNetwork());
         if (b.isPresent()) {
            // On URI format
            return b.get();
         }
      }
      return null;
   }

   @Subscribe
   public void paymentRequestException(PaymentRequestException ex){
      //todo: maybe hint the user, that the merchant might broadcast the transaction later anyhow
      // and we should move funds to a new address to circumvent it
      Utils.showSimpleMessageDialog(this,
            String.format(getString(R.string.payment_request_error_while_getting_ack), ex.getMessage()));
   }


   @Subscribe
   public void paymentRequestAck(PaymentACK paymentACK){
      if (paymentACK != null){
         BroadcastTransactionActivity.callMe(this, _account.getId(), _isColdStorage, _signedTransaction, _transactionLabel, BROADCAST_REQUEST_CODE);
      }
   }

   @Subscribe
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
      _oneBtcInFiat = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      updateUi();
   }

   @Subscribe
   public void selectedCurrencyChanged(SelectedCurrencyChanged event) {
      _oneBtcInFiat = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      updateUi();
   }

   @Subscribe
   public void syncFinished(SyncStopped event) {
      if (_xpubSyncing) {
         _xpubSyncing = false;
         _receivingAddress = _mbwManager.getWalletManager(true).getAccount(_receivingAcc).getReceivingAddress();
         if (_progress != null) _progress.dismiss();
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
      }
   }

   @Subscribe
   public void syncFailed(SyncFailed event) {
      if (_progress != null) _progress.dismiss();
      //todo: warn the user about address reuse for xpub
   }

}