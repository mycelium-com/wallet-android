package com.mycelium.giftbox.purchase

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.cards.GiftBoxFragment
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.Status
import com.mycelium.giftbox.getDateString
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.purchase.viewmodel.GiftboxBuyResultViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.txdetails.*
import com.mycelium.wallet.activity.txdetails.BtcDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.txdetails.BtcvDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxBuyResultBinding
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import kotlinx.android.synthetic.main.details_common.*
import kotlinx.android.synthetic.main.details_common.view.*
import kotlinx.android.synthetic.main.fragment_giftbox_buy_result.*
import kotlinx.android.synthetic.main.giftcard_send_info.*
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GiftBoxBuyResultFragment : Fragment() {
    private lateinit var tx: TransactionSummary
    private val viewModel: GiftboxBuyResultViewModel by viewModels()
    private val activityViewModel: GiftBoxViewModel by activityViewModels()
    private var binding: FragmentGiftboxBuyResultBinding? = null

    val args by navArgs<GiftBoxBuyResultFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentGiftboxBuyResultBinding.inflate(inflater).apply {
            binding = this
            lifecycleOwner = this@GiftBoxBuyResultFragment
            vm = viewModel
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.minerFeeFiat.value = args.minerFeeFiat?.toStringWithUnit()
        viewModel.minerFeeCrypto.value = "~" + args.minerFeeCrypto?.toStringWithUnit()
        loadProduct()
        loadOrder()
        binding?.more?.setOnClickListener {
            viewModel.more.value = !viewModel.more.value!!
        }

        startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.SECONDS.toMillis(10)) {
            updateAllUi()
        }

        activityViewModel.currentTab.postValue(GiftBoxFragment.PURCHASES)
    }

    private fun updateAllUi() {
        args.accountId?.let { accountId ->
            val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
            val account = walletManager.getAccount(accountId)
            args.transaction?.id?.let { txId ->
                tx = account?.getTxSummary(txId)!!
                val findFragmentById =
                    childFragmentManager.findFragmentById(R.id.spec_details_fragment)
                val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
                if (findFragmentById != null) {
                    transaction.remove(findFragmentById)
                }
                if (account is EthAccount || account is ERC20Account) {
                    transaction.add(
                        R.id.spec_details_fragment,
                        EthDetailsFragment.newInstance(tx)
                    )
                } else if (account is FioAccount) {
                    transaction.add(
                        R.id.spec_details_fragment,
                        FioDetailsFragment.newInstance(tx)
                    )
                } else if (account is BitcoinVaultHdAccount) {
                    transaction.add(R.id.spec_details_fragment, newInstance(tx, accountId))
                } else {
                    transaction.add(
                        R.id.spec_details_fragment,
                        newInstance(tx, false, accountId)
                    )
                }
                transaction.commit()

                updateUi()
            }
        } ?: run {
            binding?.more?.visibility = View.GONE
        }
    }

    private fun loadProduct() {
        args.productResponse?.let {
            binding?.detailsHeader?.ivImage?.loadImage(it.cardImageUrl)
            viewModel.setProduct(it)
        } ?: GitboxAPI.giftRepository.getProduct(lifecycleScope, args.orderResponse.productCode!!, {
            binding?.detailsHeader?.ivImage?.loadImage(it?.product?.cardImageUrl)
            viewModel.setProduct(it?.product!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    private fun loadOrder() {
        if (args.orderResponse is OrderResponse) {
            updateOrder(args.orderResponse as OrderResponse)
        } else {
            GitboxAPI.giftRepository.getOrder(lifecycleScope, args.orderResponse.clientOrderId!!, {
                updateOrder(it!!)
            }, { _, msg ->
                Toaster(this).toast(msg, true)
            })
        }
    }

    private fun updateOrder(order: OrderResponse) {
        viewModel.setOrder(order)
        when (order.status) {
            Status.pROCESSING -> {
                binding?.orderStatus?.let {
                    it.text = getString(R.string.processing)
                    val color = resources.getColor(R.color.giftbox_processing)
                    it.setTextColor(color)
                    it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_history,
                        0,
                        0,
                        0
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        it.compoundDrawableTintList = ColorStateList.valueOf(color)
                    }
                }
            }
            Status.sUCCESS -> {
                binding?.orderStatus?.let {
                    it.text = getString(R.string.success_s, order.timestamp?.getDateString(resources))
                    val color = resources.getColor(R.color.bequant_green)
                    it.setTextColor(color)
                    it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_fio_name_ok,
                        0,
                        0,
                        0
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        it.compoundDrawableTintList = ColorStateList.valueOf(color)
                    }
                }
            }
            Status.eRROR -> {
                binding?.orderStatus?.let {
                    it.text = getString(R.string.purchase_failed)
                    val color = resources.getColor(R.color.sender_recyclerview_background_red)
                    it.setTextColor(color)
                    it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_bequant_clear_24,
                        0,
                        0,
                        0
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        it.compoundDrawableTintList = ColorStateList.valueOf(color)
                    }
                }
            }
        }
    }

    private fun updateUi() {
        // Set Hash
        tvHash?.run {
            setColuMode(false)
            setTransaction(tx)
        }

        // Set Confirmed
        val confirmations = tx.getConfirmations()
        var confirmed: String
        confirmed = if (confirmations > 0) {
            resources.getString(R.string.confirmed_in_block, tx.getHeight())
        } else {
            resources.getString(R.string.no)
        }

        // check if tx is in outgoing queue
        if (tx.isQueuedOutgoing()) {
            tcdConfirmations?.setNeedsBroadcast()
            tvConfirmations?.text = ""
            confirmed = resources.getString(R.string.transaction_not_broadcasted_info)
        } else {
            tcdConfirmations?.setConfirmations(confirmations)
            tvConfirmations?.text = confirmations.toString()
        }
        tvConfirmed?.text = confirmed

        // Set Date & Time
        val date = Date(tx.getTimestamp() * 1000L)
        val locale = resources.configuration.locale
        val dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale)
        val dateString = dayFormat.format(date)
        tvDate?.text = dateString
        val hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale)
        val timeString = hourFormat.format(date)
        tvTime?.text = timeString
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}