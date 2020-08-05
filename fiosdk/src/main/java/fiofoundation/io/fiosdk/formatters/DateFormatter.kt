package fiofoundation.io.fiosdk.formatters

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class DateFormatter {

    companion object {

        const val BACKEND_DATE_PATTERN = "yyyy-MM-dd'T'kk:mm:ss.SSS"

        const val BACKEND_DATE_PATTERN_WITH_TIMEZONE = "yyyy-MM-dd'T'kk:mm:ss.SSS zzz"

        const val BACKEND_DATE_TIME_ZONE = "UTC"

        @Throws(ParseException::class)
        fun convertBackendTimeToMilli(backendTime: String): Long {
            val datePatterns = arrayOf(BACKEND_DATE_PATTERN, BACKEND_DATE_PATTERN_WITH_TIMEZONE)

            for (datePattern in datePatterns) {
                try {
                    val sdf = SimpleDateFormat(datePattern)
                    sdf.timeZone = TimeZone.getTimeZone(BACKEND_DATE_TIME_ZONE)
                    val parsedDate = sdf.parse(backendTime)
                    return parsedDate.time
                } catch (ex: ParseException) {
                    // Keep going even if exception is thrown for trying different date pattern
                } catch (ex: IllegalArgumentException) {
                    // Keep going even if exception is thrown for trying different date pattern
                }

            }

            throw ParseException("Unable to parse input backend time with supported date patterns!", 0)
        }

        fun convertMilliSecondToBackendTimeString(timeInMilliSeconds: Long): String {
            val sdf = SimpleDateFormat(BACKEND_DATE_PATTERN)
            sdf.timeZone = TimeZone.getTimeZone(BACKEND_DATE_TIME_ZONE)

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeInMilliSeconds

            return sdf.format(calendar.time)
        }

    }
}