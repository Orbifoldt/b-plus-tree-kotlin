package bptree

import org.assertj.core.api.ObjectAssert

object BPTreeAssertions {
    /**
     * Creates a new instance of [ObjectAssert] (instead of [org.assertj.core.api.MapAssert]).
     */
    fun <K: Comparable<K>, V> assertThat(tree: BPTree<K, V>): ObjectAssert<BPTree<K, V>> =
        org.assertj.core.api.Assertions.assertThat(tree as Any) as ObjectAssert<BPTree<K, V>>

    fun <K : Comparable<K>, V> ObjectAssert<BPTree<K, V>>.isValid(): ObjectAssert<BPTree<K, V>> =
        this.matches { tree ->
            try {
                tree.validate()
                true
            } catch (e: IllegalStateException) {
                // Throw to preserve the stacktrace (instead of setting custom fail message and returning false)
                throw AssertionError("Expected tree to be valid, but found violation: ${e.message}").apply {
                    stackTrace = e.stackTrace
                }
            }
        }

    fun <K : Comparable<K>, V> ObjectAssert<BPTree<K, V>>.hasSize(expected: Int): ObjectAssert<BPTree<K, V>> =
        this.apply {
            extracting { it.size }.`as` { "Checking tree's size" }
                .isEqualTo(expected)
        }

    fun <K : Comparable<K>, V> ObjectAssert<BPTree<K, V>>.isEmpty(): ObjectAssert<BPTree<K, V>> =
        this.withFailMessage { "Expected " }.matches { it.isEmpty() }
}