package com.mycelium.wallet.activity.update

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.UpdateNotificationActivity
import com.mycelium.wallet.databinding.FragmentAppUpdateNotificationBinding
import com.mycelium.wallet.external.partner.openLink
import com.mycelium.wapi.api.response.VersionInfoExResponse

class UpdateNotificationBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.MyceliumModern_BottomSheetDialogTheme_Transparent

    private var binding: FragmentAppUpdateNotificationBinding? = null

    private var versionNumber: String = ""
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentAppUpdateNotificationBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val response = arguments?.getSerializable(UpdateNotificationActivity.RESPONSE) as VersionInfoExResponse
        versionNumber = response.versionNumber
        binding?.header?.titleTextView?.text = getString(R.string.update_acvailable)
        binding?.header?.versionTextView?.text = getString(R.string.current_version_s, BuildConfig.VERSION_NAME)
        if (response.versionMessage?.isNotEmpty() == true &&
            !"Update available".equals(response.versionMessage, true)) {
            binding?.updateMessage?.text = response.versionMessage
            binding?.updateMessage?.isVisible = true
            binding?.listTypeTextView?.isVisible = true
        } else {
            binding?.updateMessage?.isVisible = false
            binding?.listTypeTextView?.isVisible = false
        }
        binding?.newVersion?.text = getString(R.string.new_version_s, response.versionNumber)
        binding?.skip?.setOnClickListener {
            MbwManager.getInstance(requireContext()).versionManager.ignoreVersion(versionNumber)
            dismiss()
        }
        binding?.updateFromMycelium?.setOnClickListener {
            openLink(response.directDownload.toString())
        }
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse(MYCELIUM_GOOGLE_PLAY_URI))
        val hasPlaystore = context?.packageManager?.resolveActivity(playStoreIntent, PackageManager.GET_RESOLVED_FILTER) != null
        if (!hasPlaystore) {
            binding?.updateFromGooglePlay?.visibility = View.GONE
        }
        binding?.updateFromGooglePlay?.setOnClickListener {
            openLink(MYCELIUM_GOOGLE_PLAY_URI)
        }
        binding?.listTypeTextView?.text = binding?.listTypeTextView?.text?.toSpannable()?.apply {
            setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isFitToContents = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        val MYCELIUM_GOOGLE_PLAY_URI = "market://details?id=com.mycelium.wallet"
    }
}