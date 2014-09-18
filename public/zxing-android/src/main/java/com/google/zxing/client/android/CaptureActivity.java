/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

   private static final String TAG = CaptureActivity.class.getSimpleName();

   private CameraManager cameraManager;
   private CaptureActivityHandler handler;
   private Result savedResultToShow;
   private ViewfinderView viewfinderView;
   private boolean hasSurface;
   private Collection<BarcodeFormat> decodeFormats;
   private Map<DecodeHintType, ?> decodeHints;
   private String characterSet;
   private InactivityTimer inactivityTimer;
   private BeepManager beepManager;
   private AmbientLightManager ambientLightManager;
   private boolean enableContinuousFocus;

   ViewfinderView getViewfinderView() {
      return viewfinderView;
   }

   public Handler getHandler() {
      return handler;
   }

   CameraManager getCameraManager() {
      return cameraManager;
   }

   @Override
   public void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      Window window = getWindow();
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      if (android.os.Build.VERSION.SDK_INT != android.os.Build.VERSION_CODES.GINGERBREAD
            && android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.GINGERBREAD_MR1) {
         window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
      }
      setContentView(R.layout.capture);

      hasSurface = false;
      inactivityTimer = new InactivityTimer(this);
      beepManager = new BeepManager(this);
      ambientLightManager = new AmbientLightManager(this);

      Intent intent = getIntent();
      if (intent != null) {
         enableContinuousFocus = intent.getBooleanExtra(Intents.Scan.ENABLE_CONTINUOUS_FOCUS, false);
      } else {
         enableContinuousFocus = true;
      }

      showTorchState(false);
      showFocusState(enableContinuousFocus);

      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
   }

   @SuppressWarnings("deprecation")
   @Override
   protected void onResume() {
      super.onResume();

      // CameraManager must be initialized here, not in onCreate(). This is
      // necessary because we don't
      // want to open the camera driver and measure the screen size if we're
      // going to show the help on
      // first launch. That led to bugs where the scanning rectangle was the
      // wrong size and partially
      // off screen.
      cameraManager = new CameraManager(getApplication());
      viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
      viewfinderView.setCameraManager(cameraManager);
      handler = null;

      resetStatusView();

      SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      if (hasSurface) {
         // The activity was paused but not stopped, so the surface still
         // exists. Therefore
         // surfaceCreated() won't be called, so init the camera here.
         initCamera(surfaceHolder);
      } else {
         // Install the callback and wait for surfaceCreated() to init the
         // camera.
         surfaceHolder.addCallback(this);
         surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
      }

      beepManager.updatePrefs();
      ambientLightManager.start(cameraManager);

      inactivityTimer.onResume();

      Intent intent = getIntent();

      decodeFormats = null;
      characterSet = null;

      if (intent != null) {

         // Scan the formats the intent requested, and return the result to
         // the calling activity.
         decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
         decodeHints = DecodeHintManager.parseDecodeHints(intent);

         if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
            int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
            int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
            if (width > 0 && height > 0) {
               cameraManager.setManualFramingRect(width, height);
            }
         }
         characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
      }
   }

   @Override
   protected void onPause() {
      if (handler != null) {
         handler.quitSynchronously();
         handler = null;
      }
      inactivityTimer.onPause();
      ambientLightManager.stop();
      cameraManager.closeDriver();
      if (!hasSurface) {
         SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
         SurfaceHolder surfaceHolder = surfaceView.getHolder();
         surfaceHolder.removeCallback(this);
      }
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      inactivityTimer.shutdown();
      super.onDestroy();
   }

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
      switch (keyCode) {
      case KeyEvent.KEYCODE_BACK:
         setResult(RESULT_CANCELED);
         finish();
         return true;
      case KeyEvent.KEYCODE_FOCUS:
      case KeyEvent.KEYCODE_CAMERA:
         // Handle these events so they don't launch the Camera app
         return true;
         // Use volume up/down to turn on light
      case KeyEvent.KEYCODE_VOLUME_DOWN:
         setTorch(false);
         return true;
      case KeyEvent.KEYCODE_VOLUME_UP:
         setTorch(true);
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   private void setTorch(boolean setOn) {
      cameraManager.setTorch(setOn);
      showTorchState(setOn);
   }


   public void toggleTorch(View view) {
      boolean state = cameraManager.toggleTorch();
      showTorchState(state);
   }

   @SuppressLint("NewApi")
   public static void setAlpha(View view, float alpha) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
         AlphaAnimation aa = new AlphaAnimation(alpha, alpha);
         aa.setDuration(Long.MAX_VALUE);
         view.startAnimation(aa);
      } else {
         view.setAlpha(alpha);
      }
   }

   private void showTorchState(boolean state) {
      ImageView buttonFlash = (ImageView) findViewById(R.id.button_toggle_flash);
      // if we change our MinApi level to 16, change this to setImageAlpha
      setAlpha(buttonFlash, state ? 1.0f : 0.5f);
   }

   private void showFocusState(boolean state) {
      ImageView buttonFocus = (ImageView) findViewById(R.id.button_toggle_focus);
      setAlpha(buttonFocus, state ? 1.0f : 0.5f);
   }

   public void toggleFocus(View view) {
      onPause();
      enableContinuousFocus = !enableContinuousFocus;
      showFocusState(enableContinuousFocus);
      onResume();
   }

   private void decodeOrStoreSavedBitmap(Result result) {
      // Bitmap isn't used yet -- will be used soon
      if (handler == null) {
         savedResultToShow = result;
      } else {
         if (result != null) {
            savedResultToShow = result;
         }
         if (savedResultToShow != null) {
            Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
            handler.sendMessage(message);
         }
         savedResultToShow = null;
      }
   }

   @Override
   public void surfaceCreated(SurfaceHolder holder) {
      if (holder == null) {
         Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
      }
      if (!hasSurface) {
         hasSurface = true;
         initCamera(holder);
      }
   }

   @Override
   public void surfaceDestroyed(SurfaceHolder holder) {
      hasSurface = false;
   }

   @Override
   public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

   }

   /**
    * A valid barcode has been found, so give an indication of success and show
    * the results.
    * 
    * @param rawResult
    *           The contents of the barcode.
    * @param scaleFactor
    *           amount by which thumbnail was scaled
    * @param barcode
    *           A greyscale bitmap of the camera data which was decoded.
    */
   public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
      inactivityTimer.onActivity();
      boolean fromLiveScan = barcode != null;
      if (fromLiveScan) {
         beepManager.playBeepSoundAndVibrate();
         drawResultPoints(barcode, scaleFactor, rawResult);
      }

      handleDecodeExternally(rawResult, barcode);
   }

   /**
    * Superimpose a line for 1D or dots for 2D to highlight the key features of
    * the barcode.
    * 
    * @param barcode
    *           A bitmap of the captured image.
    * @param scaleFactor
    *           amount by which thumbnail was scaled
    * @param rawResult
    *           The decoded results which contains the points to draw.
    */
   private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
      ResultPoint[] points = rawResult.getResultPoints();
      if (points != null && points.length > 0) {
         Canvas canvas = new Canvas(barcode);
         Paint paint = new Paint();
         paint.setColor(getResources().getColor(R.color.result_points));
         if (points.length == 2) {
            paint.setStrokeWidth(4.0f);
            drawLine(canvas, paint, points[0], points[1], scaleFactor);
         } else if (points.length == 4
               && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
            // Hacky special case -- draw two lines, for the barcode and
            // metadata
            drawLine(canvas, paint, points[0], points[1], scaleFactor);
            drawLine(canvas, paint, points[2], points[3], scaleFactor);
         } else {
            paint.setStrokeWidth(10.0f);
            for (ResultPoint point : points) {
               canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
            }
         }
      }
   }

   private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
      if (a != null && b != null) {
         canvas.drawLine(scaleFactor * a.getX(), scaleFactor * a.getY(), scaleFactor * b.getX(),
               scaleFactor * b.getY(), paint);
      }
   }

   // Briefly show the contents of the barcode, then handle the result outside
   // Barcode Scanner.
   private void handleDecodeExternally(Result rawResult, Bitmap barcode) {

      if (barcode != null) {
         viewfinderView.drawResultBitmap(barcode);
      }

      // Hand back whatever action they requested - this can be changed to
      // Intents.Scan.ACTION when
      // the deprecated intent is retired.
      Intent intent = new Intent(getIntent().getAction());
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
      intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
      intent.putExtra(Intents.Scan.ENABLE_CONTINUOUS_FOCUS, enableContinuousFocus ? 1 : 0 );
      byte[] rawBytes = rawResult.getRawBytes();
      if (rawBytes != null && rawBytes.length > 0) {
         intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
      }
      Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
      if (metadata != null) {
         if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
            intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION, metadata.get(ResultMetadataType.UPC_EAN_EXTENSION)
                  .toString());
         }
         Integer orientation = (Integer) metadata.get(ResultMetadataType.ORIENTATION);
         if (orientation != null) {
            intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
         }
         String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
         if (ecLevel != null) {
            intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
         }
         @SuppressWarnings("unchecked")
         Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
         if (byteSegments != null) {
            int i = 0;
            for (byte[] byteSegment : byteSegments) {
               intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
               i++;
            }
         }
      }
      sendReplyMessage(R.id.return_scan_result, intent);
   }

   private void sendReplyMessage(int id, Object arg) {
      Message message = Message.obtain(handler, id, arg);
      handler.sendMessage(message);
   }

   private void initCamera(SurfaceHolder surfaceHolder) {
      if (surfaceHolder == null) {
         throw new IllegalStateException("No SurfaceHolder provided");
      }
      if (cameraManager.isOpen()) {
         Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
         return;
      }
      try {
         int rotation = new RotationUtil(this).getDisplayOrientationForCameraParameters();
         cameraManager.openDriver(surfaceHolder, enableContinuousFocus, rotation);
         // Creating the handler starts the preview, which can also throw a
         // RuntimeException.
         if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
         }
         decodeOrStoreSavedBitmap(null);
      } catch (IOException ioe) {
         Log.w(TAG, ioe);
         displayFrameworkBugMessageAndExit();
      } catch (RuntimeException e) {
         // Barcode Scanner has seen crashes in the wild of this variety:
         // java.?lang.?RuntimeException: Fail to connect to camera service
         Log.w(TAG, "Unexpected error initializing camera", e);
         displayFrameworkBugMessageAndExit();
      }
   }

   private void displayFrameworkBugMessageAndExit() {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("Barcode Scanner");
      builder.setMessage(getString(R.string.msg_camera_framework_bug));
      builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
      builder.setOnCancelListener(new FinishListener(this));
      builder.show();
   }

   private void resetStatusView() {
      viewfinderView.setVisibility(View.VISIBLE);
   }

   public void drawViewfinder() {
      viewfinderView.drawViewfinder();
   }
}
