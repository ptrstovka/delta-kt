class Attributes {

    companion object {

        fun compose(a: Map<String, String>, b: Map<String, String>): Map<String, String> {
            val attributes = HashMap(b)
            // TODO: Remove null values?
            a.forEach { (k, v) ->
                // TODO: Check for value if allowing null values.
                if (! b.containsKey(k)) {
                    attributes[k] = v
                }
            }
            return attributes
        }

        fun diff(a: Map<String, String>, b: Map<String, String>): Map<String, String> {
            return (a.keys + b.keys).fold(mutableMapOf(), { acc, key ->
                if (a[key] != b[key]) {
                    val value = b[key]
                    if (value != null) {
                        acc[key] = value
                    }
                }

                acc
            })
        }

    }

}