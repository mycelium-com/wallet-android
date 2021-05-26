package com.mycelium.giftbox.cardDetails

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxAmountBinding
import com.mycelium.wapi.api.lib.CurrencyCode
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.isNullOrZero
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import kotlinx.android.synthetic.main.layout_fio_request_notification.*
import java.math.BigDecimal
import java.math.BigInteger

class AmountInputFragment : Fragment(), NumberEntry.NumberEntryListener {
    private lateinit var binding: FragmentGiftboxAmountBinding
    private var _numberEntry: NumberEntry? = null

    private lateinit var _mbwManager: MbwManager
    val args by navArgs<AmountInputFragmentArgs>()
    private val zeroUsd =
        Value(Utils.getTypeByName(CurrencyCode.USD.shortString)!!, 0.toBigInteger())
    private var _amount: Value =
        zeroUsd
        set(value) {
            field = value
            binding.tvAmount.text = value.toStringWithUnit()
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGiftboxAmountBinding>(
            inflater,
            R.layout.fragment_giftbox_amount,
            container,
            false
        ).apply { lifecycleOwner = this@AmountInputFragment }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _mbwManager = MbwManager.getInstance(activity?.applicationContext)
        with(binding) {
            btOk.setOnClickListener {
                LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(Intent(ACTION_AMOUNT_SELECTED).apply {
                        putExtra(AMOUNT_KEY, _amount)
                    })
                findNavController().navigateUp()
            }
            btMax.setOnClickListener {
                setEnteredAmount(toUnits(args.product.maximum_value).toString())
            }
        }

        initNumberEntry(savedInstanceState)
    }

    private fun toUnits(amount: BigDecimal): BigInteger =
        amount.multiply(100.toBigDecimal()).setScale(0).toBigIntegerExact()

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putSerializable(ENTERED_AMOUNT, _amount)
    }

    private fun initNumberEntry(savedInstanceState: Bundle?) {
        // Load saved state
        if (savedInstanceState != null) {
            _amount = savedInstanceState.getSerializable(ENTERED_AMOUNT) as Value
        } else {
            _amount = args.amount ?: zeroUsd
        }

        // Init the number pad
        val amountString: String
        if (!isNullOrZero(_amount)) {
            val denomination = _mbwManager.getDenomination(_amount.type)
            amountString = _amount.toString(denomination)
        } else {
            amountString = ""
        }
        _numberEntry = NumberEntry(2, this, activity, amountString)
    }


    override fun onEntryChanged(entry: String, wasSet: Boolean) {
        if (!wasSet) {
            // if it was change by the user pressing buttons (show it unformatted)
            setEnteredAmount(entry)
        }
        checkEntry()
    }

    private fun setEnteredAmount(value: String) {
        if (value.isEmpty()) {
            _amount = valueOf(_amount.type, BigInteger.ZERO)
        } else {
            _amount = valueOf(_amount.type, value)
        }
    }

    private fun checkEntry() {
        val valid = !isNullOrZero(_amount)
                && _amount.moreOrEqualThan(
            valueOf(
                _amount.type,
                toUnits(args.product.minimum_value)
            )
        )
                && _amount.lessOrEqualThan(
            valueOf(
                _amount.type,
                toUnits(args.product.maximum_value)
            )
        )
        binding.btOk.isEnabled = valid
    }


    companion object {
        const val ACTION_AMOUNT_SELECTED: String = "action_amount"
        const val AMOUNT_KEY = "amount"
        const val ENTERED_AMOUNT = "enteredamount"
    }

}