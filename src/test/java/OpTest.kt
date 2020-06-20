import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class OpTest {

    @Test
    fun insert_ops_should_be_equal() {
        assertEquals(Insert("test"), Insert("test"))

        assertNotEquals(Insert("Test"), Insert("test"))

        val delta = Insert("10")
        assertEquals(delta, delta)

        assertNotEquals(Insert("10"), Insert("20"))

        assertNotEquals(Insert("10"), null)
        assertNotEquals(Insert("null"), null)

        assertNotEquals(Insert("10"), Retain(10))
        assertNotEquals(Insert("10"), Delete(10))
    }

    @Test
    fun delete_ops_should_be_equal() {
        assertEquals(Delete(10), Delete(10))

        val delta = Delete(10)
        assertEquals(delta, delta)

        assertNotEquals(Delete(10), Delete(20))

        assertNotEquals(Delete(10), null)

        assertNotEquals(Delete(10), Insert("10"))
        assertNotEquals(Delete(10), Retain(10))
    }

    @Test
    fun retain_ops_should_be_equal() {
        assertEquals(Retain(10), Retain(10))

        val delta = Retain(10)
        assertEquals(delta, delta)

        assertNotEquals(Retain(10), Retain(20))

        assertNotEquals(Retain(10), null)

        assertNotEquals(Retain(10), Insert("10"))
        assertNotEquals(Retain(10), Delete(10))
    }

}