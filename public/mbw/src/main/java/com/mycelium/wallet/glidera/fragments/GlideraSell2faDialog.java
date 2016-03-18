package com.mycelium.wallet.glidera.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.TransactionUtils;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.glidera.GlideraUtils;
import com.mycelium.wallet.glidera.activities.GlideraTransaction;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.request.SellRequest;
import com.mycelium.wallet.glidera.api.response.GlideraError;
import com.mycelium.wallet.glidera.api.response.SellResponse;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;

import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rx.Observer;

public class GlideraSell2faDialog extends DialogFragment {
    private MbwManager mbwManager;
    private GlideraService glideraService;
    private String sellPriceResponseQty;
    private String sellPriceResponseTotal;
    private String sellPriceResponseUUID;
    private String sellAddress;

    static GlideraSell2faDialog newInstance(BigDecimal qty, BigDecimal total, UUID sellPriceResponseUUID, String sellAddress) {
        Bundle bundle = new Bundle();
        bundle.putString("sellPriceResponseQty", qty.toPlainString());
        bundle.putString("sellPriceResponseTotal", total.toPlainString());
        bundle.putString("sellPriceResponseUUID", sellPriceResponseUUID.toString());
        bundle.putString("sellAddress", sellAddress);

        GlideraSell2faDialog glideraSell2faDialog = new GlideraSell2faDialog();
        glideraSell2faDialog.setArguments(bundle);

        return glideraSell2faDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_dialog_2fa, container, false));

        TextView tvPurchaseSummary = (TextView) root.findViewById(R.id.tvPurchaseSummary);
        TextView tv2FASummary = (TextView) root.findViewById(R.id.tv2FASummary);
        Button buttonResend2FA = (Button) root.findViewById(R.id.buttonResend2FA);
        EditText et2FA = (EditText) root.findViewById(R.id.et2FA);
        final Toaster toaster = new Toaster(getActivity());

        getDialog().setTitle("Confirm Your Purchase");

        String purchaseSummary = "You are about to sell " + GlideraUtils.formatBtcForDisplay(new BigDecimal(sellPriceResponseQty)) +
                " for " + GlideraUtils.formatFiatForDisplay(new BigDecimal(sellPriceResponseTotal)) + ".";

        tvPurchaseSummary.setText(purchaseSummary);

        /*
        Hide the 2fa stuff on sell
         */
        tv2FASummary.setVisibility(View.GONE);
        buttonResend2FA.setVisibility(View.GONE);
        et2FA.setVisibility(View.GONE);

        Button buttonCancel = (Button) root.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GlideraSell2faDialog.this.getDialog().cancel();
            }
        });

        final Button buttonContinue = (Button) root.findViewById(R.id.buttonContinue);
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonContinue.setEnabled(false);

                Optional<Address> optionalRefundAddress = mbwManager.getSelectedAccount().getReceivingAddress();

                if (optionalRefundAddress.isPresent()) {
                    Address refundAddress = optionalRefundAddress.get();
                    UUID uuid = UUID.fromString(sellPriceResponseUUID);

                    List<WalletAccount.Receiver> receivers = new ArrayList<WalletAccount.Receiver>();
                    receivers.add(new WalletAccount.Receiver(Address.fromString(sellAddress), Bitcoins.valueOf(sellPriceResponseQty)));

                    WalletAccount selectedAccount = mbwManager.getSelectedAccount();
                    final StandardTransactionBuilder.UnsignedTransaction unsignedTransaction;

                    try {
                        unsignedTransaction = selectedAccount.createUnsignedTransaction(receivers, TransactionUtils.DEFAULT_KB_FEE);
                    } catch (StandardTransactionBuilder.OutputTooSmallException outputTooSmallException) {
                        outputTooSmallException.printStackTrace();
                        buttonContinue.setEnabled(false);
                        toaster.toast("Amount too small",true);
                        return;
                    } catch (StandardTransactionBuilder.InsufficientFundsException insufficientFundsException) {
                        insufficientFundsException.printStackTrace();
                        buttonContinue.setEnabled(false);
                        toaster.toast("Insufficient funds", true);
                        return;
                    } catch (StandardTransactionBuilder.UnableToBuildTransactionException unableToBuildTransactionException) {
                        unableToBuildTransactionException.printStackTrace();
                        buttonContinue.setEnabled(false);
                        toaster.toast("An error occured", true);
                        return;
                    }

                    final Transaction signedTransaction;
                    try {
                        signedTransaction = selectedAccount.signTransaction(unsignedTransaction, AesKeyCipher.defaultKeyCipher());
                    } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                        invalidKeyCipher.printStackTrace();
                        buttonContinue.setEnabled(false);
                        toaster.toast("An error occured", true);
                        return;
                    }

                    final String rawTransaction;
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    try {
                        Hex.encode(signedTransaction.toBytes(), stream);
                        rawTransaction = stream.toString();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        buttonContinue.setEnabled(false);
                        toaster.toast("An error occured", true);
                        return;
                    }

                    SellRequest sellRequest = new SellRequest(refundAddress, rawTransaction, uuid, false, null);

                    glideraService.sell(sellRequest).subscribe(new Observer<SellResponse>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            GlideraError error = GlideraService.convertRetrofitException(e);
                            if (error != null && error.getCode() != null) {
                                if( error.getCode() == 5004 ) {
                                    toaster.toast("An error has occured and your coin returned.",true);
                                }
                                else if( error.getCode() == 5005 ) {
                                    toaster.toast("An error has occured, please contact Glidera support",true);
                                }
                                else {
                                    toaster.toast("Unable to sell at this time",true);
                                }
                            }
                            buttonContinue.setEnabled(true);
                        }

                        @Override
                        public void onNext(SellResponse sellResponse) {
                            Intent intent = new Intent(getActivity(), GlideraTransaction.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("transactionuuid", sellResponse.getTransactionUuid().toString());
                            intent.putExtras(bundle);
                            startActivity(intent);
                        }
                    });
                }
            }
        });

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glideraService = GlideraService.getInstance();
        mbwManager = MbwManager.getInstance(this.getActivity());

        sellPriceResponseQty = getArguments().getString("sellPriceResponseQty");
        sellPriceResponseTotal = getArguments().getString("sellPriceResponseTotal");
        sellPriceResponseUUID = getArguments().getString("sellPriceResponseUUID");
        sellAddress = getArguments().getString("sellAddress");
    }
}
