package com.mycelium.wallet.activity.modern.model.accounts

/**
 * Interface for items in [com.mycelium.wallet.activity.modern.adapter.AccountListAdapter] to handle them as one type.
 */
interface AccountListItem {
    fun getType(): Type

    enum class Type(val typeId: Int) {
        GROUP_TITLE_TYPE(2),
        ACCOUNT_TYPE(3),
        TOTAL_BALANCE_TYPE(4),
        GROUP_ARCHIVED_TITLE_TYPE(5),
        UNKNOWN(6),
        INVESTMENT_TYPE(7),
        ADD_ACCOUNT_TYPE(8),
        GROUP_TYPE(9);

        companion object {
            fun fromId(id: Int) : Type {
                return when (id) {
                    2 -> GROUP_TITLE_TYPE
                    3 -> ACCOUNT_TYPE
                    4 -> TOTAL_BALANCE_TYPE
                    5 -> GROUP_ARCHIVED_TITLE_TYPE
                    7 -> INVESTMENT_TYPE
                    8 -> ADD_ACCOUNT_TYPE
                    9 -> GROUP_TYPE
                    else -> UNKNOWN
                }
            }
        }
    }
}