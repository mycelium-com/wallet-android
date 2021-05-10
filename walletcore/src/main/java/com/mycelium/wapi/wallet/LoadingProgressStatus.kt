package com.mycelium.wapi.wallet

/**
 * This class represent current wallet loading statuses. Comments are supposed string representations in English.
 */
class LoadingProgressStatus(val state: State, val done: Int? = null, val total: Int? = null) {
    enum class State {
        STARTING, MIGRATING, LOADING, MIGRATING_N_OF_M_HD, LOADING_N_OF_M_HD, MIGRATING_N_OF_M_SA, LOADING_N_OF_M_SA
    }
}