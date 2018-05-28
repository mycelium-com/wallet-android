package com.mycelium.wallet.activity.rmc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.rmc.adapter.AddressWidgetAdapter;
import com.mycelium.wallet.activity.view.ViewPagerIndicator;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.ReceivingAddressChanged;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class RMCAddressFragment extends Fragment {

    public static final String RMC_ACTIVE_PUSH_NOTIFICATION = "rmc_active_push_notification";
    private View _root;

    @BindView(R.id.active_in_day_progress)
    protected ProgressBar activeProgressBar;

    @BindView(R.id.active_in_day)
    protected TextView activeInDay;


    @BindView(R.id.view_pager)
    protected ViewPager viewPager;

    @BindView(R.id.title)
    protected TextView titleView;

    @BindView(R.id.pager_indicator)
    protected ViewPagerIndicator indicator;

    private MbwManager _mbwManager;
    private SharedPreferences sharedPreferences;
    private AddressWidgetAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _mbwManager = MbwManager.getInstance(getActivity());
        sharedPreferences = getActivity().getSharedPreferences("rmc_notification", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _root = Preconditions.checkNotNull(inflater.inflate(R.layout.rmc_address_view, container, false));
        ButterKnife.bind(this, _root);
        return _root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new AddressWidgetAdapter(getActivity(), _mbwManager);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                titleView.setText(adapter.getPageTitle(position));
            }
        });
        indicator.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(1);
        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(0, true);
            }
        }, 3000);

        updateUi();
    }

    @OnClick(R.id.visit_rmc_one)
    void rmcOneClick() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://rmc.one")));
    }

    @Override
    public void onResume() {
        getEventBus().register(this);
        updateUi();
        super.onResume();
    }

    @Override
    public void onPause() {
        getEventBus().unregister(this);
        super.onPause();
    }

    private Bus getEventBus() {
        return _mbwManager.getEventBus();
    }

    private void activeBtnProgress() {
        Calendar calendarStart = Keys.getActiveStartDay();
        Calendar calendarEnd = Keys.getActiveEndDay();
        int progress = (int) TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis() - calendarStart.getTimeInMillis());
        int total = (int) TimeUnit.MILLISECONDS.toDays(calendarEnd.getTimeInMillis() - calendarStart.getTimeInMillis());
        activeProgressBar.setProgress(progress);
        activeProgressBar.setMax(total);
        activeInDay.setText(getString(R.string.rmc_active_in_159_days, total - progress));
    }

    @OnClick(R.id.rmc_active_set_reminder)
    void setReminderClick() {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_rmc_reminder, null, false);
        ((Switch) view.findViewById(R.id.add_push_notification)).setChecked(sharedPreferences.getBoolean(RMC_ACTIVE_PUSH_NOTIFICATION, false));
        new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (((Switch) view.findViewById(R.id.add_to_calendar)).isChecked()) {
                            addEventToCalendar();
                        }
                        sharedPreferences.edit().putBoolean(RMC_ACTIVE_PUSH_NOTIFICATION
                                , ((Switch) view.findViewById(R.id.add_push_notification)).isChecked())
                                .apply();
                    }
                }).setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    private void addEventToCalendar() {

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        Calendar start = Keys.getActiveEndDay();
        long dtstart = start.getTimeInMillis();
        intent.putExtra("beginTime", dtstart);
        intent.putExtra("allDay", true);
        intent.putExtra("title", getString(R.string.rmc_activate));
        intent.putExtra("description", getString(R.string.rmc_activate_rmc));
        try {
            startActivity(intent);
        } catch (Exception ignore) {
            Toast.makeText(getActivity(), R.string.error_start_google_calendar, Toast.LENGTH_LONG).show();
        }
    }


    private void updateUi() {
        activeBtnProgress();
        adapter.notifyDataSetChanged();
    }

    @Subscribe
    public void receivingAddressChanged(ReceivingAddressChanged event) {
        updateUi();
    }

    @Subscribe
    public void accountChanged(AccountChanged event) {
        updateUi();
    }

    @Subscribe
    public void balanceChanged(BalanceChanged event) {
        updateUi();
    }
}
