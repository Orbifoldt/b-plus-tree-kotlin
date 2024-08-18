package org.example

import java.util.AbstractMap.SimpleEntry


class BPTree(val degree: Int) {
    init {
        require(degree > 1)
    }

    var root: Node = LeafNode(mutableListOf(), mutableListOf(), null, null)
        private set

    fun get(key: Int): String? = root.get(key)

    @Synchronized  // TODO: optimize locking
    fun insert(key: Int, value: String) {
        println("Inserting value '$value' for key '$key'")
        val newNodeInfo = root.insert(key, value, degree)
        if (newNodeInfo != null) {
            val (newKey, newNode) = newNodeInfo
            root = InternalNode(mutableListOf(newKey), mutableListOf(root, newNode), null, null)
        }
        validate()
    }

    @Synchronized  // TODO: optimize locking
    fun remove(key: Int): String? {
        println("Deleting '$key'")
        val result =  root.remove(key, degree)
        // TODO: handle underflow of root itself
        // TODO: write tests
        return result
    }


    internal fun validate() {
        validateNode(root, isRoot = true)
    }

    private fun validateNode(node: Node, isRoot: Boolean) {
        node.checkIsValid(degree, isRoot)
        if (node is InternalNode) node.children.forEach { validateNode(it, false) }
    }

    val entries: Set<Map.Entry<Int, String>>
        get() = leafIterable().flatMapTo(mutableSetOf()) {
            it.keys.zip(it.values).map { (key, value) -> SimpleEntry(key, value) }
        }

    val keys: Set<Int>
        get() = leafIterable().flatMapTo(mutableSetOf()) { it.keys }

    val values: List<String>
        get() = leafIterable().flatMap { it.values }

    /**
     * List keys in order
     */
    fun orderedKeys(): List<Int> = leafIterable().flatMap { it.keys }

    private fun getSmallestLeaf(): LeafNode {
        var currentNode = root
        while(currentNode is InternalNode){
            currentNode = currentNode.children.first()
        }
        return currentNode as LeafNode
    }

//    private fun leafSequence(): Sequence<LeafNode> = sequence {
//        var current: LeafNode? = getSmallestLeaf()
//        while(current != null) {
//            yield(current)
//            current = current.rightNeighbor
//        }
//    }

    private fun leafIterator(): Iterator<LeafNode> = iterator {
        var current: LeafNode? = getSmallestLeaf()
        while(current != null) {
            yield(current)
            current = current.rightNeighbor
        }
    }

    private fun leafIterable() =  Iterable { leafIterator() }

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
