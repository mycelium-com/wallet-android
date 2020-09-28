package com.mycelium.wallet.activity.fio.registerdomain

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mycelium.wallet.R

class RegisterFIODomainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_fio_domain)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = "Register FIO Domain"
        }
    }
}
