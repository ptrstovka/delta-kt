interface EachLine {

    fun predicate(line: Delta, attributes: Map<String, String>, index: Int): Boolean

}