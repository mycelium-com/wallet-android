package com.mycelium.bequant.kyc;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mycelium.bequant.kyc.verticalStepper.VerticalStepperAdapter;
import com.mycelium.wallet.R;

import java.util.ArrayList;

public class MainStepperAdapter extends VerticalStepperAdapter {

    ArrayList<String> list = new ArrayList<String>() {{
        add("Phone Number");
        add("Personal information");
        add("Residential Address");
        add("Documents & Selfie");
    }};

    public MainStepperAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public CharSequence getTitle(int position) {
        return list.get(position);
    }

    @Nullable
    @Override
    public CharSequence getSummary(int position) {
        return "Summary " + position;
    }

    @Override
    public boolean isEditable(int position) {
        return position == 1;
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public Void getItem(int position) {
        return null;
    }

    @NonNull
    @Override
    public View onCreateContentView(Context context, int position) {
        View content = new MainItemView(context);

        Button actionContinue = content.findViewById(R.id.action_continue);
        actionContinue.setEnabled(position < getCount() - 1);
        actionContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        Button actionBack = content.findViewById(R.id.action_back);
        actionBack.setEnabled(position > 0);
        actionBack.findViewById(R.id.action_back).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        previous();
                    }
                });

        return content;
    }
}