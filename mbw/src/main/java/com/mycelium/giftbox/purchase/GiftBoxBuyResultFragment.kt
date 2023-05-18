package com.mycelium.giftbox.purchase

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.mycelium.giftbox.cards.GiftBoxFragment
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.Status
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.purchase.viewmodel.GiftboxBuyResultViewModel
import com.mycelium.view.TextDrawable
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.txdetails.BtcDetailsFragment
import com.mycelium.wallet.activity.txdetails.BtcvDetailsFragment
import com.mycelium.wallet.activity.txdetails.EthDetailsFragment
import com.mycelium.wallet.activity.txdetails.FioDetailsFragment
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxBuyResultBinding
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import kotlinx.coroutines.Job
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GiftBoxBuyResultFragment : Fragment() {
    private lateinit var tx: TransactionSummary
    private val viewModel: GiftboxBuyResultViewModel by viewModels()
    private val activityViewModel: GiftBoxViewModel by activityViewModels()
    private var binding: FragmentGiftboxBuyResultBinding? = null
    var updateJob: Job? = null
    val args by navArgs<GiftBoxBuyResultFragmentArgs>()
    private var refreshItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        activityViewModel.currentTab.postValue(GiftBoxFragment.PURCHASES)
        binding?.finish?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.orderScheme?.paymentText?.setOnClickListener {
            if (args.accountId != null) {
                findNavController().navigate(GiftBoxBuyResultFragmentDirections.actionTransactionList(args.accountId))
            }
        }
        binding?.orderScheme?.successText?.setOnClickListener {
            activityViewModel.currentTab.value = GiftBoxFragment.CARDS
            findNavController().navigate(GiftBoxBuyResultFragmentDirections.actionMyGiftCards())
        }
    }

    override fun onResume() {
        super.onResume()
        updateJob = startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.SECONDS.toMillis(15)) {
            updateAllUi()
            loadOrder(false, true)
        }
    }

    override fun onPause() {
        updateJob?.cancel()
        super.onPause()
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
                    transaction.add(R.id.spec_details_fragment, BtcvDetailsFragment.newInstance(tx, accountId))
                } else {
                    transaction.add(
                            R.id.spec_details_fragment,
                            BtcDetailsFragment.newInstance(tx, false, accountId)
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
            binding?.detailsHeader?.ivImage?.loadImage(it.cardImageUrl,
                    RequestOptions().transforms(CenterCrop(),
                            RoundedCorners(resources.getDimensionPixelSize(R.dimen.giftbox_small_corner))))
            viewModel.setProduct(it)
        } ?: GitboxAPI.giftRepository.getProduct(lifecycleScope, args.orderResponse.productCode!!, {
            binding?.detailsHeader?.ivImage?.loadImage(it?.product?.cardImageUrl,
                    RequestOptions().transforms(CenterCrop(),
                            RoundedCorners(resources.getDimensionPixelSize(R.dimen.giftbox_small_corner))))
            viewModel.setProduct(it?.product!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    private fun loadOrder(withLoader: Boolean = true, updateFromRemote:Boolean = false) {
        if (args.orderResponse is OrderResponse && !updateFromRemote) {
            updateOrder(args.orderResponse as OrderResponse)
        } else {
            if (withLoader) {
                loader(true)
            }
            showRefresh()
            GitboxAPI.giftRepository.getOrder(lifecycleScope, args.orderResponse.clientOrderId!!, {
                updateOrder(it!!)
            }, { _, msg ->
                Toaster(this).toast(msg, true)
            }, {
                hideRefresh()
                if (withLoader) {
                    loader(false)
                }
            })
        }
    }

    private fun updateOrder(order: OrderResponse) {
        viewModel.setOrder(order)
        var paymentText = getString(R.string.gift_card_after_confirmed)
        if (args.accountId == null) {
            paymentText = paymentText.replace("<[^>]*>".toRegex(), "")
        }
        binding?.orderScheme?.paymentText?.setOnClickListener(null)
        when (order.status) {
            Status.pROCESSING -> {
                binding?.orderScheme?.paidIcon?.setImageResource(R.drawable.ic_vertical_stepper_done)
                binding?.orderScheme?.paidIcon?.setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_completed)
                binding?.orderScheme?.line1?.setBackgroundResource(R.drawable.line_dash_green)
                binding?.orderScheme?.paymentIcon?.setImageDrawable(TextDrawable(resources, "2").apply {
                    setFontSize(16f)
                    setFontColor(resources.getColor(R.color.bequant_green))
                })
                binding?.orderScheme?.paymentIcon?.setBackgroundResource(R.drawable.circle_dash_green)
                binding?.orderScheme?.paymentText?.text = Html.fromHtml(paymentText)
                binding?.orderScheme?.line2?.setBackgroundResource(R.drawable.line_dash_gray)
                val grayColor = resources.getColor(R.color.giftbox_gray)
                binding?.orderScheme?.successIcon?.setImageDrawable(TextDrawable(resources, "3").apply {
                    setFontSize(16f)
                    setFontColor(grayColor)
                })
                binding?.orderScheme?.successIcon?.setBackgroundResource(R.drawable.circle_dash_gray)
                binding?.orderScheme?.successTitle?.setTextColor(grayColor)
                binding?.orderScheme?.successText?.setTextColor(grayColor)
                binding?.finish?.text = getString(R.string.button_ok)
                binding?.finish?.setOnClickListener {
                    activityViewModel.currentTab.value = GiftBoxFragment.PURCHASES
                    findNavController().popBackStack()
                }
            }
            Status.sUCCESS -> {
                binding?.orderScheme?.paidIcon?.setImageResource(R.drawable.ic_vertical_stepper_done)
                binding?.orderScheme?.paidIcon?.setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_completed)
                binding?.orderScheme?.line1?.setBackgroundColor(resources.getColor(R.color.bequant_green))
                binding?.orderScheme?.paymentIcon?.setImageResource(R.drawable.ic_vertical_stepper_done)
                binding?.orderScheme?.paymentIcon?.setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_completed)
                binding?.orderScheme?.paymentText?.text = Html.fromHtml(paymentText)
                binding?.orderScheme?.line2?.setBackgroundColor(resources.getColor(R.color.bequant_green))
                binding?.orderScheme?.successIcon?.setImageResource(R.drawable.ic_vertical_stepper_done)
                binding?.orderScheme?.successIcon?.setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_completed)
                binding?.finish?.text = getString(R.string.mygiftcards)
                binding?.finish?.setOnClickListener {
                    activityViewModel.currentTab.value = GiftBoxFragment.CARDS
                    findNavController().popBackStack()
                }
            }
            Status.eRROR -> {
                binding?.orderScheme?.paidIcon?.setImageResource(R.drawable.ic_vertical_stepper_done)
                binding?.orderScheme?.paidIcon?.setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_completed)
                binding?.orderScheme?.line1?.setBackgroundResource(R.drawable.line_dash_gray)
                binding?.orderScheme?.paymentTitle?.text = getString(R.string.failed)
                binding?.orderScheme?.paymentTitle?.setTextColor(resources.getColor(R.color.sender_recyclerview_background_red))
                binding?.orderScheme?.paymentText?.text = getString(R.string.giftbox_failed_text)
                binding?.orderScheme?.paymentIcon?.setImageResource(R.drawable.ic_bequant_clear_24)
                binding?.orderScheme?.paymentIcon?.background = null
                binding?.orderScheme?.line2?.setBackgroundResource(R.drawable.line_dash_gray)
                binding?.orderScheme?.successIcon?.setImageDrawable(TextDrawable(resources, "3").apply {
                    setFontSize(16f)
                    setFontColor(resources.getColor(R.color.giftbox_gray))
                })
                binding?.orderScheme?.successIcon?.setBackgroundResource(R.drawable.circle_dash_gray)
                binding?.finish?.text = getString(R.string.return_to_payment)
            }
            Status.EXPIRED -> {
                binding?.orderScheme?.paidIcon?.setImageResource(R.drawable.ic_vertical_stepper_done)
                binding?.orderScheme?.paidIcon?.setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_completed)
                binding?.orderScheme?.line1?.setBackgroundResource(R.drawable.line_dash_gray)
                binding?.orderScheme?.paymentTitle?.text = getString(R.string.failed)
                binding?.orderScheme?.paymentTitle?.setTextColor(resources.getColor(R.color.sender_recyclerview_background_red))
                binding?.orderScheme?.paymentText?.text = Html.fromHtml(getString(R.string.giftbox_expired_text))
                binding?.orderScheme?.paymentText?.setOnClickListener {
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SENDTO)
                                .setData(Uri.parse("mailto:support@mycelium.com")),
                            getString(R.string.send_mail)
                        )
                    )
                }
                binding?.orderScheme?.paymentIcon?.setImageResource(R.drawable.ic_bequant_clear_24)
                binding?.orderScheme?.paymentIcon?.background = null
                binding?.orderScheme?.line2?.setBackgroundResource(R.drawable.line_dash_gray)
                binding?.orderScheme?.successIcon?.setImageDrawable(TextDrawable(resources, "3").apply {
                    setFontSize(16f)
                    setFontColor(resources.getColor(R.color.giftbox_gray))
                })
                binding?.orderScheme?.successIcon?.setBackgroundResource(R.drawable.circle_dash_gray)
                binding?.finish?.text = getString(R.string.return_to_payment)
            }
        }
        if (BuildConfig.DEBUG) {
            binding?.orderScheme?.paidTitle?.setOnClickListener {
                order.status = Status.eRROR
                updateOrder(order)
            }
        }
    }

    private fun updateUi() {
        // Set Hash
        binding?.txDetails?.tvHash?.run {
            setColuMode(false)
            setTransaction(tx)
        }

        // Set Confirmed
        val confirmations = tx.confirmations
        var confirmed = if (confirmations > 0) {
            resources.getString(R.string.confirmed_in_block, tx.height)
        } else {
            resources.getString(R.string.no)
        }

        // check if tx is in outgoing queue
        if (tx.isQueuedOutgoing) {
            binding?.txDetails?.tcdConfirmations?.setNeedsBroadcast()
            binding?.txDetails?.tvConfirmations?.text = ""
            confirmed = resources.getString(R.string.transaction_not_broadcasted_info)
        } else {
            binding?.txDetails?.tcdConfirmations?.setConfirmations(confirmations)
            binding?.txDetails?.tvConfirmations?.text = confirmations.toString()
        }
        binding?.txDetails?.tvConfirmed?.text = confirmed

        // Set Date & Time
        val date = Date(tx.timestamp * 1000L)
        val locale = resources.configuration.locale
        binding?.txDetails?.tvDate?.text = DateFormat.getDateInstance(DateFormat.LONG, locale).format(date)
        binding?.txDetails?.tvTime?.text = DateFormat.getTimeInstance(DateFormat.LONG, locale).format(date)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.refresh, menu);
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        refreshItem = menu.findItem(R.id.miRefresh)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.miRefresh -> {
                    loadOrder(false, true)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun hideRefresh() {
        refreshItem?.actionView = null
    }

    private fun showRefresh() {
        refreshItem?.setActionView(R.layout.actionbar_indeterminate_progress)?.apply {
            actionView?.findViewById<ImageView>(R.id.ivTorIcon)?.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}