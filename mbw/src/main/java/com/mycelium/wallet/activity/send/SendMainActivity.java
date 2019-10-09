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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.mrd.bitlib.FeeEstimator;
import com.mrd.bitlib.FeeEstimatorBuilder;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.paymentrequest.PaymentRequestException;
import com.mycelium.paymentrequest.PaymentRequestInformation;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.GetAmountActivity;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.modern.AddressBookFragment;
import com.mycelium.wallet.activity.modern.GetFromAddressBookActivity;
import com.mycelium.wallet.activity.pop.PopActivity;
import com.mycelium.wallet.activity.send.adapter.AddressViewAdapter;
import com.mycelium.wallet.activity.send.adapter.FeeLvlViewAdapter;
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter;
import com.mycelium.wallet.activity.send.event.BroadcastResultListener;
import com.mycelium.wallet.activity.send.event.SelectListener;
import com.mycelium.wallet.activity.send.helper.FeeItemsBuilder;
import com.mycelium.wallet.activity.send.model.AddressItem;
import com.mycelium.wallet.activity.send.model.FeeItem;
import com.mycelium.wallet.activity.send.model.FeeLvlItem;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.activity.util.AnimationUtils;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.content.HandleConfigFactory;
import com.mycelium.wallet.content.ResultType;
import com.mycelium.wallet.content.StringHandleConfig;
import com.mycelium.wallet.event.*;
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.content.GenericAssetUri;
import com.mycelium.wapi.content.GenericAssetUriParser;
import com.mycelium.wapi.content.WithCallback;
import com.mycelium.wapi.content.btc.BitcoinUri;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.BitcoinBasedGenericTransaction;
import com.mycelium.wapi.wallet.BroadcastResult;
import com.mycelium.wapi.wallet.BroadcastResultType;
import com.mycelium.wapi.wallet.FeeEstimationsGeneric;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.BtcTransaction;
import com.mycelium.wapi.wallet.btc.FeePerKbFee;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature;
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.coinapult.CoinapultAccount;
import com.mycelium.wapi.wallet.coinapult.Currency;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency;
import com.mycelium.wapi.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.colu.ColuTransaction;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.colu.coins.MASSCoin;
import com.mycelium.wapi.wallet.colu.coins.MTCoin;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException;
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException;
import com.mycelium.wapi.wallet.exceptions.GenericOutputTooSmallException;
import com.mycelium.wapi.wallet.exceptions.GenericTransactionBroadcastException;
import com.squareup.otto.Subscribe;

import org.bitcoin.protocols.payments.PaymentACK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAddress;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAssetUri;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getHdKeyNode;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getPopRequest;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getPrivateKey;

public class SendMainActivity extends FragmentActivity implements BroadcastResultListener {
    private static final String TAG = "SendMainActivity";
    private static final int GET_AMOUNT_RESULT_CODE = 1;
    private static final int SCAN_RESULT_CODE = 2;
    private static final int ADDRESS_BOOK_RESULT_CODE = 3;
    private static final int MANUAL_ENTRY_RESULT_CODE = 4;
    private static final int REQUEST_PICK_ACCOUNT = 5;
    protected static final int SIGN_TRANSACTION_REQUEST_CODE = 6;
    private static final int REQUEST_PAYMENT_HANDLER = 8;
    private static final int REQUEST_BTC_ACCOUNT = 9;
    public static final String RAW_PAYMENT_REQUEST = "rawPaymentRequest";

    public static final String ACCOUNT = "account";
    private static final String AMOUNT = "amount";
    public static final String IS_COLD_STORAGE = "isColdStorage";
    public static final String RECEIVING_ADDRESS = "receivingAddress";
    public static final String HD_KEY = "hdKey";
    public static final String TRANSACTION_LABEL = "transactionLabel";
    public static final String ASSET_URI = "assetUri";
    public static final String FEE_LVL = "feeLvl";
    public static final String SELECTED_FEE = "selectedFee";
    public static final String PAYMENT_FETCHED = "paymentFetched";
    private static final String PAYMENT_REQUEST_HANDLER_ID = "paymentRequestHandlerId";
    public static final String SIGNED_TRANSACTION = "signedTransaction";
    public static final String TRANSACTION_FIAT_VALUE = "transaction_fiat_value";
    public static final String FEE_ESTIMATION = "fee_estimation";
    private static final long FEE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(2);
    private boolean spendingUnconfirmed;

    private enum TransactionStatus {
        MISSING_ARGUMENTS, OUTPUT_TOO_SMALL, INSUFFICIENT_FUNDS, INSUFFICIENT_FUNDS_FOR_FEE, OK
    }

    @BindView(R.id.progressBarBusy)
    View progressBarBusy;
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
    @BindView(R.id.tvSatFeeValue)
    TextView tvSatFeeValue;
    @BindView(R.id.btEnterAmount)
    ImageButton btEnterAmount;
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
    View llEnterRecipient;
    @BindView(R.id.llRecipientAddress)
    LinearLayout llRecipientAddress;
    @BindView(R.id.colu_tips_check_address)
    View tipsCheckAddress;
    @BindView(R.id.tvFeeWarning)
    TextView tvFeeWarning;
    @BindView(R.id.tvStaleWarning)
    TextView tvStaleWarning;

    @BindView(R.id.receiversAddressList)
    SelectableRecyclerView receiversAddressesList;
    @BindView(R.id.feeLvlList)
    SelectableRecyclerView feeLvlList;
    @BindView(R.id.feeValueList)
    SelectableRecyclerView feeValueList;

    private MbwManager mbwManager;

    private PaymentRequestHandler paymentRequestHandler;
    private String paymentRequestHandlerUuid;

    private WalletAccount activeAccount;
    private Value amountToSend;
    private GenericAddress receivingAddress;
    private List<GenericAddress> receivingAddressesList = new ArrayList<>();
    private String receivingLabel;
    private String transactionLabel;
    private GenericAssetUri genericUri;
    private boolean isColdStorage;
    private TransactionStatus transactionStatus = TransactionStatus.OK;
    private GenericTransaction transaction;
    private GenericTransaction signedTransaction;
    private MinerFee feeLvl;
    private Value selectedFee;
    private ProgressDialog progressDialog;
    private UUID receivingAcc;
    private boolean xpubSyncing = false;
    private boolean paymentFetched = false;
    private FeeEstimationsGeneric feeEstimation;
    private SharedPreferences transactionFiatValuePref;
    private FeeItemsBuilder feeItemsBuilder;
    private boolean showStaleWarning = false;

