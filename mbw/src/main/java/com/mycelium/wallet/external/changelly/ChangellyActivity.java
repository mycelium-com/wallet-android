package com.mycelium.wallet.external.changelly;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.wallet.R;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransactionOffer;

import java.util.ArrayList;

import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;

public class ChangellyActivity extends Activity {
    private static String TAG = "ChangellyActivity";

    public enum ChangellyUITypes {
        Loading,
        RetryLater,
        Main
    }

    private ListView currenciesListView;
    private CurrenciesAdapter adapter;
    private CurrencyInfo selectedCurrency;

    private TextView tvCurrValue;
    private TextView tvMinAmountValue;
    private TextView tvOfferValue;
    private TextView tvTandC;
    private CheckBox cbTandC;
    private EditText etAmount;
    private Button btTakeOffer;
    private View llChangellyCurrInfo;
    private Receiver receiver;

    private ArrayList<String> currencies;

    private ArrayList<CurrencyInfo> currenciesList;

    //the user wallet address
    private String _walletAddress;

    class Receiver extends BroadcastReceiver {
        private Receiver() { }  // prevents instantiation
        @Override
        public void onReceive(Context context, Intent intent) {
                    String from, to;
                    double amount;
                    switch(intent.getAction()) {
                        case ChangellyService.INFO_CURRENCIES:
                            Log.d(TAG,"receiver, got currencies");
                            ArrayList<String> currenciesRes = intent.getStringArrayListExtra(ChangellyService.CURRENCIES);
                            if(currenciesRes != null) {
                                Log.d(TAG, "currencies=" + currenciesRes);
                                // convert to upper case
                                currencies = new ArrayList<String>();
                                for(String curr: currenciesRes) {
                                        currencies.add(curr.toUpperCase());
                                }
                                // update UI
                                currenciesList.clear();
                                for(String curr: currencies) {
                                    currenciesList.add(new CurrencyInfo(curr, R.drawable.changelly_square_logo_dark));
                                }
                                adapter.notifyDataSetChanged();
                                setLayout(ChangellyUITypes.Main);
                            }
                            break;
                        case ChangellyService.INFO_MIN_AMOUNT:
                            from = intent.getStringExtra(ChangellyService.FROM);
                            to = intent.getStringExtra(ChangellyService.TO);
                            amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);
                            if(from != null && to != null && to.compareToIgnoreCase(ChangellyService.BTC)==0
                                    && from.compareToIgnoreCase(selectedCurrency.getName())==0) {
                                Log.d(TAG, "Received minimum amount: " + amount + " " + from);
                                tvMinAmountValue.setText(Double.toString(amount));
                            }
                            break;
                        case ChangellyService.INFO_EXCH_AMOUNT:
                            from = intent.getStringExtra(ChangellyService.FROM);
                            to = intent.getStringExtra(ChangellyService.TO);
                            amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);
                            if(from != null && to != null && to.compareToIgnoreCase(ChangellyService.BTC)==0
                                    && from.compareToIgnoreCase(selectedCurrency.getName())==0) {
                                Log.d(TAG, "Received offer: " + amount + " " + to);
                                tvOfferValue.setText(Double.toString(amount));
                            }
                            //TODO: enable request offer
                            break;
                        case ChangellyService.INFO_TRANSACTION:
                            ChangellyTransactionOffer offer = (ChangellyTransactionOffer) intent.getSerializableExtra(ChangellyService.OFFER);
                            String txtOffer = tvOfferValue.getText().toString();
                            Double dblOffer;
                            try {
                                dblOffer = Double.parseDouble(txtOffer);

                            } catch(NumberFormatException e) {
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

    private void requestOfferFunction() {
        String txtMinAmount = tvMinAmountValue.getText().toString();
        String txtAmount = etAmount.getText().toString();
        Double dblMinAmount;
        Double dblAmount;
        try {
            dblMinAmount = Double.parseDouble(txtMinAmount);
            dblAmount = Double.parseDouble(txtAmount);
        } catch(NumberFormatException e) {
            Toast.makeText(ChangellyActivity.this, "Error parsing double values", Toast.LENGTH_SHORT).show();
            return;
        }
        if(txtMinAmount.compareToIgnoreCase(getString(R.string.changelly_loading)) == 0) {
            Toast.makeText(ChangellyActivity.this, "Please wait while loading minimum amount information.", Toast.LENGTH_SHORT).show();
            return;
        } else if(dblAmount.compareTo(dblMinAmount)  < 0) {
            toast("Error, amount is lower than minimum required.");
            return;
        } // TODO: compare with maximum
        Intent changellyServiceIntent = new Intent(this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_EXCHANGE_AMOUNT)
                .putExtra(ChangellyService.FROM, selectedCurrency.getName())
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, dblAmount);
        startService(changellyServiceIntent);
        toast("Waiting for offer...");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_activity);

