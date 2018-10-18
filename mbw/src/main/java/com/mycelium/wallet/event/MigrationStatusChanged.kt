package com.mycelium.wallet.event

import com.mycelium.wapi.wallet.LoadingProgressStatus

class MigrationStatusChanged(val newStatus: LoadingProgressStatus)