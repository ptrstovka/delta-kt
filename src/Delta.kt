class Delta {

    private val ops = mutableListOf<Op<*>>()

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
     * Retrieve the length of the delta.
     */
    fun length(): Int {
        return ops.size
    }

    /**
     * Retrieve the Op at given index.
     */
    fun getOpAtIndex(index: Int): Op<*>? {
        return ops.getOrNull(index)
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


}