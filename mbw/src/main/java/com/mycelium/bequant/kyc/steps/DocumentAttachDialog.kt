package com.mycelium.bequant.kyc.steps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.DialogBequantDocumentAttachBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class DocumentAttachDialog : BottomSheetDialogFragment() {
    private var hasCameraPermission: Boolean = false
    var binding: DialogBequantDocumentAttachBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            DialogBequantDocumentAttachBinding.inflate(inflater, container, false)
                .apply {
                    binding = this
                }
                .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.camera?.setOnClickListener {
            hasCameraPermission = Utils.hasOrRequestAccess(this, Manifest.permission.CAMERA, CAMERA_REQUEST_CODE)
            if (!hasCameraPermission) {
                // finishError(R.string.no_camera_permission);
                return@setOnClickListener
            }
            startCamera()
            dismissAllowingStateLoss()
        }
        binding?.upload?.setOnClickListener {
            targetFragment?.startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    targetRequestCode)
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun startCamera() {
        val photoFile = createImageFile()
        val authority = "${BuildConfig.APPLICATION_ID}.files"
        val photoURI = FileProvider.getUriForFile(requireContext(), authority, photoFile)
        targetFragment?.startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, photoURI), targetRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                }
            }
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = File(requireContext().filesDir, "tmp/bequant/Pictures")
        if(!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoFile = this
            delete() // Once its last file handler is garbage collected, the file will be deleted.
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 504

        var currentPhotoFile: File? = null
    }
}
