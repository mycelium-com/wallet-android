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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.TransactionUtils;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mycelium.paymentrequest.PaymentRequestException;
import com.mycelium.paymentrequest.PaymentRequestInformation;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.BitcoinUriWithAddress;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.R;
import com.mycelium.wallet.RmcUri;
import com.mycelium.wallet.StringHandleConfig;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.GetAmountActivity;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.modern.AddressBookFragment;
import com.mycelium.wallet.activity.modern.GetFromAddressBookActivity;
import com.mycelium.wallet.coinapult.CoinapultAccount;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.colu.ColuCurrencyValue;
import com.mycelium.wallet.colu.ColuManager;
import com.mycelium.wallet.colu.ColuTransactionData;
import com.mycelium.wallet.colu.json.ColuBroadcastTxid;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44AccountExternalSignature;
import com.mycelium.wapi.wallet.currency.BitcoinValue;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.currency.ExchangeBasedBitcoinValue;
import com.squareup.otto.Subscribe;

import org.bitcoin.protocols.payments.PaymentACK;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.mrd.bitlib.StandardTransactionBuilder.estimateTransactionSize;
import static com.mrd.bitlib.util.CoinUtil.Denomination.BTC;
import static com.mrd.bitlib.util.CoinUtil.Denomination.mBTC;

public class SendMainActivity extends Activity {

    private static final String TAG = "SendMainActivity";

    private static final int GET_AMOUNT_RESULT_CODE = 1;
    private static final int SCAN_RESULT_CODE = 2;
    private static final int ADDRESS_BOOK_RESULT_CODE = 3;
    private static final int MANUAL_ENTRY_RESULT_CODE = 4;
    private static final int REQUEST_PICK_ACCOUNT = 5;
    protected static final int SIGN_TRANSACTION_REQUEST_CODE = 6;
    private static final int BROADCAST_REQUEST_CODE = 7;
    private static final int REQUEST_PAYMENT_HANDLER = 8;
    private static final int REQUET_BTC_ACCOUNT = 9;
    public static final String RAW_PAYMENT_REQUEST = "rawPaymentRequest";

    public static final String ACCOUNT = "account";
    private static final String AMOUNT = "amount";
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
    private static final String RMC_URI = "rmcUri";
    public static final String NO_MY_ACCOUNTS = "no_my_accounts";

    private enum TransactionStatus {
        MissingArguments, OutputTooSmall, InsufficientFunds, OK
    }

    @BindView(R.id.tvAmount)
    TextView tvAmount;
    @BindView(R.id.tvError)
    TextView tvError;
    @BindView(R.id.tvAmountFiat)
    TextView tvAmountFiat;
    @BindView(R.id.tvAmountTitle)
    TextView tvAmountTitle;
    @BindView(R.id.tvUnconfirmedWarning)
    TextView tvUnconfirmedWarning;
    @BindView(R.id.tvReceiver)
    TextView tvReceiver;
    @BindView(R.id.tvRecipientTitle)
    TextView tvRecipientTitle;
    @BindView(R.id.tvWarning)
    TextView tvWarning;
    @BindView(R.id.tvReceiverLabel)
    TextView tvReceiverLabel;
    @BindView(R.id.tvReceiverAddress)
    TextView tvReceiverAddress;
    @BindView(R.id.tvTransactionLabelTitle)
    TextView tvTransactionLabelTitle;
    @BindView(R.id.tvTransactionLabel)
    TextView tvTransactionLabel;
    @BindView(R.id.tvFeeValue)
    TextView tvFeeValue;
    @BindView(R.id.tvSatFeeValue)
    TextView tvSatFeeValue;
    @BindView(R.id.btEnterAmount)
    ImageButton btEnterAmount;
    @BindView(R.id.btFeeLvl)
    Button btFeeLvl;
    @BindView(R.id.btClipboard)
    Button btClipboard;
    @BindView(R.id.btSend)
    Button btSend;
    @BindView(R.id.btAddressBook)
    Button btAddressBook;
    @BindView(R.id.btManualEntry)
    Button btManualEntry;
    @BindView(R.id.btScan)
    Button btScan;
    @BindView(R.id.pbSend)
    ProgressBar pbSend;
    @BindView(R.id.llFee)
    LinearLayout llFee;
    @BindView(R.id.llEnterRecipient)
    LinearLayout llEnterRecipient;
    @BindView(R.id.llRecipientAddress)
    LinearLayout llRecipientAddress;
    @BindView(R.id.btFromBtcAccount)
    Button btFeeFromAccount;
    @BindView(R.id.colu_tips_check_address)
    View tips_check_address;
    private MbwManager _mbwManager;

    private PaymentRequestHandler _paymentRequestHandler;
    private String _paymentRequestHandlerUuid;

    protected WalletAccount _account;
    private CurrencyValue _amountToSend;
    private BitcoinValue _lastBitcoinAmountToSend = null;
    private Address _receivingAddress;
    private String _receivingLabel;
    protected String _transactionLabel;
    private BitcoinUri _bitcoinUri;
    private RmcUri _rmcUri;
    protected boolean _isColdStorage;
    private TransactionStatus _transactionStatus;
    protected UnsignedTransaction _unsigned;
    protected CoinapultAccount.PreparedCoinapult _preparedCoinapult;
    protected ColuBroadcastTxid.Json _preparedColuTx;
    private Transaction _signedTransaction;
    private MinerFee _fee;
    private ProgressDialog _progress;
    private UUID _receivingAcc;
    private boolean _xpubSyncing = false;
    private boolean _spendingUnconfirmed = false;
    private boolean _paymentFetched = false;
    private WalletAccount feeColuAccount;
    private ProgressDialog progress;

    public static Intent getIntent(Activity currentActivity, UUID account, boolean isColdStorage) {
        return new Intent(currentActivity, SendMainActivity.class)
                .putExtra(ACCOUNT, account)
                .putExtra(IS_COLD_STORAGE, isColdStorage);
    }

    public static Intent getIntent(Activity currentActivity, UUID account,
                                   Long amountToSend, Address receivingAddress, boolean isColdStorage) {
        return getIntent(currentActivity, account, isColdStorage)
                .putExtra(AMOUNT, ExactBitcoinValue.from(amountToSend))
                .putExtra(RECEIVING_ADDRESS, receivingAddress);
    }

