package com.google.zxing.client.android;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Surface;
import android.view.WindowManager;

public class RotationUtil {

   private int _deviceRotationSetting;
   private int _deviceOrientation;

   public RotationUtil(Context context) {
      WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      Configuration config = context.getResources().getConfiguration();

      // Determine rotation
      int r = windowManager.getDefaultDisplay().getRotation();

      // Determine default orientation
      if (((r == Surface.ROTATION_0 || r == Surface.ROTATION_180) && config.orientation == Configuration.ORIENTATION_LANDSCAPE)
            || ((r == Surface.ROTATION_90 || r == Surface.ROTATION_270) && config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
         _deviceOrientation = Configuration.ORIENTATION_LANDSCAPE;
      } else {
         _deviceOrientation = Configuration.ORIENTATION_PORTRAIT;
      }

      _deviceRotationSetting = r;
   }

   public boolean flipWidthAndHeight() {
      switch (_deviceRotationSetting) {
      case Surface.ROTATION_0:
         // Rotated 90 degrees compared to ZXing's default mode if device is in
         // portrait mode.
         // Flip if in native portrait mode
         return _deviceOrientation == Configuration.ORIENTATION_PORTRAIT;
      case Surface.ROTATION_90:
         // Rotated 0 degrees compared to ZXing's default mode if device is in
         // portrait mode
         // Flip if in native landscape mode
         return _deviceOrientation == Configuration.ORIENTATION_LANDSCAPE;
      case Surface.ROTATION_180:
         // Rotated 270 degrees compared to ZXing's default mode if device is in
         // portrait mode
         // Flip if in portrait mode
         return _deviceOrientation == Configuration.ORIENTATION_PORTRAIT;
      case Surface.ROTATION_270:
         // Rotated 180 degrees compared to ZXing's default mode if device is in
         // portrait mode
         // Flip if in native landscape mode
         return _deviceOrientation == Configuration.ORIENTATION_LANDSCAPE;
      default:
         return false;
      }
   }

   public int getDisplayOrientationForCameraParameters() {
      if (_deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
         switch (_deviceRotationSetting) {
         case Surface.ROTATION_0:
            return 90;
         case Surface.ROTATION_90:
            return 0;
         case Surface.ROTATION_180:
            return 270;
         case Surface.ROTATION_270:
            return 180;
         default:
            return 0;
         }
      } else {
         switch (_deviceRotationSetting) {
         case Surface.ROTATION_0:
            return 0;
         case Surface.ROTATION_90:
            return 270;
         case Surface.ROTATION_180:
            return 180;
         case Surface.ROTATION_270:
            return 90;
         default:
            return 0;
         }
      }
   }

   public byte[] rotateImageData(byte[] data, int width, int height) {
      switch (getDisplayOrientationForCameraParameters()) {
      case 0:
         return data;
      case 90:
         return rotate90(data, width, height);
      case 180:
         return rotate180(data, width, height);
      case 270:
         return rotate270(data, width, height);
      default:
         return data;
      }
   }

   private static byte[] rotate90(byte[] data, int width, int height) {
      byte[] rotatedData = new byte[data.length];
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++)
            rotatedData[x * height + height - y - 1] = data[x + y * width];
      }
      return rotatedData;
   }

   private static byte[] rotate270(byte[] data, int width, int height) {
      byte[] rotatedData = new byte[data.length];
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++)
            rotatedData[(width - x - 1) * height + y] = data[x + y * width];
      }
      return rotatedData;
   }

   private static byte[] rotate180(byte[] data, int width, int height) {
      byte[] rotatedData = new byte[data.length];
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++)
            rotatedData[(width - x - 1) + width * (height - y - 1)] = data[x + y * width];
      }
      return rotatedData;
   }

}
