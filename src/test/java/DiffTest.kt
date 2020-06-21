import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiffTest {

    @Test
    internal fun it_should_diff_insert() {
        val a = Delta().insert("A")
        val b = Delta().insert("AB")
        val expected = Delta().retain(1).insert("B")
        assertEquals(expected, a.diff(b))
    }
}