package com.mycelium.bequant.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentCountryFailBinding

class CountryFailFragment : Fragment(R.layout.fragment_country_fail) {

    var binding: FragmentCountryFailBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentCountryFailBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }.root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = "Registration"
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        binding?.backButton?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.finishButton?.setOnClickListener {
            try {
                findNavController().navigate(CountryFailFragmentDirections.actionFinish())
            } catch (e: IllegalArgumentException) {
                activity?.finish()
            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}