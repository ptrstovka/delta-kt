import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyMap

class DeltaUtilsTest {

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
        verify(predicate, times(2)).predicate(com.nhaarman.mockitokotlin2.any(), ArgumentMatchers.anyMap(), ArgumentMatchers.anyInt())
    }


    @Test
    internal fun it_should_filter_ops() {
        val delta = Delta()
                .insert("Hello")
                .insert("New", mapOf("url" to "image.png"))
                .insert("World!")

        val ops = delta.filter { it is Insert && it.attributes.isEmpty() }

        assertEquals(2, ops.size)
    }

    @Test
    internal fun it_should_iterate_through_ops() {
        val delta = Delta()
                .insert("Hello")
                .insert("New", mapOf("url" to "image.png"))
                .insert("World!")

        val predicate = mock<(op: Op<*>) -> Unit>()
        delta.forEach(predicate)

        verify(predicate, times(3)).invoke(any())
    }

    @Test
    internal fun it_should_map_ops() {
        val delta = Delta()
                .insert("Hello")
                .insert("New", mapOf("url" to "image.png"))
                .insert("World!")

        val values = delta.map {
            if (it is Insert && it.attributes.isEmpty()) {
                it.value
            } else {
                ""
            }
        }

        assertEquals(listOf("Hello", "", "World!"), values)
    }

    @Test
    internal fun it_should_partition_ops() {
        val delta = Delta()
                .insert("Hello")
                .insert("New", mapOf("url" to "image.png"))
                .insert("World!")

        val (passed, failed) = delta.partition { it.attributes.isEmpty() }

        assertEquals(listOf(delta.getOpAtIndex(0), delta.getOpAtIndex(2)), passed)
        assertEquals(listOf(delta.getOpAtIndex(1)), failed)
    }

    @Test
    internal fun it_should_retrieve_length_of_the_document() {
        assertEquals(
                3,
                Delta().insert("AB", mapOf("bold" to "true")).insert("1").length()
        )

        assertEquals(
                6,
                Delta().insert("AB", mapOf("bold" to "true"))
                        .insert("1")
                        .retain(2, mapOf("bold" to "false"))
                        .delete(1)
                        .length()
        )
    }

    @Test
    internal fun it_should_return_length_of_change() {
        val delta = Delta()
                .insert("AB", mapOf("bold" to "true"))
                .retain(2, mapOf("bold" to "false"))
                .delete(1)

        assertEquals(1, delta.changeLength())
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
    fun it_should_slice_in_range() {
        assertEquals(
                Delta().insert("23456"),
                Delta().insert("0123456789").slice(2..7)
        )
    }

    @Test
    fun it_should_slice_in_range_multiple_ops() {
        val slice = Delta()
                .insert("0123", mapOf("bold" to "true"))
                .insert("4567")
                .slice(3..5)

        assertEquals(
                Delta().insert("3", mapOf("bold" to "true")).insert("4"),
                slice
        )
    }

    @Test
    internal fun it_should_slice_in_the_end_multiple_ops() {
        val slice = Delta()
                .retain(2)
                .insert("A", mapOf("bold" to "true"))
                .insert("B")
                .slice(2..3)

        assertEquals(
                Delta().insert("A", mapOf("bold" to "true")),
                slice
        )
    }

    @Test
    internal fun it_should_self_when_slice_without_params() {
        val delta = Delta()
                .retain(2)
                .insert("A", mapOf("bold" to "true"))
                .insert("B")

        assertEquals(delta, delta.slice())
    }

    @Test
    internal fun it_should_split_ops_on_slice() {
        val slice = Delta()
                .insert("AB", mapOf("bold" to "true"))
                .insert("C")
                .slice(1..2)

        assertEquals(
                Delta().insert("B", mapOf("bold" to "true")),
                slice
        )
    }

    @Test
    internal fun it_should_split_ops_multiple_times_on_slice() {
        val slice = Delta()
                .insert("ABC", mapOf("bold" to "true"))
                .insert("D")
                .slice(1..2)

        assertEquals(
                Delta().insert("B", mapOf("bold" to "true")),
                slice
        )
    }
}