package com.mycelium.wallet.activity.exchange;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;

import com.mycelium.wallet.R;

public class ExchangeActivity extends AppCompatActivity {

    private static int theme = R.style.MyceliumModern_Light;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(theme);
        setContentView(R.layout.activity_exchange);
        setTitle(getString(R.string.excange_title));
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, new ExchangeFragment(), "ExchangeFragment")
                .commitAllowingStateLoss();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exchange_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.colorize) {
            theme = theme == R.style.MyceliumModern_Light ?
                    R.style.MyceliumModern_Dark : R.style.MyceliumModern_Light;
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