    private int feeFirstItemWidth;
    private int addressFirstItemWidth;

    private DialogFragment activityResultDialog;

    private final Map<GenericAssetInfo, Feature> featureMap;

    private FeeViewAdapter feeViewAdapter;
    private boolean showSendBtn = true;

    private AsyncTask<Void, Void, Void> updateUiAsyncTask = null;

    public SendMainActivity() {
        featureMap = new HashMap<>();
        featureMap.put(MTCoin.INSTANCE, Feature.COLU_PREPARE_OUTGOING_TX);
        featureMap.put(MASSCoin.INSTANCE, Feature.COLU_PREPARE_OUTGOING_TX);
        featureMap.put(RMCCoin.INSTANCE, Feature.COLU_PREPARE_OUTGOING_TX);
    }

    public static Intent getIntent(Activity currentActivity, UUID account, boolean isColdStorage) {
        return new Intent(currentActivity, SendMainActivity.class)
                .putExtra(ACCOUNT, account)
                .putExtra(IS_COLD_STORAGE, isColdStorage);
    }

    public static Intent getIntent(Activity currentActivity, UUID account,
                                   long amountToSend, GenericAddress receivingAddress, boolean isColdStorage) {
        return getIntent(currentActivity, account, isColdStorage)
                .putExtra(AMOUNT, Value.valueOf(Utils.getBtcCoinType(), amountToSend))
                .putExtra(RECEIVING_ADDRESS, receivingAddress);
    }

    public static Intent getIntent(Activity currentActivity, UUID account, HdKeyNode hdKey) {
        return getIntent(currentActivity, account, false)
                .putExtra(HD_KEY, hdKey);
    }

    public static Intent getIntent(Activity currentActivity, UUID account, GenericAssetUri uri, boolean isColdStorage) {
        return getIntent(currentActivity, account, isColdStorage)
                .putExtra(AMOUNT, uri.getValue())
                .putExtra(RECEIVING_ADDRESS, uri.getAddress())
                .putExtra(TRANSACTION_LABEL, uri.getLabel())
                .putExtra(ASSET_URI, uri);
    }

    public static Intent getIntent(Activity currentActivity, UUID account, byte[] rawPaymentRequest, boolean isColdStorage) {
        return getIntent(currentActivity, account, isColdStorage)
                .putExtra(RAW_PAYMENT_REQUEST, rawPaymentRequest);
    }

