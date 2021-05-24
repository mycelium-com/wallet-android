package com.mycelium.wapi.api.request


open class CancelableRequest(var cancel: (() -> Unit)? = null)