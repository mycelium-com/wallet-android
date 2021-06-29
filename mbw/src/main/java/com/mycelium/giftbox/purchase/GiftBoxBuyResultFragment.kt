package com.mycelium.giftbox.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.details.MODE
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.txdetails.*
import com.mycelium.wallet.activity.txdetails.BtcDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.txdetails.BtcvDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.txdetails.EthDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.txdetails.FioDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay
import com.mycelium.wallet.activity.util.TransactionDetailsLabel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxBuyResultBinding
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthTransaction
import com.mycelium.wapi.wallet.fio.FioAccount
import kotlinx.android.synthetic.main.fragment_giftbox_buy_result.*
import kotlinx.android.synthetic.main.fragment_giftbox_details_header.*
import kotlinx.android.synthetic.main.fragment_giftbox_details_header.tvExpire
import kotlinx.android.synthetic.main.giftcard_send_info.*
import java.text.DateFormat
import java.util.*


class GiftBoxBuyResultFragment : Fragment() {
    private lateinit var tx: TransactionSummary
    val viewModel: GiftboxBuyResultViewModel by viewModels()
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
        setupNavigation()
        viewModel.totalAmountFiatString.value = args.totalFiat.toStringWithUnit()
        viewModel.totalAmountCryptoString.value = "~" + args.totalCrypto.toStringWithUnit()
        viewModel.minerFeeFiat.value = args.minerFeeFiat.toStringWithUnit()
        viewModel.minerFeeCrypto.value = "~" + args.minerFeeCrypto.toStringWithUnit()
        val product = args.productResponse
        with(binding) {
            ivImage.loadImage(product.cardImageUrl)
            tvName.text = product.name
            tvExpire.text =
                if (product?.expiryInMonths != null) "${product.expiryDatePolicy} (${product.expiryInMonths} months)" else "Does not expire"
            tvCardValueHeader.text =
                """From ${product?.minimumValue} to ${product?.maximumValue} ${product?.currencyCode?.toUpperCase()}"""
            tvQuantity.text = args.quantity.toString()
        }
        view.findViewById<TextView>(R.id.tvCountry).text = product?.countries?.mapNotNull {
            CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
        }?.joinToString { it.name }

        binding?.btSend?.setOnClickListener {
            if (args.quantity == 1) {
                loadOrder()
            } else gotoMainPage()

        }
        binding?.btSend?.text =
            if (args.quantity == 1) getString(R.string.gift_card) else getString(R.string.gift_cards)
        binding?.more?.setOnClickListener {
            viewModel.more.value = !viewModel.more.value!!
        }
        val accountId = args.accountId
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        val account = walletManager.getAccount(accountId)
        tx =  account?.getTxSummary(args.transaction.id)!!

        if (childFragmentManager.findFragmentById(R.id.spec_details_fragment) == null) {
            val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
            if (account is EthAccount || account is ERC20Account) {
                transaction.add(R.id.spec_details_fragment, EthDetailsFragment.newInstance(tx))
            } else if (account is FioAccount) {
                transaction.add(R.id.spec_details_fragment, FioDetailsFragment.newInstance(tx))
            } else if (account is BitcoinVaultHdAccount) {
                transaction.add(R.id.spec_details_fragment, newInstance(tx, accountId))
            } else {
                transaction.add(R.id.spec_details_fragment, newInstance(tx, false, accountId))
            }
            transaction.commit()
        }
        updateUi()
    }

    private fun setupNavigation() {
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun gotoMainPage() {
        findNavController().navigate(
            GiftBoxBuyResultFragmentDirections.toGiftBox(true)
        )
    }

    private fun updateUi() {
        // Set Hash
        val tvHash: TransactionDetailsLabel = binding?.root?.findViewById(R.id.tvHash)!!
        tvHash.setColuMode(false)
        tvHash.setTransaction(tx)

        // Set Confirmed
        val confirmations: Int = tx.getConfirmations()
        var confirmed: String
        confirmed = if (confirmations > 0) {
            resources.getString(R.string.confirmed_in_block, tx.getHeight())
        } else {
            resources.getString(R.string.no)
        }

        // check if tx is in outgoing queue
        val confirmationsDisplay: TransactionConfirmationsDisplay =
            binding?.root?.findViewById(R.id.tcdConfirmations)!!
        val confirmationsCount: TextView = binding?.root?.findViewById(R.id.tvConfirmations)!!
        if (tx != null && tx.isQueuedOutgoing()) {
            confirmationsDisplay.setNeedsBroadcast()
            confirmationsCount.text = ""
            confirmed = resources.getString(R.string.transaction_not_broadcasted_info)
        } else {
            confirmationsDisplay.setConfirmations(confirmations)
            confirmationsCount.text = confirmations.toString()
        }
        (binding?.root?.findViewById<View>(R.id.tvConfirmed) as TextView).text =
            confirmed

        // Set Date & Time
        val date: Date = Date(tx.getTimestamp() * 1000L)
        val locale = resources.configuration.locale
        val dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale)
        val dateString = dayFormat.format(date)
        (binding?.root?.findViewById<View>(R.id.tvDate) as TextView).text = dateString
        val hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale)
        val timeString = hourFormat.format(date)
        (binding?.root?.findViewById<View>(R.id.tvTime) as TextView).text = timeString
    }

    private fun loadOrder() {
        loader(true)
        GitboxAPI.giftRepository.getOrders(scope = lifecycleScope, success = {
            val order = it?.items?.sortedBy { it.timestamp }?.reversed()?.firstOrNull()
            order?.let {
                findNavController().navigate(
                    GiftBoxBuyResultFragmentDirections.toDetails(it, MODE.STATUS)
                )
            } ?: run {
                gotoMainPage()
            }
        }, error = { _, msg ->
            Toaster(this).toast(msg, true)
            gotoMainPage()
        }, finally = {
            loader(false)
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

class GiftboxBuyResultViewModel : ViewModel() {
    val totalAmountFiatString = MutableLiveData("")
    val totalAmountCryptoString = MutableLiveData("")
    val minerFeeFiat = MutableLiveData("")
    val minerFeeCrypto = MutableLiveData("")
    val more = MutableLiveData(true)
    val moreText = Transformations.map(more) {
        if (it) {
            "Show transaction details >"
        } else {
            "Show transaction details (hide)"
        }
    }
}