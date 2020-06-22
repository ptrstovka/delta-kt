import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DiffTest {

    @Test
    internal fun it_should_diff_insert() {
        val a = Delta().insert("A")
        val b = Delta().insert("AB")
        val expected = Delta().retain(1).insert("B")
        assertEquals(expected, a.diff(b))
    }

    @Test
    internal fun it_should_diff_delete() {
        val a = Delta().insert("AB")
        val b = Delta().insert("A")
        val expected = Delta().retain(1).delete(1)
        assertEquals(expected, a.diff(b))
    }

    @Test
    internal fun it_should_diff_retain() {
        val a = Delta().insert("A")
        val b = Delta().insert("A")
        val expected = Delta()
        assertEquals(expected, a.diff(b))
    }

    @Test
    internal fun it_should_diff_format() {
        val a = Delta().insert("A")
        val b = Delta().insert("A", mapOf("bold" to "true"))
        val expected = Delta().retain(1, mapOf("bold" to "true"))
        assertEquals(expected, a.diff(b))
    }

    // TODO: Diff object attributes when inserting value object

    // TODO: Diff embed integer match

    // TODO: Diff embed integer mismatch

    // TODO: Diff embed object match

    // TODO: Diff embed object mismatch

    // TODO: Diff embed object change

    // TODO: Diff embed false positive

    @Test
    internal fun it_should_throw_error_on_non_documents() {
        val a = Delta().insert("A")
        val b = Delta().retain(1).insert("B")

        assertThrows<Error> {
            a.diff(b)
        }

        assertThrows<Error> {
            b.diff(a)
        }
    }

    // TODO: It works probably only on embed
//    @Test
//    internal fun it_should_diff_inconvenient_indexes() {
//        val a = Delta()
//                .insert("12", mapOf("bold" to "true"))
//                .insert("34", mapOf("italic" to "true"))
//        val b = Delta()
//                .insert("123", mapOf("color" to "red"))
//        val expected = Delta()
//                .retain(2, mapOf("bold" to "true"))
//                .retain(1, mapOf("italic" to "true"))
//                .delete(1)
//        assertEquals(expected, a.diff(b))
//    }

    @Test
    internal fun it_should_test_combination() {
        val a = Delta()
                .insert("Bad", mapOf("color" to "red"))
                .insert("cat", mapOf("color" to "blue"))
        val b = Delta()
                .insert("Good", mapOf("bold" to "true"))
                .insert("dog", mapOf("italic" to "true"))
        val expected = Delta()
                .insert("Good", mapOf("bold" to "true"))
                .delete(2)
                .retain(1, mapOf("italic" to "true"))
                .delete(3)
                .insert("og", mapOf("italic" to "true"))
        assertEquals(expected, a.diff(b))
    }

    @Test
    internal fun it_should_diff_same_document() {
        val a = Delta().insert("A").insert("B", mapOf("bold" to "true"))
        val expected = Delta()
        assertEquals(expected, a.diff(a))
    }

    @Test
    internal fun it_should_check_for_immutability() {
        val attr1 = mapOf("color" to "red")
        val attr2 = mapOf("color" to "red")
        val a1 = Delta().insert("A", attr1)
        val a2 = Delta().insert("A", attr1)
        val b1 = Delta().insert("A", mapOf("bold" to "true")).insert("B")
        val b2 = Delta().insert("A", mapOf("bold" to "true")).insert("B")
        val expected = Delta()
                .retain(1, mapOf("bold" to "true"))
                .insert("B")
        assertEquals(expected, a1.diff(b1))
        assertEquals(a2, a1)
        assertEquals(b2, b2)
        assertEquals(attr2, attr1)
    }

    @Test
    internal fun it_should_test_diff_on_non_document() {
        val a = Delta().insert("Test")
        val b = Delta().delete(4)
        assertThrows<Error> {
            a.diff(b)
        }
    }
}