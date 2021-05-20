package com.mycelium.wapi.api.request


open class CancelableRequest constructor(@Transient var cancel: (() -> Unit)? = null)
