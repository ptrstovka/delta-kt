import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ComposeTest {

    @Test
    internal fun it_should_compose_insert_and_insert() {
        val a = Delta().insert("A")
        val b = Delta().insert("B")
        assertEquals(
                Delta().insert("B").insert("A"),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_insert_and_retain() {
        val a = Delta().insert("A")
        val b = Delta().retain(1, mapOf("bold" to "true", "color" to "red"))
        assertEquals(
                Delta().insert("A", mapOf("bold" to "true", "color" to "red")),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_insert_and_delete() {
        val a = Delta().insert("A")
        val b = Delta().delete(1)
        assertEquals(
                Delta(),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_delete_and_insert() {
        val a = Delta().delete(1)
        val b = Delta().insert("B")
        assertEquals(
                Delta().insert("B").delete(1),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_delete_and_retain() {
        val a = Delta().delete(1)
        val b = Delta().retain(1, mapOf("bold" to "true", "color" to "red"))
        assertEquals(
                Delta().delete(1).retain(1, mapOf("bold" to "true", "color" to "red")),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_delete_and_delete() {
        val a = Delta().delete(1)
        val b = Delta().delete(1)
        assertEquals(
                Delta().delete(2),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_retain_and_insert() {
        val a = Delta().retain(1, mapOf("color" to "blue"))
        val b = Delta().insert("B")
        assertEquals(
                Delta().insert("B").retain(1, mapOf("color" to "blue")),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_retain_and_retain() {
        val a = Delta().retain(1, mapOf("color" to "blue"))
        val b = Delta().retain(1, mapOf("bold" to "true", "color" to "red", "font" to "null"))
        assertEquals(
                Delta().retain(1, mapOf("bold" to "true", "color" to "red", "font" to "null")),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_compose_retain_and_delete() {
        val a = Delta().retain(1, mapOf("color" to "blue"))
        val b = Delta().delete(1)
        assertEquals(
                Delta().delete(1),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_insert_text_in_the_middle_of_text() {
        val a = Delta().insert("Hello")
        val b = Delta().retain(3).insert("X")
        assertEquals(
                Delta().insert("HelXlo"),
                a.compose(b)
        )
    }

    @Test
    internal fun it_should_insert_and_delete_ordering() {
        val a = Delta().insert("Hello")
        val b = Delta().insert("Hello")
        val insertFirst = Delta()
                .retain(3)
                .insert("X")
                .delete(1)
        val deleteFirst = Delta()
                .retain(3)
                .delete(1)
                .insert("X")
        val expected = Delta().insert("HelXo")
        assertEquals(expected, a.compose(insertFirst))
        assertEquals(expected, b.compose(deleteFirst))
    }

    // TODO: Test insert embed

    @Test
    internal fun it_should_delete_entire_text() {
        val a = Delta().retain(4).insert("Hello")
        val b = Delta().delete(9)
        assertEquals(Delta().delete(4), a.compose(b))
    }

    @Test
    internal fun it_should_retain_more_than_length_of_text() {
        val a = Delta().insert("Hello")
        val b = Delta().retain(10)
        assertEquals(Delta().insert("Hello"), a.compose(b))
    }

    // TODO: Test retain empty embed

    // TODO: Test remove all attributes

    // TODO: Test remove all embed attributes

    @Test
    internal fun it_should_be_immutable() {
        val attr1 = mapOf("bold" to "true")
        val attr2 = mapOf("bold" to "true")
        val a1 = Delta().insert("Test", attr1)
        val a2 = Delta().insert("Test", attr1)
        val b1 = Delta().retain(1, mapOf("color" to "red")).delete(2)
        val b2 = Delta().retain(1, mapOf("color" to "red")).delete(2)
        val expected = Delta()
                .insert("T", mapOf("color" to "red", "bold" to "true"))
                .insert("t", attr1)
        assertEquals(expected, a1.compose(b1))
        assertEquals(a2, a1)
        assertEquals(b2, b1)
        assertEquals(attr2, attr1)
    }

    @Test
    internal fun it_should_retain_start_optimization() {
        val a = Delta()
                .insert("A", mapOf("bold" to "true"))
                .insert("B")
                .insert("C", mapOf("bold" to "true"))
                .delete(1)
        val b = Delta().retain(3).insert("D")
        val expected = Delta()
                .insert("A", mapOf("bold" to "true"))
                .insert("B")
                .insert("C", mapOf("bold" to "true"))
                .insert("D")
                .delete(1)
        assertEquals(expected, a.compose(b))
    }

    @Test
    internal fun it_should_retain_start_optimization_split() {
        val a = Delta()
                .insert("A", mapOf("bold" to "true"))
                .insert("B")
                .insert("C", mapOf("bold" to "true"))
                .retain(5)
                .delete(1)
        val b = Delta().retain(4).insert("D")
        val expected = Delta()
                .insert("A", mapOf("bold" to "true"))
                .insert("B")
                .insert("C", mapOf("bold" to "true"))
                .retain(1)
                .insert("D")
                .retain(4)
                .delete(1)
        assertEquals(expected, a.compose(b))
    }

    @Test
    internal fun it_should_retain_end_optimization() {
        val a = Delta()
                .insert("A", mapOf("bold" to "true"))
                .insert("B")
                .insert("C", mapOf("bold" to "true"))
        val b = Delta().delete(1)
        val expected = Delta().insert("B").insert("C", mapOf("bold" to "true"))
        assertEquals(expected, a.compose(b))
    }

    @Test
    internal fun it_should_retain_end_optimization_join() {
        val a = Delta()
                .insert("A", mapOf("bold" to "true"))
                .insert("B")
                .insert("C", mapOf("bold" to "true"))
                .insert("D")
                .insert("E", mapOf("bold" to "true"))
                .insert("F")
        val b = Delta().retain(1).delete(1)
        val expected = Delta()
                .insert("AC", mapOf("bold" to "true"))
                .insert("D")
                .insert("E", mapOf("bold" to "true"))
                .insert("F")
        assertEquals(expected, a.compose(b))
    }
}