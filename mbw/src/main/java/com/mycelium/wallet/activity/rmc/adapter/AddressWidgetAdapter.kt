package com.mycelium.wallet.activity.rmc.adapter

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.rmc.BtcPoolStatisticsManager
import com.mycelium.wallet.activity.rmc.BtcPoolStatisticsManager.PoolStatisticInfo
import com.mycelium.wallet.databinding.RmcAddressProfitMeterBinding
import com.mycelium.wallet.databinding.RmcAddressStatisticBinding
import com.mycelium.wapi.wallet.colu.ColuAccount
import java.math.BigDecimal
import java.text.DecimalFormat

class AddressWidgetAdapter(private val context: Context, private val mbwManager: MbwManager) :
    PagerAdapter() {
    private var poolStatisticInfo: PoolStatisticInfo? = null
    private val sharedPreferences: SharedPreferences
    private var coluAccount: ColuAccount? = null

    private var angle = 0
    private var value = 0f
    private var satPerSec: BigDecimal? = null
    private var accrued: BigDecimal = BigDecimal.ZERO

    init {
        sharedPreferences =
            context.getSharedPreferences(PREFERENCE_RMC_PROFIT_METER, Context.MODE_PRIVATE)
        val account = mbwManager.selectedAccount
        if (account is ColuAccount) {
            coluAccount = mbwManager.selectedAccount as ColuAccount

            poolStatisticInfo = PoolStatisticInfo(
                sharedPreferences.getLong(TOTAL_RMC_HASHRATE, 0),
                sharedPreferences.getLong(
                    YOUR_RMC_HASHRATE + coluAccount?.receiveAddress.toString(),
                    0
                )
            )
            poolStatisticInfo?.difficulty = sharedPreferences.getLong(DIFFICULTY, 0)
            accrued = BigDecimal(
                sharedPreferences.getString(
                    ACCRUED_INCOME + coluAccount!!.receiveAddress.toString(),
                    "0"
                )
            )

            val task = BtcPoolStatisticsTask(coluAccount)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view: View
        if (position == 0) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.rmc_address_statistic, container, false)
            val holder = StatisticHolder(view)
            view.tag = holder
        } else {
            view = LayoutInflater.from(context)
                .inflate(R.layout.rmc_address_profit_meter, container, false)
            val profitMeterHolder = ProfitMeterHolder(view)
            view.tag = profitMeterHolder
        }
        container.addView(view)
        return view
    }


    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int = 2

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

    override fun getPageTitle(position: Int): CharSequence =
        if (position == 0) {
            context.getString(R.string.rmc_basic_parameters)
        } else {
            context.getString(R.string.rmc_profit_meter)
        }

    override fun getItemPosition(`object`: Any): Int =
        POSITION_NONE

    internal inner class ProfitMeterHolder(view: View) {
        val binding = RmcAddressProfitMeterBinding.bind(view)

        private var updateAdo: Runnable? = null


        init {
            if (coluAccount != null) {
                val rmc = coluAccount!!.accountBalance.confirmed.valueAsBigDecimal
                val split =
                    rmc.setScale(4, BigDecimal.ROUND_DOWN).toPlainString().split("\\.".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                binding.rmcValue.text = split[0]
                binding.rmcValueAfterDot.text = "." + split[1]
                poolStatisticInfo?.let { poolStatisticInfo ->
                    if (poolStatisticInfo.yourRmcHashrate != 0L && poolStatisticInfo.difficulty != 0L) {
                        satPerSec =
                            BigDecimal.valueOf(poolStatisticInfo.yourRmcHashrate).multiply(
                                BLOCK_REWARD
                            )
                                .divide(
                                    BigDecimal.valueOf(poolStatisticInfo.difficulty)
                                        .multiply(POW_2_32),
                                    4,
                                    BigDecimal.ROUND_UP
                                )
                        val adotime =
                            sharedPreferences.getLong(
                                ADOTIME + coluAccount!!.receiveAddress.toString(),
                                0
                            )
                        if (adotime != 0L) {
                            angle = (sharedPreferences.getInt(
                                ADOANGLE + coluAccount!!.receiveAddress.toString(),
                                0
                            )
                                    + 6 * (System.currentTimeMillis() - adotime) / 1000).toInt()
                            value = angle / 6 * (satPerSec?.toFloat() ?: 0f)
                        }
                        binding.speed.text =
                            context.getString(
                                R.string.n_sat_min,
                                ((satPerSec?.toFloat() ?: 0f) * 60).toLong()
                            )
                        binding.accruedValue.text =
                            accrued.stripTrailingZeros().toPlainString() + " BTC"
                        if (updateAdo == null) {
                            updateAdo = object : Runnable {
                                override fun run() {
                                    angle = (angle + 6) % 360
                                    val editor = sharedPreferences.edit()
                                    if (angle == 0) {
                                        accrued = accrued.add(
                                            BigDecimal.valueOf(value.toDouble()).movePointLeft(8)
                                        ).setScale(8, BigDecimal.ROUND_UP)
                                        editor.putString(
                                            ACCRUED_INCOME + coluAccount!!.receiveAddress.toString(),
                                            accrued.toPlainString()
                                        )
                                        binding.accruedValue.text =
                                            accrued.stripTrailingZeros().toPlainString() + " BTC"
                                        value = 0f
                                    } else {
                                        value += satPerSec?.toFloat() ?: 0f
                                    }
                                    editor.putLong(
                                        ADOTIME + coluAccount!!.receiveAddress.toString(),
                                        System.currentTimeMillis()
                                    )
                                    editor.putInt(
                                        ADOANGLE + coluAccount!!.receiveAddress.toString(),
                                        angle
                                    )
                                    editor.apply()
                                    binding.adometr.text =
                                        "+" + (if (Math.round(value) > 0) Math.round(value)
                                            .toString()
                                        else adoFormat.format(value.toDouble()))
                                    binding.profitMeter.setAngle(angle)
                                    binding.profitMeter.postDelayed(this, 1000)
                                }
                            }
                            updateAdo?.run()
                        }
                    }
                }
            }
        }
    }


    internal inner class StatisticHolder(view: View) {
        val binding = RmcAddressStatisticBinding.bind(view)

        init {
            val name = mbwManager.metadataStorage.getLabelByAccount(mbwManager.selectedAccount.id)
            binding.tvLabel.text = name
            binding.tvAddress.text = mbwManager.selectedAccount.receiveAddress.toString()
            poolStatisticInfo?.let { poolStatisticInfo ->
                if (poolStatisticInfo.totalRmcHashrate != 0L) {
                    // peta flops
                    binding.tvTotalHP.text =
                        BigDecimal(poolStatisticInfo.totalRmcHashrate).movePointLeft(15)
                            .setScale(6, BigDecimal.ROUND_DOWN).stripTrailingZeros()
                            .toPlainString()
                } else {
                    binding.tvTotalHP.setText(R.string.not_available)
                }
                if (poolStatisticInfo.yourRmcHashrate != 0L) {
                    // tera flops
                    binding.tvUserHP.text =
                        BigDecimal(poolStatisticInfo.yourRmcHashrate).movePointLeft(12)
                            .setScale(6, BigDecimal.ROUND_DOWN).stripTrailingZeros()
                            .toPlainString()
                } else {
                    binding.tvUserHP.setText(R.string.not_available)
                }
            }
        }
    }

    internal inner class BtcPoolStatisticsTask(private val coluAccount: ColuAccount?) :
        AsyncTask<Void?, Void?, PoolStatisticInfo?>() {

        override fun doInBackground(vararg params: Void?): PoolStatisticInfo? {
            val btcPoolStatisticsManager = BtcPoolStatisticsManager(
                coluAccount
            )
            return btcPoolStatisticsManager.statistics
        }

        override fun onPostExecute(result: PoolStatisticInfo?) {
            if (result != null) {
                val editor = sharedPreferences.edit()
                if (result.totalRmcHashrate != -1L) {
                    poolStatisticInfo!!.totalRmcHashrate = result.totalRmcHashrate
                    editor.putLong(TOTAL_RMC_HASHRATE, result.totalRmcHashrate)
                }
                if (result.difficulty != 0L) {
                    poolStatisticInfo!!.difficulty = result.difficulty
                    editor.putLong(DIFFICULTY, result.difficulty)
                }
                if (result.yourRmcHashrate != -1L) {
                    poolStatisticInfo!!.yourRmcHashrate = result.yourRmcHashrate
                    editor.putLong(
                        YOUR_RMC_HASHRATE + coluAccount!!.receiveAddress.toString(),
                        result.yourRmcHashrate
                    )
                }
                if (result.accruedIncome != -1L) {
                    editor.putString(
                        ACCRUED_INCOME + coluAccount!!.receiveAddress.toString(),
                        BigDecimal.valueOf(result.accruedIncome).movePointLeft(8)
                            .setScale(8, BigDecimal.ROUND_UP).toPlainString()
                    )
                }
                editor.apply()
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val TOTAL_RMC_HASHRATE = "total_rmc_hashrate"
        private const val YOUR_RMC_HASHRATE = "your_rmc_hashrate"
        private const val DIFFICULTY = "difficulty"
        private const val ACCRUED_INCOME = "accrued_income"
        private val POW_2_32: BigDecimal = BigDecimal.valueOf(4294967296L)
        private val BLOCK_REWARD: BigDecimal = BigDecimal.valueOf(1250000000)
        const val PREFERENCE_RMC_PROFIT_METER: String = "rmc_profit_meter"
        private val adoFormat = DecimalFormat("#.####")
        const val ADOANGLE: String = "adoangle"
        const val ADOTIME: String = "adotime"
    }
}