    @SuppressLint({"ShowToast", "StaticFieldLeak"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: profile. slow!
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_main_activity);
        ButterKnife.bind(this);
        mbwManager = MbwManager.getInstance(getApplication());
        feeItemsBuilder = new FeeItemsBuilder(mbwManager.getExchangeRateManager(), mbwManager.getFiatCurrency());

        // Get intent parameters
        UUID accountId = checkNotNull((UUID) getIntent().getSerializableExtra(ACCOUNT));

        amountToSend = (Value) getIntent().getSerializableExtra(AMOUNT);

        // May be null
        receivingAddress = (GenericAddress) getIntent().getSerializableExtra(RECEIVING_ADDRESS);
        //May be null
        transactionLabel = getIntent().getStringExtra(TRANSACTION_LABEL);
        //May be null
        genericUri = (GenericAssetUri) getIntent().getSerializableExtra(ASSET_URI);

        // did we get a raw payment request
        byte[] rawPaymentRequest = getIntent().getByteArrayExtra(RAW_PAYMENT_REQUEST);

        isColdStorage = getIntent().getBooleanExtra(IS_COLD_STORAGE, false);
        String crashHint = TextUtils.join(", ", getIntent().getExtras().keySet()) + " (account id was " + accountId + ")";
        WalletAccount account = mbwManager.getWalletManager(isColdStorage).getAccount(accountId);
        this.activeAccount = checkNotNull(account, crashHint);
        feeLvl = mbwManager.getMinerFee();
        feeEstimation = this.activeAccount.getDefaultFeeEstimation();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                feeEstimation = SendMainActivity.this.activeAccount.getFeeEstimations();
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                showStaleWarning = feeEstimation.getLastCheck() < System.currentTimeMillis() - FEE_EXPIRATION_TIME;
                updateUi();
            }
        }.execute();

        selectedFee = getCurrentFeeEstimation();

        showStaleWarning = feeEstimation.getLastCheck() < System.currentTimeMillis() - FEE_EXPIRATION_TIME;

        // Load saved state, overwriting amount and address
        if (savedInstanceState != null) {
            setAmountToSend((Value) savedInstanceState.getSerializable(AMOUNT));
            receivingAddress = (GenericAddress) savedInstanceState.getSerializable(RECEIVING_ADDRESS);
            transactionLabel = savedInstanceState.getString(TRANSACTION_LABEL);
            feeLvl = (MinerFee) savedInstanceState.getSerializable(FEE_LVL);
            genericUri = (GenericAssetUri) savedInstanceState.getSerializable(ASSET_URI);
            paymentFetched = savedInstanceState.getBoolean(PAYMENT_FETCHED);
            signedTransaction = (GenericTransaction) savedInstanceState.getSerializable(SIGNED_TRANSACTION);
            selectedFee = (Value) savedInstanceState.getSerializable(SELECTED_FEE);
            // get the payment request handler from the BackgroundObject cache - if the application
            // has restarted since it was cached, the user gets queried again
            paymentRequestHandlerUuid = savedInstanceState.getString(PAYMENT_REQUEST_HANDLER_ID);
            if (paymentRequestHandlerUuid != null) {
                paymentRequestHandler = (PaymentRequestHandler) mbwManager.getBackgroundObjectsCache()
                        .getIfPresent(paymentRequestHandlerUuid);
            }
            feeEstimation = (FeeEstimationsGeneric) savedInstanceState.getSerializable(FEE_ESTIMATION);
        }

        //if we do not have a stored receiving address, and got a keynode, we need to figure out the address
        if (receivingAddress == null) {
            HdKeyNode hdKey = (HdKeyNode) getIntent().getSerializableExtra(HD_KEY);
            if (hdKey != null) {
                setReceivingAddressFromKeynode(hdKey);
            }
        }

        // check whether the account can spend, if not, ask user to select one
        if (activeAccount.canSpend()) {
            // See if we can create the transaction with what we have
            updateTransactionStatusAndUi();
        } else {
            //we need the user to pick a spending account - the activity will then init sendmain correctly
            GenericAssetUri uri;
            if (genericUri == null) {
                uri = BitcoinUri.from(receivingAddress,
                        getValueToSend(), transactionLabel, null);
            } else {
                uri = genericUri;
            }

            if (rawPaymentRequest != null) {
                GetSpendingRecordActivity.callMeWithResult(this, rawPaymentRequest, REQUEST_PICK_ACCOUNT);
            } else {
                GetSpendingRecordActivity.callMeWithResult(this, uri, REQUEST_PICK_ACCOUNT);
            }

            //no matter whether the user did successfully send or tapped back - we do not want to stay here with a wrong account selected
            finish();
            return;
        }

        // lets see if we got a raw Payment request (probably by downloading a file with MIME application/bitcoin-paymentrequest)
        if (rawPaymentRequest != null && paymentRequestHandler == null) {
            verifyPaymentRequest(rawPaymentRequest);
        }

        // lets check whether we got a payment request uri and need to fetch payment data
        if (genericUri instanceof WithCallback
                && !Strings.isNullOrEmpty(((WithCallback) genericUri).getCallbackURL()) && paymentRequestHandler == null) {
            verifyPaymentRequest(genericUri);
        }

        if (!(account instanceof HDAccount  || account instanceof SingleAddressAccount || account instanceof ColuAccount)) {
            llFee.setVisibility(GONE);
        }

        // Amount Hint
        tvAmount.setHint(getResources().getString(R.string.amount_hint_denomination,
                mbwManager.getDenomination().getUnicodeString(this.activeAccount.getCoinType().getSymbol())));
        tipsCheckAddress.setVisibility(this.activeAccount.getCoinType() instanceof ColuMain ? View.VISIBLE : View.GONE);

        Point outSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(outSize);
        int senderFinalWidth = outSize.x;
        feeFirstItemWidth = (senderFinalWidth - getResources().getDimensionPixelSize(R.dimen.item_dob_width)) / 2;
        addressFirstItemWidth = (senderFinalWidth - getResources().getDimensionPixelSize(R.dimen.item_addr_width)) / 2;

        initFeeView();
        initFeeLvlView();

        transactionFiatValuePref = getSharedPreferences(TRANSACTION_FIAT_VALUE, MODE_PRIVATE);
    }

    private void setUpMultiAddressView() {
        tvReceiverAddress.setVisibility(View.GONE);
        tvReceiver.setVisibility(View.GONE);
        receiversAddressesList.setHasFixedSize(true);
        receiversAddressesList.setItemWidth(getResources().getDimensionPixelSize(R.dimen.item_addr_width));

        // these labels needed for readability
        Map<AddressType, String[]> addressLabels = new HashMap<>();
        addressLabels.put(AddressType.P2PKH, new String[] {"Legacy", "P2PKH"});
        addressLabels.put(AddressType.P2WPKH, new String[] {"SegWit native", "Bech32"});
        addressLabels.put(AddressType.P2SH_P2WPKH, new String[] {"SegWit compat.", "P2SH"});

        List<AddressItem> addressesList = new ArrayList<>();
        for (GenericAddress address : receivingAddressesList) {
            BtcAddress btcAddress = (BtcAddress)address;
            String[] btcAddressLabels = checkNotNull(addressLabels.get(btcAddress.getType()));
            addressesList.add(new AddressItem(address,
                    btcAddressLabels[1],
                    btcAddressLabels[0],
                    SelectableRecyclerView.SRVAdapter.VIEW_TYPE_ITEM));
        }

        AddressViewAdapter adapter  = new AddressViewAdapter(addressesList, addressFirstItemWidth);
        receiversAddressesList.setAdapter(adapter);
        receiversAddressesList.setSelectListener(new SelectListener() {
            @Override
            public void onSelect(RecyclerView.Adapter adapter, int position) {
                AddressItem item = ((AddressViewAdapter) adapter).getItem(position);
                receivingAddress = item.getAddress();
                updateTransactionStatusAndUi();
            }
        });
        receiversAddressesList.setSelectedItem(receivingAddressesList.size() - 1);
    }

    @UiThread
    private void initFeeView() {
        feeValueList.setHasFixedSize(true);
        feeViewAdapter = new FeeViewAdapter(feeFirstItemWidth);
        feeValueList.setAdapter(feeViewAdapter);
        feeValueList.setSelectedItem(selectedFee);
        feeValueList.setSelectListener(new SelectListener() {
            @Override
            public void onSelect(RecyclerView.Adapter adapter, int position) {
                FeeItem item = ((FeeViewAdapter) adapter).getItem(position);
                selectedFee = Value.valueOf(item.value.type, item.feePerKb);
                btSend.setEnabled(false); //should be enabled(depends from tx status) after update tx
                updateTransactionStatusAndUi();
                ScrollView scrollView = findViewById(R.id.root);

                if(showSendBtn && scrollView.getMaxScrollAmount() - scrollView.getScaleY() > 0) {
                    scrollView.smoothScrollBy(0, scrollView.getMaxScrollAmount());
                    showSendBtn = false;
                }
            }
        });
    }

    @UiThread
    private void initFeeLvlView() {
        feeLvlList.setHasFixedSize(true);
        List<FeeLvlItem> feeLvlItems = new ArrayList<>();
        for (MinerFee fee : MinerFee.values()) {
            int blocks = 0;
            switch (fee) {
                case LOWPRIO:
                    blocks = 20;
                    break;
                case ECONOMIC:
                    blocks = 10;
                    break;
                case NORMAL:
                    blocks = 3;
                    break;
                case PRIORITY:
                    blocks = 1;
                    break;
            }
            String duration = Utils.formatBlockcountAsApproxDuration(this, blocks);
            feeLvlItems.add(new FeeLvlItem(fee, "~" + duration, SelectableRecyclerView.SRVAdapter.VIEW_TYPE_ITEM));
        }

        final FeeLvlViewAdapter feeLvlViewAdapter = new FeeLvlViewAdapter(feeLvlItems, feeFirstItemWidth);
        feeLvlList.setAdapter(feeLvlViewAdapter);
        feeLvlList.setSelectListener(new SelectListener() {
            @Override
            public void onSelect(RecyclerView.Adapter adapter, int position) {
                FeeLvlItem item = ((FeeLvlViewAdapter) adapter).getItem(position);
                feeLvl = item.minerFee;
                updateTransactionStatusAndUi();
                updateFeeDataset();
            }
        });
        feeLvlList.setSelectedItem(feeLvl);
    }

    private void updateFeeDataset() {
        List<FeeItem> feeItems = feeItemsBuilder.getFeeItemList(activeAccount.getBasedOnCoinType(), feeEstimation, feeLvl, estimateTxSize());
        feeViewAdapter.setDataset(feeItems);
        if (feeViewAdapter.getSelectedItem() < feeViewAdapter.getItemCount()) {
            FeeItem selectedFeeItem = feeViewAdapter.getItem(feeViewAdapter.getSelectedItem());
            if (selectedFeeItem != null && selectedFeeItem.feePerKb == selectedFee.value) {
                // everything is fine already
                return;
            }
        }
        if (isInRange(feeItems, selectedFee)) {
            feeValueList.setSelectedItem(selectedFee);
        } else {
            feeValueList.setSelectedItem(getCurrentFeeEstimation());
        }
    }

    private boolean isInRange(List<FeeItem> feeItems, Value fee) {
        return feeItems.get(0).feePerKb <= fee.value && fee.value <= feeItems.get(feeItems.size() - 1).feePerKb;
    }

    private int estimateTxSize() {
        if (transaction != null) {
            return transaction.getEstimatedTransactionSize();
        } else {
            return activeAccount.getTypicalEstimatedTransactionSize();
        }
    }

    // returns the amcountToSend in Bitcoin - it tries to get it from the entered amount and
    // only uses the ExchangeRate-Manager if we dont have it already converted
    private Value getValueToSend() {
        if (Value.isNullOrZero(amountToSend)) {
            return null;
        }
        return amountToSend;
    }

    private void setAmountToSend(Value toSend) {
        amountToSend = toSend;
    }

    private void verifyPaymentRequest(GenericAssetUri uri) {
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
        savedInstanceState.putSerializable(AMOUNT, amountToSend);
        savedInstanceState.putSerializable(RECEIVING_ADDRESS, receivingAddress);
        savedInstanceState.putString(TRANSACTION_LABEL, transactionLabel);
        savedInstanceState.putSerializable(FEE_LVL, feeLvl);
        savedInstanceState.putSerializable(SELECTED_FEE, selectedFee);
        savedInstanceState.putBoolean(PAYMENT_FETCHED, paymentFetched);
        savedInstanceState.putSerializable(ASSET_URI, genericUri);
        savedInstanceState.putSerializable(PAYMENT_REQUEST_HANDLER_ID, paymentRequestHandlerUuid);
        savedInstanceState.putSerializable(SIGNED_TRANSACTION, signedTransaction);
        savedInstanceState.putSerializable(FEE_ESTIMATION , feeEstimation);
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
        startActivityForResult(intent, REQUEST_BTC_ACCOUNT);
    }

    @OnClick(R.id.btScan)
    void onClickScan() {
        StringHandleConfig config = HandleConfigFactory.returnKeyOrAddressOrUriOrKeynode();
        ScanActivity.callMe(this, SCAN_RESULT_CODE, config);
    }

    @OnClick(R.id.btAddressBook)
    void onClickAddressBook() {
        Intent intent = new Intent(this, GetFromAddressBookActivity.class);
        startActivityForResult(intent, ADDRESS_BOOK_RESULT_CODE);
    }

    @OnClick(R.id.btManualEntry)
    void onClickManualEntry() {
        Intent intent = new Intent(this, ManualAddressEntry.class)
                .putExtra(ACCOUNT, activeAccount.getId())
                .putExtra(IS_COLD_STORAGE, isColdStorage);
        startActivityForResult(intent, MANUAL_ENTRY_RESULT_CODE);
    }

    @OnClick(R.id.btClipboard)
    void onClickClipboard() {
        GenericAssetUri uri = getUriFromClipboard();
        if (uri != null) {
            makeText(this, getResources().getString(R.string.using_address_from_clipboard), LENGTH_SHORT).show();
            receivingAddress = uri.getAddress();
            if (uri.getValue() != null && !uri.getValue().isNegative()) {
                amountToSend = uri.getValue();
            }
            updateTransactionStatusAndUi();
        }
    }

    @OnClick(R.id.btEnterAmount)
    void onClickAmount() {
        Value presetAmount = amountToSend;
        if (Value.isNullOrZero(presetAmount)) {
            // if no amount is set so far, use an unknown amount but in the current accounts currency
            presetAmount = Value.valueOf(activeAccount.getCoinType(), 0);
        }
        GetAmountActivity.callMeToSend(this, GET_AMOUNT_RESULT_CODE, activeAccount.getId(), presetAmount, selectedFee.value,
                activeAccount.getCoinType(), isColdStorage, receivingAddress);
    }

    @OnClick(R.id.btSend)
    void onClickSend() {
        if (isColdStorage || activeAccount instanceof HDAccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            signTransaction();
        } else {
            mbwManager.getVersionManager().showFeatureWarningIfNeeded(SendMainActivity.this,
                    featureMap.get(activeAccount.getCoinType()), true, new Runnable() {
                        @Override
                        public void run() {
                            mbwManager.runPinProtectedFunction(SendMainActivity.this, pinProtectedSignAndSend);
                        }
                    });
        }
        UUID receivingAccount = mbwManager.getWalletManager(false).getAccountByAddress(receivingAddress);
        if(receivingAccount != null) {
            MbwManager.getEventBus().post( new AccountChanged(receivingAccount));
        }
    }

    final Runnable pinProtectedSignAndSend = new Runnable() {
        @Override
        public void run() {
            // if we have a payment request, check if it is expired
            if (paymentRequestHandler != null && paymentRequestHandler.getPaymentRequestInformation().isExpired()) {
                makeText(SendMainActivity.this, getString(R.string.payment_request_not_sent_expired), LENGTH_LONG).show();
                return;
            }
            if (activeAccount instanceof ColuAccount) {
                sendTransaction();
            } else {
                signTransaction();
            }
        }
    };

    @SuppressLint("StaticFieldLeak")
    private void sendTransaction() {
        progressDialog = new ProgressDialog(SendMainActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.sending_assets, activeAccount.getCoinType().getSymbol()));
        progressDialog.show();
        disableButtons();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    if(transaction instanceof ColuTransaction) {
                        ((ColuTransaction) transaction).getFundingAccounts().add(Utils.getLinkedAccount(activeAccount, mbwManager.getWalletManager(false).getAccounts()));
                    }
                    activeAccount.signTx(transaction, AesKeyCipher.defaultKeyCipher());
                    activeAccount.broadcastTx(transaction);
                    return true;
                } catch (GenericTransactionBroadcastException |
                        KeyCipher.InvalidKeyCipher e) {
                    Log.e(TAG, "", e);
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean isSent) {
                super.onPostExecute(isSent);
                progressDialog.dismiss();
                if (isSent) {
                    mbwManager.getWalletManager(false).startSynchronization(activeAccount.getId());
                    Toast.makeText(SendMainActivity.this, R.string.transaction_sent, Toast.LENGTH_SHORT).show();
                    SendMainActivity.this.finish();
                } else {
                    Toast.makeText(SendMainActivity.this, getString(R.string.asset_failed_to_broadcast, activeAccount.getCoinType().getSymbol()), Toast.LENGTH_SHORT).show();
                    updateUi();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @OnClick(R.id.tvUnconfirmedWarning)
    void onClickUnconfirmedWarning() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.spending_unconfirmed_title))
                .setMessage(getString(R.string.spending_unconfirmed_description))
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Recalculate the transaction based on the current choices.
     */
    private void updateTransactionStatus() {
        Value toSend = getValueToSend();

        boolean hasAddressData = toSend != null && receivingAddress != null;

        try {
            if (paymentRequestHandler != null && paymentRequestHandler.hasValidPaymentRequest()) {
                transactionStatus = handlePaymentRequest(toSend);
            } else if (hasAddressData) {
                // createTx potentially takes long, if server interaction is involved
                transaction = activeAccount.createTx(receivingAddress, toSend, new FeePerKbFee(selectedFee));
                spendingUnconfirmed = activeAccount.isSpendingUnconfirmed(transaction);
                transactionStatus = TransactionStatus.OK;
            } else {
                transactionStatus = TransactionStatus.MISSING_ARGUMENTS;
            }
        } catch (GenericBuildTransactionException ex) {
            transactionStatus = TransactionStatus.MISSING_ARGUMENTS;
        } catch (GenericOutputTooSmallException ex) {
            transactionStatus = TransactionStatus.OUTPUT_TOO_SMALL;
        } catch (GenericInsufficientFundsException ex) {
            transactionStatus = TransactionStatus.INSUFFICIENT_FUNDS;
        }
    }

    // Handles BTC payment request
    private TransactionStatus handlePaymentRequest(Value toSend)
            throws GenericBuildTransactionException,
            GenericInsufficientFundsException,
            GenericOutputTooSmallException {
        PaymentRequestInformation paymentRequestInformation = paymentRequestHandler.getPaymentRequestInformation();
        OutputList outputs = paymentRequestInformation.getOutputs();

        // has the payment request an amount set?
        if (paymentRequestInformation.hasAmount()) {
            setAmountToSend(Value.valueOf(Utils.getBtcCoinType(), paymentRequestInformation.getOutputs().getTotalAmount()));
        } else {
            if (amountToSend == null) {
                return TransactionStatus.MISSING_ARGUMENTS;
            }
            // build new output list with user specified amount
            outputs = outputs.newOutputsWithTotalAmount(toSend.value);
        }

        AbstractBtcAccount btcAccount = (AbstractBtcAccount) activeAccount;
        transaction = btcAccount.createTxFromOutputList(outputs, new FeePerKbFee(selectedFee).getFeePerKb().value);
        spendingUnconfirmed = activeAccount.isSpendingUnconfirmed(transaction);
        receivingAddress = null;
        transactionLabel = paymentRequestInformation.getPaymentDetails().memo;
        return TransactionStatus.OK;
    }

    @SuppressLint("StaticFieldLeak")
    private void updateTransactionStatusAndUi() {
        if (updateUiAsyncTask != null) {
            updateUiAsyncTask.cancel(true);
        }
        updateUiAsyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                progressBarBusy.setVisibility(VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                updateTransactionStatus();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                updateUi();
                progressBarBusy.setVisibility(GONE);
            }
        };
        updateUiAsyncTask.execute();
    }

    @UiThread
    private void updateUi() {
        // TODO: profile. slow!
        updateFeeText();
        updateFeeDataset();
        updateRecipient();
        updateAmount();
        updateError();

        // Enable/disable send button
        btSend.setEnabled(transactionStatus == TransactionStatus.OK);
        findViewById(R.id.root).invalidate();
    }

    @UiThread
    private void updateRecipient() {
        boolean hasPaymentRequest = paymentRequestHandler != null && paymentRequestHandler.hasValidPaymentRequest();
        if (receivingAddress == null && !hasPaymentRequest) {
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
        if (receivingLabel != null) {
            label = receivingLabel;
        } else if (receivingAddress != null) {
            label = getAddressLabel(receivingAddress);
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
            String choppedAddress = AddressUtils.toMultiLineString(receivingAddress.toString());
            tvReceiver.setText(choppedAddress);
        }

        if (hasPaymentRequest) {
            PaymentRequestInformation paymentRequestInformation = paymentRequestHandler.getPaymentRequestInformation();
            if (paymentRequestInformation.hasValidSignature()) {
                tvReceiver.setText(paymentRequestInformation.getPkiVerificationData().displayName);
            } else {
                tvReceiver.setText(getString(R.string.label_unverified_recipient));
            }
        }

        // show address (if available - some PRs might have more than one address or a not decodeable input)
        if (hasPaymentRequest && receivingAddress != null) {
            tvReceiverAddress.setText(AddressUtils.toDoubleLineString(receivingAddress.toString()));
            tvReceiverAddress.setVisibility(VISIBLE);
        } else {
            tvReceiverAddress.setVisibility(GONE);
        }

        //Check the wallet manager to see whether its our own address, and whether we can spend from it
        WalletManager walletManager = mbwManager.getWalletManager(false);
        if (receivingAddress != null && walletManager.isMyAddress(receivingAddress)) {
            // Show a warning as we are sending to one of our own addresses
            tvWarning.setVisibility(VISIBLE);
            if (walletManager.hasPrivateKey(receivingAddress)) {
                tvWarning.setText(R.string.my_own_address_warning);
                tvWarning.setTextColor(getResources().getColor(R.color.yellow));
            } else {
                // ... which is read-only
                tvWarning.setText(R.string.read_only_warning);
                tvWarning.setTextColor(getResources().getColor(R.color.red));
            }
        } else {
            tvWarning.setVisibility(GONE);
        }

        //if present, show transaction label
        if (transactionLabel != null) {
            tvTransactionLabelTitle.setVisibility(VISIBLE);
            tvTransactionLabel.setVisibility(VISIBLE);
            tvTransactionLabel.setText(transactionLabel);
        } else {
            tvTransactionLabelTitle.setVisibility(GONE);
            tvTransactionLabel.setVisibility(GONE);
        }
    }

    private String getAddressLabel(GenericAddress address) {
        UUID accountId = mbwManager.getAccountId(address, activeAccount.getCoinType()).orNull();
        if (accountId != null) {
            // Get the name of the account
            return mbwManager.getMetadataStorage().getLabelByAccount(accountId);
        }
        // We don't have it in our accounts, look in address book, returns empty string by default
        return mbwManager.getMetadataStorage().getLabelByAddress(address);
    }

    private void updateAmount() {
        // Update Amount
        if (amountToSend == null) {
            // No amount to show
            tvAmountTitle.setText(R.string.enter_amount_title);
            tvAmount.setText("");
            tvAmountFiat.setVisibility(GONE);
        } else {
            tvAmountTitle.setText(R.string.amount_title);
            switch (transactionStatus) {
                case OUTPUT_TOO_SMALL:
                    // Amount too small
                    tvAmount.setText(ValueExtensionsKt.toStringWithUnit(getValueToSend(), mbwManager.getDenomination()));
                    tvAmountFiat.setVisibility(GONE);
                    break;
                case INSUFFICIENT_FUNDS:
                    // Insufficient funds
                    tvAmount.setText(ValueExtensionsKt.toStringWithUnit(amountToSend, mbwManager.getDenomination()));
                    break;
                default:
                    // Set Amount
                    if (!Value.isNullOrZero(amountToSend)) {
                        // show the user entered value as primary amount
                        Value primaryAmount = amountToSend;
                        Value alternativeAmount = mbwManager.getExchangeRateManager().get(primaryAmount,
                                primaryAmount.type.equals(activeAccount.getCoinType())
                                        ? mbwManager.getFiatCurrency() : activeAccount.getCoinType());
                        String sendAmount = ValueExtensionsKt.toStringWithUnit(primaryAmount, mbwManager.getDenomination());
                        if (!primaryAmount.getCurrencySymbol().equals(Utils.getBtcCoinType().getSymbol())) {
                            // if the amount is not in BTC, show a ~ to inform the user, its only approximate and depends
                            // on an FX rate
                            sendAmount = "~" + sendAmount;
                        }
                        tvAmount.setText(sendAmount);
                        if (Value.isNullOrZero(alternativeAmount)) {
                            tvAmountFiat.setVisibility(GONE);
                        } else {
                            // show the alternative amount
                            String alternativeAmountString =
                                    ValueExtensionsKt.toStringWithUnit(alternativeAmount, mbwManager.getDenomination());

                            if (!alternativeAmount.getCurrencySymbol().equals(Utils.getBtcCoinType().getSymbol())) {
                                // if the amount is not in BTC, show a ~ to inform the user, its only approximate and depends
                                // on a FX rate
                                alternativeAmountString = "~" + alternativeAmountString;
                            }

                            tvAmountFiat.setText(alternativeAmountString);
                            tvAmountFiat.setVisibility(VISIBLE);
                        }
                    } else {
                        tvAmount.setText("");
                        tvAmountFiat.setText("");
                    }
                    break;
            }
        }

        // Disable Amount button if we have a payment request with valid amount
        if (paymentRequestHandler != null && paymentRequestHandler.getPaymentRequestInformation().hasAmount()) {
            btEnterAmount.setEnabled(false);
        }
    }

    private Value getCurrentFeeEstimation() {
        switch (feeLvl) {
            case LOWPRIO:
                return Value.valueOf(activeAccount.getCoinType(), feeEstimation.getLow().value);
            case ECONOMIC:
                return Value.valueOf(activeAccount.getCoinType(), feeEstimation.getEconomy().value);
            case NORMAL:
                return Value.valueOf(activeAccount.getCoinType(), feeEstimation.getNormal().value);
            case PRIORITY:
                return Value.valueOf(activeAccount.getCoinType(), feeEstimation.getHigh().value);
            default:
                return Value.valueOf(activeAccount.getCoinType(), feeEstimation.getNormal().value);
        }
    }

    void updateError() {
        boolean tvErrorShow;
        switch (transactionStatus) {
            case OUTPUT_TOO_SMALL:
                // Amount too small
                if (activeAccount instanceof CoinapultAccount) {
                    CoinapultAccount coinapultAccount = (CoinapultAccount) activeAccount;
                    tvError.setText(getString(R.string.coinapult_amount_too_small,
                            ((Currency) coinapultAccount.getCoinType()).minimumConversationValue,
                            coinapultAccount.getCoinType().getSymbol())
                    );
                } else {
                    tvError.setText(R.string.amount_too_small_short);
                }
                tvErrorShow = true;
                break;
            case INSUFFICIENT_FUNDS:
                tvError.setText(R.string.insufficient_funds);
                tvErrorShow = true;
                break;
            case INSUFFICIENT_FUNDS_FOR_FEE:
                tvError.setText(R.string.requires_btc_amount);
                tvErrorShow = true;
                break;
            default:
                tvErrorShow = false;
                //check if we need to warn the user about unconfirmed funds
                tvUnconfirmedWarning.setVisibility(spendingUnconfirmed ? VISIBLE : GONE);
                break;
        }

        if(tvErrorShow && tvError.getVisibility() != VISIBLE) {
            AnimationUtils.expand(tvError, null);
        } else if(!tvErrorShow && tvError.getVisibility() == VISIBLE) {
            AnimationUtils.collapse(tvError, null);
        }
    }

    @UiThread
    private void updateFeeText() {
        // Update Fee-Display
        String feeWarning = null;
        tvFeeWarning.setOnClickListener(null);
        if (selectedFee.value == 0) {
            feeWarning = getString(R.string.fee_is_zero);
        }
        if (transaction != null && transaction.type instanceof BitcoinBasedCryptoCurrency
                && !(transaction.type instanceof ColuMain)) {
            // shows number of Ins/Outs and estimated size of transaction for bitcoin based currencies
            UnsignedTransaction unsigned = ((BitcoinBasedGenericTransaction) transaction).getUnsignedTx();
            if (unsigned != null) {
                int inCount = unsigned.getFundingOutputs().length;
                int outCount = unsigned.getOutputs().length;

                FeeEstimator feeEstimator = new FeeEstimatorBuilder().setArrayOfInputs(unsigned.getFundingOutputs())
                        .setArrayOfOutputs(unsigned.getOutputs())
                        .createFeeEstimator();
                int size = feeEstimator.estimateTransactionSize();

                tvSatFeeValue.setText(inCount + " In- / " + outCount + " Outputs, ~" + size + " bytes");

                long fee = unsigned.calculateFee();
                if (fee != size * selectedFee.value / 1000) {
                    Value value = Value.valueOf(activeAccount.getCoinType(), fee);
                    Value fiatValue = mbwManager.getExchangeRateManager().get(value, mbwManager.getFiatCurrency());
                    String fiat = "";
                    if (fiatValue != null) {
                        fiat = ValueExtensionsKt.toStringWithUnit(fiatValue, mbwManager.getDenomination());
                    }
                    fiat = fiat.isEmpty() ? "" : " (" + fiat + ")";
                    feeWarning = getString(R.string.fee_change_warning
                            , ValueExtensionsKt.toStringWithUnit(value, mbwManager.getDenomination())
                            , fiat);
                    tvFeeWarning.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new AlertDialog.Builder(SendMainActivity.this)
                                    .setMessage(R.string.fee_change_description)
                                    .setPositiveButton(R.string.button_ok, null).create()
                                    .show();
                        }
                    });
                }
            }
        }
        tvFeeWarning.setVisibility(feeWarning != null ? View.VISIBLE : View.GONE);
        tvFeeWarning.setText(feeWarning != null ? Html.fromHtml(feeWarning) : null);
        tvStaleWarning.setVisibility(showStaleWarning ? VISIBLE : GONE);
    }

    @Override
    protected void onResume() {
        MbwManager.getEventBus().register(this);

        // If we don't have a fresh exchange rate, now is a good time to request one, as we will need it in a minute
        if (!mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable()) {
            mbwManager.getExchangeRateManager().requestRefresh();
        }

        btClipboard.setEnabled(getUriFromClipboard() != null);
        pbSend.setVisibility(GONE);

        updateTransactionStatusAndUi();
        super.onResume();
        if (activityResultDialog != null) {
            activityResultDialog.show(getSupportFragmentManager(), "ActivityResultDialog");
            activityResultDialog = null;
        }
    }

    @Override
    protected void onPause() {
        MbwManager.getEventBus().unregister(this);
        mbwManager.getVersionManager().closeDialog();
        super.onPause();
    }

    protected void signTransaction() {
        // if we have a payment request, check if it is expired
        if (paymentRequestHandler != null && paymentRequestHandler.getPaymentRequestInformation().isExpired()) {
            makeText(this, getString(R.string.payment_request_not_sent_expired), LENGTH_LONG).show();
            return;
        }

        disableButtons();
        SignTransactionActivity.callMe(this, activeAccount.getId(), isColdStorage, transaction, SIGN_TRANSACTION_REQUEST_CODE);
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

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + " and resultCode=" + resultCode);
        switch (requestCode) {
            case SCAN_RESULT_CODE: onScanResult(resultCode, intent); break;
            case ADDRESS_BOOK_RESULT_CODE: onAddressBookResult(resultCode, intent); break;
            case MANUAL_ENTRY_RESULT_CODE: onManualEntryResult(resultCode, intent); break;
            case GET_AMOUNT_RESULT_CODE: onGetAmountResult(resultCode, intent); break;
            case SIGN_TRANSACTION_REQUEST_CODE: onSignTransactionResult(resultCode, intent); break;
            case REQUEST_PAYMENT_HANDLER: onRequestPaymentResult(resultCode, intent); break;
            default: super.onActivityResult(requestCode, resultCode, intent); break;
        }
    }

    private void onScanResult(int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            if (intent != null) {
                String error = intent.getStringExtra(StringHandlerActivity.RESULT_ERROR);
                if (error != null) {
                    makeText(this, error, LENGTH_LONG).show();
                }
            }
        } else {
            ResultType type = (ResultType) intent.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY);
            switch (type) {
                case PRIVATE_KEY:
                    InMemoryPrivateKey key = getPrivateKey(intent);
                    PublicKey publicKey = key.getPublicKey();
                    for (Address address : publicKey.getAllSupportedAddresses(mbwManager.getNetwork()).values()) {
                        receivingAddressesList.add(AddressUtils.fromAddress(address));
                    }
                    if (receivingAddressesList.size() == 1) {
                        receivingAddress = receivingAddressesList.get(0);
                    } else {
                        setUpMultiAddressView();
                    }
                    break;
                case ADDRESS:
                    receivingAddress = getAddress(intent);
                    break;
                case ASSET_URI:
                    GenericAssetUri uri = getAssetUri(intent);
                    if (uri instanceof BitcoinUri && ((BitcoinUri) uri).getCallbackURL() != null) {
                        //we contact the merchant server instead of using the params
                        genericUri = uri;
                        paymentFetched = false;
                        verifyPaymentRequest(genericUri);
                        return;
                    }
                    receivingAddress = uri.getAddress();
                    transactionLabel = uri.getLabel();
                    if (uri.getValue() != null && uri.getValue().isPositive()) {
                        //we set the amount to the one contained in the qr code, even if another one was entered previously
                        if (!Value.isNullOrZero(amountToSend)) {
                            makeText(this, R.string.amount_changed, LENGTH_LONG).show();
                        }
                        setAmountToSend(uri.getValue());
                    }
                    break;
                case HD_NODE:
                    setReceivingAddressFromKeynode(getHdKeyNode(intent));
                    break;
                case POP_REQUEST:
                    PopRequest popRequest = getPopRequest(intent);
                    startActivity(new Intent(this, PopActivity.class)
                            .putExtra("popRequest", popRequest)
                            .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                    break;
                default:
                    throw new IllegalStateException("Unexpected result type from scan: " + type.toString());
            }
        }
        updateTransactionStatusAndUi();
    }

    private void onAddressBookResult(int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            // Get result from address chooser
            GenericAddress address = (GenericAddress) intent.getSerializableExtra(AddressBookFragment.ADDRESS_RESULT_NAME);
            if (address == null) {
                return;
            }
            receivingAddress = address;
            if (intent.getExtras().containsKey(AddressBookFragment.ADDRESS_RESULT_LABEL)) {
                receivingLabel = intent.getStringExtra(AddressBookFragment.ADDRESS_RESULT_LABEL);
            }
            // this is where colusend is calling tryCreateUnsigned
            // why is amountToSend not set ?
            updateTransactionStatusAndUi();
        }
    }

    private void onManualEntryResult(int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            receivingAddress = (GenericAddress) checkNotNull(intent
                    .getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME));

            updateTransactionStatusAndUi();
        }
    }

    private void onGetAmountResult(int resultCode, Intent intent) {
        if(resultCode == RESULT_OK){
            // Get result from AmountEntry
            Value enteredAmount = (Value) intent.getSerializableExtra(GetAmountActivity.AMOUNT);
            setAmountToSend(enteredAmount);
            updateTransactionStatusAndUi();
        }
    }

    private void onSignTransactionResult(int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            signedTransaction = (GenericTransaction) checkNotNull(intent.getSerializableExtra(SIGNED_TRANSACTION));
            // if we have a payment request with a payment_url, handle the send differently:
            if (paymentRequestHandler != null
                    && paymentRequestHandler.getPaymentRequestInformation().hasPaymentCallbackUrl()) {

                // check again if the payment request isn't expired, as signing might have taken some time
                // (e.g. with external signature provider)
                if (!paymentRequestHandler.getPaymentRequestInformation().isExpired()) {
                    // first send signed tx directly to the Merchant, and broadcast
                    // it only if we get a ACK from him (in paymentRequestAck)
                    BtcTransaction btcTransaction = (BtcTransaction) signedTransaction;
                    Address address = new Address(activeAccount.getReceiveAddress().getBytes());
                    paymentRequestHandler.sendResponse(btcTransaction.getTx(), address);
                } else {
                    makeText(this, getString(R.string.payment_request_not_sent_expired), LENGTH_LONG).show();
                }
            } else {
                activityResultDialog = BroadcastDialog.create(activeAccount, isColdStorage, signedTransaction);
            }
        }
    }

    private void onRequestPaymentResult(int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            paymentRequestHandlerUuid = checkNotNull(intent.getStringExtra("REQUEST_PAYMENT_HANDLER_ID"));
            paymentRequestHandler = (PaymentRequestHandler) mbwManager.getBackgroundObjectsCache()
                    .getIfPresent(paymentRequestHandlerUuid);
            updateTransactionStatusAndUi();
        } else {
            // user canceled - also leave this activity
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void broadcastResult(@NonNull BroadcastResult broadcastResult) {
        Intent result = new Intent();
        if (broadcastResult.getResultType() == BroadcastResultType.SUCCESS) {
            if (transactionLabel != null) {
                mbwManager.getMetadataStorage().storeTransactionLabel(HexUtils.toHex(signedTransaction.getId()), transactionLabel);
            }
            String hash = HexUtils.toHex(signedTransaction.getId());
            String fiat = getFiatValue();
            if (fiat != null) {
                transactionFiatValuePref.edit().putString(hash, fiat).apply();
            }
            result.putExtra(Constants.TRANSACTION_FIAT_VALUE_KEY, fiat)
                  .putExtra(Constants.TRANSACTION_ID_INTENT_KEY, hash);
        }
        setResult(broadcastResult.getResultType() == BroadcastResultType.SUCCESS ? RESULT_OK : RESULT_CANCELED, result);
        finish();
    }

    private String getFiatValue() {
        Value fiat = mbwManager.getExchangeRateManager().get(amountToSend, mbwManager.getCurrencySwitcher().getCurrentFiatCurrency());
        return fiat != null ? ValueExtensionsKt.toStringWithUnit(fiat) : null;
    }

    private void setReceivingAddressFromKeynode(HdKeyNode hdKeyNode) {
        progressDialog = ProgressDialog.show(this, "", getString(R.string.retrieving_pubkey_address), true);
        receivingAcc = mbwManager.getWalletManager(true)
                .createAccounts(new UnrelatedHDAccountConfig(Collections.singletonList(hdKeyNode))).get(0);
        xpubSyncing = true;
        if (!mbwManager.getWalletManager(true).startSynchronization(receivingAcc)) {
            MbwManager.getEventBus().post(new SyncFailed());
        }
    }

    private GenericAssetUri getUriFromClipboard() {
        String string = Utils.getClipboardString(this).trim();
        if (string.matches("[a-zA-Z0-9]+")) {
            // Raw format
            GenericAddress address = activeAccount.getCoinType().parseAddress(string);
            return address != null ? GenericAssetUriParser.createUriByCoinType(activeAccount.getCoinType(),
                    address, null, null, null) : null;
        } else {
            GenericAssetUri uri = mbwManager.getContentResolver().resolveUri(string);
            if (uri == null || uri.getAddress() == null) {
                return null;
            }
            return (uri.getAddress().getCoinType() == activeAccount.getCoinType()) ? uri : null;
        }
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
            activityResultDialog = BroadcastDialog.create(activeAccount, isColdStorage, signedTransaction);
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
        if (xpubSyncing) {
            xpubSyncing = false;
            WalletAccount account = mbwManager.getWalletManager(true).getAccount(receivingAcc);
            receivingAddress =  account.getReceiveAddress();
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            updateTransactionStatusAndUi();
        }
    }

    @Subscribe
    public void syncFailed(SyncFailed event) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        makeText(this, R.string.warning_sync_failed_reusing_first, LENGTH_LONG).show();
    }
}
