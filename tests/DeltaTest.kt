import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class DeltaTest {

    // Insert

    @Test
    fun it_should_insert_text() {
        val delta = Delta().insert("test")
        assertEquals(1, delta.length())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Insert)
        assertEquals("test", op?.value)
    }

    @Test
    fun it_should_insert_text_before_delete() {
        assertEquals(
            Delta().delete(1).insert("a"),
            Delta().insert("a").delete(1)
        )
    }

    @Test
    fun it_should_insert_text_after_delete_with_merge() {
        assertEquals(
            Delta().insert("a").delete(1).insert("b"),
            Delta().insert("ab").delete(1)
        )
    }

    @Test
    fun it_should_insert_text_after_delete_without_merge() {
        assertEquals(
            Delta().insert("a").delete(1).insert("b"),
            Delta().insert("a").insert("b").delete(1)
        )
    }

    // Delete

    @Test
    fun it_should_not_add_delete_op_on_zero_or_negative() {
        assertEquals(
            0,
            Delta().delete(0).length()
        )

        assertEquals(
            0,
            Delta().delete(-1).length()
        )
    }

    @Test
    fun it_should_add_delta_op_on_positive() {
        val delta = Delta().delete(1)
        assertEquals(1, delta.length())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Delete)
        assertEquals(1, op?.value)
    }

    // Retain

    @Test
    fun it_should_not_add_retain_op_on_zero_or_negative() {
        assertEquals(
            0,
            Delta().retain(0).length()
        )

        assertEquals(
            0,
            Delta().retain(-1).length()
        )
    }

    // Push

    @Test
    fun it_should_push_to_empty_delta() {
        assertEquals(
            1,
            Delta().push(Insert("text")).length()
        )
    }

    @Test
    fun it_should_merge_two_deletes() {
        val delta = Delta().delete(10)
        delta.push(Delete(10))

        assertEquals(1, delta.length())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Delete)
        assertEquals(20, op?.value)
    }

    @Test
    fun it_should_merge_two_inserts() {
        val delta = Delta().insert("a")
        delta.push(Insert("b"))

        assertEquals(1, delta.length())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Insert)
        assertEquals("ab", op?.value)
    }

    @Test
    fun it_should_merge_two_inserts_when_matching_attributes() {
        val delta = Delta().insert("a", mapOf("bold" to "true"))
        delta.push(Insert("b", mapOf("bold" to "true")))

        assertEquals(1, delta.length())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Insert)
        assertEquals("ab", op?.value)
        assertEquals(mapOf("bold" to "true"), op?.attributes)
    }

    // Equal

    @Test
    fun it_should_add_retain_op_on_positive() {
        assertEquals(
            1,
            Delta().retain(1).length()
        )
    }

    @Test
    fun deltas_should_be_equal() {
        assertEquals(
            Delta().insert("1").insert("2").insert("3"),
            Delta().insert("1").insert("2").insert("3")
        )

        assertNotEquals(
            Delta().insert("1").insert("2").insert("3"),
            Delta().insert("10").insert("20").insert("30")
        )
    }

}