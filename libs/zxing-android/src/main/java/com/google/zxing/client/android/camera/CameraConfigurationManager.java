/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.client.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.camera.open.CameraFacing;
import com.google.zxing.client.android.camera.open.OpenCamera;

import java.util.*;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
final class CameraConfigurationManager {

   private static final String TAG = "CameraConfiguration";

   // This is bigger than the size of a small screen, which is still supported. The routine
   // below will still select the default (presumably 320x240) size for these. This prevents
   // accidental selection of very low resolution on some devices.
   private static final int MIN_PREVIEW_PIXELS = 470 * 320; // normal screen
   private static final int MAX_PREVIEW_PIXELS = 1280 * 800;
   //private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
   //private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;

   private final Context context;
   private int cwNeededRotation;
   private int cwRotationFromDisplayToCamera;
   private Point bestPreviewSize;
   private Point screenResolution;
   private Point cameraResolution;
   private Point previewSizeOnScreen;

   CameraConfigurationManager(Context context) {
      this.context = context;
   }

   /**
    * Reads, one time, values from the camera that are needed by the app.
    */
   @SuppressWarnings("deprecation")
   void initFromCameraParameters(OpenCamera camera) {
      Camera.Parameters parameters = camera.getCamera().getParameters();
      WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      Display display = manager.getDefaultDisplay();

      int displayRotation = display.getRotation();
      int cwRotationFromNaturalToDisplay;
      switch (displayRotation) {
         case Surface.ROTATION_0:
            cwRotationFromNaturalToDisplay = 0;
            break;
         case Surface.ROTATION_90:
            cwRotationFromNaturalToDisplay = 90;
            break;
         case Surface.ROTATION_180:
            cwRotationFromNaturalToDisplay = 180;
            break;
         case Surface.ROTATION_270:
            cwRotationFromNaturalToDisplay = 270;
            break;
         default:
            // Have seen this return incorrect values like -90
            if (displayRotation % 90 == 0) {
               cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
            } else {
               throw new IllegalArgumentException("Bad rotation: " + displayRotation);
            }
      }
      Log.i(TAG, "Display at: " + cwRotationFromNaturalToDisplay);

      int cwRotationFromNaturalToCamera = camera.getOrientation();
      Log.i(TAG, "Camera at: " + cwRotationFromNaturalToCamera);

      // Still not 100% sure about this. But acts like we need to flip this:
      if (camera.getFacing() == CameraFacing.FRONT) {
         cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
         Log.i(TAG, "Front camera overriden to: " + cwRotationFromNaturalToCamera);
      }

      cwRotationFromDisplayToCamera =
            (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
      Log.i(TAG, "Final display orientation: " + cwRotationFromDisplayToCamera);
      if (camera.getFacing() == CameraFacing.FRONT) {
         Log.i(TAG, "Compensating rotation for front camera");
         cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
      } else {
         cwNeededRotation = cwRotationFromDisplayToCamera;
      }
      Log.i(TAG, "Clockwise rotation from display to camera: " + cwNeededRotation);

      screenResolution = new Point(display.getWidth(), display.getHeight());
      Log.i(TAG, "Screen resolution: " + screenResolution);
      cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution);
      Log.i(TAG, "Camera resolution: " + cameraResolution);
      bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution);
      Log.i(TAG, "Best available preview size: " + bestPreviewSize);

      boolean isScreenPortrait = screenResolution.x < screenResolution.y;
      boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

