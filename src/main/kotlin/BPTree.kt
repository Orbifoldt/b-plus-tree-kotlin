package org.example


class BPTree(val degree: Int) {
    init {
        require(degree > 1)
    }

    var root: Node = LeafNode(mutableListOf(), mutableListOf())
        private set

    fun insert(key: Int, value: String) {
        println("Inserting value '$value' for key '$key'")
        val newNodeInfo = root.insert(key, value, degree)
        if (newNodeInfo != null) {
            val (newKey, newNode) = newNodeInfo
            root = InternalNode(mutableListOf(newKey), mutableListOf(root, newNode))
        }
        validate()
    }

    fun listValues(): MutableList<String> {
        // TODO: give leaves pointers to neighboring leaves to speed this up
        // Depth-first search for leafs
        val output = mutableListOf<String>()
        val nodesToCheck = ArrayDeque(listOf(root))
        while (nodesToCheck.isNotEmpty()) {
            when (val node = nodesToCheck.removeFirst()) {
                is LeafNode -> output += node.values
                is InternalNode -> nodesToCheck.addAll(node.children)
            }
        }
        println(output)
        return output
    }

    fun get(key: Int): String? = root.get(key)

    internal fun validate() {
        validateNode(root, isRoot = true)
    }

    private fun validateNode(node: Node, isRoot: Boolean) {
        node.isConsistent(degree, isRoot)
        if (node is InternalNode) node.children.forEach { validateNode(it, false) }
    }

//    override fun toString(): String = buildString {
//        var currentRow = mutableListOf(root)
//        var nextRow = mutableListOf<Node3>()
//        while(currentRow.isNotEmpty()){
//            for(x in currentRow){
//                when (x) {
//                    is LeafNode3 -> {
//                        append(x.values.joinToString("|"))
//                        append("   ")
//                    }
//                    is InternalNode3 -> {
//                        append(x.keys.joinToString("|"))
//                        append("   ")
//                        nextRow.addAll(x.children)
//                    }
//                }
//            }
//            append("\n")
//            currentRow = nextRow
//            nextRow = mutableListOf()
//        }
//    }

    override fun toString(): String = buildString { root.appendTo(this, 0) }.removeSuffix("\n")
}


sealed interface Node {
    val keys: MutableList<Int>

    /**
     * @return the value corresponding to [key] if it's present, or null otherwise.
     */
    fun get(key: Int): String?

    /**
     * Insert a key-value pair into the subtree defined by this node. When, after insertion, a node exceeds its capacity
     * (at most [degree]` - 1` children for [InternalNode]s or [degree] values for [LeafNode]s) it will be split.
     * This splitting will propagate upwards as long as nodes exceed their capacity. When this node is split it's
     * mutated, the upper half of the keys and values are moved to a new node.
     *
     * @return a [NewNode] in case this node was split, or `null` otherwise.
     */
    fun insert(key: Int, value: String, degree: Int): NewNode?

    /**
     * Split the node into two at the given index.
     *
     * @return a [SplitResult] containing the key at which the node was split, the current node and the new node. When
     * this is a [LeafNode] the key is copied, else if this is a [InternalNode] the key is moved. The
     * current node has all keys and children/values removed that were put into the new node.
     */
    fun split(index: Int): SplitResult

    fun isConsistent(degree: Int, isRoot: Boolean)

    fun appendTo(stringBuilder: StringBuilder, depth: Int): StringBuilder
}

data class NewNode(val key: Int, val node: Node)
data class SplitResult(val key: Int, val leftNode: Node, val rightNode: Node)

private fun Node.indexOf(key: Int): Int = keys.withIndex()
    .firstNotNullOfOrNull { if (it.value > key) it.index else null }
    ?: keys.size

private operator fun Int.plus(bool: Boolean) = if (bool) this + 1 else this
private const val ROUND_UP_SPLITTING = false


