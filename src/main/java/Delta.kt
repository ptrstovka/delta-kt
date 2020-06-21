import name.fraser.neil.plaintext.diff_match_patch
import kotlin.math.min

class Delta {

    private val ops = mutableListOf<Op<*>>()

    constructor()

    constructor(ops: List<Op<*>>) {
        ops.forEach { this.ops.add(it) }
    }

    /**
     * Add a Insert operation.
     */
    fun insert(value: String, attributes: Map<String, String> = mutableMapOf()): Delta {
        if (value.isEmpty()) {
            return this
        }

        push(
            Insert(value, attributes)
        )

        return this
    }

    /**
     * Add a Delete operation.
     */
    fun delete(length: Int, attributes: Map<String, String> = mutableMapOf()): Delta {
        if (length <= 0) {
            return this
        }

        push(
            Delete(length, attributes)
        )

        return this
    }

    /**
     * Add a retain operation.
     */
    fun retain(length: Int, attributes: Map<String, String> = mutableMapOf()): Delta {
        if (length <= 0) {
            return this
        }

        push(
            Retain(length, attributes)
        )

        return this
    }

    /**
     * Push an Op to the delta ops.
     */
    fun push(newOp: Op<*>): Delta {
        var index = ops.size
        var lastOp = ops.lastOrNull()

        // If last operation is delete and new operation is also
        // a delete, we will merge them to only one delete operation.
        if (newOp is Delete && lastOp is Delete) {
            ops[index - 1] = Delete(lastOp.value + newOp.value)

            return this
        }

        // Since it does not matter if we insert before or after
        // deleting at the same index, always prefer to insert first.
        if (lastOp is Delete && newOp is Insert) {
            index -= 1
            lastOp = ops.getOrNull(index - 1)
            if (lastOp == null) {
                ops.add(0, newOp)
                return this
            }
        }

        // If Ops have same attributes
        if (lastOp != null && newOp.attributes == lastOp.attributes) {
            // TODO: Add merge logic to Op class
            // If there are two insersts, we will merge them to one.
            if (newOp is Insert && lastOp is Insert) {
                ops[index - 1] = Insert("${lastOp.value}${newOp.value}", newOp.attributes)

                return this
            // If the Ops are two retains, we will also merge them to one.
            } else if (newOp is Retain && lastOp is Retain) {
                ops[index - 1] = Retain(lastOp.value + newOp.value, newOp.attributes)

                return this
            }
        }

        if (index == ops.size) {
            ops.add(newOp)
        } else {
            ops.add(index, newOp)
        }

        return this
    }

    /**
     * Remove last Op if it is Retain.
     */
    fun chop(): Delta {
        val lastOp = ops.lastOrNull()

        if (lastOp != null && lastOp is Retain && lastOp.attributes.isEmpty()) {
            ops.removeAt(ops.size - 1)
        }

        return this
    }

    /**
     * Return the length of the Insert after change by Delete.
     */
    fun changeLength(): Int {
        return reduce(0) { length, op ->
            when (op) {
                is Insert -> length + op.length()
                is Delete -> length - op.length()
                else -> length
            }
        }
    }

    /**
     * Retrieve sum of the Op's sizes.
     */
    fun length(): Int {
        return reduce(0) { length, op ->
            length + op.length()
        }
    }

    fun <T> reduce(initialValue: T, operation: (accum: T, value: Op<*>) -> T): T {
        return ops.fold(initialValue, operation)
    }

    fun <T> reduceIndexed(initialValue: T, operation: (accum: T, value: Op<*>, index: Int) -> T): T {
        return ops.foldIndexed(initialValue) { index: Int, acc: T, op: Op<*> ->
            operation(acc, op, index)
        }
    }

    fun filter(predicate: (op: Op<*>) -> Boolean): List<Op<*>> {
        return ops.filter(predicate)
    }

    fun filterIndexed(predicate: (index: Int, op: Op<*>) -> Boolean): List<Op<*>> {
        return ops.filterIndexed(predicate)
    }

    fun forEach(action: (op: Op<*>) -> Unit) {
        ops.forEach(action)
    }

    fun forEachIndexed(action: (index: Int, op: Op<*>) -> Unit) {
        ops.forEachIndexed(action)
    }

    fun <T> map(transform: (op: Op<*>) -> T): List<T> {
        return ops.map(transform)
    }

    fun <T> mapIndexed(transform: (index: Int, op: Op<*>) -> T): List<T> {
        return ops.mapIndexed(transform)
    }

    fun partition(predicate: (op: Op<*>) -> Boolean): Pair<List<Op<*>>, List<Op<*>>> {
        return ops.partition(predicate)
    }

    fun slice(): Delta {
        return slice(0)
    }

    fun slice(start: Int, end: Int = Int.MAX_VALUE): Delta {
        return slice(start until end)
    }

    fun slice(range: IntRange): Delta {
        val ops = mutableListOf<Op<*>>()

        var index = 0
        val iterator = iterator()
        while (index < range.last && iterator.hasNext()) {
            var nextOp: Op<*>
            if (index < range.first) {
                nextOp = iterator.next(range.first - index)
            } else {
                nextOp = iterator.next(range.last - index)
                ops.add(nextOp)
            }
            index += nextOp.length()
        }

        return Delta(ops)
    }

    fun concat(other: Delta): Delta {
        val delta = Delta(ops.slice(0 until ops.size))
        val op = other.getOpAtIndex(0)
        if (op != null) {
            delta.push(op)
            if (other.ops.size > 1) {
                delta.setOps(
                        delta.ops + other.ops.slice(1 until other.ops.size)
                )
            }
        }
        return delta
    }

