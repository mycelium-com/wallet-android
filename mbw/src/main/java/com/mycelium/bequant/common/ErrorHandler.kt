package com.mycelium.bequant.common

import android.content.Context
import android.widget.Toast
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule


class ErrorHandler(val context: Context) {
    // we have two types of errors: related to the authorization and to the trading
    // try to parse payload as trading error first. it is distinguished by the presence of 'error' field.
    // else read as authorization error
    fun handle(errorPayload: String) {
        val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerKotlinModule()
        try {
            val error = mapper.readValue(errorPayload, com.mycelium.bequant.remote.trading.model.Error::class.java).error
            if (error != null) {
                Toast.makeText(context, "${error.message}. ${error.description}", Toast.LENGTH_LONG).show()
            } else {
                mapper.readValue(errorPayload, com.mycelium.bequant.remote.client.models.Error::class.java).apply {
                    Toast.makeText(context, this.message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (ignore: Exception) {
            Toast.makeText(context, errorPayload, Toast.LENGTH_LONG).show()
        }
    }
}