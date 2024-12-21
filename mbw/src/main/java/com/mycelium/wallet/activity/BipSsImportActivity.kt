package com.mycelium.wallet.activity

import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.BipSss.IncompatibleSharesException
import com.mrd.bitlib.crypto.BipSss.InvalidContentTypeException
import com.mrd.bitlib.crypto.BipSss.NotEnoughSharesException
import com.mrd.bitlib.crypto.BipSss.combine
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.getShare
import com.mycelium.wallet.content.HandleConfigFactory
import com.mycelium.wallet.content.ResultType
import com.mycelium.wallet.databinding.ActivityBipSsImportBinding

class BipSsImportActivity : AppCompatActivity() {
    private val shares = mutableListOf<BipSss.Share>()
    private lateinit var binding: ActivityBipSsImportBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(ActivityBipSsImportBinding.inflate(layoutInflater).apply {
            binding = this
        }.root)

        val share = intent.getSerializableExtra("share") as BipSss.Share?
        shares.add(share!!)

        binding.btScan.setOnClickListener {
            ScanActivity.callMe(this, REQUEST_SHARE_CODE, HandleConfigFactory.share)
        }
        updateUI()
    }

    private fun updateUI() {
        val last = shares.last()
        var status = getString(R.string.sss_share_number_scanned, last.shareNumber)
        try {
            val secret = combine(shares)
            // Success, send the result back immediately
            val result = Intent()
            result.putExtra(RESULT_SECRET, secret)
            setResult(RESULT_OK, result)
            finish()
            return
        } catch (e: IncompatibleSharesException) {
            status += "\n"
            status += getString(R.string.sss_incompatible_shares_warning)
            //remove the last one again, it did not fit
            shares.remove(last)
        } catch (e: NotEnoughSharesException) {
            status += "\n"
            if (e.needed == 1) {
                status += getString(R.string.sss_one_more_share_needed)
            } else {
                status += getString(R.string.sss_more_shares_needed, e.needed)
            }
        } catch (e: InvalidContentTypeException) {
            status += "\n"
            status += getString(R.string.sss_unrecognized_share_warning)
        }

        binding.tvStatus.text = status
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        if (RESULT_OK == resultCode) {
            if (REQUEST_SHARE_CODE == requestCode) {
                val type =
                    data?.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY) as ResultType?
                if (type == ResultType.SHARE) {
                    val share = data.getShare()
                    shares.add(share)
                }
            }
        } else {
            ScanActivity.toastScanError(resultCode, intent, this)
        }
        updateUI()
    }

    companion object {
        const val RESULT_SECRET: String = "secret"
        private const val REQUEST_SHARE_CODE = 1

        @JvmStatic
        fun callMe(currentActivity: Activity, share: BipSss.Share?, requestCode: Int) =
            currentActivity.startActivityForResult(
                Intent(currentActivity, BipSsImportActivity::class.java)
                    .putExtra("share", share), requestCode
            )
    }
}