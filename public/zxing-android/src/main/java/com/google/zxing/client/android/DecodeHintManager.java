/*
 * Copyright (C) 2013 ZXing authors
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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.DecodeHintType;

/**
 * @author Lachezar Dobrev
 */
final class DecodeHintManager {
  
  private static final String TAG = DecodeHintManager.class.getSimpleName();

  // This pattern is used in decoding integer arrays.
  private static final Pattern COMMA = Pattern.compile(",");

  private DecodeHintManager() {}

  /**
   * <p>Split a query string into a list of name-value pairs.</p>
   * 
   * <p>This is an alternative to the {@link Uri#getQueryParameterNames()} and
   * {@link Uri#getQueryParameters(String)}, which are quirky and not suitable
   * for exist-only Uri parameters.</p>
   * 
   * <p>This method ignores multiple parameters with the same name and returns the
   * first one only. This is technically incorrect, but should be acceptable due
   * to the method of processing Hints: no multiple values for a hint.</p>
   * 
   * @param query query to split
   * @return name-value pairs
   */
  private static Map<String,String> splitQuery(String query) {
    Map<String,String> map = new HashMap<String,String>();
    int pos = 0;
    while (pos < query.length()) {
      if (query.charAt(pos) == '&') {
        // Skip consecutive ampersand separators.
        pos ++;
        continue;
      }
      int amp = query.indexOf('&', pos);
      int equ = query.indexOf('=', pos);
      if (amp < 0) {
        // This is the last element in the query, no more ampersand elements.
        String name;
        String text;
        if (equ < 0) {
          // No equal sign
          name = query.substring(pos);
          name = name.replace('+', ' '); // Preemptively decode +
          name = Uri.decode(name);
          text = "";
        } else {
          // Split name and text.
          name = query.substring(pos, equ);
          name = name.replace('+', ' '); // Preemptively decode +
          name = Uri.decode(name);
          text = query.substring(equ + 1);
          text = text.replace('+', ' '); // Preemptively decode +
          text = Uri.decode(text);
        }
        if (!map.containsKey(name)) {
          map.put(name, text);
        }
        break;
      }
      if (equ < 0 || equ > amp) {
        // No equal sign until the &: this is a simple parameter with no value.
        String name = query.substring(pos, amp);
        name = name.replace('+', ' '); // Preemptively decode +
        name = Uri.decode(name);
        if (!map.containsKey(name)) {
          map.put(name, "");
        }
        pos = amp + 1;
        continue;
      }
      String name = query.substring(pos, equ);
      name = name.replace('+', ' '); // Preemptively decode +
      name = Uri.decode(name);
      String text = query.substring(equ+1, amp);
      text = text.replace('+', ' '); // Preemptively decode +
      text = Uri.decode(text);
      if (!map.containsKey(name)) {
        map.put(name, text);
      }
      pos = amp + 1;
    }
    return map;
  }

   static Map<DecodeHintType, Object> parseDecodeHints(Intent intent) {
    Bundle extras = intent.getExtras();
    if (extras == null || extras.isEmpty()) {
      return null;
    }
    Map<DecodeHintType,Object> hints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);

    for (DecodeHintType hintType: DecodeHintType.values()) {

      if (hintType == DecodeHintType.CHARACTER_SET ||
          hintType == DecodeHintType.NEED_RESULT_POINT_CALLBACK ||
          hintType == DecodeHintType.POSSIBLE_FORMATS) {
        continue; // This hint is specified in another way
      }

      String hintName = hintType.name();
      if (extras.containsKey(hintName)) {
        if (hintType.getValueType().equals(Void.class)) {
          // Void hints are just flags: use the constant specified by the DecodeHintType
          hints.put(hintType, Boolean.TRUE);
        } else {
          Object hintData = extras.get(hintName);
          if (hintType.getValueType().isInstance(hintData)) {
            hints.put(hintType, hintData);
          } else {
            Log.w(TAG, "Ignoring hint " + hintType + " because it is not assignable from " + hintData);
          }
        }
      }
    }

    Log.i(TAG, "Hints from the Intent: " + hints);
    return hints;
  }

}
