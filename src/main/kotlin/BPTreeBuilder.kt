package org.example

/**
 * Builds a new B+ Tree of degree [degree] by creating a root node with the specified [keys], and populating it using
 * the given [builderAction].
 *
 * Example:
 * ```kotlin
 * val tree = buildBPTree(degree = 3, 8) {
 *         internal(4) {
 *             internal(1) {
 *                 leaf(0 to "0")
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
 *             internal(14) {
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
fun buildBPTree(degree: Int, vararg keys: Int, builderAction: BPTreeBuilder.() -> Unit) =
    BPTreeBuilder(degree, *keys)
        .apply(builderAction)
        .build()

class BPTreeBuilder internal constructor(private val degree: Int, vararg keys: Int) {
    private val root = InternalNodeBuilder(degree, *keys, isRoot = true)

    fun internal(vararg keys: Int, childrenBuilderAction: InternalNodeBuilder.() -> Unit) =
        root.internal(*keys, childrenBuilderAction = childrenBuilderAction)

    fun leaf(leafBuilderAction: LeafBuilder.() -> Unit) = root.leaf(leafBuilderAction)

    fun leaf(vararg keyValues: Pair<Int, String>) = root.leaf(*keyValues)

    fun build(): BPTree {
        val rootNode = try {
            root.build()
        } catch (e: IllegalStateException) {
            throw IllegalArgumentException(e.message, e)
        }
        setNeighborsForAllLayers(rootNode)
        return BPTree(degree, rootNode)
    }

    private fun setNeighborsForAllLayers(rootNode: InternalNode) {
        var currentLayer = ArrayDeque<Node>(listOf(rootNode))
        var nextLayer = ArrayDeque<Node>()
        while (currentLayer.isNotEmpty()) {
            currentLayer.zipWithNext().forEach { (left, right) ->
                if (left is LeafNode && right is LeafNode) {
                    left.rightNeighbor = right; right.leftNeighbor = left
                } else if (left is InternalNode && right is InternalNode) {
                    left.rightNeighbor = right; right.leftNeighbor = left
                } else throw IllegalArgumentException("Expected all children to be of same type")
            }

            currentLayer.filterIsInstance<InternalNode>().forEach { nextLayer.addAll(it.children) }
            currentLayer = nextLayer
            nextLayer = ArrayDeque()
        }
    }
}

internal sealed interface NodeBuilder {
    fun build(): Node
}

class InternalNodeBuilder internal constructor(
    private val degree: Int,
    vararg keys: Int,
    private val isRoot: Boolean = false,
) : NodeBuilder {
    private val keys = mutableListOf<Int>().apply { addAll(keys.toList()) }
    private val children = mutableListOf<NodeBuilder>()

    fun internal(vararg keys: Int, childrenBuilderAction: InternalNodeBuilder.() -> Unit) {
        children += InternalNodeBuilder(degree, *keys)
            .apply(childrenBuilderAction)
    }

    fun leaf(leafBuilderAction: LeafBuilder.() -> Unit) {
        children += LeafBuilder(degree).apply(leafBuilderAction)
    }

    fun leaf(vararg keyValues: Pair<Int, String>) {
        children += LeafBuilder(degree, keyValues.toList())
    }

    override fun build(): InternalNode {
        val children: MutableList<Node> = if (children.all { it is InternalNodeBuilder }) {
            children.filterIsInstance<InternalNodeBuilder>()
                .map(InternalNodeBuilder::build)
                .toMutableList()
        } else if (children.all { it is LeafBuilder }) {
            children.filterIsInstance<LeafBuilder>()  // to please the compiler
                .map(LeafBuilder::build)
                .toMutableList()
        } else throw IllegalArgumentException("Node may only contain one type of nodes, found both internal and leaves")

        return InternalNode(keys, children).also { it.checkIsValid(degree, isRoot) }
    }
}

class LeafBuilder internal constructor(
    private val degree: Int,
    keyValues: List<Pair<Int, String>> = emptyList(),
) : NodeBuilder {
    private val keyValuePairs = mutableListOf<Pair<Int, String>>().apply { addAll(keyValues) }

    infix fun keyValue(keyValue: Pair<Int, String>) {
        keyValuePairs.add(keyValue)
    }

    override fun build(): LeafNode {
        return LeafNode(
            keys = keyValuePairs.map { it.first }.toMutableList(),
            values = keyValuePairs.map { it.second }.toMutableList(),
        )
            .also { it.checkIsValid(degree, false) }
    }
}
