package com.mycelium.giftbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_gift_box_root.*

class GiftBoxRootActivity : AppCompatActivity(R.layout.activity_gift_box_root) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = (nav_host_fragment as NavHostFragment).navController.navInflater
        val graph = inflater.inflate(R.navigation.giftbox)
        nav_host_fragment.findNavController().setGraph(graph, intent.extras)

        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_back_arrow))
            setTitle("Gift cards")
        }
    }
}