        _walletAddress = getIntent().getExtras().getString("walletAddress");
        currenciesListView = (ListView) findViewById(R.id.list);
        currenciesList = new ArrayList<>();

        tvCurrValue = (TextView) findViewById(R.id.tvSelectedCurrValue);
        tvMinAmountValue = (TextView) findViewById(R.id.tvMinAmountValue);
        tvOfferValue = (TextView) findViewById(R.id.tvOfferValue);
        etAmount = (EditText) findViewById(R.id.tvAmountValue);
        tvTandC = (TextView) findViewById(R.id.tvTandC);
        cbTandC = (CheckBox) findViewById(R.id.cbTandC);
        llChangellyCurrInfo = findViewById(R.id.llChangellyCurrInfo);
        llChangellyCurrInfo.setVisibility(View.GONE); // cannot edit field before selecting a currency

        tvTandC.setMovementMethod(LinkMovementMethod.getInstance());

        cbTandC.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isTandCChecked) {
                        btTakeOffer.setEnabled(isTandCChecked);
                    }
                }
        );
        etAmount.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        requestOfferFunction();
                    }
                }
        );

        btTakeOffer = (Button) findViewById(R.id.btChangellyCreateTransaction);
        btTakeOffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txtMinAmount = tvMinAmountValue.getText().toString();
                String txtAmount = etAmount.getText().toString();
                Double dblMinAmount;
                Double dblAmount;

                try {
                    dblMinAmount = Double.parseDouble(txtMinAmount);
                    dblAmount = Double.parseDouble(txtAmount);
//                    dblOffer = Double.parseDouble(txtOffer);
                } catch(NumberFormatException e) {
                    toast("Error parsing double values");
                    return;
                }
                if(txtMinAmount.compareToIgnoreCase(getString(R.string.changelly_loading)) == 0) {
                    toast("Please wait while loading minimum amount information.");
                    return;
                } else if(dblAmount.compareTo(dblMinAmount)  < 0) {
                    toast("Error, amount is lower than minimum required.");
                    return;
                } // TODO: compare with maximum
                Intent changellyServiceIntent = new Intent(ChangellyActivity.this, ChangellyService.class)
                        .setAction(ChangellyService.ACTION_CREATE_TRANSACTION)
                        .putExtra(ChangellyService.FROM, selectedCurrency.getName())
                        .putExtra(ChangellyService.TO, ChangellyService.BTC)
                        .putExtra(ChangellyService.AMOUNT, dblAmount)
                        .putExtra(ChangellyService.DESTADDRESS, _walletAddress);
                startService(changellyServiceIntent);
                toast("Accepted offer...");
            }
        });
        btTakeOffer.setEnabled(false);

        adapter = new CurrenciesAdapter(this, R.layout.changelly_currencies_list_item, currenciesList);
        adapter.setClickListener(new CurrenciesAdapter.ClickListener() {
            @Override
            public void itemClick(CurrencyInfo info) {
                // request minimum amount and price from service
                if(info != null) {
                    etAmount.setText(null);
                    selectedCurrency = info;
                    tvCurrValue.setText(info.getName());
                    tvMinAmountValue.setText(R.string.changelly_loading);
                    tvOfferValue.setText("");
                    llChangellyCurrInfo.setVisibility(View.VISIBLE);
                    // load min amount
                    Intent changellyServiceIntent = new Intent(ChangellyActivity.this, ChangellyService.class)
                            .setAction(ChangellyService.ACTION_GET_MIN_EXCHANGE)
                            .putExtra(ChangellyService.FROM, info.getName())
                            .putExtra(ChangellyService.TO, ChangellyService.BTC);
                    startService(changellyServiceIntent);
                }
            }
        } );
        currenciesListView.setAdapter(adapter);

        //display the loading spinner
        setLayout(ChangellyActivity.ChangellyUITypes.Loading);

        receiver = new Receiver();
        // The filter's action is BROADCAST_ACTION
        for(String action: new String[]{
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
}
