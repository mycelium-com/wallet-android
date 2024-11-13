package com.mycelium.giftbox.purchase

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.model.MCPrice
import com.mycelium.giftbox.client.model.getCardValue
import com.mycelium.giftbox.client.models.PriceResponse
import com.mycelium.giftbox.purchase.viewmodel.getCurrencyId
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.NumberEntry
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxAmountBinding
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.isNullOrZero
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        Value.zeroValue(Utils.getTypeByName(args.mcproduct.currency)!!)
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
                getPriceResponse(value!!).collect { response ->
                    val exchangeRate = response?.rate ?: BigDecimal.ZERO
                    if (/*it?.status != PriceResponse.Status.sUCCESS ||*/ exchangeRate <= BigDecimal.ZERO) {
                        withContext(Dispatchers.Main) {
                            AlertDialog.Builder(requireActivity())
                                    .setTitle(R.string.error)
                                    .setMessage(getString(R.string.giftcard_coin_not_acceptable, value.type.name))
                                    .setPositiveButton(R.string.button_ok) { _, _ ->
                                        findNavController().popBackStack()
                                    }
                                    .show()
                        }
                        return@collect
                    }
                    withContext(Dispatchers.Main) {
                        //update crypto amount
//                        val cryptoAmountFromFiat = value.valueAsLong.toBigDecimal().setScale(account?.coinType?.unitExponent!!) /
//                                        exchangeRate.movePointRight(Utils.getTypeByName(args.mcproduct.currency!!)!!.unitExponent)
//                        val cryptoAmountValue =
//                            valueOf(
//                                account?.coinType!!,
//                                toUnits(account?.coinType!!, cryptoAmountFromFiat)
//                            )
                        val cryptoAmountValue =
                            valueOf(
                                account?.coinType!!,
                                toUnits(account?.coinType!!, response?.amount!!)
                            )
                        binding.tvCryptoAmount.isVisible = true
                        binding.tvCryptoAmount.text = cryptoAmountValue.toStringFriendlyWithUnit()

                        //update spendable
                        val maxSpendable = getMaxSpendable()
                        val fiatSpendable = maxSpendable.valueAsBigDecimal.multiply(exchangeRate)
                        binding.spendableLayout.isVisible = true

                        binding.tvSpendableFiatAmount.text = "~" + valueOf(
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
                setEnteredAmount(args.mcproduct.maxFaceValue.toPlainString()!!)
                numberEntry!!.setEntry(args.mcproduct.maxFaceValue, getMaxDecimal(_amount?.type!!))
            }
            tvCardValue.text = args.mcproduct.getCardValue()
            if(account is ERC20Account) {
                binding.parentAccountLayout.isVisible = true
                binding.erc20Tips.isVisible = true
                val parentAccount = (account as ERC20Account).ethAcc
                binding.parentAccountTitle.text = "${parentAccount.label}:"
                binding.parentAccountBalance.text = parentAccount.accountBalance.spendable.toStringFriendlyWithUnit()
            } else {
                binding.parentAccountLayout.isVisible = false
                binding.erc20Tips.isVisible = false
            }
        }
        lifecycleScope.launch(IO) {
            val maxSpendable = getMaxSpendable()
            withContext(Dispatchers.Main) {
                binding.spendableLayout.isVisible = true
                binding.tvSpendableCryptoAmount.text = maxSpendable.toStringFriendlyWithUnit()
            }
        }

        initNumberEntry(savedInstanceState)
    }

    private fun getMaxSpendable() =
        account?.calculateMaxSpendableAmount(feeEstimation.normal, null, null)!!

    private val feeEstimation by lazy {
        mbwManager.getFeeProvider(account?.basedOnCoinType).estimation
    }

    private fun getMaxDecimal(assetInfo: AssetInfo): Int =
            (assetInfo as? FiatType)?.unitExponent
                    ?: assetInfo.unitExponent - mbwManager.getDenomination(_amount?.type).scale

    fun zeroValue(): Value = Value.zeroValue(Utils.getTypeByName(args.mcproduct.currency)!!)

    private fun toUnits(assetInfo: String, amount: BigDecimal): BigInteger =
        toUnits(Utils.getTypeByName(args.mcproduct.currency)!!, amount)

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
        GitboxAPI.mcGiftRepository.getPrice(lifecycleScope,
            code = args.mcproduct.id ?: "",
//            quantity = 1,
            amount = _amount?.valueAsLong?.div(100)?.toInt()!!,
            currencyId = args.mcproduct.currency!!,
            success = { priceResponse ->
//                val conversionError = priceResponse!!.status == PriceResponse.Status.eRROR
                val maxSpendableFiat = convertToFiat(priceResponse, getMaxSpendable())
                val insufficientFunds = _amount!!.moreThan(maxSpendableFiat!!)
                val exceedCardPrice = _amount!!.moreThan(
                    valueOf(
                        _amount!!.type,
                        toUnits(args.mcproduct.currency!!, args.mcproduct.maxFaceValue)
                    )
                )
                val minimumPrice = valueOf(
                    _amount!!.type,
                    toUnits(args.mcproduct.currency!!, args.mcproduct.minFaceValue)
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
                        if (/*conversionError ||*/ insufficientFunds || exceedCardPrice || lessMinimumCardPrice) R.color.red_error else R.color.white,
                        null
                    )
                )
                binding.btOk.isEnabled = !(/*conversionError ||*/ insufficientFunds || exceedCardPrice || lessMinimumCardPrice)
                if (insufficientFunds /*&& !conversionError*/) {
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
                toUnits(args.mcproduct.currency!!, args.mcproduct.minFaceValue)
            )
        )
                && _amount!!.lessOrEqualThan(
            valueOf(
                _amount!!.type,
                toUnits(args.mcproduct.currency!!, args.mcproduct.maxFaceValue)
            )
        )
        binding.btOk.isEnabled = isValid
    }

    private fun convertToFiat(response: MCPrice?, value: Value): Value? =
        (response?.rate ?: BigDecimal.ZERO)?.let {
            val fiat = value.valueAsBigDecimal.multiply(it)
            valueOf(zeroFiatValue.type, toUnits(zeroFiatValue.type, fiat))
        }


    private fun getPriceResponse(value: Value): Flow<MCPrice?> {
        return callbackFlow {
            GitboxAPI.mcGiftRepository.getPrice(lifecycleScope,
                code = args.mcproduct.id!!,
//                quantity = args.quantity,
                amount = value.valueAsBigDecimal.toInt(),
                currencyId = args.mcproduct.currency!!,
                success = { response ->
//                    if (response!!.status == PriceResponse.Status.eRROR) {
//                        return@getPrice
//                    }
                    trySend(response)
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