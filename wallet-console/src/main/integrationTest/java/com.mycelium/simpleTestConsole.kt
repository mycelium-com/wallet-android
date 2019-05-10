import junit.framework.TestCase.assertEquals
import org.junit.Test

class SimpleTestConsole {
    private val a = "testConsole"
    @Test
    @Throws(Exception::class)
    fun getString_console() {
        assertEquals("testConsole", a)
    }

    @Test
    @Throws(Exception::class)
    fun addition_correct_console() {
        assertEquals(4, 2 + 2)
    }
}