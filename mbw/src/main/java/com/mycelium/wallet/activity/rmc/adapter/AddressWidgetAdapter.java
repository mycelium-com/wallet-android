package com.mycelium.wallet.activity.rmc.adapter;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.mycelium.wapi.wallet.WalletAccount;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddressWidgetAdapter extends PagerAdapter {
    private static final String TOTAL_RMC_HASHRATE = "total_rmc_hashrate";
    private static final String YOUR_RMC_HASHRATE = "your_rmc_hashrate";
    private static final String DIFFICULTY = "difficulty";
    private static final String ACCRUED_INCOME = "accrued_income";
    private static final BigDecimal POW_2_32 = BigDecimal.valueOf(4294967296L);
    private static final BigDecimal BLOCK_REWARD = BigDecimal.valueOf(1250000000);
    public static final String PREFERENCE_RMC_PROFIT_METER = "rmc_profit_meter";
    private static final DecimalFormat adoFormat = new DecimalFormat("#.####");
    public static final String ADOANGLE = "adoangle";
    public static final String ADOTIME = "adotime";

    private Context context;
    private MbwManager mbwManager;
    private BtcPoolStatisticsManager.PoolStatisticInfo poolStatisticInfo;
    private SharedPreferences sharedPreferences;
    private ColuAccount coluAccount;

    private int angle = 0;
    private float value = 0;
    private BigDecimal satPerSec;
    private BigDecimal accrued = BigDecimal.ZERO;

    public AddressWidgetAdapter(Context context, MbwManager mbwManager) {
        this.context = context;
        this.mbwManager = mbwManager;
        sharedPreferences = context.getSharedPreferences(PREFERENCE_RMC_PROFIT_METER, Context.MODE_PRIVATE);
        WalletAccount account = mbwManager.getSelectedAccount();
        if(account instanceof ColuAccount) {
            coluAccount = (ColuAccount) mbwManager.getSelectedAccount();


            poolStatisticInfo = new BtcPoolStatisticsManager.PoolStatisticInfo(
                    sharedPreferences.getLong(TOTAL_RMC_HASHRATE, 0)
                    , sharedPreferences.getLong(YOUR_RMC_HASHRATE + coluAccount.getAddress().toString(), 0));
            poolStatisticInfo.difficulty = sharedPreferences.getLong(DIFFICULTY, 0);
            accrued = new BigDecimal(sharedPreferences.getString(ACCRUED_INCOME + coluAccount.getAddress().toString(), "0"));

            BtcPoolStatisticsTask task = new BtcPoolStatisticsTask(coluAccount);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    class ProfitMeterHolder {

        @BindView(R.id.profit_meter)
        protected ProfitMeterView profitMeterView;

        @BindView(R.id.adometr)
        protected TextView adometr;

        @BindView(R.id.speed)
        protected TextView speed;

        @BindView(R.id.accrued_value)
        protected TextView accruedValue;

        @BindView(R.id.rmc_value)
        protected TextView rmcValue;

        @BindView(R.id.rmc_value_after_dot)
        protected TextView rmcValueAfterDot;

        private Runnable updateAdo;


        public ProfitMeterHolder(View view) {
            ButterKnife.bind(this, view);
            if(coluAccount == null) {
                return;
            }
            BigDecimal rmc = coluAccount.getCurrencyBasedBalance().confirmed.getExactValue().getValue();
            String[] split = rmc.setScale(4, BigDecimal.ROUND_DOWN).toPlainString().split("\\.");
            rmcValue.setText(split[0]);
            rmcValueAfterDot.setText("." + split[1]);
            if (poolStatisticInfo != null && poolStatisticInfo.yourRmcHashrate != 0 && poolStatisticInfo.difficulty != 0) {
                satPerSec = BigDecimal.valueOf(poolStatisticInfo.yourRmcHashrate).multiply(BLOCK_REWARD)
                        .divide(BigDecimal.valueOf(poolStatisticInfo.difficulty).multiply(POW_2_32), 4, BigDecimal.ROUND_UP);
                long adotime = sharedPreferences.getLong(ADOTIME + coluAccount.getAddress().toString(), 0);
                if(adotime != 0) {
                    angle = (int) (sharedPreferences.getInt(ADOANGLE + coluAccount.getAddress().toString(), 0)
                                                                    + 6 * (System.currentTimeMillis() - adotime) / 1000);
                    value = angle / 6 * satPerSec.floatValue();
                }
                speed.setText(context.getString(R.string.n_sat_min, (long) (satPerSec.floatValue() * 60)));
                accruedValue.setText(accrued.stripTrailingZeros().toPlainString() + " BTC");
                if (updateAdo == null) {
                    updateAdo = new Runnable() {
                        @Override
                        public void run() {
                            angle = (angle + 6) % 360;
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if (angle == 0) {
                                accrued = accrued.add(BigDecimal.valueOf(value).movePointLeft(8)).setScale(8, BigDecimal.ROUND_UP);
                                editor.putString(ACCRUED_INCOME + coluAccount.getAddress().toString(), accrued.toPlainString());
                                accruedValue.setText(accrued.stripTrailingZeros().toPlainString() + " BTC");
                                value = 0;
                            } else {
                                value += satPerSec.floatValue();
                            }
                            editor.putLong(ADOTIME + coluAccount.getAddress().toString(), System.currentTimeMillis());
                            editor.putInt(ADOANGLE + coluAccount.getAddress().toString(), angle);
                            editor.apply();
                            adometr.setText("+" + (Math.round(value) > 0 ?
                                    String.valueOf(Math.round(value)) : adoFormat.format(value)));
                            profitMeterView.setAngle(angle);
                            profitMeterView.postDelayed(this, 1000);
                        }
                    };
                    updateAdo.run();
                }
            }
        }
    }


    class StatisticHolder {

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
            ButterKnife.bind(this, view);
            String name = mbwManager.getMetadataStorage().getLabelByAccount(mbwManager.getSelectedAccount().getId());
            tvLabel.setText(name);
            tvAddress.setText(mbwManager.getSelectedAccount().getReceivingAddress().get().toString());
            AssetMetadata assetMetadata = mbwManager.getColuManager().getAssetMetadata(ColuAccount.ColuAssetType.RMC);
            tvTotalIssued.setText(assetMetadata != null ?
                    assetMetadata.getTotalSupply().stripTrailingZeros().toPlainString()
                    : context.getString(R.string.not_available));

            if (poolStatisticInfo != null) {
                if (poolStatisticInfo.totalRmcHashrate != 0) {
                    // peta flops
                    tvTotalHP.setText(new BigDecimal(poolStatisticInfo.totalRmcHashrate).movePointLeft(15)
                            .setScale(6, BigDecimal.ROUND_DOWN).stripTrailingZeros().toPlainString());
                } else {
                    tvTotalHP.setText(R.string.not_available);
                }
                if (poolStatisticInfo.yourRmcHashrate != 0) {
                    // tera flops
                    tvUserHP.setText(new BigDecimal(poolStatisticInfo.yourRmcHashrate).movePointLeft(12)
                            .setScale(6, BigDecimal.ROUND_DOWN).stripTrailingZeros().toPlainString());
                } else {
                    tvUserHP.setText(R.string.not_available);
                }
            }
        }
    }

    class BtcPoolStatisticsTask extends AsyncTask<Void, Void, BtcPoolStatisticsManager.PoolStatisticInfo> {

        private ColuAccount coluAccount;

        public BtcPoolStatisticsTask(ColuAccount coluAccount) {
            this.coluAccount = coluAccount;
        }

        @Override
        protected BtcPoolStatisticsManager.PoolStatisticInfo doInBackground(Void... params) {
            BtcPoolStatisticsManager btcPoolStatisticsManager = new BtcPoolStatisticsManager(coluAccount);
            return btcPoolStatisticsManager.getStatistics();
        }

        @Override
        protected void onPostExecute(BtcPoolStatisticsManager.PoolStatisticInfo result) {
            if (result != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (result.totalRmcHashrate != -1) {
                    poolStatisticInfo.totalRmcHashrate = result.totalRmcHashrate;
                    editor.putLong(TOTAL_RMC_HASHRATE, result.totalRmcHashrate);
                }
                if (result.difficulty != 0) {
                    poolStatisticInfo.difficulty = result.difficulty;
                    editor.putLong(DIFFICULTY, result.difficulty);
                }
                if (result.yourRmcHashrate != -1) {
                    poolStatisticInfo.yourRmcHashrate = result.yourRmcHashrate;
                    editor.putLong(YOUR_RMC_HASHRATE + coluAccount.getAddress().toString(), result.yourRmcHashrate);
                }
                if (result.accruedIncome != -1) {
                    editor.putString(ACCRUED_INCOME + coluAccount.getAddress().toString()
                            , BigDecimal.valueOf(result.accruedIncome).movePointLeft(8).setScale(8, BigDecimal.ROUND_UP).toPlainString());
                }
                editor.apply();
                notifyDataSetChanged();
            }
        }
    }
}
