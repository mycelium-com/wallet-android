package com.mycelium.wallet.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.mycelium.wallet.R;

@SuppressLint("RestrictedApi")
public class CustomCaptureActivity extends AppCompatActivity implements DecoratedBarcodeView.TorchListener {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private ImageButton switchFlashlightButton;
    private ImageButton switchCameraButton;
    private boolean isFrontCamera = false;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_capture);

        switchFlashlightButton = findViewById(R.id.switch_flashlight);
        switchCameraButton = findViewById(R.id.switch_camera);
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setTorchListener(this);

        if (!hasFlash()) {
            switchFlashlightButton.setVisibility(View.GONE);
        }

        if (!hasCameras()) {
            switchCameraButton.setVisibility(View.GONE);
        }

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    /**
     * Check if the device's camera has a Flashlight.
     *
     * @return true if there is Flashlight, otherwise false.
     */
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * @return true if the device has more than 1 camera
     */
    private boolean hasCameras() {
        PackageManager pm = getPackageManager();
        boolean frontCam, rearCam;

        frontCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        rearCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);

        return frontCam && rearCam;
    }

    public void onSwitchFlashlightPressed(View view) {
        if (isFlashOn) {
            setTorchOff();
        } else {
            barcodeScannerView.setTorchOn();
            isFlashOn = true;
        }
    }

    private void setTorchOff() {
        barcodeScannerView.setTorchOff();
        isFlashOn = false;
    }

    @Override
    public void onTorchOn() {
        switchFlashlightButton.setColorFilter(getResources().getColor(R.color.white_transparent));
    }

    @Override
    public void onTorchOff() {
        switchFlashlightButton.setColorFilter(getResources().getColor(R.color.black_transparent));
    }

    public void onSwitchCameraPressed(View view) {
        if (isFrontCamera) {
            setTorchOff();
            switchFlashlightButton.setEnabled(true);
            switchFlashlightButton.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_black));

            barcodeScannerView.initializeFromIntent(new Intent().putExtra(Intents.Scan.CAMERA_ID, Camera.CameraInfo.CAMERA_FACING_BACK));
            isFrontCamera = false;
            onPause();
            onResume();
        } else {
            setTorchOff();
            switchFlashlightButton.setEnabled(false);
            switchFlashlightButton.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_off));

            barcodeScannerView.initializeFromIntent(new Intent().putExtra(Intents.Scan.CAMERA_ID, Camera.CameraInfo.CAMERA_FACING_FRONT));
            isFrontCamera = true;
            onPause();
            onResume();
        }
    }
}
