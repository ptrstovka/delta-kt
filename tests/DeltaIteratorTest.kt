import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DeltaIteratorTest {

    lateinit var delta: Delta

    @BeforeEach
    internal fun setUp() {
        delta = Delta()
            .insert("Hello", mapOf("bold" to "true"))
            .retain(3)
            .insert("World!", mapOf("color" to "red"))
            .delete(4)
    }

    @Test
    fun it_has_next_true() {
        assertTrue(
            DeltaIterator(delta.getOpsList()).hasNext()
        )
    }

    @Test
    fun it_has_next_false() {
        assertFalse(
            DeltaIterator(emptyList()).hasNext()
        )
    }

    @Test
    fun it_should_return_peek_length() {
        val iterator = DeltaIterator(delta.getOpsList())
        assertEquals(5, iterator.peekLength())
        iterator.next()
        assertEquals(3, iterator.peekLength())
        iterator.next()
        assertEquals(6, iterator.peekLength())
        iterator.next()
        assertEquals(4, iterator.peekLength())
    }

    @Test
    fun it_should_return_peek_length_after_offset() {
        val iterator = DeltaIterator(delta.getOpsList())
        iterator.next(2)
        assertEquals(5 - 2, iterator.peekLength())
    }

    @Test
    fun it_should_left_with_no_ops() {
        val iterator = DeltaIterator(emptyList())
        assertEquals(
            Int.MAX_VALUE,
            iterator.peekLength()
        )
    }

    @Test
    fun it_should_return_peek_type() {
        val iterator = DeltaIterator(delta.getOpsList())
        assertEquals("insert", iterator.peekType())
        iterator.next()
        assertEquals("retain", iterator.peekType())
        iterator.next()
        assertEquals("insert", iterator.peekType())
        iterator.next()
        assertEquals("delete", iterator.peekType())
        iterator.next()
        assertEquals("retain", iterator.peekType())
    }

    @Test
    fun it_should_return_next_item() {
        val iterator = DeltaIterator(delta.getOpsList())
        for (i in 0 until delta.size()) {
            assertEquals(iterator.next(), delta.getOpAtIndex(i))
        }
        assertEquals(iterator.next(), Retain(Int.MAX_VALUE))
        assertEquals(iterator.next(4), Retain(Int.MAX_VALUE))
        assertEquals(iterator.next(), Retain(Int.MAX_VALUE))
    }

    @Test
    fun it_should_return_next_items_with_offset() {
        val iterator = DeltaIterator(delta.getOpsList())
        assertEquals(
            iterator.next(2),
            Insert("He", mapOf("bold" to "true"))
        )
        assertEquals(
            iterator.next(10),
            Insert("llo", mapOf("bold" to "true"))
        )
        assertEquals(
            iterator.next(1),
            Retain(1)
        )
        assertEquals(
            iterator.next(2),
            Retain(2)
        )
    }

    @Test
    fun it_should_return_rest_of_list() {
        val iterator = DeltaIterator(this.delta.getOpsList())
        iterator.next(2)
        assertEquals(listOf(
            Insert("llo", mapOf("bold" to "true")),
            Retain(3),
            Insert("World!", mapOf("color" to "red")),
            Delete(4)
        ), iterator.rest())
        iterator.next(3)
        assertEquals(listOf(
            Retain(3),
            Insert("World!", mapOf("color" to "red")),
            Delete(4)
        ), iterator.rest())
        iterator.next(3)
        iterator.next(6)
        iterator.next(4)
        assertEquals(emptyList<Op<*>>(), iterator.rest())
    }

}