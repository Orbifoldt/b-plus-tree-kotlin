package bptree

import org.example.bptree.bptree.InternalNode
import org.example.bptree.bptree.LeafNode
import org.example.bptree.bptree.Node
import java.util.AbstractMap.SimpleEntry


class BPTree<K : Comparable<K>, V>(val degree: Int) : MutableMap<K, V> {
    init { require(degree > 1) }

    var root: Node<K, V> = LeafNode(mutableListOf(), mutableListOf(), null, null)
        private set

    internal constructor(degree: Int, root: Node<K, V>) : this(degree) {
        this.root = root
    }

    override fun get(key: K): V? = root.get(key)

    @Synchronized  // TODO: optimize locking
    override fun put(key: K, value: V): V? {
        val newNodeInfo = root.insert(key, value, degree)
        if (newNodeInfo != null) {
            val (newKey, newNode) = newNodeInfo
            root = InternalNode(mutableListOf(newKey), mutableListOf(root, newNode), null, null)
        }
        validate()
        return null // TODO
    }

    override fun putAll(from: Map<out K, V>) {
        from.toSortedMap()  // this should optimize bulk insert (TODO: load-test)
            .forEach { (key, value) -> put(key, value) }
    }

    @Synchronized  // TODO: optimize locking
    override fun remove(key: K): V? {
        val result =  root.remove(key, degree)

        if(root is InternalNode && root.keys.isEmpty()){
            assert((root as InternalNode).children.size == 1) { "When there are no keys there should be 1 child" }
            root = (root as InternalNode).children.first()
        }
        return result
    }

    internal fun validate() {
        validateNode(root, isRoot = true)
    }

    private fun validateNode(node: Node<K, V>, isRoot: Boolean) {
        node.checkIsValid(degree, isRoot)
        if (node is InternalNode) node.children.forEach { validateNode(it, false) }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = leafIterable().flatMapTo(mutableSetOf()) {
            it.keys.zip(it.values).map { (key, value) -> SimpleEntry(key, value) }
        }

    override val keys: MutableSet<K>
        get() = leafIterable().flatMapTo(mutableSetOf()) { it.keys }

    override val values: MutableCollection<V>
        get() = leafIterable().flatMapTo(mutableListOf()) { it.values }

    override fun clear() {
        root = LeafNode(mutableListOf(), mutableListOf(), null, null)
    }

    override fun isEmpty() = root is LeafNode && (root as LeafNode).values.isEmpty()

    override fun containsValue(value: V): Boolean = leafIterable().find { it.values.contains(value) } != null

    override fun containsKey(key: K): Boolean = get(key) != null

    override val size: Int
        get() = leafIterable().sumOf { it.values.size }

    /**
     * Returns a sorted [List] of all keys in this tree.
     */
    fun orderedKeys(): List<K> = leafIterable().flatMap { it.keys }

    private fun getSmallestLeaf(): LeafNode<K, V> {
        var currentNode = root
        while(currentNode is InternalNode){
            currentNode = currentNode.children.first()
        }
        return currentNode as LeafNode
    }

    private fun leafIterator(): Iterator<LeafNode<K, V>> = iterator {
        var current: LeafNode<K, V>? = getSmallestLeaf()
        while(current != null) {
            yield(current)
            current = current.rightNeighbor
        }
    }

    private fun leafIterable() =  Iterable { leafIterator() }

    override fun toString(): String = buildString { root.appendTo(this, 0) }.removeSuffix("\n")
}
