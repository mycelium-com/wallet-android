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
