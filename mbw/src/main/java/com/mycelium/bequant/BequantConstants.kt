package com.mycelium.bequant


object BequantConstants {
    const val PARTNER_ID = "bequant"
    const val LINK_TERMS_OF_USE = "https://bequant.io/terms-of-use"
    const val LINK_SUPPORT_CENTER = "https://support.bequant.io"
    const val LINK_GOOGLE_AUTHENTICATOR = "https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2"
    const val KYC_ENDPOINT = "https://tbmv1srjdj.execute-api.us-east-1.amazonaws.com/prd-acc/"
    const val ACCOUNT_ENDPOINT = "https://fynh6mvro0.execute-api.us-east-1.amazonaws.com/prd/"
    const val AUTH_ENDPOINT = "https://xwpe71x4sg.execute-api.us-east-1.amazonaws.com/prd-reg/"
    const val VERSION_POSTFIX = "api/2/"
    const val ACCOUNT_ENDPOINT_POSTFIX = ACCOUNT_ENDPOINT + VERSION_POSTFIX
    const val ACTION_BEQUANT_KEYS = "bequant_keys"
    const val ACTION_BEQUANT_EMAIL_CONFIRMED = "bequant_email_confirmed"
    const val ACTION_BEQUANT_TOTP_CONFIRMED = "bequant_totp_confirmed"
    const val ACTION_BEQUANT_RESET_PASSWORD_CONFIRMED = "bequant_reset_password_confirmed"
    const val ACTION_BEQUANT_SHOW_REGISTER = "bequant_show_register"
    const val ACTION_COUNTRY_SELECTED = "action_country_selected"
    const val ACTION_EXCHANGE = "action_exchange"

    const val EXCHANGE_RATES= "bequant_exchange_rates"
    const val SESSION_KEY = "session"
    const val ACCESS_TOKEN_KEY = "access_token"
    const val EMAIL_KEY = "email"
    const val PHONE_KEY = "phone"
    const val KYC_UUID_KEY = "kyc_uuid"
    const val PRIVATE_KEY = "private_key"
    const val PUBLIC_KEY = "public_key"
    const val HIDE_ZERO_BALANCE_KEY = "hide_zero_balance"
    const val INTRO_KEY = "intro"
    const val COUNTRY_MODEL_KEY = "phoneModel"
    const val KYC_REQUEST_KEY ="kyc_request"
    const val KYC_STATUS_KEY ="kyc_status"
    const val KYC_STATUS_MESSAGE_KEY ="kyc_status_message"
    const val LAST_SYMBOLS_UPDATE = "last_symbols_update"

    const val REQUEST_CODE_EXCHANGE_COINS = 3001

    const val LOADER_TAG = "loader"
    const val HIDE_VALUE = "*********"

    const val TYPE_SEARCH = 0
    const val TYPE_ITEM = 2
    const val TYPE_SPACE = 1

    const val PUBLIC_REPOSITORY = "bequant_public_repository"
    val EXCLUDE_COIN_LIST = listOf("USDB", "EURB", "GBPB")
    fun changeCoinToServer(symbol: String) = if (symbol == "USDT") "USD" else symbol
}