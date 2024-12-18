package com.mycelium.wallet.activity.export

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.Gf256
import com.mycelium.wallet.activity.export.adapter.SharesAdapter
import com.mycelium.wallet.databinding.ActivityShamirSharingBinding


class ShamirSharingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShamirSharingBinding

    private val sharesAdapter = SharesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShamirSharingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val secretText = intent.getStringExtra("data")

        binding.rvSharesContainer.adapter = sharesAdapter
        // Button click listener
        binding.btnGenerateShares.setOnClickListener {
            val totalShares = binding.etNumberOfShares.text.toString().toIntOrNull()
            val threshold = binding.etThreshold.text.toString().toIntOrNull()

            if (totalShares != null && threshold != null && threshold <= totalShares) {
                val secret = secretText!!.toByteArray()
                val shares = BipSss.split(secret, totalShares, threshold)

                sharesAdapter.submitList(shares.map { it.toString() })
            } else {
                Toast.makeText(this, "Invalid input. Check your values.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
