package com.mycelium.wallet.external.changelly;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.SelectListener;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.activity.view.ValueKeyboard;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransactionOffer;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;
import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;

public class ChangellyActivity extends Activity {
    private static String TAG = "ChangellyActivity";

    public enum ChangellyUITypes {
        Loading,
        RetryLater,
        Main
    }

    private TextView tvMinAmountValue;
    @BindView(R.id.fromValue)
    TextView etAmount;

    @BindView(R.id.fromCurrency)
    TextView fromCurrency;

    @BindView(R.id.toValue)
    TextView tvOfferValue;

    @BindView(R.id.btChangellyCreateTransaction)
    Button btTakeOffer;

    @BindView(R.id.currencySelector)
    SelectableRecyclerView currencySelector;

    @BindView(R.id.accountSelector)
    SelectableRecyclerView accountSelector;

    @BindView(R.id.numeric_keyboard)
    ValueKeyboard valueKeyboard;

    private CurrencyAdapter currencyAdapter;
    private AccountAdapter accountAdapter;
    private Receiver receiver;
    private MbwManager mbwManager;

    private void requestOfferFunction() {
        String txtMinAmount = tvMinAmountValue.getText().toString();
        String txtAmount = etAmount.getText().toString();
        Double dblMinAmount;
        Double dblAmount;
        try {
            dblMinAmount = Double.parseDouble(txtMinAmount);
            dblAmount = Double.parseDouble(txtAmount);
        } catch (NumberFormatException e) {
            Toast.makeText(ChangellyActivity.this, "Error parsing double values", Toast.LENGTH_SHORT).show();
            return;
        }
        if (txtMinAmount.compareToIgnoreCase(getString(R.string.changelly_loading)) == 0) {
            Toast.makeText(ChangellyActivity.this, "Please wait while loading minimum amount information.", Toast.LENGTH_SHORT).show();
            return;
        } else if (dblAmount.compareTo(dblMinAmount) < 0) {
            toast("Error, amount is lower than minimum required.");
            return;
        } // TODO: compare with maximum
        CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
        Intent changellyServiceIntent = new Intent(this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_EXCHANGE_AMOUNT)
                .putExtra(ChangellyService.FROM, item.currency)
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, dblAmount);
        startService(changellyServiceIntent);
        toast("Waiting for offer...");
    }

    private void requestReversOfferFunction() {
        String txtAmount = tvOfferValue.getText().toString();
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(txtAmount);
        } catch (NumberFormatException e) {
            Toast.makeText(ChangellyActivity.this, "Error parsing double values", Toast.LENGTH_SHORT).show();
            return;
        }
        CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
        Intent changellyServiceIntent = new Intent(this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_EXCHANGE_AMOUNT)
                .putExtra(ChangellyService.TO, item.currency)
                .putExtra(ChangellyService.FROM, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, dblAmount);
        startService(changellyServiceIntent);
        toast("Waiting for offer...");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_activity);
        ButterKnife.bind(this);
        mbwManager = MbwManager.getInstance(this);

        tvMinAmountValue = (TextView) findViewById(R.id.tvMinAmountValue);

        tvMinAmountValue.setVisibility(View.GONE); // cannot edit field before selecting a currency

        valueKeyboard.setMaxDecimals(8);
        valueKeyboard.setInputListener(new ValueKeyboard.SimpleInputListener() {
            @Override
            public void done() {
                currencySelector.setVisibility(View.VISIBLE);
                accountSelector.setVisibility(View.VISIBLE);
            }
        });

        currencySelector.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        accountSelector.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        int senderFinalWidth = getWindowManager().getDefaultDisplay().getWidth();
        int firstItemWidth = (senderFinalWidth - getResources().getDimensionPixelSize(R.dimen.item_dob_width)) / 2;

        currencyAdapter = new CurrencyAdapter(firstItemWidth);
        currencySelector.setAdapter(currencyAdapter);
        currencySelector.setSelectListener(new SelectListener() {
            @Override
            public void onSelect(RecyclerView.Adapter adapter, int position) {
                CurrencyAdapter.Item item = currencyAdapter.getItem(position);
                if (item != null) {
                    fromCurrency.setText(item.currency);
                    etAmount.setText(null);
                    tvMinAmountValue.setText(R.string.changelly_loading);
                    tvOfferValue.setText("");
                    tvMinAmountValue.setVisibility(View.VISIBLE);
                    // load min amount
                    Intent changellyServiceIntent = new Intent(ChangellyActivity.this, ChangellyService.class)
                            .setAction(ChangellyService.ACTION_GET_MIN_EXCHANGE)
                            .putExtra(ChangellyService.FROM, item.currency)
                            .putExtra(ChangellyService.TO, ChangellyService.BTC);
                    startService(changellyServiceIntent);
                }
            }
        });

        accountAdapter = new AccountAdapter(mbwManager
                , mbwManager.getWalletManager(false).getActiveAccounts(), firstItemWidth);
        accountSelector.setAdapter(accountAdapter);

        //display the loading spinner
        setLayout(ChangellyActivity.ChangellyUITypes.Loading);

        receiver = new Receiver();
        // The filter's action is BROADCAST_ACTION
        for (String action : new String[]{
                ChangellyService.INFO_CURRENCIES,
                ChangellyService.INFO_EXCH_AMOUNT,
                ChangellyService.INFO_MIN_AMOUNT,
                ChangellyService.INFO_TRANSACTION,
                ChangellyService.INFO_ERROR}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        }

        Intent changellyServiceIntent = new Intent(this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_CURRENCIES);
        startService(changellyServiceIntent);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /* Activity UI logic Start */
    private void setLayout(ChangellyUITypes uiType) {
        findViewById(R.id.llChangellyValidationWait).setVisibility(View.GONE);
        findViewById(R.id.llChangellyLoadingProgress).setVisibility(View.GONE); // always gone
        findViewById(R.id.llChangellyErrorWrapper).setVisibility(View.GONE);
        findViewById(R.id.llChangellyMain).setVisibility(View.GONE);
        switch (uiType) {
            case Loading:
                findViewById(R.id.llChangellyValidationWait).setVisibility(View.VISIBLE);
                break;
            case RetryLater:
                findViewById(R.id.llChangellyErrorWrapper).setVisibility(View.VISIBLE);
            case Main:
                findViewById(R.id.llChangellyMain).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (valueKeyboard.getVisibility() == View.VISIBLE) {
            valueKeyboard.done();
        } else if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    private boolean avoidTextChangeEvent = false;

    @OnTextChanged(value=R.id.fromValue,callback = AFTER_TEXT_CHANGED)
    public void afterEditTextInputFrom(Editable editable){
        if(!avoidTextChangeEvent) {
            requestOfferFunction();
        }
    }

    @OnTextChanged(value=R.id.toValue,callback = AFTER_TEXT_CHANGED)
    public void afterEditTextInputTo(Editable editable){
        if(!avoidTextChangeEvent) {
            requestReversOfferFunction();
        }
    }

    @OnClick(R.id.fromValue)
    void clickFromValue() {
        valueKeyboard.setVisibility(View.VISIBLE);
        valueKeyboard.setInputTextView(etAmount);
        valueKeyboard.setEntry(etAmount.getText().toString());
    }

    @OnClick(R.id.toValue)
    void clickToValue() {
        valueKeyboard.setVisibility(View.VISIBLE);
        valueKeyboard.setInputTextView(tvOfferValue);
        valueKeyboard.setEntry(tvOfferValue.getText().toString());
        currencySelector.setVisibility(View.GONE);
        accountSelector.setVisibility(View.GONE);
    }

    @OnClick(R.id.btChangellyCreateTransaction)
    void offerClick() {
        String txtMinAmount = tvMinAmountValue.getText().toString();
        String txtAmount = etAmount.getText().toString();
        Double dblMinAmount;
        Double dblAmount;

        try {
            dblMinAmount = Double.parseDouble(txtMinAmount);
            dblAmount = Double.parseDouble(txtAmount);
//                    dblOffer = Double.parseDouble(txtOffer);
        } catch (NumberFormatException e) {
            toast("Error parsing double values");
            return;
        }
        if (txtMinAmount.compareToIgnoreCase(getString(R.string.changelly_loading)) == 0) {
            toast("Please wait while loading minimum amount information.");
            return;
        } else if (dblAmount.compareTo(dblMinAmount) < 0) {
            toast("Error, amount is lower than minimum required.");
            return;
        } // TODO: compare with maximum
        CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
        WalletAccount walletAccount = accountAdapter.getItem(accountSelector.getSelectedItem()).account;
        Intent changellyServiceIntent = new Intent(ChangellyActivity.this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_CREATE_TRANSACTION)
                .putExtra(ChangellyService.FROM, item.currency)
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, dblAmount)
                .putExtra(ChangellyService.DESTADDRESS, walletAccount.getReceivingAddress().get().toString());
        startService(changellyServiceIntent);
        toast("Accepted offer...");
    }

    class Receiver extends BroadcastReceiver {
        private Receiver() {
        }  // prevents instantiation

        @Override
        public void onReceive(Context context, Intent intent) {
            String from, to;
            double amount;

            switch (intent.getAction()) {
                case ChangellyService.INFO_CURRENCIES:
                    Log.d(TAG, "receiver, got currencies");
                    ArrayList<String> currenciesRes = intent.getStringArrayListExtra(ChangellyService.CURRENCIES);
                    if (currenciesRes != null) {
                        Log.d(TAG, "currencies=" + currenciesRes);

                        List<CurrencyAdapter.Item> itemList = new ArrayList<>();
                        itemList.add(new CurrencyAdapter.Item(null, CurrencyAdapter.VIEW_TYPE_PADDING));
                        for (String curr : currenciesRes) {
                            if (!curr.equalsIgnoreCase("btc")) {
                                itemList.add(new CurrencyAdapter.Item(curr.toUpperCase(), CurrencyAdapter.VIEW_TYPE_ITEM));
                            }
                        }
                        itemList.add(new CurrencyAdapter.Item(null, CurrencyAdapter.VIEW_TYPE_PADDING));
                        currencyAdapter.setItems(itemList);
                        setLayout(ChangellyUITypes.Main);
                    }
                    break;
                case ChangellyService.INFO_MIN_AMOUNT:
                    from = intent.getStringExtra(ChangellyService.FROM);
                    to = intent.getStringExtra(ChangellyService.TO);
                    amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);
                    CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
                    if (from != null && to != null && to.compareToIgnoreCase(ChangellyService.BTC) == 0
                            && from.compareToIgnoreCase(item.currency) == 0) {
                        Log.d(TAG, "Received minimum amount: " + amount + " " + from);
                        tvMinAmountValue.setText(Double.toString(amount));
                    }
                    break;
                case ChangellyService.INFO_EXCH_AMOUNT:
                    from = intent.getStringExtra(ChangellyService.FROM);
                    to = intent.getStringExtra(ChangellyService.TO);
                    amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);
                    item = currencyAdapter.getItem(currencySelector.getSelectedItem());
                    if (from != null && to != null) {
                        avoidTextChangeEvent = true;
                        if (to.equalsIgnoreCase(ChangellyService.BTC)
                                && from.equalsIgnoreCase(item.currency)) {
                            Log.d(TAG, "Received offer: " + amount + " " + to);
                            tvOfferValue.setText(Double.toString(amount));
                        } else if (from.equalsIgnoreCase(ChangellyService.BTC)
                                && to.equalsIgnoreCase(item.currency)) {
                            Log.d(TAG, "Received offer: " + amount + " " + to);
                            etAmount.setText(Double.toString(amount));
                        }
                        avoidTextChangeEvent = false;
                    }
                    //TODO: enable request offer
                    break;
                case ChangellyService.INFO_TRANSACTION:
                    ChangellyTransactionOffer offer = (ChangellyTransactionOffer) intent.getSerializableExtra(ChangellyService.OFFER);
                    String txtOffer = tvOfferValue.getText().toString();
                    Double dblOffer;
                    try {
                        dblOffer = Double.parseDouble(txtOffer);

                    } catch (NumberFormatException e) {
                        Toast.makeText(ChangellyActivity.this,
                                "Service unavailable",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    offer.amountTo = dblOffer;
                    Intent offerIntent = new Intent(ChangellyActivity.this, ChangellyOfferActivity.class)
                            .putExtra(ChangellyService.OFFER, offer);
                    startActivity(offerIntent);
                    break;
                case INFO_ERROR:
                    Toast.makeText(ChangellyActivity.this,
                            "Service unavailable",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}
