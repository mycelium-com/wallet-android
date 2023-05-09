package com.mycelium.giftbox.purchase

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.PriceResponse
import com.mycelium.giftbox.client.models.getCardValue
import com.mycelium.giftbox.purchase.viewmodel.getCurrencyId
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxAmountBinding
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.isNullOrZero
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import kotlinx.android.synthetic.main.fragment_giftbox_amount.*
import kotlinx.android.synthetic.main.layout_fio_request_notification.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class AmountInputFragment : Fragment(), NumberEntry.NumberEntryListener {
    private lateinit var binding: FragmentGiftboxAmountBinding
    private var numberEntry: NumberEntry? = null
    private lateinit var mbwManager: MbwManager
    val args by navArgs<AmountInputFragmentArgs>()

    val zeroFiatValue by lazy {
        Value.zeroValue(Utils.getTypeByName(args.product.currencyCode)!!)
    }
    private val zeroCryptoValue by lazy {
        account?.coinType?.value(0)
    }

    private val account by lazy {
        MbwManager.getInstance(requireContext()).getWalletManager(false)
            .getAccount(args.accountId)
    }

    private var _amount: Value? = null
        set(value) {
            field = value
            lifecycleScope.launch(IO) {
                getPriceResponse(value!!).collect {
                    withContext(Dispatchers.Main) {
                        val exchangeRate = BigDecimal(it!!.exchangeRate)
                        //update crypto amount
                        val cryptoAmountFromFiat =
                            value.valueAsLong.toBigDecimal()
                                .setScale(account?.coinType?.unitExponent!!) / toUnits(
                                Utils.getTypeByName(
                                    args.product.currencyCode!!
                                )!!, exchangeRate
                            ).toBigDecimal()
                        val cryptoAmountValue =
                            valueOf(
                                account?.coinType!!,
                                toUnits(account?.coinType!!, cryptoAmountFromFiat)
                            )
                        tvCryptoAmount.isVisible = true
                        tvCryptoAmount.text = cryptoAmountValue.toStringFriendlyWithUnit()

                        //update spendable
                        val maxSpendable = getMaxSpendable()
                        val fiatSpendable = maxSpendable.valueAsBigDecimal.multiply(exchangeRate)
                        spendableLayout.isVisible = true

                        tvSpendableFiatAmount.text = "~" + valueOf(
                            zeroFiatValue.type,
                            toUnits(zeroFiatValue.type, fiatSpendable!!)
                        ).toStringFriendlyWithUnit()
                    }
                }
            }
        }


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View =
            FragmentGiftboxAmountBinding.inflate(inflater).apply {
                binding = this
                lifecycleOwner = this@AmountInputFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mbwManager = MbwManager.getInstance(activity?.applicationContext)

        with(binding) {
            btOk.setOnClickListener {
                LocalBroadcastManager.getInstance(requireContext())
                        .sendBroadcast(Intent(ACTION_AMOUNT_SELECTED).putExtra(AMOUNT_KEY, _amount))
                findNavController().navigateUp()
            }
            btMax.setOnClickListener {
                setEnteredAmount(args.product.maximumValue.toPlainString()!!)
                numberEntry!!.setEntry(args.product.maximumValue, getMaxDecimal(_amount?.type!!))
            }
            tvCardValue.text = args.product.getCardValue()
        }
        lifecycleScope.launch(IO) {
            val maxSpendable = getMaxSpendable()
            withContext(Dispatchers.Main) {
                spendableLayout.isVisible = true
                tvSpendableCryptoAmount.text = maxSpendable.toStringFriendlyWithUnit()
            }
        }

        initNumberEntry(savedInstanceState)
    }

    private fun getMaxSpendable() =
        account?.calculateMaxSpendableAmount(feeEstimation.normal, null, null)!!

    private val feeEstimation by lazy {
        mbwManager.getFeeProvider(account?.basedOnCoinType).estimation
    }

    private fun getMaxDecimal(assetInfo: AssetInfo): Int {
        return (assetInfo as? FiatType)?.unitExponent
            ?: assetInfo.unitExponent - mbwManager.getDenomination(_amount?.type).scale
    }

    fun zeroValue(): Value {
        return Value.zeroValue(Utils.getTypeByName(args.product.currencyCode)!!)
    }

    private fun toUnits(assetInfo: String, amount: BigDecimal): BigInteger =
        toUnits(Utils.getTypeByName(args.product.currencyCode)!!, amount)

    private fun toUnits(assetInfo: AssetInfo, amount: BigDecimal): BigInteger =
        amount.movePointRight(assetInfo.unitExponent).setScale(0, RoundingMode.HALF_UP)
            .toBigIntegerExact()

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putSerializable(ENTERED_AMOUNT, _amount)
    }

    private fun initNumberEntry(savedInstanceState: Bundle?) {
        // Load saved state
        _amount = if (savedInstanceState != null) {
            savedInstanceState.getSerializable(ENTERED_AMOUNT) as Value
        } else {
            args.amount ?: zeroValue()
        }

        // Init the number pad
        val amountString: String = if (!isNullOrZero(_amount)) {
            val denomination = mbwManager.getDenomination(_amount?.type)
            _amount?.toString(denomination) ?: ""
        } else {
            ""
        }
        numberEntry = NumberEntry(getMaxDecimal(_amount?.type!!), this, activity, amountString)

        updateAmountsDisplay(amountString)
    }


    override fun onEntryChanged(entry: String, wasSet: Boolean) {
        if (!wasSet) {
            // if it was change by the user pressing buttons (show it unformatted)
            setEnteredAmount(entry)
        }
        updateAmountsDisplay(entry)
    }

    private fun updateAmountsDisplay(amountText: String) {
        binding.tvAmount.text = amountText
        binding.btCurrency.text = _amount?.currencySymbol?.toUpperCase()
    }


    private fun setEnteredAmount(value: String) {
        _amount = if (value.isEmpty()) {
            zeroValue()
        } else {
            _amount?.type?.value(value)
        }
        binding.btOk.isEnabled = false
        GitboxAPI.giftRepository.getPrice(lifecycleScope,
            code = args.product.code ?: "",
            quantity = 1,
            amount = _amount?.valueAsLong?.div(100)?.toInt()!!,
            currencyId = zeroCryptoValue!!.getCurrencyId(),
            success = { priceResponse ->
                val conversionError = priceResponse!!.status == PriceResponse.Status.eRROR
                val maxSpendableFiat = convertToFiat(priceResponse, getMaxSpendable())
                val insufficientFunds = _amount!!.moreThan(maxSpendableFiat!!)
                val exceedCardPrice = _amount!!.moreThan(
                    valueOf(
                        _amount!!.type,
                        toUnits(args.product.currencyCode!!, args.product.maximumValue)
                    )
                )
                val minimumPrice = valueOf(
                    _amount!!.type,
                    toUnits(args.product.currencyCode!!, args.product.minimumValue)
                )
                val lessMinimumCardPrice = _amount!!.lessThan(
                    minimumPrice
                )
                if (exceedCardPrice) {
                    Toaster(requireContext()).toast(getString(R.string.exceed_card_value), true)
                }
                if (lessMinimumCardPrice) {
                    Toaster(requireContext()).toast(
                        "Minimal card value: " + minimumPrice.toStringFriendlyWithUnit(),
                        true
                    )
                }
                binding.tvAmount.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        if (conversionError || insufficientFunds || exceedCardPrice || lessMinimumCardPrice) R.color.red_error else R.color.white,
                        null
                    )
                )
                binding.btOk.isEnabled = !(conversionError || insufficientFunds || exceedCardPrice || lessMinimumCardPrice)
                if (insufficientFunds && !conversionError) {
                    Toaster(requireContext()).toast("Insufficient funds", true)
                }
            }, error = { _, error ->

            },
            finally = {
            })
    }

    private fun checkEntry() {
        val isValid = !isNullOrZero(_amount)
                && _amount!!.moreOrEqualThan(
            valueOf(
                _amount!!.type,
                toUnits(args.product.currencyCode!!, args.product.minimumValue)
            )
        )
                && _amount!!.lessOrEqualThan(
            valueOf(
                _amount!!.type,
                toUnits(args.product.currencyCode!!, args.product.maximumValue)
            )
        )
        binding.btOk.isEnabled = isValid
    }

    private fun convertToFiat(priceResponse: PriceResponse, value: Value): Value? =
        priceResponse.exchangeRate?.let {
            val fiat = value.valueAsBigDecimal.multiply(BigDecimal(it))
            Value.valueOf(zeroFiatValue.type, toUnits(zeroFiatValue.type, fiat))
        }

    private fun getPriceResponse(value: Value): Flow<PriceResponse?> {
        return callbackFlow {
            GitboxAPI.giftRepository.getPrice(lifecycleScope,
                code = args.product.code!!,
                quantity = args.quantity,
                amount = value.valueAsBigDecimal.toInt(),
                currencyId = zeroCryptoValue!!.getCurrencyId(),
                success = { priceResponse ->
                    if (priceResponse!!.status == PriceResponse.Status.eRROR) {
                        return@getPrice
                    }
                    offer(priceResponse)
                },
                error = { _, error ->
                    close()
                },
                finally = {
                    close()
                })
            awaitClose { }
        }
    }

    companion object {
        const val ACTION_AMOUNT_SELECTED = "action_amount"
        const val AMOUNT_KEY = "amount"
        const val ENTERED_AMOUNT = "entered_amount"
    }
}