   public static Intent getIntent(Activity currentActivity, UUID account, HdKeyNode hdKey) {
      return getIntent(currentActivity, account, false)
              .putExtra(HD_KEY, hdKey);
   }

    public static Intent getIntent(Activity currentActivity, UUID account, BitcoinUri uri, boolean isColdStorage) {
        return getIntent(currentActivity, account, uri.amount, uri.address, isColdStorage)
                .putExtra(TRANSACTION_LABEL, uri.label)
                .putExtra(BITCOIN_URI, uri);
    }

    public static Intent getIntent(Activity currentActivity, UUID account, RmcUri uri, boolean isColdStorage) {
        return getIntent(currentActivity, account, isColdStorage)
                .putExtra(AMOUNT, new ColuCurrencyValue(uri.amount, "RMC"))
                .putExtra(RECEIVING_ADDRESS, uri.address)
                .putExtra(TRANSACTION_LABEL, uri.label)
                .putExtra(RMC_URI, uri);
    }

    public static Intent getIntent(Activity currentActivity, UUID account, byte[] rawPaymentRequest, boolean isColdStorage) {
        return getIntent(currentActivity, account, isColdStorage)
                .putExtra(RAW_PAYMENT_REQUEST, rawPaymentRequest);
    }

    private boolean isCoinapult() {
        return _account != null && _account instanceof CoinapultAccount;
    }

