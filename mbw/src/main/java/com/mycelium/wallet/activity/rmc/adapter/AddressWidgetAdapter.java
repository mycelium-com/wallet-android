package com.mycelium.wallet.activity.rmc.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.rmc.BtcPoolStatisticsManager;
import com.mycelium.wallet.activity.rmc.view.ProfitMeterView;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.colu.json.AssetMetadata;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by elvis on 15.11.17.
 */

public class AddressWidgetAdapter extends PagerAdapter {
    private Context context;
    private MbwManager mbwManager;

    public AddressWidgetAdapter(Context context, MbwManager mbwManager) {
        this.context = context;
        this.mbwManager = mbwManager;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view;
        if (position == 0) {
            view = LayoutInflater.from(context).inflate(R.layout.rmc_address_statistic, container, false);
            StatisticHolder holder = new StatisticHolder(view);
            view.setTag(holder);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.rmc_address_profit_meter, container, false);
            ProfitMeterHolder profitMeterHolder = new ProfitMeterHolder(view);
            view.setTag(profitMeterHolder);
        }
        container.addView(view);
        return view;
    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return context.getString(R.string.rmc_basic_parameters);
        } else {
            return context.getString(R.string.rmc_profit_meter);
        }
    }

    class ProfitMeterHolder {
        private View view;
        private int angle = 0;
        private int value = 0;

        @BindView(R.id.profit_meter)
        protected ProfitMeterView profitMeterView;

        @BindView(R.id.adometr)
        protected TextView adometr;

        @BindView(R.id.speed)
        protected TextView speed;


        public ProfitMeterHolder(View view) {
            this.view = view;
            ButterKnife.bind(this, view);
            profitMeterView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    angle = (angle + 6) % 360;
                    if (angle == 0) {
                        value = 0;
                    }
                    adometr.setText("+" + value++);
                    speed.setText(context.getString(R.string.n_sat_min, 60));
                    profitMeterView.setAngle(angle);
                    profitMeterView.postDelayed(this, 1000);
                }
            }, 1000);
        }
    }

    class StatisticHolder {
        private View view;

        @BindView(R.id.tvLabel)
        protected TextView tvLabel;

        @BindView(R.id.tvAddress)
        protected TextView tvAddress;

        @BindView(R.id.tvTotalHP)
        protected TextView tvTotalHP;

        @BindView(R.id.tvUserHP)
        protected TextView tvUserHP;

        @BindView(R.id.tvTotalIssued)
        protected TextView tvTotalIssued;


        public StatisticHolder(View view) {
            this.view = view;
            ButterKnife.bind(this, view);
            String name = mbwManager.getMetadataStorage().getLabelByAccount(mbwManager.getSelectedAccount().getId());
            tvLabel.setText(name);
            tvAddress.setText(mbwManager.getSelectedAccount().getReceivingAddress().get().toString());
            AssetMetadata assetMetadata = mbwManager.getColuManager().getAssetMetadata(ColuAccount.ColuAssetType.RMC);
            tvTotalIssued.setText(assetMetadata != null ?
                    assetMetadata.getTotalSupply().stripTrailingZeros().toPlainString()
                    : context.getString(R.string.not_available));

            ColuAccount coluAccount = (ColuAccount) mbwManager.getSelectedAccount();
            BtcPoolStatisticsTask task = new BtcPoolStatisticsTask(coluAccount);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        class BtcPoolStatisticsTask extends AsyncTask<Void, Void, BtcPoolStatisticsManager.PoolStatisticInfo> {

            private ColuAccount coluAccount;

            public BtcPoolStatisticsTask(ColuAccount coluAccount) {
                this.coluAccount = coluAccount;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected BtcPoolStatisticsManager.PoolStatisticInfo doInBackground(Void... params) {
                BtcPoolStatisticsManager btcPoolStatisticsManager = new BtcPoolStatisticsManager(coluAccount);
                return btcPoolStatisticsManager.getStatistics();
            }

            @Override
            protected void onPostExecute(BtcPoolStatisticsManager.PoolStatisticInfo result) {
                if (result == null)
                    return;
                if (result.totalRmcHashrate != 0) {
                    // peta flops
                    tvTotalHP.setText(new BigDecimal(result.totalRmcHashrate).movePointLeft(15)
                            .setScale(6, BigDecimal.ROUND_DOWN).stripTrailingZeros().toPlainString());
                } else {
                    tvTotalHP.setText(R.string.not_available);
                }
                if (result.yourRmcHashrate != 0) {
                    // tera flops
                    tvUserHP.setText(new BigDecimal(result.yourRmcHashrate).movePointLeft(12)
                            .setScale(6, BigDecimal.ROUND_DOWN).stripTrailingZeros().toPlainString());
                } else {
                    tvUserHP.setText(R.string.not_available);
                }

            }
        }
    }
}
