import NodeAssertions.andChildren
import NodeAssertions.containsExactly
import NodeAssertions.containsExactlyKeys
import org.assertj.core.api.Assertions.assertThat
import org.example.InternalNode
import org.example.LeafNode
import org.junit.jupiter.api.Nested
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
        //        [ 3 | 9 ]               [ 3 | 5 | 9 ]
        //       /    |    \     =>      /    |   |    \
        //      0   3|4|6   9           0   3|4   5|6   9
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(3, 4, 6), mutableListOf("3", "4", "6"))
        val child2 = LeafNode(mutableListOf(9), mutableListOf("9"))
        val parent = InternalNode(mutableListOf(3, 9), mutableListOf(child0, child1, child2))

        val b = parent.insert(5, "5", 4)

        assertThat(b).isNull()  // parent doesn't get split
        assertThat(parent.keys).containsExactly(3, 5, 9)
        assertThat(parent.children).hasSize(4)

        val (newChild0, newChild1, newChild2, newChild3) = parent.children

        assertThat(newChild0).isSameAs(child0)
        assertThat(child0.keys).containsExactly(0)

        assertThat(newChild1).isSameAs(child1)
        assertThat(child1.keys).containsExactly(3, 4)

        assertThat(newChild3).isSameAs(child2)
        assertThat(child2.keys).containsExactly(9)

        assertThat(newChild2.keys).containsExactly(5, 6)
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
        //       /   |   |    \      =>      [ 1 | 2 ]        [ 5 ]
        //      0    1   2   3|4|5           /   |   \       /     \
        //                                  0    1    2     3|4    5|6
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(1), mutableListOf("1"))
        val child2 = LeafNode(mutableListOf(2), mutableListOf("2"))
        val child3 = LeafNode(mutableListOf(3, 4, 5), mutableListOf("3", "4", "5"))
        val parent = InternalNode(mutableListOf(1, 2, 3), mutableListOf(child0, child1, child2, child3))

        val x = parent.insert(6, "6", 4)

        assertThat(x).isNotNull; x!!
        val (key, node) = x

        // New (root) key should be 3, and it should be moved out (rather than copied)
        assertThat(key).isEqualTo(3)
        assertThat(node.keys).containsExactly(5)
        assertThat(parent.keys).containsExactly(1, 2)  // NB: parent is no longer root of the tree
    }

    @Nested
    inner class `when deleting keys in leafs from a two layer tree and` {
        //     [ 1 | 2 ]
        //     /   |   \
        //    0    1    2|3
        val degree = 3
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(1), mutableListOf("1"))
        val child2 = LeafNode(mutableListOf(2,3), mutableListOf("2", "3"))
        val parent = InternalNode(mutableListOf(1, 2), mutableListOf(child0, child1, child2))
        init {
            setNeighborsMulti(child0, child1, child2)
        }

        @Test
        fun `when deleting the left most key and the node becomes empty it should be merged to the right`() {
            //     [ 1 | 2 ]            [ 2 ]
            //     /   |   \      =>    /   \
            //    0    1    2|3        1     2|3
            val deletionResult = parent.remove(0, degree)

            assertThat(deletionResult).isEqualTo("0")

            assertThat(parent.children).containsExactly(child1, child2)
            assertThat(parent).containsExactlyKeys(2)

            assertThat(parent.children[0] as LeafNode).containsExactly(1 to "1")
            assertThat(parent.children[1] as LeafNode).containsExactly(2 to "2", 3 to "3")
        }

//        @Test
//        fun `when deleting the middle key and the node becomes empty it should borrow from the right`() {
//            //     [ 1 | 2 ]            [ 1 | 3 ]
//            //     /   |   \      =>    /   |   \
//            //    0    1    2|3        0    2    3
//            val deletionResult = parent.remove(1, degree = 3)
//
//            assertThat(deletionResult).isEqualTo("1")
//
//            assertThat(parent.children).hasSize(3)
//            assertThat(parent.keys).containsExactly(1, 3)
//
//            assertThat(parent.children[0] as LeafNode).containsExactly(0 to "0")
//            assertThat(parent.children[1] as LeafNode).containsExactly(2 to "2")
//            assertThat(parent.children[2] as LeafNode).containsExactly(3 to "3")
//        }

