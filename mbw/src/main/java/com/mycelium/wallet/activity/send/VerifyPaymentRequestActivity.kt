package com.mycelium.wallet.activity.send

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.mycelium.net.ServerEndpointType
import com.mycelium.paymentrequest.PaymentRequestException
import com.mycelium.paymentrequest.PaymentRequestInformation
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.VerifyPaymentRequestActivityBinding
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.WithCallback
import com.squareup.okhttp.OkHttpClient
import com.squareup.otto.Subscribe
import org.ocpsoft.prettytime.PrettyTime
import org.ocpsoft.prettytime.units.JustNow
import org.ocpsoft.prettytime.units.Millisecond
import java.util.Date
import java.util.UUID

class VerifyPaymentRequestActivity : AppCompatActivity() {
    private var progress: ProgressDialog? = null
    private var requestHandler: PaymentRequestHandler? = null
    private var requestException: Throwable? = null
    private var mbw: MbwManager? = null
    private var requestInformation: PaymentRequestInformation? = null
    private var paymentRequestHandlerUuid: String? = null
    private var checkExpired: Handler? = null
    private var expiredUpdater: Runnable? = null

    private lateinit var binding: VerifyPaymentRequestActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(VerifyPaymentRequestActivityBinding.inflate(layoutInflater).apply {
            binding = this
        }.root)
        mbw = MbwManager.getInstance(this)

        // only popup the keyboard if the user taps the textbox
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val assetUri = intent.getSerializableExtra(CALLBACK_URI) as AssetUri?
        val rawPaymentRequest = intent.getSerializableExtra(RAW_PR) as ByteArray?

        // either one of them must be set...
        Preconditions.checkArgument(
            (assetUri is WithCallback
                    && !Strings.isNullOrEmpty((assetUri as WithCallback).callbackURL))
                    || rawPaymentRequest != null
        )

        binding.btAccept.isEnabled = false

        if (savedInstanceState != null) {
            paymentRequestHandlerUuid = savedInstanceState.getString(PAYMENT_REQUEST_HANDLER_ID)
            if (paymentRequestHandlerUuid != null) {
                requestHandler = mbw!!.backgroundObjectsCache
                    .getIfPresent(paymentRequestHandlerUuid) as PaymentRequestHandler?
            }
        }
        var progressMsg = getString(R.string.payment_request_fetching_payment_request)

        if (requestHandler == null) {
            paymentRequestHandlerUuid = UUID.randomUUID().toString()

            // check if we are currently in TOR-only mode - if so, setup the PaymentRequestHandler
            // that all http(s) calls get routed over TOR
            if (mbw!!.torMode == ServerEndpointType.Types.ONLY_TOR && mbw!!.torManager != null) {
                requestHandler =
                    object : PaymentRequestHandler(MbwManager.getEventBus(), mbw!!.network) {
                        override fun getHttpClient(): OkHttpClient {
                            val client = super.getHttpClient()
                            return mbw!!.torManager.setupClient(client)
                        }
                    }
                progressMsg += getString(R.string.payment_request_over_tor)
            } else {
                requestHandler = PaymentRequestHandler(MbwManager.getEventBus(), mbw!!.network)
            }
            mbw!!.backgroundObjectsCache.put(paymentRequestHandlerUuid, requestHandler)
        }

        progress = ProgressDialog.show(this, "", progressMsg, true)

        if (rawPaymentRequest != null) {
            requestHandler!!.parseRawPaymentRequest(rawPaymentRequest)
        } else {
            requestHandler!!.fetchPaymentRequest(assetUri)
        }

        checkExpired = Handler()
        expiredUpdater = object : Runnable {
            override fun run() {
                updateExpireWarning()
                checkExpired!!.postDelayed(this, 1000)
            }
        }

