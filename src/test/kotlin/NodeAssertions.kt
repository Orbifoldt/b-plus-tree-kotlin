import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.ObjectAssert
import org.example.InternalNode
import org.example.LeafNode
import org.example.Node

object NodeAssertions {
    fun ObjectAssert<Node>.isLeaf(): ObjectAssert<LeafNode> =
        this.isInstanceOf(LeafNode::class.java) as ObjectAssert<LeafNode>

    fun ObjectAssert<Node>.isInternal(): ObjectAssert<InternalNode> =
        this.isInstanceOf(InternalNode::class.java) as ObjectAssert<InternalNode>

    fun ObjectAssert<LeafNode>.containsExactly(vararg keyValuePairs: Pair<Int, String>): ObjectAssert<LeafNode> = this.also {
        extracting { it.keys.zip(it.values) }.asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactly(*keyValuePairs)
    }
    fun ObjectAssert<LeafNode>.isEmpty(): ObjectAssert<LeafNode> = this.also {
        extracting { it.keys.zip(it.values) }.asInstanceOf(InstanceOfAssertFactories.LIST)
            .isEmpty()
    }

    fun ObjectAssert<InternalNode>.containsExactlyKeys(vararg keys: Int): ObjectAssert<InternalNode> = this.also {
        extracting { it.keys }.asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactly(*keys.toTypedArray())
    }

    fun ObjectAssert<InternalNode>.andChildren(vararg children: Node): ObjectAssert<InternalNode> = this.also {
        extracting { it.children }.asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactly(*children)
    }
}