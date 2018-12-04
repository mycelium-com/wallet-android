package com.mycelium.wapi.wallet

/**
 * This class represent current wallet loading statuses. Comments are supposed string representations in English.
 */
sealed class LoadingProgressStatus(vararg val formatArgumets: String) {
    /**
     * Starting application
     */
    class Starting : LoadingProgressStatus()
    /**
     * Migrating accounts
     */
    class Migrating : LoadingProgressStatus()

    /**
     * Loading accounts
     */
    class Loading : LoadingProgressStatus()

    /**
     * Migrating [n] account of total [m] HD accounts
     */
    class MigratingNOfMHD(n: String, m: String): LoadingProgressStatus(n, m)

    /**
     * Loading [n] account of total [m] HD accounts
     */
    class LoadingNOfMHD(n: String, m: String): LoadingProgressStatus(n, m)

    /**
     * Migrating [n] account of total [m] Single Address accounts
     */
    class MigratingNOfMSA(n: String, m: String): LoadingProgressStatus(n, m)

    /**
     * Loading [n] account of total [m] Single Address accounts
     */
    class LoadingNOfMSA(n: String, m: String): LoadingProgressStatus(n, m)
}