import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyMap

internal class DeltaTest {

    @Test
    fun delta_should_be_empty_on_after_construction() {
        assertEquals(
            0,
            Delta().size()
        )
    }

    // Insert

    @Test
    fun it_should_insert_text() {
        val delta = Delta().insert("test")

        assertEquals(1, delta.size())
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
            Delta().delete(0).size()
        )

        assertEquals(
            0,
            Delta().delete(-1).size()
        )
    }

    @Test
    fun it_should_add_delta_op_on_positive() {
        val delta = Delta().delete(1)
        assertEquals(1, delta.size())
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
            Delta().retain(0).size()
        )

        assertEquals(
            0,
            Delta().retain(-1).size()
        )
    }

    // Push

    @Test
    fun it_should_push_to_empty_delta() {
        assertEquals(
            1,
            Delta().push(Insert("text")).size()
        )
    }

    @Test
    fun it_should_merge_two_deletes() {
        val delta = Delta().delete(10)
        delta.push(Delete(10))

        assertEquals(1, delta.size())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Delete)
        assertEquals(20, op?.value)
    }

    @Test
    fun it_should_merge_two_inserts() {
        val delta = Delta().insert("a")
        delta.push(Insert("b"))

        assertEquals(1, delta.size())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Insert)
        assertEquals("ab", op?.value)
    }

    @Test
    fun it_should_merge_two_inserts_when_matching_attributes() {
        val delta = Delta().insert("a", mapOf("bold" to "true"))
        delta.push(Insert("b", mapOf("bold" to "true")))

        assertEquals(1, delta.size())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Insert)
        assertEquals("ab", op?.value)
        assertEquals(mapOf("bold" to "true"), op?.attributes)
    }

    @Test
    fun it_should_merge_two_retains_when_matching_attributes()
    {
        val delta = Delta().retain(1, mapOf("bold" to "true"))
        delta.push(Retain(2, mapOf("bold" to "true")))

        assertEquals(1, delta.size())
        val op = delta.getOpAtIndex(0)
        assertNotNull(op)
        assertTrue(op is Retain)
        assertEquals(3, op?.value)
        assertEquals(mapOf("bold" to "true"), op?.attributes)
    }

    @Test
    fun it_should_add_insert_when_attributes_does_not_match() {
        val delta = Delta().insert("a", mapOf("bold" to "true"))
        delta.push(Insert("b"))

        assertEquals(2, delta.size())
        val op1 = delta.getOpAtIndex(0)
        assertNotNull(op1)
        assertTrue(op1 is Insert)
        assertEquals("a", op1?.value)
        assertEquals(mapOf("bold" to "true"), op1?.attributes)
        val op2 = delta.getOpAtIndex(1)
        assertNotNull(op2)
        assertTrue(op2 is Insert)
        assertEquals("b", op2?.value)
        assertEquals(emptyMap<String, String>(), op2?.attributes)
    }

    @Test
    fun it_should_add_retain_when_attributes_does_not_match() {
        val delta = Delta().retain(1, mapOf("bold" to "true"))
        delta.push(Retain(2))

        assertEquals(2, delta.size())
        val op1 = delta.getOpAtIndex(0)
        assertNotNull(op1)
        assertTrue(op1 is Retain)
        assertEquals(1, op1?.value)
        assertEquals(mapOf("bold" to "true"), op1?.attributes)
        val op2 = delta.getOpAtIndex(1)
        assertNotNull(op2)
        assertTrue(op2 is Retain)
        assertEquals(2, op2?.value)
        assertEquals(emptyMap<String, String>(), op2?.attributes)
    }

    // Equal

    @Test
    fun it_should_add_retain_op_on_positive() {
        assertEquals(
            1,
            Delta().retain(1).size()
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

    // Helpers

    @Test
    fun it_should_concat_empty_delta() {
        val delta = Delta().insert("Test")
        val concat = Delta()
        assertEquals(
                Delta().insert("Test"),
                delta.concat(concat)
        )
    }

    @Test
    fun it_should_concat_without_merge() {
        val delta = Delta().insert("Test")
        val original = Delta(listOf(Insert("Test")))
        val concat = Delta().insert("!", mapOf("bold" to "true"))
        val expected = Delta().insert("Test").insert("!", mapOf("bold" to "true"))
        assertEquals(expected, delta.concat(concat))
        assertEquals(original, delta)
    }

    @Test
    fun it_should_concat_with_merge() {
        val delta = Delta().insert("Test", mapOf("bold" to "true"))
        val original = Delta(listOf(Insert("Test", mapOf("bold" to "true"))))
        val concat = Delta().insert("!", mapOf("bold" to "true")).insert("\n")
        val expected = Delta().insert("Test!", mapOf("bold" to "true")).insert("\n")
        assertEquals(expected, delta.concat(concat))
        assertEquals(original, delta)
    }

    @Test
    fun it_should_chop_retain() {
        assertEquals(
                Delta().insert("Test"),
                Delta().insert("Test").retain(4).chop()
        )
    }

    @Test
    fun it_should_chop_insert() {
        assertEquals(
                Delta().insert("Test"),
                Delta().insert("Test").chop()
        )
    }

    @Test
    fun it_should_chop_formatted_retain() {
        assertEquals(
                Delta().insert("Test").retain(4, mapOf("bold" to "true")),
                Delta().insert("Test").retain(4, mapOf("bold" to "true")).chop()
        )
    }

    @Test
    fun it_should_expect_predicate_to_be_called() {
        val delta = Delta()
                .insert("Hello\n\n")
                .insert("World", mapOf("bold" to "true"))
                .insert("abcd", mapOf("color" to "red"))
                .insert("\n", mapOf("align" to "right"))
                .insert("!")

        val predicate = mock<EachLine> {
            on { predicate(any(), anyMap(), anyInt()) } doReturn true
        }

        delta.eachLine(predicate)

        val deltaCaptor = argumentCaptor<Delta>()
        val attributeCaptor = argumentCaptor<Map<String, String>>()
        val indexCaptor = argumentCaptor<Int>()
        verify(predicate, times(4)).predicate(
                deltaCaptor.capture(), attributeCaptor.capture(), indexCaptor.capture()
        )

        assertEquals(Delta().insert("Hello"), deltaCaptor.firstValue)
        assertEquals(emptyMap<String, String>(), attributeCaptor.firstValue)
        assertEquals(0, indexCaptor.firstValue)

        assertEquals(Delta(), deltaCaptor.secondValue)
        assertEquals(emptyMap<String, String>(), attributeCaptor.secondValue)
        assertEquals(1, indexCaptor.secondValue)

        assertEquals(Delta().insert("World", mapOf("bold" to "true")).insert("abcd", mapOf("color" to "red")), deltaCaptor.thirdValue)
        assertEquals(mapOf("align" to "right"), attributeCaptor.thirdValue)
        assertEquals(2, indexCaptor.thirdValue)

        assertEquals(Delta().insert("!"), deltaCaptor.lastValue)
        assertEquals(emptyMap<String, String>(), attributeCaptor.lastValue)
        assertEquals(3, indexCaptor.lastValue)
    }

    @Test
    fun it_should_expect_predicate_to_be_called_when_trailing_newline() {
        val delta = Delta().insert("Hello\nWorld!\n")

        val predicate = mock<EachLine> {
            on { predicate(any(), anyMap(), anyInt()) } doReturn true
        }

        delta.eachLine(predicate)

        val deltaCaptor = argumentCaptor<Delta>()
        val attributeCaptor = argumentCaptor<Map<String, String>>()
        val indexCaptor = argumentCaptor<Int>()
        verify(predicate, times(2)).predicate(
                deltaCaptor.capture(), attributeCaptor.capture(), indexCaptor.capture()
        )

        assertEquals(Delta().insert("Hello"), deltaCaptor.firstValue)
        assertEquals(emptyMap<String, String>(), attributeCaptor.firstValue)
        assertEquals(0, indexCaptor.firstValue)

        assertEquals(Delta().insert("World!"), deltaCaptor.secondValue)
        assertEquals(emptyMap<String, String>(), attributeCaptor.secondValue)
        assertEquals(1, indexCaptor.secondValue)
    }

    @Test
    fun it_should_not_call_predicate_on_non_document() {
        val delta = Delta().retain(1).delete(2)

        val predicate = mock<EachLine> {
            on { predicate(any(), anyMap(), anyInt()) } doReturn true
        }

        delta.eachLine(predicate)
        verifyZeroInteractions(predicate)
    }

    @Test
    fun it_should_early_return_when_returning_false_in_predicate() {
        val delta = Delta().insert("Hello\nNew\nWorld!")

        val predicate = mock<EachLine> {
            on { predicate(any(), anyMap(), anyInt()) } doReturnConsecutively listOf(true, false)
        }

        delta.eachLine(predicate)
        verify(predicate, times(2)).predicate(any(), anyMap(), anyInt())
    }

    @Test
    fun it_should_slice_ops() {
        val slice = Delta()
            .retain(2)
            .insert("A")
            .slice(2)

        assertEquals(
            Delta().insert("A"),
            slice
        )
    }

    @Test
    fun start_and_end_chop() {
        assertEquals(
            Delta().insert("23456"),
            Delta().insert("0123456789").slice(2..7)
        )
    }

}