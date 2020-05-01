package com.mycelium.bequant.kyc.steps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import kotlinx.android.synthetic.main.dialog_bequant_document_attach.*


class DocumentAttachDialog : BottomSheetDialogFragment() {

    private var hasCameraPermission: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.dialog_bequant_document_attach, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera.setOnClickListener {

            hasCameraPermission = Utils.hasOrRequestAccess(this, Manifest.permission.CAMERA, CAMERA_REQUEST_CODE)
            if (!hasCameraPermission) {
                // finishError(R.string.no_camera_permission);
                return@setOnClickListener
            }
            startCamera()

            dismissAllowingStateLoss()
        }
        upload.setOnClickListener {

            targetFragment?.startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    targetRequestCode or REQURST_CODE_GALARY)


            dismissAllowingStateLoss()        }
    }

    private fun startCamera() {
        targetFragment?.startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                targetRequestCode or REQURST_CODE_CAMERA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE-> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                }
            }
        }
    }

    companion object {
        const val REQURST_CODE_CAMERA = 1000
        const val REQURST_CODE_GALARY = 2000
        private val CAMERA_REQUEST_CODE = 504
    }
}
