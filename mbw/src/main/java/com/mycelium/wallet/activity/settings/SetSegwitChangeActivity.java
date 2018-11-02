package com.mycelium.wallet.activity.settings;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode;

public class SetSegwitChangeActivity extends AppCompatActivity {

    public static void callMe(Activity currentActivity) {
        Intent intent = new Intent(currentActivity, SetSegwitChangeActivity.class);
        ActivityOptions options = ActivityOptions.makeCustomAnimation(currentActivity, R.anim.slide_right_in, R.anim.slide_left_out);
        currentActivity.startActivity(intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_segwit_change);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.segwit_change_mode_title);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final MbwManager mbwManager = MbwManager.getInstance(this);
        final RadioGroup radioGroup = findViewById(R.id.radio_group);

        // set selected. *2 because we have texviews
        ChangeAddressMode curMode = mbwManager.getChangeAddressMode();
        ((RadioButton) radioGroup.getChildAt(curMode.ordinal() * 2)).setChecked(true);

        // click listener. Also works on text views
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String clickedTag = v.getTag().toString();
                    mbwManager.setChangeAddressMode(ChangeAddressMode.valueOf(clickedTag));
                    ((RadioButton) radioGroup.findViewWithTag(clickedTag)).setChecked(true);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
