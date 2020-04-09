package com.mycelium.bequant.common


fun String.passwordLevel(): Int {
    var score = 0
    var upper = false
    var lower = false
    var digit = false
    var specialChar = false
    if (length in 1..5) {
        return 1
    }
    for (i in indices) {
        val c: Char = this[i]
        if (!specialChar && !Character.isLetterOrDigit(c)) {
            score++
            specialChar = true
        } else if (!digit && Character.isDigit(c)) {
            score++
            digit = true
        } else if (!upper && Character.isUpperCase(c)) {
            score++
            upper = true
        } else if (!lower && Character.isLowerCase(c)) {
            score++
            lower = true
        }
    }
    return score
}