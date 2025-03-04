package com.mycelium.wallet.activity.rmc

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.rmc.adapter.AddressWidgetAdapter
import com.mycelium.wallet.databinding.RmcAddressViewBinding
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wallet.external.partner.openLink
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RMCAddressFragment : Fragment() {

    private var _mbwManager: MbwManager? = null
    private var sharedPreferences: SharedPreferences? = null
    private var adapter: AddressWidgetAdapter? = null
    private var binding: RmcAddressViewBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _mbwManager = MbwManager.getInstance(activity)
        sharedPreferences =
            activity!!.getSharedPreferences("rmc_notification", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RmcAddressViewBinding.inflate(inflater).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AddressWidgetAdapter(requireActivity(), _mbwManager!!)
        binding?.viewPager?.adapter = adapter
        binding?.viewPager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding?.title?.text = adapter!!.getPageTitle(position)
            }
        })
        binding?.pagerIndicator?.setupWithViewPager(binding?.viewPager!!)
        binding?.viewPager?.currentItem = 1
        binding?.viewPager?.postDelayed({ binding?.viewPager?.setCurrentItem(0, true) }, 3000)

        binding?.visitRmcOne?.setOnClickListener {
            openLink("https://rmc.one")
        }
        binding?.rmcActiveSetReminder?.setOnClickListener { setReminderClick() }
        updateUi()
    }


    override fun onResume() {
        eventBus.register(this)
        updateUi()
        super.onResume()
    }

    override fun onPause() {
        eventBus.unregister(this)
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private val eventBus: Bus
        get() = MbwManager.getEventBus()

    private fun activeBtnProgress() {
        val calendarStart = Keys.getActiveStartDay()
        val calendarEnd = Keys.getActiveEndDay()
        val progress =
            TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - calendarStart.timeInMillis)
                .toInt()
        val total =
            TimeUnit.MILLISECONDS.toDays(calendarEnd.timeInMillis - calendarStart.timeInMillis)
                .toInt()
        binding?.activeInDayProgress?.progress = progress
        binding?.activeInDayProgress?.max = total
        binding?.activeInDay?.text = getString(R.string.rmc_active_in_159_days, total - progress)
    }

    fun setReminderClick() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_rmc_reminder, null, false)
        (view.findViewById<View>(R.id.add_push_notification) as Switch).isChecked =
            sharedPreferences!!.getBoolean(
                RMC_ACTIVE_PUSH_NOTIFICATION,
                false
            )
        AlertDialog.Builder(activity)
            .setView(view)
            .setPositiveButton(
                R.string.save
            ) { dialogInterface, i ->
                if ((view.findViewById<View>(R.id.add_to_calendar) as Switch).isChecked) {
                    addEventToCalendar()
                }
                sharedPreferences!!.edit().putBoolean(
                    RMC_ACTIVE_PUSH_NOTIFICATION,
                    (view.findViewById<View>(R.id.add_push_notification) as Switch).isChecked
                )
                    .apply()
            }.setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    private fun addEventToCalendar() {
        val intent = Intent(Intent.ACTION_EDIT)
        intent.setType("vnd.android.cursor.item/event")
        val start = Keys.getActiveEndDay()
        val dtstart = start.timeInMillis
        intent.putExtra("beginTime", dtstart)
        intent.putExtra("allDay", true)
        intent.putExtra("title", getString(R.string.rmc_activate))
        intent.putExtra("description", getString(R.string.rmc_activate_rmc))
        try {
            startActivity(intent)
        } catch (ignore: Exception) {
            Toaster(this).toast(R.string.error_start_google_calendar, false)
        }
    }


    private fun updateUi() {
        activeBtnProgress()
        adapter!!.notifyDataSetChanged()
    }

    @Subscribe
    fun receivingAddressChanged(event: ReceivingAddressChanged?) {
        updateUi()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged?) {
        updateUi()
    }

    @Subscribe
    fun balanceChanged(event: BalanceChanged?) {
        updateUi()
    }

    companion object {
        const val RMC_ACTIVE_PUSH_NOTIFICATION: String = "rmc_active_push_notification"
    }
}
