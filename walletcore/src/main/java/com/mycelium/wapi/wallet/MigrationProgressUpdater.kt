package com.mycelium.wapi.wallet

interface MigrationProgressUpdater {
    var comment: String
    var percent: Int
}