    private boolean isColu() {
        return _account != null && _account instanceof ColuAccount;
    }

   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      // TODO: profile. slow!
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.send_main_activity);
      ButterKnife.bind(this);
      _mbwManager = MbwManager.getInstance(getApplication());

        // Get intent parameters
        UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra(ACCOUNT));

        // May be null
        setAmountToSend((CurrencyValue) getIntent().getSerializableExtra(AMOUNT));
        // May be null
        _receivingAddress = (Address) getIntent().getSerializableExtra(RECEIVING_ADDRESS);
        //May be null
        _transactionLabel = getIntent().getStringExtra(TRANSACTION_LABEL);
        //May be null
        _bitcoinUri = (BitcoinUri) getIntent().getSerializableExtra(BITCOIN_URI);
        //May be null
        _rmcUri = (RmcUri) getIntent().getSerializableExtra(RMC_URI);

        // did we get a raw payment request
        byte[] _rawPr = getIntent().getByteArrayExtra(RAW_PAYMENT_REQUEST);

        _isColdStorage = getIntent().getBooleanExtra(IS_COLD_STORAGE, false);
        _account = _mbwManager.getWalletManager(_isColdStorage).getAccount(accountId);
        _fee = _mbwManager.getMinerFee();

        // Load saved state, overwriting amount and address
        if (savedInstanceState != null) {
            setAmountToSend((CurrencyValue) savedInstanceState.getSerializable(AMOUNT));
            _receivingAddress = (Address) savedInstanceState.getSerializable(RECEIVING_ADDRESS);
            _transactionLabel = savedInstanceState.getString(TRANSACTION_LABEL);
            _fee = MinerFee.fromString(savedInstanceState.getString(FEE_LVL));
            _bitcoinUri = (BitcoinUri) savedInstanceState.getSerializable(BITCOIN_URI);
            _rmcUri = (RmcUri) savedInstanceState.getSerializable(RMC_URI);
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
         if (hdKey != null) {
            setReceivingAddressFromKeynode(hdKey);
         }
      }

        // check whether the account can spend, if not, ask user to select one
        if (_account.canSpend()) {
            // See if we can create the transaction with what we have
            _transactionStatus = tryCreateUnsignedTransaction();
        } else {
            //we need the user to pick a spending account - the activity will then init sendmain correctly
            BitcoinUri uri;
            if (_bitcoinUri == null) {
                uri = BitcoinUri.from(_receivingAddress, getBitcoinValueToSend() == null ? null : getBitcoinValueToSend().getLongValue(), _transactionLabel, null);
            } else {
                uri = _bitcoinUri;
            }

            if (_rawPr != null) {
                GetSpendingRecordActivity.callMeWithResult(this, _rawPr, REQUEST_PICK_ACCOUNT);
            } else {
                GetSpendingRecordActivity.callMeWithResult(this, uri, REQUEST_PICK_ACCOUNT);
            }

            //no matter whether the user did successfully send or tapped back - we do not want to stay here with a wrong account selected
            finish();
            return;
        }

      // lets see if we got a raw Payment request (probably by downloading a file with MIME application/bitcoin-paymentrequest)
      if (_rawPr != null && _paymentRequestHandler == null) {
         verifyPaymentRequest(_rawPr);
      }

        // lets check whether we got a payment request uri and need to fetch payment data
        if (_bitcoinUri != null && !Strings.isNullOrEmpty(_bitcoinUri.callbackURL) && _paymentRequestHandler == null) {
            verifyPaymentRequest(_bitcoinUri);
        }

      //Remove Miner fee if coinapult
      if (isCoinapult()) {
         llFee.setVisibility(GONE);
      }

        //TODO: fee from other bitcoin account if colu
        if (isColu()) {
            // no sepa payment with colu
            List<WalletAccount> walletAccountList =_mbwManager.getWalletManager(false).getActiveAccounts();
            walletAccountList = Utils.sortAccounts(walletAccountList, _mbwManager.getMetadataStorage());
            for (WalletAccount walletAccount : walletAccountList) {
                if(walletAccount.canSpend() && !walletAccount.getCurrencyBasedBalance().confirmed.isZero()
                        && walletAccount.getCurrencyBasedBalance().confirmed.isBtc()
                        && walletAccount.getBalance().confirmed > getFeePerKb().getLongValue()) {
                    feeColuAccount = walletAccount;
                    break;
                }
            }

            coluSpendAccount();
        }

        // Amount Hint
        if(_account instanceof ColuAccount){
            ColuAccount coluAccount = (ColuAccount) _account;
            tvAmount.setHint(getResources().getString(R.string.amount_hint_denomination,
                    coluAccount.getColuAsset().name));
            tips_check_address.setVisibility(View.VISIBLE);
        } else  {
            tvAmount.setHint(getResources().getString(R.string.amount_hint_denomination,
                    _mbwManager.getBitcoinDenomination().toString()));
            tips_check_address.setVisibility(View.GONE);
        }
    }

    private void coluSpendAccount() {

        if(feeColuAccount != null) {
            if(checkFee(true))  {
                btFeeFromAccount.setVisibility(View.GONE);
            } else{
                String name = _mbwManager.getMetadataStorage().getLabelByAccount(feeColuAccount.getId());
                Optional<Address> receivingAddress = feeColuAccount.getReceivingAddress();
                if (receivingAddress.isPresent()) {
                    btFeeFromAccount.setText("from " + name + " : " + receivingAddress.get().getShortAddress());
                    btFeeFromAccount.setVisibility(View.VISIBLE);
                }
            }
        } else if(isColu()){
            btFeeFromAccount.setVisibility(View.GONE);
            tvError.setText(R.string.requires_btc_amount);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private boolean checkFee(boolean rescan) {
        if (rescan) {
            ColuManager coluManager = _mbwManager.getColuManager();
            coluManager.scanForAccounts();
        }
        ColuAccount coluAccount = (ColuAccount) _account;

        long fundingAmountToSend = getFeePerKb().getLongValue();
        if (fundingAmountToSend < TransactionUtils.MINIMUM_OUTPUT_VALUE)
            fundingAmountToSend = TransactionUtils.MINIMUM_OUTPUT_VALUE;

        long spendableAmount =  coluAccount.getSatoshiAmount();
        return spendableAmount >= fundingAmountToSend;
    }

    // returns the amcountToSend in Bitcoin - it tries to get it from the entered amount and
   // only uses the ExchangeRate-Manager if we dont have it already converted
   private BitcoinValue getBitcoinValueToSend() {
      if (CurrencyValue.isNullOrZero(_amountToSend)) {
         return null;
      } else if (_amountToSend.getExactValueIfPossible().isBtc()) {
         return (BitcoinValue) _amountToSend.getExactValueIfPossible();
      } else if (_amountToSend.isBtc()) {
         return (BitcoinValue) _amountToSend;
      } else {
         if (_lastBitcoinAmountToSend == null) {
            // only convert once and keep that fx rate for further calls - the cache gets invalidated in setAmountToSend
            _lastBitcoinAmountToSend = (BitcoinValue) ExchangeBasedBitcoinValue.fromValue(_amountToSend, _mbwManager.getExchangeRateManager());
         }
         return _lastBitcoinAmountToSend;
      }
   }

   private void setAmountToSend(CurrencyValue toSend) {
      _amountToSend = toSend;
      _lastBitcoinAmountToSend = null;
   }

   private void verifyPaymentRequest(BitcoinUri uri) {
      Intent intent = VerifyPaymentRequestActivity.getIntent(this, uri);
      startActivityForResult(intent, REQUEST_PAYMENT_HANDLER);
   }

   private void verifyPaymentRequest(byte[] rawPr) {
      Intent intent = VerifyPaymentRequestActivity.getIntent(this, rawPr);
      startActivityForResult(intent, REQUEST_PAYMENT_HANDLER);
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable(AMOUNT, _amountToSend);
      savedInstanceState.putSerializable(RECEIVING_ADDRESS, _receivingAddress);
      savedInstanceState.putString(TRANSACTION_LABEL, _transactionLabel);
      savedInstanceState.putString(FEE_LVL, _fee.tag);
      savedInstanceState.putBoolean(PAYMENT_FETCHED, _paymentFetched);
      savedInstanceState.putSerializable(BITCOIN_URI, _bitcoinUri);
      savedInstanceState.putSerializable(RMC_URI, _rmcUri);
      savedInstanceState.putSerializable(PAYMENT_REQUEST_HANDLER_ID, _paymentRequestHandlerUuid);
      savedInstanceState.putSerializable(SIGNED_TRANSACTION, _signedTransaction);
   }

    @OnClick(R.id.colu_tips_check_address)
    void tipsClick() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.tips_rmc_check_address)
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show();
    }

    @OnClick(R.id.btFromBtcAccount)
    void feeFromAcc() {
        Intent intent = new Intent(this, GetBtcAccountForFeeActivity.class);
        startActivityForResult(intent, REQUET_BTC_ACCOUNT);
    }

    @OnClick(R.id.btScan)
    void onClickScan() {
        StringHandleConfig config = StringHandleConfig.returnKeyOrAddressOrUriOrKeynode();

        WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
        if(account instanceof ColuAccount) {
            config.bitcoinUriAction = StringHandleConfig.BitcoinUriAction.SEND_RMC;
            config.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.SEND_RMC;
        }

        ScanActivity.callMe(this, SCAN_RESULT_CODE, config);
    }

   @OnClick(R.id.btAddressBook)
   void onClickAddressBook() {
       Intent intent = new Intent(this, GetFromAddressBookActivity.class);
       if (isColu()) {
           intent.putExtra(NO_MY_ACCOUNTS, true);
       }
       startActivityForResult(intent, ADDRESS_BOOK_RESULT_CODE);
   }

   @OnClick(R.id.btManualEntry)
   void onClickManualEntry() {
      Intent intent = new Intent(this, ManualAddressEntry.class);
      startActivityForResult(intent, MANUAL_ENTRY_RESULT_CODE);
   }

   @OnClick(R.id.btClipboard)
   void onClickClipboard() {
      BitcoinUriWithAddress uri = getUriFromClipboard();
      if (uri != null) {
         makeText(this, getResources().getString(R.string.using_address_from_clipboard), LENGTH_SHORT).show();
         _receivingAddress = uri.address;
         if (uri.amount != null) {
            _amountToSend = ExactBitcoinValue.from(uri.amount);
         }
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
      }
   }

   @OnClick(R.id.btEnterAmount)
   void onClickAmount() {
      CurrencyValue presetAmount = _amountToSend;
      if (CurrencyValue.isNullOrZero(presetAmount)) {
         // if no amount is set so far, use an unknown amount but in the current accounts currency
         presetAmount = ExactCurrencyValue.from(null, _account.getAccountDefaultCurrency());
      }
      GetAmountActivity.callMe(this, GET_AMOUNT_RESULT_CODE, _account.getId(), presetAmount, getFeePerKb().getLongValue(), _isColdStorage);
   }

   @OnClick(R.id.btSend)
   void onClickSend() {
      if (isCoinapult()) {
         sendCoinapultTransaction();
      } else if (isColu()) {
          sendColuTransaction();
      } else if (_isColdStorage || _account instanceof Bip44AccountExternalSignature) {
         // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
         signTransaction();
      } else {
         _mbwManager.runPinProtectedFunction(this, pinProtectedSignAndSend);
      }
   }

    private void sendColuTransaction() {
        progress = new ProgressDialog(SendMainActivity.this);
        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setMessage(getString(R.string.colu_sending_via_colu));
        progress.show();
        tryCreateUnsignedColuTX(new PrepareCallback() {
            @Override
            public void success() {
                sendColu();
            }

            @Override
            public void fail() {
                progress.dismiss();
            }
        });
    }

    private void sendColu() {
        _mbwManager.getVersionManager().showFeatureWarningIfNeeded(SendMainActivity.this,
                Feature.COLU_PREPARE_OUTGOING_TX, true, new Runnable() {
                    @Override
                    public void run() {
                        _mbwManager.runPinProtectedFunction(SendMainActivity.this, new Runnable() {
                            @Override
                            public void run() {
                                if (_account instanceof ColuAccount) {
                                    Log.d(TAG, "sendColuTransaction account type is ColuAccount");
                                    final ColuAccount coluAccount = (ColuAccount) _account;
                                    final ColuManager coluManager = _mbwManager.getColuManager();
                                    disableButtons();
                                    new AsyncTask<ColuBroadcastTxid.Json, Void, Boolean>() {
                                        @Override
                                        protected Boolean doInBackground(ColuBroadcastTxid.Json... params) {
                                            Log.d(TAG, "In doInBackground: ColuPreparedTransaction");
                                            //UnsignedTransaction unsignedTx = new UnsignedTransaction();
                                            Transaction coluSignedTransaction = coluManager.signTransaction(params[0], coluAccount);
                                            if (coluSignedTransaction != null) {
                                                Log.d(TAG, "broadcasting transaction");
                                                return coluManager.broadcastTransaction(coluSignedTransaction);
                                            } else {
                                                Log.d(TAG, "Failed to sign transaction");
                                                return false;
                                            }
                                        }

                                        @Override
                                        protected void onPostExecute(Boolean aBoolean) {
                                            super.onPostExecute(aBoolean);
                                            progress.dismiss();
                                            if (aBoolean) {
                                                coluManager.startSynchronization();
                                                Toast.makeText(SendMainActivity.this, R.string.colu_succeeded_to_broadcast, Toast.LENGTH_SHORT).show();
                                                SendMainActivity.this.finish();
                                            } else {
                                                Toast.makeText(SendMainActivity.this, R.string.colu_failed_to_broadcast, Toast.LENGTH_SHORT).show();
                                                updateUi();
                                            }
                                        }
                                    }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, _preparedColuTx);
                                }
                            }
                        });
                    }
                });
    }

    private void sendCoinapultTransaction() {
        _mbwManager.getVersionManager().showFeatureWarningIfNeeded(this,
                Feature.COINAPULT_MAKE_OUTGOING_TX, true, new Runnable() {
                    @Override
                    public void run() {
                        _mbwManager.runPinProtectedFunction(SendMainActivity.this, new Runnable() {
                            @Override
                            public void run() {
                                if (_account instanceof CoinapultAccount) {
                                    final ProgressDialog progress = new ProgressDialog(SendMainActivity.this);
                                    progress.setCancelable(false);
                                    progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                    progress.setMessage(getString(R.string.coinapult_sending_via_coinapult));
                                    progress.show();
                                    final CoinapultAccount coinapultManager = (CoinapultAccount) _account;
                                    disableButtons();
                                    new AsyncTask<CoinapultAccount.PreparedCoinapult, Void, Boolean>() {
                                        @Override
                                        protected Boolean doInBackground(CoinapultAccount.PreparedCoinapult... params) {
                                            return coinapultManager.broadcast(params[0]);
                                        }

                              @Override
                              protected void onPostExecute(Boolean aBoolean) {
                                 super.onPostExecute(aBoolean);
                                 progress.dismiss();
                                 if (aBoolean) {
                                    SendMainActivity.this.finish();
                                 } else {
                                    makeText(SendMainActivity.this, R.string.coinapult_failed_to_broadcast, LENGTH_SHORT).show();
                                    updateUi();
                                 }
                              }
                           }.execute(_preparedCoinapult);
                        }
                     }
                  });
               }
            });
   }

    @OnClick(R.id.tvUnconfirmedWarning)
    void onClickUnconfirmedWarning() {
        new AlertDialog.Builder(this)
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

   @OnClick(R.id.btFeeLvl)
   void onClickFeeLevel() {
      _fee = _fee.getNext();
      _transactionStatus = tryCreateUnsignedTransaction();
      updateUi();
      //warn user if minimum fee is selected
      if (_fee == MinerFee.ECONOMIC || _fee == MinerFee.LOWPRIO) {
         makeText(this, getString(R.string.toast_warning_low_fee), LENGTH_SHORT).show();
      }
   }

   private Bitcoins getFeePerKb() {
      return _fee.getFeePerKb(_mbwManager.getWalletManager(_isColdStorage).getLastFeeEstimations());
   }

    private TransactionStatus tryCreateUnsignedTransaction() {
        Log.d(TAG, "tryCreateUnsignedTransaction");
        if (isCoinapult()) {
            return tryCreateCoinapultTX();
        } else if (isColu()) {
            return tryCreateUnsignedColuTX(null);
        } else {
            return tryCreateUnsignedTransactionFromWallet();
        }
    }

   private TransactionStatus tryCreateUnsignedTransactionFromWallet() {
      _unsigned = null;

      BitcoinValue toSend = getBitcoinValueToSend();
      boolean hasAddressData = toSend != null && toSend.getAsBitcoin() != null && _receivingAddress != null;
      boolean hasRequestData = _paymentRequestHandler != null && _paymentRequestHandler.hasValidPaymentRequest();

      // Create the unsigned transaction
      try {
         if (hasRequestData) {
            PaymentRequestInformation paymentRequestInformation = _paymentRequestHandler.getPaymentRequestInformation();
            OutputList outputs = paymentRequestInformation.getOutputs();

            // has the payment request an amount set?
            if (paymentRequestInformation.hasAmount()) {
               setAmountToSend(ExactBitcoinValue.from(paymentRequestInformation.getOutputs().getTotalAmount()));
            } else {
               if (CurrencyValue.isNullOrZero(_amountToSend)) {
                  return TransactionStatus.MissingArguments;
               }

               // build new output list with user specified amount
               outputs = outputs.newOutputsWithTotalAmount(toSend.getLongValue());
            }
            _unsigned = _account.createUnsignedTransaction(outputs, getFeePerKb().getLongValue());
            _receivingAddress = null;
            _transactionLabel = paymentRequestInformation.getPaymentDetails().memo;
            return TransactionStatus.OK;
         } else if(hasAddressData) {
            WalletAccount.Receiver receiver = new WalletAccount.Receiver(_receivingAddress, toSend.getLongValue());
            _unsigned = _account.createUnsignedTransaction(Collections.singletonList(receiver), getFeePerKb().getLongValue());
            checkSpendingUnconfirmed();
            return TransactionStatus.OK;
         } else {
            return TransactionStatus.MissingArguments;
         }
      } catch (InsufficientFundsException e) {
         makeText(this, getResources().getString(R.string.insufficient_funds), LENGTH_LONG).show();
         return TransactionStatus.InsufficientFunds;
      } catch (OutputTooSmallException e1) {
         makeText(this, getResources().getString(R.string.amount_too_small), LENGTH_LONG).show();
         return TransactionStatus.OutputTooSmall;
      } catch (UnableToBuildTransactionException e) {
         makeText(this, getResources().getString(R.string.unable_to_build_tx), LENGTH_LONG).show();
         // under certain conditions the max-miner-fee check fails - report it back to the server, so we can better
         // debug it
         _mbwManager.reportIgnoredException("MinerFeeException", e);
         return TransactionStatus.MissingArguments;
      }
   }

    private ColuBroadcastTxid.Json createEmptyColuBroadcastJson() {
        ColuBroadcastTxid.Json result = new ColuBroadcastTxid.Json();
        result.txHex = "";
        return result;
    }

    private TransactionStatus tryCreateUnsignedColuTX(final PrepareCallback callback) {
        Log.d(TAG, "tryCreateUnsignedColuTX start");
        if (_account instanceof ColuAccount) {
            final ColuAccount coluAccount = (ColuAccount) _account;
            _unsigned = null;
            _preparedCoinapult = null;

            if (CurrencyValue.isNullOrZero(_amountToSend) || _receivingAddress == null) {
                Log.d(TAG, "tryCreateUnsignedColuTX Missing argument: amountToSend or receiving address is null.");
                if (_amountToSend != null) {
                    Log.d(TAG, "_amountToSend=" + _amountToSend);
                } else {
                    Log.d(TAG, "_amountToSend is null");
                }
                if (_receivingAddress != null) {
                    Log.d(TAG, " receivingAddress=" + _receivingAddress.toString());
                }
                return TransactionStatus.MissingArguments;
            }

            if (!_amountToSend.getCurrency().equals(coluAccount.getColuAsset().name)) {
                //TODO: add new code for incorrect input data (wrong asset type)
                Log.d(TAG, "tryCreateUnsignedColuTX Missing argument: incorrect asset type: " + _amountToSend.getCurrency()
                        + " expected: " + coluAccount.getColuAsset().name);
                return TransactionStatus.MissingArguments;
            }

            ExactCurrencyValue nativeAmount = ExactCurrencyValue.from(_amountToSend.getValue(), _amountToSend.getCurrency());
            Log.d(TAG, "preparing colutx");
            final ColuManager coluManager = _mbwManager.getColuManager();
            long feePerKb = getFeePerKb().getLongValue();
            ColuTransactionData coluTransactionData = new ColuTransactionData(_receivingAddress, nativeAmount,
                    coluAccount, feePerKb);

            if(callback != null) {
                new AsyncTask<ColuTransactionData, Void, ColuBroadcastTxid.Json>() {
                    @Override
                    protected ColuBroadcastTxid.Json doInBackground(ColuTransactionData... params) {

                        if (!checkFee(true)) {

                            // Handling the abnormal use case when a colu account doesn't have enough funds
                            // and however it is chosen itself for funding
                            if (coluAccount.getLinkedAccount() == feeColuAccount) {
                                return createEmptyColuBroadcastJson();
                            }

                            //Create funding transaction and broadcast it to network
                            List<WalletAccount.Receiver> receivers = new ArrayList<WalletAccount.Receiver>();
                            long feePerKb = getFeePerKb().getLongValue();
                            long fundingAmountToSend = feePerKb;

                            if (feePerKb < TransactionUtils.MINIMUM_OUTPUT_VALUE)
                                fundingAmountToSend = TransactionUtils.MINIMUM_OUTPUT_VALUE;

                            WalletAccount.Receiver coluReceiver = new WalletAccount.Receiver(_account.getReceivingAddress().get(), fundingAmountToSend + ColuManager.DUST_OUTPUT_SIZE + ColuManager.METADATA_OUTPUT_SIZE);
                            receivers.add(coluReceiver);
                            try {
                                UnsignedTransaction fundingTransaction = feeColuAccount.createUnsignedTransaction(receivers, feePerKb);
                                Transaction signedFundingTransaction = feeColuAccount.signTransaction(fundingTransaction, AesKeyCipher.defaultKeyCipher());
                                WalletAccount.BroadcastResult broadcastResult = feeColuAccount.broadcastTransaction(signedFundingTransaction);
                                if (broadcastResult != WalletAccount.BroadcastResult.SUCCESS) {
                                    return createEmptyColuBroadcastJson();
                                }
                            } catch (OutputTooSmallException | InsufficientFundsException | UnableToBuildTransactionException | KeyCipher.InvalidKeyCipher ex) {
                                return createEmptyColuBroadcastJson();
                            }

                            //Before sending colu transaction preparation request we make sure colu see the new funding transaction

                            for (int attemtps = 0; attemtps < 10; attemtps++) {
                                if (checkFee(true)) {
                                    Log.d(TAG, "Now we have enough fee on the address");
                                    break;
                                }

                                try {
                                    Thread.sleep(ColuManager.TIME_INTERVAL_BETWEEN_BALANCE_FUNDING_CHECKS);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                        }

                        return coluManager.prepareColuTx(params[0].getReceivingAddress(), params[0].getNativeAmount(),
                                params[0].getColuAccount(), params[0].getFeePerKb());
                    }

                    @Override
                    protected void onPostExecute(ColuBroadcastTxid.Json preparedTransaction) {
                        super.onPostExecute(preparedTransaction);
                        Log.d(TAG, "onPostExecute prepareColuTransaction");
                        //TODO: add feedback to user about fees when missing funds
                        if (preparedTransaction != null && !preparedTransaction.txHex.isEmpty()) {
                            Log.d(TAG, " preparedTransaction=" + preparedTransaction.txHex);
                            _preparedColuTx = preparedTransaction;
                            if (callback != null) {
                                callback.success();
                            }
                            Toast.makeText(SendMainActivity.this, R.string.colu_succeeded_to_prepare, Toast.LENGTH_SHORT).show();
                        } else {
                            if (callback != null) {
                                callback.fail();
                            }
                            Toast.makeText(SendMainActivity.this, getString(R.string.colu_failed_to_prepare, ((ColuAccount) _account).getColuAsset().label), Toast.LENGTH_SHORT).show();
                            updateUi();
                        }
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, coluTransactionData);
            }
            return TransactionStatus.OK;
        }
        // if we arrive here it means account is not colu type
        Log.e(TAG, "tryCreateUnsignedColuTX: We should not arrive here.");
        return TransactionStatus.MissingArguments;
    }

    private interface PrepareCallback {
        void success();
        void fail();
    }

    private TransactionStatus tryCreateCoinapultTX() {
        if (_account instanceof CoinapultAccount) {
            CoinapultAccount coinapultAccount = (CoinapultAccount) _account;
            _unsigned = null;
            _preparedCoinapult = null;

         if (CurrencyValue.isNullOrZero(_amountToSend) || _receivingAddress == null) {
            return TransactionStatus.MissingArguments;
         }

         try {
            // try to get it in the accounts native currency, but dont convert anything
            Optional<ExactCurrencyValue> nativeAmount = CurrencyValue.checkCurrencyAmount(
                  _amountToSend,
                  coinapultAccount.getCoinapultCurrency().name
            );

            BigDecimal minimumConversationValue = coinapultAccount.getCoinapultCurrency().minimumConversationValue;
            if (nativeAmount.isPresent()) {
               if (nativeAmount.get().getValue().compareTo(minimumConversationValue) < 0) {
                  //trying to send less than coinapults minimum withdrawal
                  return TransactionStatus.OutputTooSmall;
               }
               _preparedCoinapult = coinapultAccount.prepareCoinapultTx(_receivingAddress, nativeAmount.get());
               return TransactionStatus.OK;
            } else {
               // if we dont have it in the account-native currency, send it as bitcoin value and
               // let coinapult to the conversation

               // convert it to native, only to check if its larger the the minValue
               BigDecimal nativeValue = CurrencyValue.fromValue(
                     _amountToSend, coinapultAccount.getCoinapultCurrency().name,
                     _mbwManager.getExchangeRateManager()).getValue();

               if (nativeValue == null || nativeValue.compareTo(minimumConversationValue) < 0) {
                  // trying to send less than coinapults minimum withdrawal, or no fx rate available
                  return TransactionStatus.OutputTooSmall;
               }
               WalletAccount.Receiver receiver = new WalletAccount.Receiver(_receivingAddress, getBitcoinValueToSend().getLongValue());
               _preparedCoinapult = coinapultAccount.prepareCoinapultTx(receiver);
               return TransactionStatus.OK;
            }
         } catch (InsufficientFundsException e) {
            makeText(this, getResources().getString(R.string.insufficient_funds), LENGTH_LONG).show();
            return TransactionStatus.InsufficientFunds;
         }
      } else {
         throw new IllegalStateException("only attempt this for coinapult accounts");
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
      _spendingUnconfirmed = false;
   }

    private void updateUi() {
        // TODO: profile. slow!
        updateRecipient();
        updateAmount();
        updateFee();

      // Enable/disable send button
      btSend.setEnabled(_transactionStatus == TransactionStatus.OK);
      findViewById(R.id.root).invalidate();
   }

    private void updateFee() {
        coluSpendAccount();
    }

    private void updateRecipient() {
        boolean hasPaymentRequest = _paymentRequestHandler != null && _paymentRequestHandler.hasValidPaymentRequest();
        if (_receivingAddress == null && !hasPaymentRequest) {
            // Hide address, show "Enter"
            tvRecipientTitle.setText(R.string.enter_recipient_title);
            llEnterRecipient.setVisibility(View.VISIBLE);
            llRecipientAddress.setVisibility(View.GONE);
            tvWarning.setVisibility(View.GONE);
            return;
        }
        // Hide "Enter", show address
        tvRecipientTitle.setText(R.string.recipient_title);
        llRecipientAddress.setVisibility(View.VISIBLE);
        llEnterRecipient.setVisibility(View.GONE);

      // See if the address is in the address book or one of our accounts
        String label = null;
        if (_receivingLabel != null) {
            label = _receivingLabel;
        } else if (_receivingAddress != null) {
            label = getAddressLabel(_receivingAddress);
        }
      if (label == null || label.length() == 0) {
         // Hide label
         tvReceiverLabel.setVisibility(GONE);
      } else {
         // Show label
         tvReceiverLabel.setText(label);
         tvReceiverLabel.setVisibility(VISIBLE);
      }

      // Set Address
      if (!hasPaymentRequest) {
         String choppedAddress = _receivingAddress.toMultiLineString();
         tvReceiver.setText(choppedAddress);
      }

      if (hasPaymentRequest) {
         PaymentRequestInformation paymentRequestInformation = _paymentRequestHandler.getPaymentRequestInformation();
         if (paymentRequestInformation.hasValidSignature()) {
            tvReceiver.setText(paymentRequestInformation.getPkiVerificationData().displayName);
         } else {
            tvReceiver.setText(getString(R.string.label_unverified_recipient));
         }
      }

      // show address (if available - some PRs might have more than one address or a not decodeable input)
      if (hasPaymentRequest && _receivingAddress != null) {
         tvReceiverAddress.setText(_receivingAddress.toDoubleLineString());
         tvReceiverAddress.setVisibility(VISIBLE);
      } else {
         tvReceiverAddress.setVisibility(GONE);
      }

      //Check the wallet manager to see whether its our own address, and whether we can spend from it
      WalletManager walletManager = _mbwManager.getWalletManager(false);
      if (_receivingAddress != null && walletManager.isMyAddress(_receivingAddress)) {
         if (walletManager.hasPrivateKeyForAddress(_receivingAddress)) {
            // Show a warning as we are sending to one of our own addresses
            tvWarning.setVisibility(VISIBLE);
            tvWarning.setText(R.string.my_own_address_warning);
            tvWarning.setTextColor(getResources().getColor(R.color.yellow));
         } else {
            // Show a warning as we are sending to one of our own addresses,
            // which is read-only
            tvWarning.setVisibility(VISIBLE);
            tvWarning.setText(R.string.read_only_warning);
            tvWarning.setTextColor(getResources().getColor(R.color.red));
         }
      } else {
         tvWarning.setVisibility(GONE);
      }

      //if present, show transaction label
      if (_transactionLabel != null) {
         tvTransactionLabelTitle.setVisibility(VISIBLE);
         tvTransactionLabel.setVisibility(VISIBLE);
         tvTransactionLabel.setText(_transactionLabel);
      } else {
         tvTransactionLabelTitle.setVisibility(GONE);
         tvTransactionLabel.setVisibility(GONE);
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
         tvAmountTitle.setText(R.string.enter_amount_title);
         tvAmount.setText("");
         tvAmountFiat.setVisibility(GONE);
         tvError.setVisibility(GONE);
      } else {
         tvAmountTitle.setText(R.string.amount_title);
         switch (_transactionStatus) {
            case OutputTooSmall:
               // Amount too small
               tvAmount.setText(_mbwManager.getBtcValueString(getBitcoinValueToSend().getLongValue()));
               tvAmountFiat.setVisibility(GONE);
               if (isCoinapult()) {
                  CoinapultAccount coinapultAccount = (CoinapultAccount) _account;
                  tvError.setText(
                        getString(
                              R.string.coinapult_amount_too_small,
                              coinapultAccount.getCoinapultCurrency().minimumConversationValue,
                              coinapultAccount.getCoinapultCurrency().name)
                  );
               } else {
                  tvError.setText(R.string.amount_too_small_short);
               }
               tvError.setVisibility(VISIBLE);
               break;
            case InsufficientFunds:
               // Insufficient funds
               tvAmount.setText(
                     Utils.getFormattedValueWithUnit(_amountToSend, _mbwManager.getBitcoinDenomination())
               );
               tvError.setText(R.string.insufficient_funds);
               tvError.setVisibility(VISIBLE);
               break;
            default:
               // Set Amount
               if (!CurrencyValue.isNullOrZero(_amountToSend)) {
                  // show the user entered value as primary amount
                  CurrencyValue primaryAmount = _amountToSend;
                  CurrencyValue alternativeAmount;
                  if (primaryAmount.getCurrency().equals(_account.getAccountDefaultCurrency())) {
                     if (primaryAmount.isBtc()) {
                        // if the accounts default currency is BTC and the user entered BTC, use the current
                        // selected fiat as alternative currency
                        alternativeAmount = CurrencyValue.fromValue(
                              primaryAmount, _mbwManager.getFiatCurrency(), _mbwManager.getExchangeRateManager()
                        );
                     } else {
                        // if the accounts default currency isn't BTC, use BTC as alternative
                        alternativeAmount = ExchangeBasedBitcoinValue.fromValue(
                              primaryAmount, _mbwManager.getExchangeRateManager()
                        );
                     }
                  } else {
                     // use the accounts default currency as alternative
                     alternativeAmount = CurrencyValue.fromValue(
                           primaryAmount, _account.getAccountDefaultCurrency(), _mbwManager.getExchangeRateManager()
                     );
                  }
                  String sendAmount = Utils.getFormattedValueWithUnit(primaryAmount, _mbwManager.getBitcoinDenomination());
                   if(isColu()){
                       sendAmount = Utils.getColuFormattedValueWithUnit(primaryAmount);
                   } else if (!primaryAmount.isBtc()) {
                     // if the amount is not in BTC, show a ~ to inform the user, its only approximate and depends
                     // on a FX rate
                     sendAmount = "~ " + sendAmount;
                  }
                  tvAmount.setText(sendAmount);
                  if (CurrencyValue.isNullOrZero(alternativeAmount)) {
                     tvAmountFiat.setVisibility(GONE);
                  } else {
                     // show the alternative amount
                     String alternativeAmountString =
                           Utils.getFormattedValueWithUnit(alternativeAmount, _mbwManager.getBitcoinDenomination());

                  if (!alternativeAmount.isBtc()) {
                     // if the amount is not in BTC, show a ~ to inform the user, its only approximate and depends
                     // on a FX rate
                     alternativeAmountString = "~ " + alternativeAmountString;
                  }

                     tvAmountFiat.setText(alternativeAmountString);
                     tvAmountFiat.setVisibility(VISIBLE);
                  }
               } else {
                  tvAmount.setText("");
                  tvAmountFiat.setText("");
               }

               tvError.setVisibility(GONE);
               //check if we need to warn the user about unconfirmed funds
               tvUnconfirmedWarning.setVisibility(_spendingUnconfirmed ? VISIBLE : GONE);
               break;
         }
      }

      // Disable Amount button if we have a payment request with valid amount
      if (_paymentRequestHandler != null && _paymentRequestHandler.getPaymentRequestInformation().hasAmount()) {
         btEnterAmount.setEnabled(false);
      }

      // Update Fee-Display
      btFeeLvl.setText(_fee.getMinerFeeName(this));
      String duration = Utils.formatBlockcountAsApproxDuration(this, _fee.getNBlocks());
      if (_unsigned == null) {
         // Only show button for fee lvl, cannot calculate fee yet
         tvFeeValue.setVisibility(GONE);
         tvSatFeeValue.setText(">"+(_fee.getFeePerKb(_mbwManager.getWalletManager(_isColdStorage).getLastFeeEstimations()).getLongValue()/1000L)+"sat/byte, ~"+ duration);
      } else {
         // Show fee fully calculated

         long fee = _unsigned.calculateFee();

         Denomination bitcoinDenomination = _mbwManager.getBitcoinDenomination();
         //show fee lvl on button - show the fees in mBtc if Btc is the denomination
         Denomination feeDenomination = bitcoinDenomination == BTC ? mBTC : bitcoinDenomination;

         String feeString = CoinUtil.valueString(fee, feeDenomination, true) + " " + feeDenomination.getUnicodeName();

         CurrencyValue fiatFee = CurrencyValue.fromValue(
               ExactBitcoinValue.from(fee),
               _mbwManager.getFiatCurrency(),
               _mbwManager.getExchangeRateManager()
         );

         if (!CurrencyValue.isNullOrZero(fiatFee)) {
            // Show approximate fee in fiat
            feeString += ", " + Utils.getFormattedValueWithUnit(fiatFee, _mbwManager.getBitcoinDenomination());
         }
         tvFeeValue.setVisibility(VISIBLE);
         tvFeeValue.setText(String.format("(%s)", feeString));
         int inCount = _unsigned.getFundingOutputs().length;
         int outCount = _unsigned.getOutputs().length;
         int size = estimateTransactionSize(inCount, outCount);
         tvSatFeeValue.setText(inCount + " In- / " + outCount + " Outputs, ~" + size + " bytes, \n" + (fee / size) + " sat/byte, ~" + duration);
      }
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);

      // If we don't have a fresh exchange rate, now is a good time to request one, as we will need it in a minute
      if (!_mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable()) {
         _mbwManager.getExchangeRateManager().requestRefresh();
      }

      btClipboard.setEnabled(getUriFromClipboard() != null);
      pbSend.setVisibility(GONE);

      updateUi();
      super.onResume();
   }

   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      _mbwManager.getVersionManager().closeDialog();
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
        if (_paymentRequestHandler != null) {
            if (_paymentRequestHandler.getPaymentRequestInformation().isExpired()) {
                makeText(this, getString(R.string.payment_request_not_sent_expired), LENGTH_LONG).show();
                return;
            }
        }

        disableButtons();
        SignTransactionActivity.callMe(this, _account.getId(), _isColdStorage, _unsigned, SIGN_TRANSACTION_REQUEST_CODE);
    }

   protected void disableButtons() {
      pbSend.setVisibility(VISIBLE);
      btSend.setEnabled(false);
      btAddressBook.setEnabled(false);
      btManualEntry.setEnabled(false);
      btClipboard.setEnabled(false);
      btScan.setEnabled(false);
      btEnterAmount.setEnabled(false);
   }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + " and resultCode=" + resultCode);
        if (requestCode == SCAN_RESULT_CODE) {
            if (resultCode != RESULT_OK) {
                if (intent != null) {
                    String error = intent.getStringExtra(StringHandlerActivity.RESULT_ERROR);
                    if (error != null) {
                        makeText(this, error, LENGTH_LONG).show();
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
                    if (uri.amount != null && uri.amount > 0) {
                        //we set the amount to the one contained in the qr code, even if another one was entered previously
                        if (!CurrencyValue.isNullOrZero(_amountToSend)) {
                            makeText(this, R.string.amount_changed, LENGTH_LONG).show();
                        }
                        setAmountToSend(ExactBitcoinValue.from(uri.amount));
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
            if (intent.getExtras().containsKey(AddressBookFragment.ADDRESS_RESULT_LABEL)) {
                _receivingLabel = intent.getStringExtra(AddressBookFragment.ADDRESS_RESULT_LABEL);
            }
            // this is where colusend is calling tryCreateUnsigned
            // why is amountToSend not set ?
            _transactionStatus = tryCreateUnsignedTransaction();
            updateUi();
        } else if (requestCode == MANUAL_ENTRY_RESULT_CODE && resultCode == RESULT_OK) {
            _receivingAddress = Preconditions.checkNotNull((Address) intent
                    .getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME));

            _transactionStatus = tryCreateUnsignedTransaction();
            updateUi();
        } else if (requestCode == GET_AMOUNT_RESULT_CODE && resultCode == RESULT_OK) {
            // Get result from AmountEntry
            CurrencyValue enteredAmount = (CurrencyValue) intent.getSerializableExtra(GetAmountActivity.AMOUNT);
            setAmountToSend(enteredAmount);
            if (!CurrencyValue.isNullOrZero(_amountToSend)) {
                _transactionStatus = tryCreateUnsignedTransaction();
            }
            updateUi();
        } else if (requestCode == SIGN_TRANSACTION_REQUEST_CODE) {
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
                        _paymentRequestHandler.sendResponse(_signedTransaction, _account.getReceivingAddress().get());
                    } else {
                        makeText(this, getString(R.string.payment_request_not_sent_expired), LENGTH_LONG).show();

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
            if (resultCode == RESULT_OK) {
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
        } else if(requestCode == REQUET_BTC_ACCOUNT){
            if(resultCode == RESULT_OK) {
                UUID id = (UUID) intent.getSerializableExtra(AddressBookFragment.ADDRESS_RESULT_ID);
                feeColuAccount = _mbwManager.getWalletManager(false).getAccount(id);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

   private void setReceivingAddressFromKeynode(HdKeyNode hdKeyNode) {
      _progress = ProgressDialog.show(this, "", getString(R.string.retrieving_pubkey_address), true);
      _receivingAcc = _mbwManager.getWalletManager(true).createUnrelatedBip44Account(hdKeyNode);
      _xpubSyncing = true;
      _mbwManager.getWalletManager(true).startSynchronization(_receivingAcc);
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
   public void paymentRequestException(PaymentRequestException ex) {
      //todo: maybe hint the user, that the merchant might broadcast the transaction later anyhow
      // and we should move funds to a new address to circumvent it
      Utils.showSimpleMessageDialog(this,
            String.format(getString(R.string.payment_request_error_while_getting_ack), ex.getMessage()));
   }

   @Subscribe
   public void paymentRequestAck(PaymentACK paymentACK) {
      if (paymentACK != null) {
         BroadcastTransactionActivity.callMe(this, _account.getId(), _isColdStorage, _signedTransaction, _transactionLabel, BROADCAST_REQUEST_CODE);
      }
   }

   @Subscribe
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
      updateUi();
   }

   @Subscribe
   public void selectedCurrencyChanged(SelectedCurrencyChanged event) {
      updateUi();
   }

   @Subscribe
   public void syncFinished(SyncStopped event) {
      if (_xpubSyncing) {
         _xpubSyncing = false;
         _receivingAddress = _mbwManager.getWalletManager(true).getAccount(_receivingAcc).getReceivingAddress().get();
         if (_progress != null) {
            _progress.dismiss();
         }
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
      }
   }

   @Subscribe
   public void syncFailed(SyncFailed event) {
      if (_progress != null) {
         _progress.dismiss();
      }
      makeText(this, R.string.warning_sync_failed_reusing_first , LENGTH_LONG).show();
   }
}
