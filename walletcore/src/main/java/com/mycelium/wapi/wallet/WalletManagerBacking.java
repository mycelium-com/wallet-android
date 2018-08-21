/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.wallet;

import com.mycelium.wapi.api.lib.FeeEstimation;
import com.mycelium.wapi.wallet.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.single.SingleAddressAccountContext;

import java.util.List;
import java.util.UUID;

public interface WalletManagerBacking extends SecureKeyValueStoreBacking {
    void beginTransaction();

    void setTransactionSuccessful();

    void endTransaction();

    void createBip44AccountContext(HDAccountContext context);

    void upgradeBip44AccountContext(HDAccountContext context);

    List<HDAccountContext> loadBip44AccountContexts();

    Bip44AccountBacking getBip44AccountBacking(UUID accountId);

    void deleteBip44AccountContext(UUID accountId);

    void createSingleAddressAccountContext(SingleAddressAccountContext context);

    List<SingleAddressAccountContext> loadSingleAddressAccountContexts();

    SingleAddressAccountBacking getSingleAddressAccountBacking(UUID accountId);

    void deleteSingleAddressAccountContext(UUID accountId);
    void saveLastFeeEstimation(FeeEstimation feeEstimation);

    FeeEstimation loadLastFeeEstimation();
}
