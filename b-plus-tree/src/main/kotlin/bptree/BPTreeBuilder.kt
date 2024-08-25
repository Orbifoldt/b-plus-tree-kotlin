package bptree

import org.example.bptree.bptree.InternalNode
import org.example.bptree.bptree.LeafNode
import org.example.bptree.bptree.Node

/**
 * Builds a new B+ Tree of degree [degree] by creating a root node with the specified [keys], and populating it using
 * the given [builderAction].
 *
 * Example:
 * ```kotlin
 * val tree = buildBPTree(degree = 3, 8) {
 *         internal(4) {
 *             internal(1, 2) {
 *                 leaf(0 to "0")
 *                 leaf(1 to "1")
 *                 leaf(2 to "2")
 *             }
 *             internal(6) {
 *                 leaf(4 to "4", 5 to "5")
 *                 leaf(6 to "6", 7 to "7")
 *             }
 *         }
 *         internal(12) {
 *             internal(10) {
 *                 leaf {  // alternative syntax
 *                     keyValue(8 to "8")
 *                     keyValue(9 to "9")
 *                 }
 *                 leaf(10 to "10")
 *             }
 *             internal(13) {
 *                 leaf(12 to "12")
 *                 leaf(14 to "14")
 *             }
 *         }
 *     }
 * ```
 *
 * @throws IllegalArgumentException when the provided [keys] and [builderAction] would create an invalid B+ Tree of
 * the provided [degree].
 */
fun <K : Comparable<K>, V> buildBPTree(degree: Int, vararg keys: K, builderAction: BPTreeBuilder<K, V>.() -> Unit) =
    BPTreeBuilder<K, V>(degree, *keys)
        .apply(builderAction)
        .build()

class BPTreeBuilder<K : Comparable<K>, V> internal constructor(private val degree: Int, vararg keys: K) {
    private val root = InternalNodeBuilder<K, V>(degree, *keys, isRoot = true)

    fun internal(vararg keys: K, childrenBuilderAction: InternalNodeBuilder<K, V>.() -> Unit) =
        root.internal(*keys, childrenBuilderAction = childrenBuilderAction)

    fun leaf(leafBuilderAction: LeafBuilder<K, V>.() -> Unit) = root.leaf(leafBuilderAction)

    fun leaf(vararg keyValues: Pair<K, V>) = root.leaf(*keyValues)

    fun build(): BPTree<K, V> {
        val rootNode = try {
            root.build()
        } catch (e: IllegalStateException) {
            throw IllegalArgumentException(e.message, e)
        }
        setNeighborsForAllLayers(rootNode)
        return BPTree(degree, rootNode)
    }

    private fun setNeighborsForAllLayers(rootNode: InternalNode<K, V>) {
        var currentLayer = ArrayDeque<Node<K, V>>(listOf(rootNode))
        var nextLayer = ArrayDeque<Node<K, V>>()
        while (currentLayer.isNotEmpty()) {
            currentLayer.zipWithNext().forEach { (left, right) ->
                if (left is LeafNode && right is LeafNode) {
                    left.rightNeighbor = right; right.leftNeighbor = left
                } else if (left is InternalNode && right is InternalNode) {
                    left.rightNeighbor = right; right.leftNeighbor = left
                } else throw IllegalArgumentException("Expected all children to be of same type")
            }

            currentLayer.filterIsInstance<InternalNode<K, V>>().forEach { nextLayer.addAll(it.children) }
            currentLayer = nextLayer
            nextLayer = ArrayDeque()
        }
    }
}

internal sealed interface NodeBuilder<K : Comparable<K>, V> {
    fun build(): Node<K, V>
}

class InternalNodeBuilder<K : Comparable<K>, V> internal constructor(
    private val degree: Int,
    vararg keys: K,
    private val isRoot: Boolean = false,
) : NodeBuilder<K, V> {
    private val keys = mutableListOf<K>().apply { addAll(keys.toList()) }
    private val children = mutableListOf<NodeBuilder<K, V>>()

    fun internal(vararg keys: K, childrenBuilderAction: InternalNodeBuilder<K, V>.() -> Unit) {
        children += InternalNodeBuilder<K, V>(degree, *keys)
            .apply(childrenBuilderAction)
    }

    fun leaf(leafBuilderAction: LeafBuilder<K, V>.() -> Unit) {
        children += LeafBuilder<K, V>(degree).apply(leafBuilderAction)
    }

    fun leaf(vararg keyValues: Pair<K, V>) {
        children += LeafBuilder(degree, keyValues.toList())
    }

    override fun build(): InternalNode<K, V> {
        val children: MutableList<Node<K, V>> = if (children.all { it is InternalNodeBuilder }) {
            children.filterIsInstance<InternalNodeBuilder<K, V>>()
                .map(InternalNodeBuilder<K, V>::build)
                .toMutableList()
        } else if (children.all { it is LeafBuilder }) {
            children.filterIsInstance<LeafBuilder<K, V>>()  // to please the compiler
                .map(LeafBuilder<K, V>::build)
                .toMutableList()
        } else throw IllegalArgumentException("Node may only contain one type of nodes, found both internal and leaves")

        return InternalNode(keys, children).also { it.checkIsValid(degree, isRoot) }
    }
}

class LeafBuilder<K : Comparable<K>, V> internal constructor(
    private val degree: Int,
    keyValues: List<Pair<K, V>> = emptyList(),
) : NodeBuilder<K, V> {
    private val keyValuePairs = mutableListOf<Pair<K, V>>().apply { addAll(keyValues) }

    infix fun keyValue(keyValue: Pair<K, V>) {
        keyValuePairs.add(keyValue)
    }

    override fun build(): LeafNode<K, V> {
        return LeafNode(
            keys = keyValuePairs.map { it.first }.toMutableList(),
            values = keyValuePairs.map { it.second }.toMutableList(),
        )
            .also { it.checkIsValid(degree, false) }
    }
}