data class LeafNode(
    override val keys: MutableList<Int>,
    val values: MutableList<String>,
) : Node {
    override fun get(key: Int): String? {
        val index = keys.indexOf(key)
        return if (index >= 0) {
            values[index]
        } else {
            null
        }
    }

    override fun insert(key: Int, value: String, degree: Int): NewNode? {
        val index = indexOf(key)
        keys.add(index, key)
        values.add(index, value)

        if (keys.size > degree - 1) {
            val (_, _, newNode) = split((keys.size + ROUND_UP_SPLITTING) / 2)
            return NewNode(newNode.keys.first(), newNode)
        }
        return null
    }

    override fun split(index: Int): SplitResult {
        // EXAMPLE: split at 1
        //              [ 1 ]
        // 0|1|2   =>   /   \
        //             0    1|2
        assert(index in keys.indices) { "Expected index $index to be in ${keys.indices}" }

        val choseOne = keys[index]  // key that is _copied_ up

        val rhs = LeafNode(
            keys = keys.drop(index).toMutableList(),
            values = values.drop(index).toMutableList()
        )
        this.keys.subList(index, this.keys.size).clear()
        this.values.subList(index, this.values.size).clear()

        return SplitResult(choseOne, leftNode = this, rightNode = rhs)
    }

    override fun isConsistent(degree: Int, isRoot: Boolean) {
        check(keys.size == values.size) { "A leaf-node should have exactly as many keys as values" }
        if (!isRoot) check(degree / 2 <= keys.size) {  // TODO: check, not sure if this is right
            "A non-root leaf node should have at least ${degree / 2} keys, but found ${keys.size} (degree = $degree)"
        }
        check(keys.size < degree) { "Leaf node should have less than $degree keys" } // TODO: or <= degree ?
    }

    override fun appendTo(stringBuilder: StringBuilder, depth: Int): StringBuilder =
        keys.zip(values).joinTo(stringBuilder, prefix = "  ".repeat(depth) + "Leaf [", postfix = "]\n")
}


data class InternalNode(
    override val keys: MutableList<Int>,
    val children: MutableList<Node>,
) : Node {
    override fun get(key: Int): String? = children[indexOf(key)].get(key)

    override fun insert(key: Int, value: String, degree: Int): NewNode? {
        val index = indexOf(key)

        val newNodeInfo = children[index].insert(key, value, degree)

        if (newNodeInfo != null) {
            // one of the children was split, insert the new child
            val (newKey, newChild) = newNodeInfo
            keys.add(index, newKey)
            children.add(index + 1, newChild)

            // Check if this itself also needs to be split
            if (keys.size > degree) {
                val (newNodesKey, _, newNode) = split((keys.size + ROUND_UP_SPLITTING) / 2)
                return NewNode(newNodesKey, newNode)
            }
        }
        return null
    }

    override fun split(index: Int): SplitResult {
        // EXAMPLE: split at 2                           [ 3 ]
        //                                             /       \
        //   [ 1 | 2 | 3 | 4 ]       =>        [ 1 | 2 ]        [ 4 ]
        //   /   |   |   |   \                 /   |   \         /  \
        //  0    1   2   3   4|5              0    1    2      3    4|5
        require(index in keys.indices) { "Expected index $index to be in ${keys.indices}" }

        val chosenOne = keys[index]  // The one that ascends: the key that is _moved_ up

        // split node into a left-hand side (this) and right-hand side (rhs)
        val rhs = InternalNode(
            keys = keys.drop(index + 1).toMutableList(),  // exclude key that ascends
            children = children.drop(index + 1).toMutableList(),
        )
        this.keys.subList(index, this.keys.size).clear()  // drop both key that ascend and everything that follows
        this.children.subList(index + 1, this.children.size).clear()

        return SplitResult(chosenOne, leftNode = this, rightNode = rhs)

    }

    override fun isConsistent(degree: Int, isRoot: Boolean) {
        // Balanced implies a node can have either only internal nodes or only leaves as children
        check(children.all { it is LeafNode } || children.none { it is LeafNode })
        { "A node's children should not contain a mix of internal- and leaf-nodes!" }

        // Number of children and keys should stay within bounds
        check(children.size == keys.size + 1) { "Internal node should have exactly `keys.size + 1` children" }
        check(keys.size <= degree) { "Internal node in tree of degree $degree can have at most $degree keys" }
        if (isRoot) {
            check(1 <= keys.size) { "Root of tree should have at least 1 key" }
        } else {
            // TODO: fix, don't think this is completely correct
            check(degree / 2 <= keys.size) { "Internal node in tree of degree $degree should have at least ${degree / 2} keys" }
        }
    }

    override fun appendTo(stringBuilder: StringBuilder, depth: Int): StringBuilder {
        keys.joinTo(stringBuilder, prefix = "  ".repeat(depth) + "Node [", postfix = "]\n")
        children.forEach {
            it.appendTo(stringBuilder, depth + 1)
        }
        return stringBuilder
    }
}
