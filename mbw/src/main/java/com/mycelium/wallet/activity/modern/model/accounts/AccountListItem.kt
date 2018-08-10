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
        UKNOWN(6);

        companion object {
            fun fromId(id: Int) : Type {
                return when (id) {
                    2 -> GROUP_TITLE_TYPE
                    3 -> ACCOUNT_TYPE
                    4 -> TOTAL_BALANCE_TYPE
                    5 -> GROUP_ARCHIVED_TITLE_TYPE
                    else -> UKNOWN
                }
            }
        }
    }
}