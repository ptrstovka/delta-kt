class Delta: Iterable<Op<*>> {

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
        return fold(0) { length, op ->
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
        return fold(0) { length, op ->
            length + op.length()
        }
    }

    fun slice(start: Int, end: Int = Int.MAX_VALUE): Delta {
        return slice(start until end)
    }

    fun slice(range: IntRange): Delta {
        val ops = mutableListOf<Op<*>>()

        var index = 0
        val iterator = iterator() as DeltaIterator
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

    fun eachLine(
            predicate: (line: Delta, attributes: Map<String, String>, index: Int) -> Boolean,
            newLine: String = "\n"
    ) {
        val iterator = iterator() as DeltaIterator
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

    /**
     * Retrieve Ops count of the Delta.
     */
    fun size(): Int {
        return ops.size
    }

    override fun iterator(): Iterator<Op<*>> {
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