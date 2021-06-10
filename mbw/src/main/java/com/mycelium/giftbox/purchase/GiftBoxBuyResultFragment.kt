package com.mycelium.giftbox.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.txdetails.BtcDetailsFragment.Companion.newInstance
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay
import com.mycelium.wallet.activity.util.TransactionDetailsLabel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxBuyResultBinding
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
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
        viewModel.totalAmountFiatString.value = args.totalFiat.toStringWithUnit()
        viewModel.totalAmountCryptoString.value = "~" + args.totalCrypto.toStringWithUnit()
        viewModel.minerFeeFiat.value = args.minerFeeFiat.toStringWithUnit()
        viewModel.minerFeeCrypto.value = "~" + args.minerFeeCrypto.toStringWithUnit()
        val product = args.productResponse.product
        with(binding){
            ivImage.loadImage(product?.cardImageUrl)
            tvName.text = product?.name
            tvExpire.text = product?.expiryDatePolicy
            tvCardValueHeader.text =
                "From " + product?.minimumValue + " to " + product?.maximumValue
        }
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        val accountId = args.accountId
        val account = walletManager.getAccount(accountId) as AbstractBtcAccount
        val transactionSummary = account.getTransactionSummary(Sha256Hash.fromString(args.txHash))
        tx = account.getTxSummary(transactionSummary.txid.bytes)!!
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.spec_details_fragment, newInstance(tx, false, accountId))
        transaction.commit()
        updateUi()
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
}