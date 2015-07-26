package com.ledger.wallet.service;

import com.ledger.wallet.service.ServiceResult;

interface ILedgerWalletService {

	ServiceResult open(int spid, in byte[] pTAData, int TAsize);
	ServiceResult initStorage(in byte[] sessionBlob, in byte[] storage);
	ServiceResult exchange(in byte[] sessionBlob, in byte[] request);
	ServiceResult exchangeExtended(in byte[] sessionBlob, byte protocol, in byte[] request, in byte[] extendedRequest);
	ServiceResult getStorage(in byte[] sessionBlob);
	ServiceResult close(in byte[] sessionBlob);

}