//        @Test
//        fun `when deleting the first key in the right most leaf it should update the parent's key`() {
//            //     [ 1 | 2 ]    (delete 2)      [ 1 | 3 ]
//            //     /   |   \        =>          /   |   \
//            //    0    1    2|3                0    1    3
//            val deletionResult = parent.remove(2, degree)
//
//            assertThat(deletionResult).isEqualTo("2")
//
//            assertThat(parent.children).hasSize(3)
//            assertThat(parent.keys).containsExactly(1, 3)
//
//            assertThat(parent.children[0] as LeafNode).containsExactly(0 to "0")
//            assertThat(parent.children[1] as LeafNode).containsExactly(1 to "1")
//            assertThat(parent.children[2] as LeafNode).containsExactly(3 to "3")
//        }

        @Test
        fun `when deleting both keys of the right most leaf, it should be merged into the middle leaf`() {
            //     [ 1 | 2 ]    (delete 2 and 3)     [ 1 ]
            //     /   |   \           =>            /   \
            //    0    1    2|3                     0     1
            val deletionResult = parent.remove(3, degree)
            val deletionResult2 = parent.remove(2, degree)

            assertThat(deletionResult).isEqualTo("3")
            assertThat(deletionResult2).isEqualTo("2")

            assertThat(parent.children).hasSize(2)
            assertThat(parent).containsExactlyKeys(1)

            assertThat(parent.children[0] as LeafNode).containsExactly(0 to "0")
            assertThat(parent.children[1] as LeafNode).containsExactly(1 to "1")
        }

        @Test
        fun `when deleting three keys the internal node should be replaced with a leaf`() {
            //     [ 1 | 2 ]    (delete 2, 3, 0)
            //     /   |   \           =>            1    (just a single leaf)
            //    0    1    2|3
            val deletionResult = parent.remove(2, degree)
            val deletionResult2 = parent.remove(3, degree)
            val deletionResult3 = parent.remove(0, degree)

            assertThat(deletionResult).isEqualTo("2")
            assertThat(deletionResult2).isEqualTo("3")
            assertThat(deletionResult3).isEqualTo("0")

            assertThat(parent.children).containsExactly(child1)
            assertThat(child1).containsExactly(1 to "1")
        }
    }

    @Nested
    inner class `When deleting from a 3-layered tree and` {
        //           [ 2 ]
        //          /     \
        //     [ 1 ]       [ 3 | 4 ]
        //     /   \      /    |    \
        //    0     1    2     3     4
        val degree = 3
        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child1 = LeafNode(mutableListOf(1), mutableListOf("1"))
        val child2 = LeafNode(mutableListOf(2), mutableListOf("2"))
        val child3 = LeafNode(mutableListOf(3), mutableListOf("3"))
        val child4 = LeafNode(mutableListOf(4), mutableListOf("4"))
        val parent0 = InternalNode(mutableListOf(1), mutableListOf(child0, child1))
        val parent1 = InternalNode(mutableListOf(3,4), mutableListOf(child2, child3, child4))
        val grandParent = InternalNode(mutableListOf(2), mutableListOf(parent0, parent1))
        init {
            setNeighborsMulti(child0, child1, child2, child3, child4)
            setNeighbors(parent0, parent1)
        }

        @Test
        fun `when deleting inside parent1 its children should be merged`(){
            //           [ 2 ]                                       [ 2 ]        TODO: should root be updated?
            //          /     \              (delete 3)             /     \
            //     [ 1 ]       [ 3 | 4 ]         =>            [ 1 ]       [ 4 ]
            //     /   \      /    |    \                      /   \       /   \
            //    0     1    2     3     4                    0     1     2     4
            val result = grandParent.remove(3, degree)

            assertThat(result).isNotNull

            assertThat(grandParent.children).hasSize(2)
            val (actualParent0, actualParent1) = grandParent.children

            assertThat(actualParent0).isInstanceOf(InternalNode::class.java); actualParent0 as InternalNode
            assertThat(actualParent0.keys).containsExactly(1)
            assertThat(actualParent0.children).hasSize(2)
            assertThat(actualParent1).isInstanceOf(InternalNode::class.java); actualParent1 as InternalNode
            assertThat(actualParent1.keys).containsExactly(4)
            assertThat(actualParent1.children).hasSize(2)

            assertThat((actualParent0.children[0] as LeafNode).keys).containsExactly(0)
            assertThat((actualParent0.children[1] as LeafNode).keys).containsExactly(1)
            assertThat((actualParent1.children[0] as LeafNode).keys).containsExactly(2)
            assertThat((actualParent1.children[1] as LeafNode).keys).containsExactly(4)
        }

        @Test
        fun `when deleting inside first child of parent1 it should be merged with child of parent0`(){
            //           [ 2 ]                                       [ 2 ]        TODO: should root be updated?
            //          /     \              (delete 2)             /     \
            //     [ 1 ]       [ 3 | 4 ]         =>            [ 1 ]       [ 4 ]
            //     /   \      /    |    \                      /   \       /   \
            //    0     1    2     3     4                    0     1     3     4
            val result = grandParent.remove(2, degree)

            assertThat(result).isNotNull

            assertThat(grandParent.children).hasSize(2)
            val (actualParent0, actualParent1) = grandParent.children

            assertThat(actualParent0).isInstanceOf(InternalNode::class.java); actualParent0 as InternalNode
            assertThat(actualParent0.keys).containsExactly(1)
            assertThat(actualParent0.children).hasSize(2)
            assertThat(actualParent1).isInstanceOf(InternalNode::class.java); actualParent1 as InternalNode
            assertThat(actualParent1.keys).containsExactly(4)
            assertThat(actualParent1.children).hasSize(2)

            assertThat((actualParent0.children[0] as LeafNode).keys).containsExactly(0)
            assertThat((actualParent0.children[1] as LeafNode).keys).containsExactly(1)
            assertThat((actualParent1.children[0] as LeafNode).keys).containsExactly(3)
            assertThat((actualParent1.children[1] as LeafNode).keys).containsExactly(4)
        }

        @Test
        fun `when deleting inside first child of parent0 it should be merged with parent1`(){
            //           [ 2 ]                                          [ 3 ]
            //          /     \              (delete 0)                /     \
            //     [ 1 ]       [ 3 | 4 ]         =>               [ 2 ]       [ 4 ]
            //     /   \      /    |    \                         /   \       /   \
            //    0     1    2     3     4                       1     2     3      4
            val result = grandParent.remove(0, degree)

            assertThat(result).isNotNull

            assertThat(grandParent.children).hasSize(2)
            val (actualParent0, actualParent1) = grandParent.children

            assertThat(actualParent0).isInstanceOf(InternalNode::class.java); actualParent0 as InternalNode
            assertThat(actualParent0.keys).containsExactly(2)
            assertThat(actualParent0.children).hasSize(2)
            assertThat(actualParent1).isInstanceOf(InternalNode::class.java); actualParent1 as InternalNode
            assertThat(actualParent1.keys).containsExactly(4)
            assertThat(actualParent1.children).hasSize(2)

            assertThat((actualParent0.children[0] as LeafNode).keys).containsExactly(1)
            assertThat((actualParent0.children[1] as LeafNode).keys).containsExactly(2)
            assertThat((actualParent1.children[0] as LeafNode).keys).containsExactly(3)
            assertThat((actualParent1.children[1] as LeafNode).keys).containsExactly(4)
        }
    }

    @Nested
    inner class `Deleting in a 4 layered tree, ` {
        // Base tree, we'll add keys and values at various places for all different scenarios
        //                   [  8  ]
        //                /           \
        //         [ 4 ]                 [ 12 ]
        //        /     \               /      \
        //   [ 2 ]      [ 6 ]      [ 10 ]      [ 14 ]
        //   /   \      /   \      /    \      /    \
        //  0     2    4     6    8     10    12     14
        val degree = 3

        val child0 = LeafNode(mutableListOf(0), mutableListOf("0"))
        val child2 = LeafNode(mutableListOf(2), mutableListOf("2"))
        val child4 = LeafNode(mutableListOf(4), mutableListOf("4"))
        val child6 = LeafNode(mutableListOf(6), mutableListOf("6"))
        val child8 = LeafNode(mutableListOf(8), mutableListOf("8"))
        val child10 = LeafNode(mutableListOf(10), mutableListOf("10"))
        val child12 = LeafNode(mutableListOf(12), mutableListOf("12"))
        val child14 = LeafNode(mutableListOf(14), mutableListOf("14"))

        val parent0 = InternalNode(mutableListOf(2), mutableListOf(child0, child2))
        val parent1 = InternalNode(mutableListOf(6), mutableListOf(child4, child6))
        val parent2 = InternalNode(mutableListOf(10), mutableListOf(child8, child10))
        val parent3 = InternalNode(mutableListOf(14), mutableListOf(child12, child14))

        val grandParent0 = InternalNode(mutableListOf(4), mutableListOf(parent0, parent1))
        val grandParent1 = InternalNode(mutableListOf(12), mutableListOf(parent2, parent3))

        val root = InternalNode(mutableListOf(8), mutableListOf(grandParent0, grandParent1))

        init {
            setNeighborsMulti(child0, child2, child4, child6, child8, child10, child12, child14)
            setNeighborsMulti(parent0, parent1, parent2, parent3)
            setNeighbors(grandParent0, grandParent1)
        }

        @Test
        fun `when deleting value from leaf with single value, it should borrow from right sibling`() {
            // Deleting 4 from this tree:
            //                   [  8  ]                                            [  8  ]
            //                /           \                                      /           \
            //         [ 4 ]                 [ 12 ]                       [ 4 ]                 [ 12 ]
            //        /     \               /      \          =>         /     \               /      \
            //   [ 2 ]      [ 6 ]      [ 10 ]      [ 14 ]           [ 2 ]      [ 7 ]      [ 10 ]      [ 14 ]
            //   /   \      /   \      /    \      /    \           /   \      /   \      /    \      /    \
            //  0    2|3   4    6|7   8     10    12     14        0    2|3   6     7    8     10    12     14
            child2.keys.add(3); child2.values.add("3")
            child6.keys.add(7); child6.values.add("7")

            root.remove(4, degree)

            assertThat(child0).containsExactly(0 to "0")
            assertThat(child2).containsExactly(2 to "2", 3 to "3")
            assertThat(child4).containsExactly(6 to "6")
            assertThat(child6).containsExactly(7 to "7")
            assertThat(child8).containsExactly(8 to "8")
            assertThat(child10).containsExactly(10 to "10")
            assertThat(child12).containsExactly(12 to "12")
            assertThat(child14).containsExactly(14 to "14")

            assertThat(parent0).containsExactlyKeys(2).andChildren(child0, child2)
            assertThat(parent1).containsExactlyKeys(7).andChildren(child4, child6)
            assertThat(parent2).containsExactlyKeys(10).andChildren(child8, child10)
            assertThat(parent3).containsExactlyKeys(14).andChildren(child12, child14)

            assertThat(grandParent0).containsExactlyKeys(4).andChildren(parent0, parent1)
            assertThat(grandParent1).containsExactlyKeys(12).andChildren(parent2, parent3)

            assertThat(root).containsExactlyKeys(8).andChildren(grandParent0, grandParent1)
        }

        @Test
        fun `when deleting value from leaf with single value, it should borrow from left sibling`() {
            // Deleting 6 from this tree:
            //                   [  8  ]                                            [  8  ]
            //                /           \                                      /           \
            //         [ 4 ]                 [ 12 ]                       [ 4 ]                 [ 12 ]
            //        /     \               /      \          =>         /     \               /      \
            //   [ 2 ]      [ 6 ]      [ 10 ]      [ 14 ]           [ 2 ]      [ 5 ]      [ 10 ]      [ 14 ]
            //   /   \      /   \      /    \      /    \           /   \      /   \      /    \      /    \
            //  0     2   4|5    6    8|9   10    12     14        0     2    4     5    8|9   10    12     14
            child4.keys.add(5); child4.values.add("5")
            child8.keys.add(9); child8.values.add("9")

            root.remove(6, degree)

            assertThat(child0).containsExactly(0 to "0")
            assertThat(child2).containsExactly(2 to "2")
            assertThat(child4).containsExactly(4 to "4")
            assertThat(child6).containsExactly(5 to "5")
            assertThat(child8).containsExactly(8 to "8", 9 to "9")
            assertThat(child10).containsExactly(10 to "10")
            assertThat(child12).containsExactly(12 to "12")
            assertThat(child14).containsExactly(14 to "14")

            assertThat(parent0).containsExactlyKeys(2).andChildren(child0, child2)
            assertThat(parent1).containsExactlyKeys(5).andChildren(child4, child6)
            assertThat(parent2).containsExactlyKeys(10).andChildren(child8, child10)
            assertThat(parent3).containsExactlyKeys(14).andChildren(child12, child14)

            assertThat(grandParent0).containsExactlyKeys(4).andChildren(parent0, parent1)
            assertThat(grandParent1).containsExactlyKeys(12).andChildren(parent2, parent3)

            assertThat(root).containsExactlyKeys(8).andChildren(grandParent0, grandParent1)
        }

        @Test
        fun `when deleting value from leaf and it cannot borrow, it should merge and its parent should borrow left`() {
            // Deleting 4 from this tree: the then empty leaf should merge into 6, then its parent should borrow leaf 3
            //                   [  8  ]                                            [  8  ]
            //                /           \                                      /           \
            //         [ 4 ]                 [ 12 ]                       [ 3 ]                 [ 12 ]
            //        /     \               /      \          =>         /     \               /      \
            //   [ 2|3 ]    [ 6 ]      [ 10 ]      [ 14 ]           [ 2 ]      [ 6 ]      [ 10 ]      [ 14 ]
            //   /  |  \    /   \      /    \      /    \           /   \      /   \      /    \      /    \
            //  0   2   3   4    6    8     10    12     14        0     2    3     6    8     10    12     14
            val child3 = LeafNode(mutableListOf(3), mutableListOf("3"))
            parent0.keys.add(3); parent0.children.add(child3)
            setNeighborsMulti(child2, child3, child4)

            root.remove(4, degree)

            assertThat(child0).containsExactly(0 to "0")
            assertThat(child2).containsExactly(2 to "2")
            assertThat(child3).containsExactly(3 to "3")
            assertThat(child6).containsExactly(6 to "6")
            assertThat(child8).containsExactly(8 to "8")
            assertThat(child10).containsExactly(10 to "10")
            assertThat(child12).containsExactly(12 to "12")
            assertThat(child14).containsExactly(14 to "14")

            assertThat(parent0).containsExactlyKeys(2).andChildren(child0, child2)
            assertThat(parent1)
//                .containsExactlyKeys(6)  // TODO: is this bad or not?
                .andChildren(child3, child6)
            assertThat(parent2).containsExactlyKeys(10).andChildren(child8, child10)
            assertThat(parent3).containsExactlyKeys(14).andChildren(child12, child14)

            assertThat(grandParent0).containsExactlyKeys(3).andChildren(parent0, parent1)
            assertThat(grandParent1).containsExactlyKeys(12).andChildren(parent2, parent3)

            assertThat(root).containsExactlyKeys(8).andChildren(grandParent0, grandParent1)
        }

        @Test
        fun `when deleting value from leaf and it cannot borrow, it should merge and its parent should borrow right`() {
            // Deleting 2 from this tree: the then empty leaf should merge into 0, then its parent should borrow leaf 4
            //                   [  8  ]                                            [  8  ]
            //                /           \                                      /           \
            //         [ 4 ]                 [ 12 ]                       [ 6 ]                 [ 12 ]
            //        /     \               /      \          =>         /     \               /      \
            //   [ 2 ]    [ 6|7 ]      [ 10 ]      [ 14 ]           [ 4 ]      [ 7 ]      [ 10 ]      [ 14 ]
            //   /   \    /  |  \      /    \      /    \           /   \      /   \      /    \      /    \
            //  0     2   4  6   7    8     10    12     14        0     4    6     7    8     10    12     14
            val child7 = LeafNode(mutableListOf(7), mutableListOf("7"))
            parent1.keys.add(7); parent1.children.add(child7)
            setNeighborsMulti(child6, child7, child8)

            root.remove(2, degree)

            assertThat(child0).containsExactly(0 to "0")
            assertThat(child4).containsExactly(4 to "4")
            assertThat(child6).containsExactly(6 to "6")
            assertThat(child7).containsExactly(7 to "7")
            assertThat(child8).containsExactly(8 to "8")
            assertThat(child10).containsExactly(10 to "10")
            assertThat(child12).containsExactly(12 to "12")
            assertThat(child14).containsExactly(14 to "14")

            assertThat(parent0).containsExactlyKeys(4).andChildren(child0, child4)
            assertThat(parent1).containsExactlyKeys(7).andChildren(child6, child7)
            assertThat(parent2).containsExactlyKeys(10).andChildren(child8, child10)
            assertThat(parent3).containsExactlyKeys(14).andChildren(child12, child14)

            assertThat(grandParent0).containsExactlyKeys(6).andChildren(parent0, parent1)
            assertThat(grandParent1).containsExactlyKeys(12).andChildren(parent2, parent3)

            assertThat(root).containsExactlyKeys(8).andChildren(grandParent0, grandParent1)
        }

        @Test
        fun `when deleting value, if leaf and its parent can't borrow both merge and grandparent should borrow right`() {
            // Deleting 6 from this tree: merge empty leaf into 4, merge parent into left, and grandparent borrows right
            //                   [  8  ]                                                  [  12  ]
            //                /           \                                            /           \
            //         [ 4 ]                 [ 12 | 16 ]                        [ 8 ]                 [ 16 ]
            //        /     \               /     |     \           =>         /     \               /      \
            //   [ 2 ]    [ 6 ]      [ 10 ]     [ 14 ]    [ 18 ]          [ 2|4 ]    [ 10 ]      [ 14 ]      [ 18 ]
            //   /   \    /   \      /    \     /    \    /    \          /  |  \    /   \      /    \      /    \
            //  0     2   4    6    8     10   12    14  16    18        0   2   4  8    10    12    14    16     18
            val child16 = LeafNode(mutableListOf(16), mutableListOf("16"))
            val child18 = LeafNode(mutableListOf(18), mutableListOf("18"))
            val parent4 = InternalNode(mutableListOf(18), mutableListOf(child16, child18))
            grandParent1.keys.add(16); grandParent1.children.add(parent4)
            setNeighborsMulti(child14, child16, child18)
            setNeighborsMulti(parent3, parent4)

            root.remove(6, degree)

            assertThat(child0).containsExactly(0 to "0")
            assertThat(child2).containsExactly(2 to "2")
            assertThat(child4).containsExactly(4 to "4")
            assertThat(child8).containsExactly(8 to "8")
            assertThat(child10).containsExactly(10 to "10")
            assertThat(child12).containsExactly(12 to "12")
            assertThat(child14).containsExactly(14 to "14")
            assertThat(child16).containsExactly(16 to "16")
            assertThat(child18).containsExactly(18 to "18")

            assertThat(parent0).containsExactlyKeys(2, 4).andChildren(child0, child2, child4)  // parent1 merged into this
            assertThat(parent2).containsExactlyKeys(10).andChildren(child8, child10)
            assertThat(parent3).containsExactlyKeys(14).andChildren(child12, child14)
            assertThat(parent4).containsExactlyKeys(18).andChildren(child16, child18)

            assertThat(grandParent0).containsExactlyKeys(8).andChildren(parent0, parent2)
            assertThat(grandParent1).containsExactlyKeys(16).andChildren(parent3, parent4)

            assertThat(root).containsExactlyKeys(12).andChildren(grandParent0, grandParent1)
        }

        @Test
        fun `when deleting value, if leaf and its parent can't borrow both merge and grandparent should borrow left`() {
            // Deleting 10 from this tree: merge empty leaf into 8, merge parent into right, & grandparent borrows left
            //                         [  8  ]                                           [  4  ]
            //                      /           \                                     /           \
            //          [ 2 | 4 ]                  [ 12 ]                      [ 2 ]                 [ 8 ]
            //        /     |     \               /      \          =>        /     \               /      \
            //  [ 1 ]     [ 3 ]    [ 6 ]      [ 10 ]      [ 14 ]          [ 1 ]     [ 3 ]     [ 6 ]     [ 12|14 ]
            //  /   \     /   \    /   \      /    \      /    \          /   \     /   \     /   \     /   |   \
            //  0    1    2    3   4    6    8     10    12    14        0     1   2     3   4     6   8    12   14
            val child1 = LeafNode(mutableListOf(1), mutableListOf("1"))
            val child3 = LeafNode(mutableListOf(3), mutableListOf("3"))
            val parentMinus1 = InternalNode(mutableListOf(1), mutableListOf(child0, child1))
            parent0.keys[0] = 3; parent0.children[0] = child2; parent0.children[1] = child3
            grandParent0.keys.addFirst( 2); grandParent0.children.addFirst(parentMinus1)
            setNeighborsMulti(child0, child1, child2, child3, child4)
            setNeighborsMulti(parentMinus1, parent0)

            root.remove(10, degree)

            assertThat(child0).containsExactly(0 to "0")
            assertThat(child1).containsExactly(1 to "1")
            assertThat(child2).containsExactly(2 to "2")
            assertThat(child3).containsExactly(3 to "3")
            assertThat(child4).containsExactly(4 to "4")
            assertThat(child6).containsExactly(6 to "6")
            assertThat(child8).containsExactly(8 to "8")  // the now empty child10 is merged into this
            assertThat(child12).containsExactly(12 to "12")
            assertThat(child14).containsExactly(14 to "14")

            assertThat(parentMinus1).containsExactlyKeys(1).andChildren(child0, child1)
            assertThat(parent0).containsExactlyKeys(3).andChildren(child2, child3)
            assertThat(parent1).containsExactlyKeys(6).andChildren(child4, child6)
            assertThat(parent3).containsExactlyKeys(12, 14).andChildren(child8, child12, child14) // parent2 merged into this

            assertThat(grandParent0).containsExactlyKeys(2).andChildren(parentMinus1, parent0)
            assertThat(grandParent1).containsExactlyKeys(8).andChildren(parent1, parent3)

            assertThat(root).containsExactlyKeys(4).andChildren(grandParent0, grandParent1)
        }

    }


    private fun <K : Comparable<K>, V> setNeighbors(left: InternalNode<K, V>, right: InternalNode<K, V>){
        left.rightNeighbor = right
        right.leftNeighbor = left
    }

    private fun <K : Comparable<K>, V> setNeighbors(left: LeafNode<K, V>, right: LeafNode<K, V>){
        left.rightNeighbor = right
        right.leftNeighbor = left
    }

    private fun <K : Comparable<K>, V> setNeighborsMulti(vararg nodes: LeafNode<K, V>){
        nodes.toList().zipWithNext { left, right -> setNeighbors(left, right) }
    }

    private fun <K : Comparable<K>, V> setNeighborsMulti(vararg nodes: InternalNode<K, V>){
        nodes.toList().zipWithNext { left, right -> setNeighbors(left, right) }
    }
}
