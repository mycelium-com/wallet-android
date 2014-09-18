/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class SimpleGestureFilter extends SimpleOnGestureListener {

   public final static int SWIPE_UP = 1;
   public final static int SWIPE_DOWN = 2;
   public final static int SWIPE_LEFT = 3;
   public final static int SWIPE_RIGHT = 4;

   public final static int MODE_TRANSPARENT = 0;
   public final static int MODE_SOLID = 1;
   public final static int MODE_DYNAMIC = 2;

   private final static int ACTION_FAKE = -13; // just an unlikely number
   private int swipe_Min_Distance = 100;
   private int swipe_Max_Distance = 4000;
   private int swipe_Min_Velocity = 1;

   private int mode = MODE_DYNAMIC;
   private boolean running = true;
   private boolean tapIndicator = false;

   private Activity context;
   private GestureDetector detector;
   private SimpleGestureListener listener;

   public SimpleGestureFilter(Activity context, SimpleGestureListener sgl) {

      this.context = context;
      this.detector = new GestureDetector(context, this);
      this.listener = sgl;
   }

   public void onTouchEvent(MotionEvent event) {

      if (!this.running)
         return;

      boolean result = this.detector.onTouchEvent(event);

      if (this.mode == MODE_SOLID)
         event.setAction(MotionEvent.ACTION_CANCEL);
      else if (this.mode == MODE_DYNAMIC) {

         if (event.getAction() == ACTION_FAKE)
            event.setAction(MotionEvent.ACTION_UP);
         else if (result)
            event.setAction(MotionEvent.ACTION_CANCEL);
         else if (this.tapIndicator) {
            event.setAction(MotionEvent.ACTION_DOWN);
            this.tapIndicator = false;
         }

      }
      // else just do nothing, it's Transparent
   }

   public void setMode(int m) {
      this.mode = m;
   }

   public int getMode() {
      return this.mode;
   }

   public void setEnabled(boolean status) {
      this.running = status;
   }

   public void setSwipeMaxDistance(int distance) {
      this.swipe_Max_Distance = distance;
   }

   public void setSwipeMinDistance(int distance) {
      this.swipe_Min_Distance = distance;
   }

   public void setSwipeMinVelocity(int distance) {
      this.swipe_Min_Velocity = distance;
   }

   public int getSwipeMaxDistance() {
      return this.swipe_Max_Distance;
   }

   public int getSwipeMinDistance() {
      return this.swipe_Min_Distance;
   }

   public int getSwipeMinVelocity() {
      return this.swipe_Min_Velocity;
   }

   @Override
   public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

      final float xDistance = Math.abs(e1.getX() - e2.getX());
      final float yDistance = Math.abs(e1.getY() - e2.getY());

      if (xDistance > this.swipe_Max_Distance || yDistance > this.swipe_Max_Distance)
         return false;

      velocityX = Math.abs(velocityX);
      velocityY = Math.abs(velocityY);
      boolean result = false;

      if (velocityX > this.swipe_Min_Velocity && xDistance > this.swipe_Min_Distance) {
         if (e1.getX() > e2.getX()) // right to left
            this.listener.onSwipe(SWIPE_LEFT);
         else
            this.listener.onSwipe(SWIPE_RIGHT);

         result = true;
      } else if (velocityY > this.swipe_Min_Velocity && yDistance > this.swipe_Min_Distance) {
         if (e1.getY() > e2.getY()) // bottom to up
            this.listener.onSwipe(SWIPE_UP);
         else
            this.listener.onSwipe(SWIPE_DOWN);

         result = true;
      }

      return result;
   }

   @Override
   public boolean onSingleTapUp(MotionEvent e) {
      this.tapIndicator = true;
      return false;
   }

   @Override
   public boolean onDoubleTap(MotionEvent arg0) {
      this.listener.onDoubleTap();
      return true;
   }

   @Override
   public boolean onDoubleTapEvent(MotionEvent arg0) {
      return true;
   }

   @Override
   public boolean onSingleTapConfirmed(MotionEvent arg0) {

      if (this.mode == MODE_DYNAMIC) { // we owe an ACTION_UP, so we fake an
         arg0.setAction(ACTION_FAKE); // action which will be converted to an
         // ACTION_UP later.
         this.context.dispatchTouchEvent(arg0);
      }

      return false;
   }

   public static interface SimpleGestureListener {
      void onSwipe(int direction);

      void onDoubleTap();
   }

}