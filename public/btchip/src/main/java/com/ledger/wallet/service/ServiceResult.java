/*
*******************************************************************************    
*   Ledger Bitcoin Hardware Wallet Java API
*   (c) 2014-2015 Ledger - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
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

package com.ledger.wallet.service;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceResult implements Parcelable {
	
	private byte[] result;
	private byte[] extendedResult;
	private String exceptionMessage;
	
	public ServiceResult() {		
	}
	
	public ServiceResult(byte[] result) {
		this.result = result;
	}
	
	public ServiceResult(byte[] result, byte[] extendedResult) {
		this.result = result;
		this.extendedResult = extendedResult;
	}
	
	public ServiceResult(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}
	
	public ServiceResult(Throwable t) {
		this.exceptionMessage = t.toString();
	}
	
	public ServiceResult(Parcel parcel) {
		this.result = readArrayFromParcel(parcel);
		this.extendedResult = readArrayFromParcel(parcel);
		this.exceptionMessage = readStringFromParcel(parcel);
	}
	
	public byte[] getResult() {
		return result;
	}
	
	public byte[] getExtendedResult() {
		return extendedResult;
	}
	
	public String getExceptionMessage() {
		return exceptionMessage;
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	private void writeArrayToParcel(byte[] array, Parcel dest) {
		if (array != null) {
			dest.writeByte((byte)0);
			dest.writeInt(array.length);
			dest.writeByteArray(array);
		}
		else {
			dest.writeByte((byte)1);
		}
	}

	private void writeStringToParcel(String data, Parcel dest) {
		if (data != null) {
			dest.writeByte((byte)0);
			dest.writeString(data);
		}
		else {
			dest.writeByte((byte)1);
		}
	}
	
	private byte[] readArrayFromParcel(Parcel src) {
		if (src.readByte() == (byte)1) {
			return null;
		}
		int arraySize = src.readInt();
		byte[] result = new byte[arraySize];
		src.readByteArray(result);
		return result;
	}
	
	private String readStringFromParcel(Parcel src) {
		if (src.readByte() == (byte)1) {
			return null;
		}
		return src.readString();
	}
		
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		writeArrayToParcel(result, dest);
		writeArrayToParcel(extendedResult, dest);
		writeStringToParcel(exceptionMessage, dest);
	}
	
	public static final Parcelable.Creator<ServiceResult> CREATOR = new Creator<ServiceResult>() {

		@Override
		public ServiceResult createFromParcel(Parcel source) {
			return new ServiceResult(source);
		}

		@Override
		public ServiceResult[] newArray(int size) {
			return new ServiceResult[size];
		}
		
	};

}