        binding.btAccept.setOnClickListener { onAcceptClick() }
        binding.btDismiss.setOnClickListener { onDismissClick() }
        binding.ivSignatureWarning.setOnClickListener { onSignatureWarningClick() }
        binding.etMerchantMemo.setOnTouchListener { v, event -> scrollIntoView() }
    }

    fun scrollIntoView(): Boolean {
        binding.etMerchantMemo.requestLayout()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PAYMENT_REQUEST_HANDLER_ID, paymentRequestHandlerUuid)
    }

    public override fun onResume() {
        MbwManager.getEventBus().register(this)
        expiredUpdater!!.run()
        super.onResume()
    }

    public override fun onPause() {
        progress!!.dismiss()
        MbwManager.getEventBus().unregister(this)
        checkExpired!!.removeCallbacks(expiredUpdater!!)
        super.onPause()
    }

    fun onAcceptClick() {
        val result = Intent()
        requestHandler!!.setMerchantMemo(binding.etMerchantMemo.text.toString())
        mbw!!.backgroundObjectsCache.put(paymentRequestHandlerUuid, requestHandler)
        result.putExtra("REQUEST_PAYMENT_HANDLER_ID", paymentRequestHandlerUuid)
        setResult(RESULT_OK, result)
        finish()
    }

    fun onDismissClick() {
        setResult(RESULT_CANCELED)
        finish()
    }

    fun onSignatureWarningClick() {
        Utils.showSimpleMessageDialog(this, getString(R.string.payment_request_warning_no_sig))
    }

    private fun updateUi() {
        if (requestException == null && requestInformation == null) {
            // no payment request (or error) available
            return
        }

        if (requestException != null) {
            binding.tvMerchant.text = requestException!!.message
            binding.tvValid.text = getString(R.string.payment_request_invalid_signature)
            binding.btAccept.isEnabled = false
            binding.llAmount.visibility = View.GONE
            binding.llTime.visibility = View.GONE
            binding.llTimeExpires.visibility = View.GONE
            binding.llMessageToMerchant.visibility = View.GONE
            binding.llMessage.visibility = View.GONE
            binding.llErrorDetailsDisplay.visibility = View.VISIBLE
            val message: String?
            val cause = requestException!!.cause
            message = if (cause != null) {
                ", " + cause.localizedMessage
            } else {
                requestException!!.localizedMessage
            }
            binding.tvErrorDetails.text = message
        } else {
            binding.llErrorDetailsDisplay.visibility = View.GONE

            if (requestInformation!!.hasValidSignature()) {
                binding.tvValid.text = getString(R.string.payment_request_signature_okay)
                val pkiVerificationData = requestInformation!!.pkiVerificationData
                binding.tvMerchant.text = pkiVerificationData.displayName
                binding.ivSignatureWarning.visibility = View.GONE
            } else {
                binding.tvValid.text = getString(R.string.payment_request_unsigned_request)
                binding.tvMerchant.text = getString(R.string.payment_request_unable_to_verify)
                binding.ivSignatureWarning.visibility = View.VISIBLE
            }

            if (!requestInformation!!.isExpired) {
                binding.btAccept.isEnabled = true
            }

            if (requestInformation!!.hasAmount()) {
                val totalAmount = requestInformation!!.outputs.totalAmount
                binding.tvAmount.text = mbw!!.getBtcValueString(totalAmount)
                val currencySwitcher = mbw!!.currencySwitcher
                if (currencySwitcher.isFiatExchangeRateAvailable(Utils.getBtcCoinType())) {
                    binding.tvFiatAmount.visibility = View.VISIBLE
                    val btcValue = Utils.getBtcCoinType().value(totalAmount)
                    val fiatValue = currencySwitcher.getAsFiatValue(btcValue)
                    var fiatAppendment = ""
                    if (fiatValue != null) {
                        fiatAppendment = String.format("(~%s)", fiatValue.toStringWithUnit())
                    }
                    binding.tvFiatAmount.text = fiatAppendment
                } else {
                    binding.tvFiatAmount.visibility = View.GONE
                }
            } else {
                binding.tvAmount.text = getString(R.string.payment_request_no_amount_specified)
                binding.tvFiatAmount.visibility = View.GONE
            }
            binding.tvMessage.text = requestInformation!!.paymentDetails.memo

            if (!requestInformation!!.hasPaymentCallbackUrl()) {
                binding.llMessageToMerchant.visibility = View.GONE
            }

            if (requestInformation!!.paymentDetails.time != null) {
                binding.tvTimeCreated.text =
                    Utils.getFormattedDate(
                        this, Date(
                            requestInformation!!.paymentDetails.time * 1000L
                        )
                    )
            } else {
                binding.tvTimeCreated.text = getString(R.string.data_not_available_short)
            }

            updateExpireWarning()
        }
    }

    private fun updateExpireWarning() {
        if (requestInformation != null) {
            if (requestInformation!!.paymentDetails.expires != null) {
                val prettyTime = PrettyTime(mbw!!.locale)
                val date = Date(requestInformation!!.paymentDetails.expires * 1000L)
                prettyTime.removeUnit(JustNow::class.java)
                prettyTime.removeUnit(Millisecond::class.java)
                val duration = prettyTime.format(date)
                binding.tvTimeExpires.text = String.format(
                    "%s\n(%s)",
                    Utils.getFormattedDate(this, date),
                    duration
                )
            } else {
                binding.tvTimeExpires.text = getString(R.string.data_not_available_short)
            }

            // show a red warning, if it is expired
            if (requestInformation!!.isExpired) {
                binding.tvTimeExpires.setTextColor(resources.getColor(R.color.status_red))
                binding.btAccept.isEnabled = false
            } else {
                // reset color
                binding.tvTimeExpires.setTextColor(binding.tvTimeExpires.textColors.defaultColor)
            }
        }
    }

    @Subscribe
    fun exchangeRatesRefreshed(event: ExchangeRatesRefreshed?) {
        updateUi()
    }

    @Subscribe
    fun onPaymentRequestFetched(paymentRequestInformation: PaymentRequestInformation?) {
        progress!!.dismiss()
        requestInformation = paymentRequestInformation
        requestException = null
        updateUi()
    }

    @Subscribe
    fun onPaymentRequestException(paymentRequestException: PaymentRequestException?) {
        progress!!.dismiss()
        requestInformation = null
        requestException = paymentRequestException
        updateUi()
    }

    companion object {
        private const val CALLBACK_URI = "payment_uri"
        private const val RAW_PR = "raw_pr"
        private const val PAYMENT_REQUEST_HANDLER_ID = "paymentRequestHandlerId"
        fun getIntent(currentActivity: Activity?, uri: AssetUri?): Intent =
            Intent(currentActivity, VerifyPaymentRequestActivity::class.java)
                .putExtra(CALLBACK_URI, uri)

        fun getIntent(currentActivity: Activity?, rawPaymentRequest: ByteArray?): Intent =
            Intent(currentActivity, VerifyPaymentRequestActivity::class.java)
                .putExtra(RAW_PR, rawPaymentRequest)
    }
}
