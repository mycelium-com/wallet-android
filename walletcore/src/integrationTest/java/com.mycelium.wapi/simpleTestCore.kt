import junit.framework.TestCase.assertEquals
import org.junit.Test

class SimpleTestCore {
    private val a = "test"
    @Test
    @Throws(Exception::class)
    fun getString() {
        assertEquals("test", a)
    }

    @Test
    @Throws(Exception::class)
    fun addition_correct() {
        assertEquals(4, 2 + 2)
    }
}