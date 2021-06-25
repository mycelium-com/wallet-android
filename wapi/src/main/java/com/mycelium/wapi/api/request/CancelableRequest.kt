package com.mycelium.wapi.api.request


open class CancelableRequest(@Transient var cancel: (() -> Unit)? = null)
