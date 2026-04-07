package com.mycelium.common

actual fun platform(): String {
    return "Desktop: ${System.getProperty("os.name")} ${System.getProperty("os.version")}"
}
