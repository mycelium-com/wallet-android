package com.mycelium.wallet.activity.rmc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.google.common.base.Preconditions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mycelium.wallet.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by elvis on 23.06.17.
 */

public class RMCAddressFragment extends Fragment {

    private View _root;
    @BindView(R.id.switcher)
    protected ViewFlipper switcher;

    @BindView(R.id.graph)
    protected GraphView graphView;

    @BindView(R.id.active_in_day_progress)
    protected ProgressBar activeProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _root = Preconditions.checkNotNull(inflater.inflate(R.layout.rmc_address_view, container, false));
        ButterKnife.bind(this, _root);
        graphView.getGridLabelRenderer().setHorizontalAxisTitle("Day");
        graphView.getGridLabelRenderer().setVerticalAxisTitle("USD");
        return _root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graphView.addSeries(series);
        activeProgressBar.setProgress(20);
        activeProgressBar.setMax(100);
    }

    @OnClick(R.id.show_graph)
    void clickShowGraph() {
        switcher.showNext();
    }

    @OnClick(R.id.show_stats)
    void clickShowStats() {
        switcher.showPrevious();
    }

    @OnClick(R.id.rmc_active_set_reminder)
    void setReminderClick() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_rmc_reminder, null, false);
        new AlertDialog.Builder(getActivity()).setTitle("RMC reminder")
                .setView(view)
                .setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).setNegativeButton("CANCEL", null)
                .create()
                .show();
    }
}
