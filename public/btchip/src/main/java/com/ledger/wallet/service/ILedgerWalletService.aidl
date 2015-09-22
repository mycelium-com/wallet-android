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

import com.ledger.wallet.service.ServiceResult;

interface ILedgerWalletService {

	ServiceResult getServiceVersion();
	ServiceResult getServiceFeatures();

	ServiceResult getPersonalization();

	ServiceResult openDefault();
	ServiceResult open(int spid, in byte[] pTAData, int TAsize);
	ServiceResult initStorage(in byte[] sessionBlob, in byte[] storage);
	ServiceResult exchange(in byte[] sessionBlob, in byte[] request);
	ServiceResult exchangeExtended(in byte[] sessionBlob, byte protocol, in byte[] request, in byte[] extendedRequest);
	ServiceResult exchangeExtendedUI(in byte[] sessionBlob, in byte[] request); 
	ServiceResult getStorage(in byte[] sessionBlob);
	ServiceResult close(in byte[] sessionBlob);

}