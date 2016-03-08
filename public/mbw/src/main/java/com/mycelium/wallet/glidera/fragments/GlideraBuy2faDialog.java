package com.mycelium.wallet.glidera.fragments;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;
import com.mycelium.wallet.glidera.GlideraUtils;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.response.TwoFactorResponse;

import java.math.BigDecimal;

import rx.Observer;

public class GlideraBuy2faDialog extends DialogFragment {
    private GlideraService glideraService;
    private String buyPriceResponseQty;
    private String buyPriceResponseTotal;
    private String mode2FA;

    static GlideraBuy2faDialog newInstance(BigDecimal qty, BigDecimal total, TwoFactorResponse.Mode mode) {
        Bundle bundle = new Bundle();
        bundle.putString("buyPriceResponseQty", qty.toPlainString());
        bundle.putString("buyPriceResponseTotal", total.toPlainString());
        bundle.putString("mode2FA", mode.toString());

        GlideraBuy2faDialog glideraBuy2faDialog = new GlideraBuy2faDialog();
        glideraBuy2faDialog.setArguments(bundle);

        return glideraBuy2faDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_dialog_2fa, container, false));

        TextView tvPurchaseSummary = (TextView) root.findViewById(R.id.tvPurchaseSummary);
        TextView tv2FASummary = (TextView) root.findViewById(R.id.tv2FASummary);
        Button buttonResend2FA = (Button) root.findViewById(R.id.buttonResend2FA);
        EditText et2FA = (EditText) root.findViewById(R.id.et2FA);

        getDialog().setTitle("Confirm Your Purchase");

        tvPurchaseSummary.setText("You are about to buy " + GlideraUtils.formatBtcForDisplay(new BigDecimal(buyPriceResponseQty)) + " for " +
                " " +
                GlideraUtils.formatFiatForDisplay(new BigDecimal(buyPriceResponseTotal)) + ".");

        if (mode2FA.equals(TwoFactorResponse.Mode.NONE.toString())) {
            tv2FASummary.setVisibility(View.GONE);
            buttonResend2FA.setVisibility(View.GONE);
            et2FA.setVisibility(View.GONE);
        } else if (mode2FA.equals(TwoFactorResponse.Mode.AUTHENTICATR.toString())) {
            tv2FASummary.setText("Please enter your 2-factor authorization (2FA) code from your Authenticator smartphone app to complete this purchase.");
            buttonResend2FA.setVisibility(View.GONE);
            et2FA.setHint("2FA Code");
        } else if (mode2FA.equals(TwoFactorResponse.Mode.PIN.toString())) {
            tv2FASummary.setText("Please enter your PIN to complete this purchase.");
            buttonResend2FA.setVisibility(View.GONE);
            et2FA.setHint("PIN");
        } else if (mode2FA.equals(TwoFactorResponse.Mode.SMS.toString())) {
            tv2FASummary.setText("A text message has been sent to your phone with a 2-factor authentication (2FA) code. Please enter it " +
                    "to confirm this purchase.");
            et2FA.setHint("2FA Code");
        }

        Button buttonCancel = (Button) root.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GlideraBuy2faDialog.this.getDialog().cancel();
            }
        });

        Button buttonContinue = (Button) root.findViewById(R.id.buttonContinue);
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //perform action
                //submit buy
            }
        });

        buttonResend2FA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                glideraService.getTwoFactor()
                        .subscribe(new Observer<TwoFactorResponse>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onNext(TwoFactorResponse twoFactorResponse) {
                                //New 2fa code was sent
                            }
                        });
            }
        });

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glideraService = GlideraService.getInstance();

        buyPriceResponseQty = getArguments().getString("buyPriceResponseQty");
        buyPriceResponseTotal = getArguments().getString("buyPriceResponseTotal");
        mode2FA = getArguments().getString("mode2FA");
    }
}
