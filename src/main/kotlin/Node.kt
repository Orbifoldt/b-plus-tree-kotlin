package org.example

sealed interface Node<K : Comparable<K>, V> {
    val keys: MutableList<K>

    val leftNeighbor: Node<K, V>?
    val rightNeighbor: Node<K, V>?

    /**
     * @return the value corresponding to [key] if it's present, or null otherwise.
     */
    fun get(key: K): V?

    /**
     * Insert a key-value pair into the subtree defined by this node. When, after insertion, a node exceeds its capacity
     * (at most [degree]` - 1` children for [InternalNode]s or values for [LeafNode]s) it will be split.
     * This splitting will propagate upwards as long as nodes exceed their capacity. When this node is split it's
     * mutated, the upper half of the keys and values are moved to a new node.
     *
     * @return a [NewNode] in case this node was split, or `null` otherwise.
     */
    fun insert(key: K, value: V, degree: Int): NewNode<K, V>?

    /**
     * Split the node into two at the given index.
     *
     * @return a [SplitResult] containing the key at which the node was split, the current node and the new node. When
     * this is a [LeafNode] the key is copied, else if this is a [InternalNode] the key is moved. The
     * current node has all keys and children/values removed that were put into the new node.
     */
    fun split(index: Int): SplitResult<K, V>

    /**
     * Removes the specified key and its corresponding value from the leaves of the subtree defined by this node.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in any leaf.
     */
    fun remove(key: K, degree: Int): V?

    /**
     * Check if this Node is valid as part of a B+ tree of degree [degree].
     * @throws [IllegalStateException] if not valid.
     */
    fun checkIsValid(degree: Int, isRoot: Boolean)

    fun appendTo(stringBuilder: StringBuilder, depth: Int): StringBuilder

    fun maxKeys(degree: Int) = degree - 1
    fun minKeys(degree: Int) = (degree - 1) / 2  // = (m + 1)/2 - 1 = ceil(m/2) - 1
}

data class NewNode<K : Comparable<K>, V>(val key: K, val node: Node<K, V>)

data class SplitResult<K : Comparable<K>, V>(val key: K, val leftNode: Node<K, V>, val rightNode: Node<K, V>)

private fun <K : Comparable<K>, V> Node<K, V>.findIndexOf(key: K): Int = keys.withIndex()
    .firstNotNullOfOrNull { (index, k) -> if (k > key) index else null }  // TODO: replace with binary search
    ?: keys.size

private operator fun Int.plus(bool: Boolean) = if (bool) this + 1 else this
private const val ROUND_UP_SPLITTING = false

data class LeafNode<K : Comparable<K>, V>(
    override val keys: MutableList<K>,
    val values: MutableList<V>,
    override var leftNeighbor: LeafNode<K, V>? = null,
    override var rightNeighbor: LeafNode<K, V>? = null,
) : Node<K, V> {
    override fun get(key: K): V? {
        val index = keys.indexOf(key)
        return if (index >= 0) {
            values[index]
        } else {
            null
        }
    }

    override fun insert(key: K, value: V, degree: Int): NewNode<K, V>? {
        val index = findIndexOf(key)
        keys.add(index, key)
        values.add(index, value)

        if (keys.size > maxKeys(degree)) {
            val (_, _, newNode) = split((keys.size + ROUND_UP_SPLITTING) / 2)
            return NewNode(newNode.keys.first(), newNode)
        }
        return null
    }

    override fun split(index: Int): SplitResult<K, V> {
        // EXAMPLE: split at 1
        //              [ 1 ]
        // 0|1|2   =>   /   \
        //             0    1|2
        assert(index in keys.indices) { "Expected index $index to be in ${keys.indices}" }

        val choseOne = keys[index]  // key that is _copied_ up

        // split node into a left-hand side (this) and a right-hand side (rhs)
        val rhs = LeafNode(
            keys = keys.drop(index).toMutableList(),
            values = values.drop(index).toMutableList(),
            leftNeighbor = this,
            rightNeighbor = this.rightNeighbor,
        )
        this.keys.subList(index, this.keys.size).clear()
        this.values.subList(index, this.values.size).clear()
        this.rightNeighbor = rhs

        return SplitResult(choseOne, leftNode = this, rightNode = rhs)
    }

    override fun remove(key: K, degree: Int): V? {
        val index = keys.indexOfOrNull(key) ?: return null

        keys.removeAt(index)
        return values.removeAt(index)
    }

    override fun checkIsValid(degree: Int, isRoot: Boolean) {
        check(keys.size == values.size) { "A leaf-node should have exactly as many keys as values" }
        check(keys.size <= maxKeys(degree)) { "Leaf node should have less than ${maxKeys(degree)} keys" }
        if (!isRoot) check(keys.size >= minKeys(degree)) {
            "A (non-root) leaf node should have at least ${minKeys(degree)} keys, but found ${keys.size} (degree = $degree)"
        }

        check(keys.zipWithNext().find { (a, b) -> a > b } == null) { "Expected keys to be sorted, but found $keys" }
    }

    override fun appendTo(stringBuilder: StringBuilder, depth: Int): StringBuilder =
        keys.zip(values).joinTo(stringBuilder, prefix = "  ".repeat(depth) + "Leaf [", postfix = "]\n")

    override fun toString(): String = "LeafNode(keys=$keys, values=$values)"  // exclude neighbors, else stack overflow
}


