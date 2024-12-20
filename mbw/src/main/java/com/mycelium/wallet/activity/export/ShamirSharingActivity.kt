package com.mycelium.wallet.activity.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.export.adapter.SharesAdapter
import com.mycelium.wallet.databinding.ActivityShamirSharingBinding


class ShamirSharingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShamirSharingBinding

    private val sharesAdapter = SharesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShamirSharingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val secret = intent.getByteArrayExtra("data")!!

        binding.rvSharesContainer.adapter = sharesAdapter
        // Button click listener
        binding.btnGenerateShares.setOnClickListener {
            val totalShares = binding.etNumberOfShares.text.toString().toIntOrNull()
            val threshold = binding.etThreshold.text.toString().toIntOrNull()

            if (totalShares != null && threshold != null && threshold <= totalShares) {
                val shares = BipSss.split(secret, totalShares, threshold)

                sharesAdapter.submitList(shares)
            } else {
                Toast.makeText(this, "Invalid input. Check your values.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun getData(context: Context, privateKey: InMemoryPrivateKey): ByteArray {
            val network = MbwManager.getInstance(context).network
            return privateKey.getPrivateKeyBytes(network)
        }

        @JvmStatic
        fun callMe(current: Activity, privateKey: InMemoryPrivateKey) =
            current.startActivity(
                Intent(current, ShamirSharingActivity::class.java)
                    .putExtra("data", getData(current, privateKey))
            )
    }
}
