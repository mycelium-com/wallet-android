package com.mycelium.bequant.kyc.steps

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.dialog_bequant_document_attach.*


class DocumentAttachDialog : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.dialog_bequant_document_attach, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera.setOnClickListener {
            dismissAllowingStateLoss()
            targetFragment?.startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    targetRequestCode or REQURST_CODE_CAMERA)
        }
        upload.setOnClickListener {
            dismissAllowingStateLoss()
            targetFragment?.startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    targetRequestCode or REQURST_CODE_GALARY)
        }
    }

    companion object {
        const val REQURST_CODE_CAMERA = 1000
        const val REQURST_CODE_GALARY = 2000
    }
}
