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

package com.mycelium.wallet.activity.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

public class QrImageView extends AppCompatImageView {
   private static final int[] ALPHA_VALUES = new int[] { 170, 70, 255 };

   private String qrCodeText;

   private Bitmap _qrImage;
   private Bitmap _qrImageScaled;
   private Bitmap _qrImageToDraw;
   private Paint _paint;
   private int _alphaIndex;
   private boolean _tapToCycleBrightness;
   private Matrix _identityMatrix;

   public QrImageView(Context context, AttributeSet attrs) {
      this(context, attrs, 0);
   }

   public QrImageView(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
      initQRDraw();
   }

   public QrImageView(Context context) {
      super(context);
      initQRDraw();
   }

   public boolean getTapToCycleBrightness() {
      return _tapToCycleBrightness;
   }

   public void setTapToCycleBrightness(boolean enabled) {
      _tapToCycleBrightness = enabled;
      _alphaIndex = 2;
   }

   @Override
   public boolean onTouchEvent(MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN && _tapToCycleBrightness) {
         // Cycle to next alpha value, and redraw
         _alphaIndex = (_alphaIndex + 1) % ALPHA_VALUES.length;
         _qrImageToDraw = null;
         this.invalidate();
      }
      return super.onTouchEvent(event);
   }

   @SuppressLint("DrawAllocation")
   @Override
   protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      Bitmap bitmap = getBitmapToDraw();
      if (bitmap != null) {
         canvas.drawBitmap(bitmap, _identityMatrix, null);
      }
   }

   @Override
   protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      _qrImageScaled = null;
      super.onSizeChanged(w, h, oldw, oldh);
   }

   private void initQRDraw() {
      _alphaIndex = 0;
      _tapToCycleBrightness = true;
      // Initialize paint once
      _paint = new Paint();
      _qrImage = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
      _identityMatrix = new Matrix();
      this.setBackgroundColor(getResources().getColor(R.color.black));
   }

   @Override
   protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      int height = getMeasuredHeight();
      setMeasuredDimension(height, height);
   }

   public void setQrCode(String qrCode) {
      if (qrCode.equals(this.qrCodeText)) {
         // Only update QR code if necessary
         return;
      }
      this.qrCodeText = qrCode;
      _qrImage = Utils.getMinimalQRCodeBitmap(qrCodeText);
      _qrImageScaled = null;
      _qrImageToDraw = null;
      this.invalidate();
   }

   /**
    * We cannot draw the scaled image directly to the canvas in onDraw. 1. Since
    * we use setAlpha we will draw on top of what is there already, making it
    * look wrong. 2. We cannot first clear the canvas and the draw, as double
    * buffering may be used, resulting in one operation in one buffer and
    * another in the other
    */
   private Bitmap getBitmapToDraw() {
      if (_qrImageToDraw == null) {
         Bitmap scaled = getScaledBitmap();
         if (scaled != null) {
            _qrImageToDraw = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(_qrImageToDraw);
            _paint.setAlpha(ALPHA_VALUES[_alphaIndex]);
            canvas.drawBitmap(scaled, _identityMatrix, _paint);
         }
      }
      return _qrImageToDraw;
   }

   private Bitmap getScaledBitmap() {
      int height = getHeight();
      int width = getWidth();
      if (_qrImageScaled == null && width > 0 && height > 0) {
         _qrImageScaled = Bitmap.createScaledBitmap(_qrImage, width, height, false);
      }
      return _qrImageScaled;
   }

   public String getQrCode() {
      return qrCodeText;
   }
}