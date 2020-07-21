package com.mycelium.bequant.common

import android.app.AlertDialog
import android.content.Context
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mycelium.bequant.remote.trading.model.Error


class ErrorHandler(val context: Context) {
    fun handle(errorPayload: String) {
        val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerKotlinModule()
        val error = mapper.readValue(errorPayload, Error::class.java).error!!
        AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("${error.message}. ${error.description}")
                .create()
                .show()
    }
}