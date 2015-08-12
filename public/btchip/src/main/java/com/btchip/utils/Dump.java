/*
*******************************************************************************    
*   BTChip Bitcoin Hardware Wallet Java API
*   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

package com.btchip.utils;

import java.io.ByteArrayOutputStream;

public class Dump {
	
    public static String dump(byte[] buffer, int offset, int length) {
        String result = "";
        for (int i=0; i<length; i++) {
                String temp = Integer.toHexString((buffer[offset + i]) & 0xff);
                if (temp.length() < 2) {
                        temp = "0" + temp;
                }
                result += temp;
        }
        return result;
    }

    public static String dump(byte[] buffer) {
        return dump(buffer, 0, buffer.length);
    }
    
    public static byte[] hexToBin(String src) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int i = 0;
        while (i < src.length()) {
                char x = src.charAt(i);
                if (!((x >= '0' && x <= '9') || (x >= 'A' && x <= 'F') || (x >= 'a' && x <= 'f'))) {
                        i++;
                        continue;
                }
                try {
                        result.write(Integer.valueOf("" + src.charAt(i) + src.charAt(i + 1), 16));
                        i += 2;
                }
                catch (Exception e) {
                        return null;
                }
        }
        return result.toByteArray();
    }
    
    
    
	

}