    fun eachLine(predicate: EachLine) {
        eachLine(predicate::predicate)
    }

    fun eachLine(
            predicate: (line: Delta, attributes: Map<String, String>, index: Int) -> Boolean,
            newLine: String = "\n"
    ) {
        val iterator = iterator()
        var line = Delta()
        var i = 0
        while (iterator.hasNext()) {
            if (iterator.peekType() != "insert") {
                return
            }

            val thisOp = iterator.peek()!!
            val start = thisOp.length() - iterator.peekLength()
            val index = if (thisOp is Insert) {
                thisOp.value.indexOf(newLine, start) - start
            } else {
                -1
            }

            if (index < 0) {
                line.push(iterator.next())
            } else if (index > 0) {
                line.push(iterator.next(index))
            } else {
                if (! predicate(line, iterator.next(1).attributes, i)) {
                    return
                }
                i++
                line = Delta()
            }
        }
        if (line.length() > 0) {
            predicate(line, mapOf(), i)
        }
    }

    fun compose(other: Delta): Delta {
        val thisIter = iterator()
        val otherIter = other.iterator()
        val ops = mutableListOf<Op<*>>()
        val firstOther = otherIter.peek()
        if (firstOther != null && firstOther is Retain && firstOther.attributes.isEmpty()) {
            var firstLeft = firstOther.value
            while (
                thisIter.peekType() == "insert" &&
                        thisIter.peekLength() <= firstLeft
            ) {
                firstLeft -= thisIter.peekLength()
                ops.add(thisIter.next())
            }
            if (firstOther.value - firstLeft > 0) {
                otherIter.next(firstOther.value - firstLeft)
            }
        }
        val delta = Delta(ops)
        while (thisIter.hasNext() || otherIter.hasNext()) {
            if (otherIter.peekType() == "insert") {
                delta.push(otherIter.next())
            } else if (thisIter.peekType() == "delete") {
                delta.push(thisIter.next())
            } else {
                val length = min(thisIter.peekLength(), otherIter.peekLength())
                val thisOp = thisIter.next(length)
                val otherOp = otherIter.next(length)
                if (otherOp is Retain) {
                    val attributes = Attributes.compose(thisOp.attributes, otherOp.attributes)
                    val newOp = if (thisOp is Retain) {
                        Retain(length, attributes)
                    } else {
                        Insert(thisOp.value as String, attributes) // TODO: Update here on embed
                    }
                    delta.push(newOp)

                    // Optimization if rest of other is just retain
                    if (! otherIter.hasNext() && delta.getOpAtIndex(delta.size() - 1) == newOp) {
                        val rest = Delta(thisIter.rest())
                        return delta.concat(rest).chop()
                    }
                    // Other op should be delete, we could be an insert or retain
                    // Insert + delete cancels out
                } else if (otherOp is Delete && thisOp is Retain) {
                    delta.push(otherOp)
                }
            }
        }
        return delta.chop()
    }

    fun diff(other: Delta): Delta {
        if (ops == other.ops) {
            return Delta()
        }
        val strings = arrayOf(this, other).map {
            it.map { op ->
              if (op is Insert) {
                  op.value
              } else {
                  val prep = if (it == other) "on" else "with"
                  throw Error("diff() called $prep non-document")
              }
            }.joinToString()
        }
        val retDelta = Delta()
        val thisIter = this.iterator()
        val otherIter = other.iterator()
        println(this)
        println(other)
        println(strings)
        diff_match_patch().diff_main(strings[0], strings[1]).forEach { component ->
            var length = component.text.length
            while (length > 0) {
                var opLength = 0
                if (component.operation == diff_match_patch.Operation.INSERT) {
                    opLength = min(otherIter.peekLength(), length)
                    retDelta.push(otherIter.next(opLength))
                } else if (component.operation == diff_match_patch.Operation.DELETE) {
                    opLength = min(length, thisIter.peekLength())
                    thisIter.next(opLength)
                    retDelta.delete(opLength)
                } else if (component.operation == diff_match_patch.Operation.EQUAL) {
                    opLength = arrayOf(thisIter.peekLength(), otherIter.peekLength(), length).min()!!
                    val thisOp = thisIter.next(opLength)
                    val otherOp = otherIter.next(opLength)
                    if (thisOp is Insert && otherOp is Insert && thisOp.value == otherOp.value) {
                      retDelta.retain(
                              opLength // TODO: Add attribute diff here
                      )
                    } else {
                        retDelta.push(otherOp).delete(opLength)
                    }
                }
                length -= opLength
            }
        }

        return retDelta.chop()
    }

    /**
     * Retrieve Ops count of the Delta.
     */
    fun size(): Int {
        return ops.size
    }

    fun iterator(): DeltaIterator {
        return DeltaIterator(ops)
    }

    /**
     * Retrieve the Op at given index.
     */
    fun getOpAtIndex(index: Int): Op<*>? {
        return ops.getOrNull(index)
    }

    /**
     * Retrieve the Ops list.
     */
    fun getOpsList(): List<Op<*>> {
        return ops
    }

    fun setOps(ops: List<Op<*>>) {
        this.ops.clear()
        ops.forEach { this.ops.add(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Delta) return false

        if (ops != other.ops) return false

        return true
    }

    override fun hashCode(): Int {
        return ops.hashCode()
    }

    override fun toString(): String {
        return "Delta(ops=$ops)"
    }

}