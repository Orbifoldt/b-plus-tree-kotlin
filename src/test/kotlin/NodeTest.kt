import org.assertj.core.api.Assertions.assertThat
import org.example.InternalNode
import org.example.LeafNode
import org.junit.jupiter.api.Test


class NodeTest {

    @Test
    fun `Should correctly split a leaf node into two nodes`() {
        val node = LeafNode(mutableListOf(1, 2, 3, 4, 5), mutableListOf("1", "2", "3", "4", "5"))
        val (_, a, b) = node.split(2)
        assertThat(a.keys).containsExactly(1, 2)
        assertThat(b.keys).containsExactly(3, 4, 5)
    }

    @Test
    fun `when inserting at parent should correctly add to leaf`() {
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(3), mutableListOf("3"))
        val child2 = LeafNode(mutableListOf(9), mutableListOf("9"))
        val parent = InternalNode(mutableListOf(3, 9), mutableListOf(child0, child1, child2))

        val a = parent.insert(6, "6", 3)

        assertThat(a).isNull()
        assertThat(child0.keys).containsExactly(0)
        assertThat(child1.keys).containsExactly(3, 6)
        assertThat(child2.keys).containsExactly(9)
        assertThat(parent.keys).containsExactly(3, 9)
        assertThat(parent.children).containsExactly(child0, child1, child2)
    }

    @Test
    fun `When inserting should correctly split a node into two nodes and add new node to the parent with a copied key`() {
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(3, 6), mutableListOf("3", "6"))
        val child2 = LeafNode(mutableListOf(9), mutableListOf("9"))
        val parent = InternalNode(mutableListOf(3, 9), mutableListOf(child0, child1, child2))

        val b = parent.insert(7, "7", 3)

        assertThat(b).isNull()  // parent doesn't get split
        assertThat(parent.keys).containsExactly(3, 6, 9)
        assertThat(parent.children).hasSize(4)

        val (newChild0, newChild1, newChild2, newChild3) = parent.children

        assertThat(newChild0).isSameAs(child0)
        assertThat(child0.keys).containsExactly(0)

        assertThat(newChild1).isSameAs(child1)
        assertThat(child1.keys).containsExactly(3)

        assertThat(newChild3).isSameAs(child2)
        assertThat(child2.keys).containsExactly(9)

        assertThat(newChild2.keys).containsExactly(6, 7)
    }

    @Test
    fun `when splitting an internal node, should correctly move the key (instead of copying)`() {
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(1), mutableListOf("1"))
        val child2 = LeafNode(mutableListOf(2), mutableListOf("2"))
        val child3 = LeafNode(mutableListOf(3), mutableListOf("3"))
        val child4 = LeafNode(mutableListOf(4), mutableListOf("4"))
        val parent = InternalNode(mutableListOf(1, 2, 3, 4), mutableListOf(child0, child1, child2, child3, child4))

        val (newKey, lhs, rhs) = parent.split(2)
        assertThat(lhs.keys).containsExactly(1, 2)
        assertThat(rhs.keys).containsExactly(4)
        assertThat(newKey).isEqualTo(3)
    }

    @Test
    fun `when insertion causes internal node to split, should correctly move the key (instead of copying)`() {
        // EXAMPLE: Inserting 5
        //                                             [ 3 ]
        //       [ 1 | 2 | 3 ]                       /       \
        //       /   |   |   \       =>      [ 1 | 2 ]        [ 4 ]
        //      0    1   2    3|4            /   |   \         /  \
        //                                  0    1    2      3    4|5
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(1), mutableListOf("1"))
        val child2 = LeafNode(mutableListOf(2), mutableListOf("2"))
        val child3 = LeafNode(mutableListOf(3, 4), mutableListOf("3", "4"))
        val parent = InternalNode(mutableListOf(1, 2, 3), mutableListOf(child0, child1, child2, child3))

        val x = parent.insert(5, "5", 3)

        assertThat(x).isNotNull; x!!
        val (key, node) = x

        // New (root) key should be 3, and it should be moved out (rather than copied)
        assertThat(key).isEqualTo(3)
        assertThat(node.keys).containsExactly(4)
        assertThat(parent.keys).containsExactly(1, 2)  // NB: parent is no longer root of the tree
    }
}