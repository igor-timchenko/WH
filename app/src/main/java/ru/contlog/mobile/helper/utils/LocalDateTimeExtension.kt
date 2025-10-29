package ru.contlog.mobile.helper.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char

val DDMMYYYYFormatter = LocalDateTime.Format {
    day(); char('.'); monthNumber(); char('.'); year()
}

val LocalDateTime.asDDMMYYYY: String
    get() {
        return this.format(DDMMYYYYFormatter)
    }