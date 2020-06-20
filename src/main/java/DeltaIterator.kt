import java.util.*

class DeltaIterator(
    private val ops: List<Op<*>>
): Iterator<Op<*>> {

    private var index = 0
    private var offset = 0

    override fun hasNext(): Boolean {
        return peekLength() < Int.MAX_VALUE
    }

    override fun next(): Op<*> {
        return next(Int.MAX_VALUE)
    }

    fun next(opsLength: Int): Op<*> {
        var length = opsLength
        val nextOp = ops.getOrNull(index)

        if (nextOp != null) {
            val offset = this.offset
            val opLength = nextOp.length()
            if (length >= opLength - offset) {
                length = opLength - offset
                this.index++
                this.offset = 0
            } else {
                this.offset += length
            }

            return if (nextOp is Delete) {
                Delete(length)
            } else {
                if (nextOp is Retain) {
                    Retain(length, nextOp.attributes)
                } else if (nextOp is Insert) {
                    Insert(nextOp.value.substring(offset, offset + length), nextOp.attributes)
                } else {
                    // offset should === 0, length should === 1
                    // TODO: Not sure what this is doing.
                    Insert(nextOp.value as String, nextOp.attributes)
                }
            }
        } else {
            return Retain(Int.MAX_VALUE)
        }
    }

    fun peekLength(): Int {
        val op = ops.getOrNull(index)
        if (op != null) {
            return op.length() - offset
        }

        return Int.MAX_VALUE
    }

    fun peek(): Op<*>? {
        return ops.getOrNull(index)
    }

    fun peekType(): String {
        val op = ops.getOrNull(index)
        if (op != null) {
            return op.type
        }

        return "retain"
    }

    fun rest(): List<Op<*>> {
        return if (! hasNext()) {
            emptyList()
        } else if (offset == 0) {
            ops.slice(index until ops.size)
        } else {
            val offset = this.offset
            val index = this.index
            val next = this.next()
            val rest = ops.slice(this.index until ops.size)
            this.offset = offset
            this.index = index
            listOf(next) + rest
        }
    }

}