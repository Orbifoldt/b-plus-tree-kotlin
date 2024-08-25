package bptree

import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.ObjectAssert
import org.example.bptree.bptree.InternalNode
import org.example.bptree.bptree.LeafNode
import org.example.bptree.bptree.Node

object NodeAssertions {
    fun <K : Comparable<K>, V> ObjectAssert<Node<K, V>>.isLeaf(): ObjectAssert<LeafNode<K, V>> =
        this.isInstanceOf(LeafNode::class.java) as ObjectAssert<LeafNode<K, V>>

    fun <K : Comparable<K>, V> ObjectAssert<Node<K, V>>.isInternal(): ObjectAssert<InternalNode<K, V>> =
        this.isInstanceOf(InternalNode::class.java) as ObjectAssert<InternalNode<K, V>>

    fun <K : Comparable<K>, V> ObjectAssert<LeafNode<K, V>>.containsExactly(vararg keyValuePairs: Pair<Int, String>): ObjectAssert<LeafNode<K, V>> =
        this.also {
            extracting { it.keys.zip(it.values) }.asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(*keyValuePairs)
        }

    fun <K : Comparable<K>, V> ObjectAssert<LeafNode<K, V>>.isEmpty(): ObjectAssert<LeafNode<K, V>> = this.also {
        extracting { it.keys.zip(it.values) }.asInstanceOf(InstanceOfAssertFactories.LIST)
            .isEmpty()
    }

    fun <K : Comparable<K>, V> ObjectAssert<InternalNode<K, V>>.containsExactlyKeys(vararg keys: Int): ObjectAssert<InternalNode<K, V>> =
        this.also {
            extracting { it.keys }.asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(*keys.toTypedArray())
        }

    fun <K : Comparable<K>, V> ObjectAssert<InternalNode<K, V>>.andChildren(vararg children: Node<K, V>): ObjectAssert<InternalNode<K, V>> =
        this.also {
            extracting { it.children }.asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(*children)
        }
}