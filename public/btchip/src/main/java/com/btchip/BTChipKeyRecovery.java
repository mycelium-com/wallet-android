package com.btchip;

public interface BTChipKeyRecovery {
	
	public byte[] recoverKey(int recId, byte[] signature, byte[] hashValue);

}
