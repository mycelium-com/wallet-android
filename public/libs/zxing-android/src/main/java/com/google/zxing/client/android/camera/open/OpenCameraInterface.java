/*
 * Copyright (C) 2012 ZXing authors
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

package com.google.zxing.client.android.camera.open;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

public final class OpenCameraInterface {

   private static final String TAG = OpenCameraInterface.class.getName();

   private OpenCameraInterface() {
   }

   /**
    * Opens a rear-facing camera with {@link Camera#open(int)}, if one exists,
    * or opens camera 0.
    */
   public static CameraWrapper open(int cameraId) {

      int numCameras;
      numCameras = Camera.getNumberOfCameras();
      if (numCameras == 0) {
         Log.w(TAG, "No cameras!");
         return null;
      }

      int index = 0;
      if(cameraId >= 0) {
         index = cameraId;
      } else {
         while (index < numCameras) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
               break;
            }
            index++;
         }
      }

      if (index < numCameras) {
         Log.i(TAG, "Opening camera #" + index);
      } else {
         Log.i(TAG, "No camera facing back; returning camera #0");
         index = 0;
      }

      return new CameraWrapper(Camera.open(index), index);
   }

   public static class CameraWrapper {
      public final Camera camera;
      public final int cameraIndex;

      public CameraWrapper(Camera camera, int cameraIndex) {
         this.camera = camera;
         this.cameraIndex = cameraIndex;
      }
   }
}
