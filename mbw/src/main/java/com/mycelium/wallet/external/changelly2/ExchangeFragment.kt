package com.mycelium.wallet.external.changelly2

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeBinding


class ExchangeFragment : Fragment() {

    var binding: FragmentChangelly2ExchangeBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2ExchangeBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.sellLayout?.coinValue?.setOnClickListener {
            binding?.sellLayout?.coinValue?.startCursor()
            binding?.buyLayout?.coinValue?.stopCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.sellLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
        }
        binding?.buyLayout?.coinValue?.setOnClickListener {
            binding?.buyLayout?.coinValue?.startCursor()
            binding?.sellLayout?.coinValue?.stopCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.buyLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
        }

        binding?.layoutValueKeyboard?.numericKeyboard?.apply {
            setMaxDecimals(8)
            setInputListener(object : ValueKeyboard.SimpleInputListener() {
                override fun done() {
                    binding?.sellLayout?.coinValue?.stopCursor()
                    binding?.buyLayout?.coinValue?.stopCursor()
//                    useAllFunds.setVisibility(View.VISIBLE);
//                    fromValue.setHint(R.string.zero);
//                    toValue.setHint(R.string.zero);
//                    isValueForOfferOk(true);
                }
            });
            setMaxText(getString(R.string.use_all_funds), 14f)
            setPasteVisibility(View.GONE)
            visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun TextView.startCursor() {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.input_cursor, 0)
        post {
            val animationDrawable = compoundDrawables[2] as AnimationDrawable
            if (!animationDrawable.isRunning) {
                animationDrawable.start()
            }
        }
    }

    private fun TextView.stopCursor() {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
}