data class InternalNode<K : Comparable<K>, V>(
    override val keys: MutableList<K>,
    val children: MutableList<Node<K, V>>,
    override var leftNeighbor: InternalNode<K, V>? = null,
    override var rightNeighbor: InternalNode<K, V>? = null,
) : Node<K, V> {
    override fun get(key: K): V? = children[findIndexOf(key)].get(key)

    override fun insert(key: K, value: V, degree: Int): NewNode<K, V>? {
        val index = findIndexOf(key)

        val newNodeInfo = children[index].insert(key, value, degree)

        if (newNodeInfo != null) {
            // one of the children was split, insert the new child
            keys.add(index, newNodeInfo.key)
            children.add(index + 1, newNodeInfo.node)

            // Check if this itself also needs to be split
            if (keys.size > maxKeys(degree)) {
                val (newNodesKey, _, newNode) = split((keys.size + ROUND_UP_SPLITTING) / 2)
                return NewNode(newNodesKey, newNode)
            }
        }
        return null
    }

    override fun split(index: Int): SplitResult<K, V> {
        // EXAMPLE: split at index 2                     [ 3 ]
        //                                             /       \
        //   [ 1 | 2 | 3 | 4 ]       =>        [ 1 | 2 ]        [ 4 ]
        //   /   |   |   |   \                 /   |   \         /  \
        //  0    1   2   3   4|5              0    1    2      3    4|5
        require(index in keys.indices) { "Index $index must be in ${keys.indices}" }

        val chosenOne = keys[index]  // The one that ascends, the key that is _moved_ up

        // split node into a left-hand side (this) and a right-hand side (rhs)
        val rhs = InternalNode(
            keys = keys.drop(index + 1).toMutableList(),  // exclude key that ascends
            children = children.drop(index + 1).toMutableList(),
            leftNeighbor = this,
            rightNeighbor = this.rightNeighbor
        )
        this.keys.subList(index, this.keys.size).clear()  // drop both key that ascend and everything that follows
        this.children.subList(index + 1, this.children.size).clear()
        this.rightNeighbor = rhs

        return SplitResult(chosenOne, leftNode = this, rightNode = rhs)

    }

    override fun remove(key: K, degree: Int): V? {
        val index = findIndexOf(key)
        val child = children[index]
        val value = child.remove(key, degree) ?: return null

        if(child.keys.size >= minKeys(degree)) return value  // No underflow, so no need to merge or borrow
        if(keys.size == 0) return value  // We're at the root, so no need to handle underflow

        val lookLeft = (index > 0)  // Only consider siblings, not general neighbors
        if(child is LeafNode){
            if(lookLeft){
                val leftSibling = child.leftNeighbor!!
                if(leftSibling.keys.size > minKeys(degree)){
                    // borrow from left sibling
                    val borrowedKey = leftSibling.keys.removeLast()
                    val borrowedValue = leftSibling.values.removeLast()
                    child.keys.add(0, borrowedKey)
                    child.values.add(0, borrowedValue)
                    keys[index - 1] = borrowedKey
                } else {
                    // merge child into left sibling
                    child.keys.moveInto(leftSibling.keys)
                    child.values.moveInto(leftSibling.values)
                    children.removeAt(index)
                    keys.removeAt(index - 1)
                }
            } else {
                val rightSibling = child.rightNeighbor!!
                if(rightSibling.keys.size > minKeys(degree)) {
                    // borrow from right sibling
                    val borrowedKey = rightSibling.keys.removeFirst()
                    val borrowedValue = rightSibling.values.removeFirst()
                    child.keys.add(borrowedKey)
                    child.values.add(borrowedValue)
                    keys[index] = rightSibling.keys.first()
                }  else {
                    // merge child into right sibling
                    child.keys.moveInto(rightSibling.keys, index = 0)
                    child.values.moveInto(rightSibling.values, index = 0)
                    children.removeAt(index)
                    keys.removeAt(index)
                }
            }

        } else if(child is InternalNode) {
            if(lookLeft){
                val leftSibling = child.leftNeighbor!!
                if(leftSibling.keys.size > minKeys(degree)){
                    // borrow from left sibling
                    val currentKey = keys[index-1]
                    val siblingsLastKey = leftSibling.keys.removeLast()
                    val siblingsLastChild = leftSibling.children.removeLast()

                    child.keys.add(0, currentKey)
                    child.children.add(0, siblingsLastChild)
                    keys[index - 1] = siblingsLastKey
                } else {
                    // merge into left sibling
                    val currentKey = keys.removeAt(index - 1)
                    children.removeAt(index)

                    leftSibling.keys.add(currentKey)
                    child.keys.moveInto(leftSibling.keys)
                    child.children.moveInto(leftSibling.children)
                }
            } else {
                val rightSibling = child.rightNeighbor!!
                if(rightSibling.keys.size > minKeys(degree)) {
                    // borrow from right sibling
                    val currentKey = keys[index]
                    val siblingsFirstKey = rightSibling.keys.removeFirst()
                    val siblingsFirstChild = rightSibling.children.removeFirst()

                    child.keys.add(currentKey)
                    child.children.add(siblingsFirstChild)
                    keys[index] = siblingsFirstKey
                } else {
                    // merge into right sibling
                    val currentKey = keys.removeAt(index)
                    children.removeAt(index)

                    rightSibling.keys.add(0, currentKey)
                    child.keys.moveInto(rightSibling.keys, index = 0)
                    child.children.moveInto(rightSibling.children, index = 0)
                }
            }

        } else throw IllegalStateException("Child node was neither leaf nor internal!")

        return value
    }

    override fun checkIsValid(degree: Int, isRoot: Boolean) {
        // Balanced implies a node can have either only internal nodes or only leaves as children
        check(children.all { it is LeafNode } || children.none { it is LeafNode }) {
            "A node's children should not contain a mix of internal- and leaf-nodes!"
        }

        // Number of children and keys should stay within bounds
        check(children.size == keys.size + 1) { "Internal node should have exactly `keys.size + 1` children" }
        check(keys.size <= maxKeys(degree)) {
            "Internal node in a tree of degree $degree can have at most ${maxKeys(degree)} keys"
        }
        when {
            isRoot -> check(keys.size >= 1) { "Root of tree should have at least 1 key" }
            else   -> check(keys.size >= minKeys(degree)) {
                "Internal (non-root) node in tree of degree $degree should have at least ${minKeys(degree)} keys"
            }
        }

        // Keys should be sorted
        check(keys.zipWithNext().find { (a, b) -> a > b } == null) { "Expected keys to be sorted, but found $keys" }
    }

    override fun appendTo(stringBuilder: StringBuilder, depth: Int): StringBuilder {
        keys.joinTo(stringBuilder, prefix = "  ".repeat(depth) + "Node [", postfix = "]\n")
        children.forEach {
            it.appendTo(stringBuilder, depth + 1)
        }
        return stringBuilder
    }

    override fun toString(): String = "InternalNode(keys=$keys, children=$children)"  // exclude neighbors, else stack overflow
}
