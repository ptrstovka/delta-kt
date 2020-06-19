abstract class Op<T>(
    val value: T,
    val attributes: Map<String, String>
) {
    abstract val type: String

    abstract fun length(): Int

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Op<*>) return false

        if (value != other.value) return false
        if (attributes != other.attributes) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + attributes.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

}

class Insert(value: String, attributes: Map<String, String> = mapOf()): Op<String>(value, attributes) {
    override val type = "insert"

    override fun length(): Int {
        return value.length
    }
}

class Delete(value: Int, attributes: Map<String, String> = mapOf()): Op<Int>(value, attributes) {
    override val type = "delete"

    override fun length(): Int {
        return value
    }
}


class Retain(value: Int, attributes: Map<String, String> = mapOf()): Op<Int>(value, attributes) {
    override val type = "retain"

    override fun length(): Int {
        return value
    }
}

fun Op<*>.isDelete(): Boolean {
    return this is Delete
}

fun Op<*>.isInsert(): Boolean {
    return this is Insert
}

fun Op<*>.isRetain(): Boolean {
    return this is Retain
}