      if (isScreenPortrait == isPreviewSizePortrait) {
         previewSizeOnScreen = bestPreviewSize;
      } else {
         previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
      }
      Log.i(TAG, "Preview size on screen: " + previewSizeOnScreen);
   }

   void setDesiredCameraParameters(OpenCamera camera, boolean safeMode, boolean enableContinousFocus) {
      Camera theCamera = camera.getCamera();
      Camera.Parameters parameters = theCamera.getParameters();

      if (parameters == null) {
         Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
         return;
      }

      Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

      if (safeMode) {
         Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
      }

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

      initializeTorch(parameters, prefs, safeMode);

      String focusMode = null;
      if (prefs.getBoolean(PreferencesActivity.KEY_AUTO_FOCUS, true)) {
         if (safeMode || prefs.getBoolean(PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, !enableContinousFocus)) {
            focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                  Camera.Parameters.FOCUS_MODE_AUTO);
         } else {
            focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                  "continuous-picture", // Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in 4.0+
                  "continuous-video",   // Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO in 4.0+
                  Camera.Parameters.FOCUS_MODE_AUTO);
         }
      }
      // Maybe selected auto-focus but not available, so fall through here:
      if (!safeMode && focusMode == null) {
         focusMode = findSettableValue(parameters.getSupportedFocusModes(),
               Camera.Parameters.FOCUS_MODE_MACRO,
               "edof"); // Camera.Parameters.FOCUS_MODE_EDOF in 2.2+
      }
      if (focusMode != null) {
         parameters.setFocusMode(focusMode);
      }

      if (prefs.getBoolean(PreferencesActivity.KEY_INVERT_SCAN, false)) {
         String colorMode = findSettableValue(parameters.getSupportedColorEffects(),
               Camera.Parameters.EFFECT_NEGATIVE);
         if (colorMode != null) {
            parameters.setColorEffect(colorMode);
         }
      }

      parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);
      theCamera.setParameters(parameters);
      theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera);

      Camera.Parameters afterParameters = theCamera.getParameters();
      Camera.Size afterSize = afterParameters.getPreviewSize();
      if (afterSize != null && (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
         Log.w(TAG, "Camera said it supported preview size " + bestPreviewSize.x + 'x' + bestPreviewSize.y +
               ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
         bestPreviewSize.x = afterSize.width;
         bestPreviewSize.y = afterSize.height;
      }
   }

   Point getCameraResolution() {
      return cameraResolution;
   }

   Point getScreenResolution() {
      return screenResolution;
   }

   boolean getTorchState(Camera camera) {
      if (camera != null) {
         Camera.Parameters parameters = camera.getParameters();
         if (parameters != null) {
            String flashMode = camera.getParameters().getFlashMode();
            return flashMode != null &&
                  (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
                        Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
         }
      }
      return false;
   }

   void setTorch(Camera camera, boolean newSetting) {
      Camera.Parameters parameters = camera.getParameters();
      doSetTorch(parameters, newSetting);
      camera.setParameters(parameters);
   }

   private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
      boolean currentSetting = FrontLightMode.readPref(prefs) == FrontLightMode.ON;
      doSetTorch(parameters, currentSetting);
   }

   private void doSetTorch(Camera.Parameters parameters, boolean newSetting) {
      String flashMode;
      if (newSetting) {
         flashMode = findSettableValue(parameters.getSupportedFlashModes(),
               Camera.Parameters.FLASH_MODE_TORCH,
               Camera.Parameters.FLASH_MODE_ON);
      } else {
         flashMode = findSettableValue(parameters.getSupportedFlashModes(),
               Camera.Parameters.FLASH_MODE_OFF);
      }
      if (flashMode != null) {
         parameters.setFlashMode(flashMode);
      }
   }

   private Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

      List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
      if (rawSupportedSizes == null) {
         Log.w(TAG, "Device returned no supported preview sizes; using default");
         Camera.Size defaultSize = parameters.getPreviewSize();
         return new Point(defaultSize.width, defaultSize.height);
      }

      // Sort by size, descending
      List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(rawSupportedSizes);
      Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
         @Override
         public int compare(Camera.Size a, Camera.Size b) {
            int aPixels = a.height * a.width;
            int bPixels = b.height * b.width;
            if (bPixels < aPixels) {
               return -1;
            }
            if (bPixels > aPixels) {
               return 1;
            }
            return 0;
         }
      });

      if (Log.isLoggable(TAG, Log.INFO)) {
         StringBuilder previewSizesString = new StringBuilder();
         for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            previewSizesString.append(supportedPreviewSize.width).append('x')
                  .append(supportedPreviewSize.height).append(' ');
         }
         Log.i(TAG, "Supported preview sizes: " + previewSizesString);
      }

      Point bestSize = null;
      float screenAspectRatio = (float) screenResolution.x / (float) screenResolution.y;

      float diff = Float.POSITIVE_INFINITY;
      for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
         int realWidth = supportedPreviewSize.width;
         int realHeight = supportedPreviewSize.height;
         int pixels = realWidth * realHeight;
         if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
            continue;
         }
         boolean isCandidatePortrait = realWidth < realHeight;
         int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
         int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
         if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
            Point exactPoint = new Point(realWidth, realHeight);
            Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
            return exactPoint;
         }
         float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
         float newDiff = Math.abs(aspectRatio - screenAspectRatio);
         if (newDiff < diff) {
            bestSize = new Point(realWidth, realHeight);
            diff = newDiff;
         }
      }

      if (bestSize == null) {
         Camera.Size defaultSize = parameters.getPreviewSize();
         bestSize = new Point(defaultSize.width, defaultSize.height);
         Log.i(TAG, "No suitable preview sizes, using default: " + bestSize);
      }

      Log.i(TAG, "Found best approximate preview size: " + bestSize);
      return bestSize;
   }

   private static String findSettableValue(Collection<String> supportedValues,
                                           String... desiredValues) {
      Log.i(TAG, "Supported values: " + supportedValues);
      String result = null;
      if (supportedValues != null) {
         for (String desiredValue : desiredValues) {
            if (supportedValues.contains(desiredValue)) {
               result = desiredValue;
               break;
            }
         }
      }
      Log.i(TAG, "Settable value: " + result);
      return result;
   }

}
