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

package com.mycelium.wapi.wallet.single

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.SingleAddressAccountBacking
import java.util.*

/**
 * The abstract context of an account
 */
class SingleAddressAccountContext constructor(
        val id: UUID,
        var addresses: Map<AddressType, Address>,
        private var isArchived: Boolean,
        private var blockHeight: Int,
        defaultAddressType: AddressType
) {
    private var isDirty = false
    var defaultAddressType = defaultAddressType
        set(value) {
            field = value
            isDirty = true
        }

    constructor(context: SingleAddressAccountContext) :
            this(context.id, context.addresses, context.isArchived(), context.getBlockHeight(), context.defaultAddressType)

    /**
     * Is this account archived?
     */
    fun isArchived() = isArchived

    /**
     * Mark this account as archived
     */
    fun setArchived(isArchived: Boolean) {
        if (this.isArchived != isArchived) {
            isDirty = true
            this.isArchived = isArchived
        }
    }

    /**
     * Get the block chain height recorded for this context
     */
    fun getBlockHeight() = blockHeight

    /**
     * Set the block chain height for this context
     */
    fun setBlockHeight(blockHeight: Int) {
        if (this.blockHeight != blockHeight) {
            isDirty = true
            this.blockHeight = blockHeight
        }
    }

    /**
     * Persist this context if it is marked as dirty
     */
    fun persistIfNecessary(backing: SingleAddressAccountBacking) {
        if (isDirty) {
            persist(backing)
        }
    }

    /**
     * Persist this context
     */
    fun persist(backing: SingleAddressAccountBacking) {
        backing.updateAccountContext(this)
        isDirty = false
    }
}
