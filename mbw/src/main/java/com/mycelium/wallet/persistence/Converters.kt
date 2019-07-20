package com.mycelium.wallet.persistence

import androidx.room.TypeConverter
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.COINS
import java.util.*

class Converters {
    @TypeConverter
    fun uuidFromString(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun uuidToString(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun cryptoCurrencyFromString(value: String?): CryptoCurrency? = value?.let(COINS::get)

    @TypeConverter
    fun cryptoCurrencyToString(value: CryptoCurrency?): String? = value?.id

    @TypeConverter
    fun balanceFromString(value: String?): Balance? = ObjectMapper().readValue(value, Balance::class.java)

    @TypeConverter
    fun balanceToString(value: Balance?): String? = ObjectMapper().writeValueAsString(value)
}