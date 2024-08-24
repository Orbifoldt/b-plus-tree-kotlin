import BPTreeAssertions.assertThat
import BPTreeAssertions.hasSize
import BPTreeAssertions.isValid
import NodeAssertions.containsExactly
import NodeAssertions.containsExactlyKeys
import NodeAssertions.isEmpty
import NodeAssertions.isInternal
import NodeAssertions.isLeaf
import org.assertj.core.api.Assertions.*
import org.example.BPTree
import org.example.buildBPTree
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BPTreeTest {

    @Test
    fun `Should be able to create an empty B+ Tree`() {
        val tree = BPTree<Int, String>(3)

        assertThat(tree.isEmpty()).isTrue()
        assertThat(tree.size).isEqualTo(0)
        assertThat(tree).hasSize(0)
    }

    @Test
    fun `An empty B+ Tree should have no keys or values`() {
        val tree = BPTree<Int, String>(3)

        assertThat(tree.values).isEmpty()
        assertThat(tree.keys).isEmpty()
    }


    @Test
    fun `Should not be able to create a B+ Tree with small or negative degree`() {
        assertThatException().isThrownBy { BPTree<Int, String>(1) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatException().isThrownBy { BPTree<Int, String>(0) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatException().isThrownBy { BPTree<Int, String>(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `Should be able to insert an item into an empty tree and then fetch it again`() {
        val tree = BPTree<Int, String>(4)
        tree[7] = "7"
        assertThat(tree[7]).isEqualTo("7")
    }

    @Test
    fun `When inserting an item into an empty tree the tree should have size 1`() {
        val tree = BPTree<Int, String>(4)
        tree[7] = "7"
        assertThat(tree).hasSize(1)
    }

    @Test
    fun `Should be able to insert multiple items and then fetch them again`() {
        val tree = BPTree<Int, String>(99)

        tree[88] = "88"
        tree[5] = "5"
        tree[13] = "13"

        assertThat(tree[5]).isEqualTo("5")
        assertThat(tree[13]).isEqualTo("13")
        assertThat(tree[88]).isEqualTo("88")
        assertThat(tree).hasSize(3)
        assertThat(tree).isValid()
    }

    @Test
    fun `When listing all items in a tree they should be returned in a sorted manner`() {
        val tree = BPTree<Int, String>(99)

        tree[88] = "88"
        tree[5] = "5"
        tree[13] = "13"

        assertThat(tree.values).containsExactly("5", "13", "88")
    }

    @Test
    fun `When listing all entries in a tree they should be returned in a sorted manner`() {
        val tree = BPTree<Int, String>(99)

        tree[88] = "88"
        tree[5] = "5"
        tree[13] = "13"

        assertThat(tree.entries.map { it.key to it.value })
            .containsExactly(5 to "5", 13 to "13", 88 to "88")
    }

    @Test
    fun `Should be able to insert multiple items at once and then fetch them again`() {
        val tree = BPTree<Int, String>(99)

        tree.putAll(mapOf(88 to "88", 5 to "5", 13 to "13"))

        assertThat(tree[5]).isEqualTo("5")
        assertThat(tree[13]).isEqualTo("13")
        assertThat(tree[88]).isEqualTo("88")
        assertThat(tree.values).containsExactly("5", "13", "88")
        assertThat(tree).hasSize(3)
        assertThat(tree).isValid()
    }

    @Test
    fun `When inserting elements beyond degree should keep all elements and still remain valid`() {
        val tree = BPTree<Int, String>(degree = 3)
        val keys = 0..50 step 10
        keys.forEach {
            tree[it] = "$it"
            tree.validate()
        }

        val expectedValues = keys.map { "$it" }
        assertThat(tree.values).containsExactlyElementsOf(expectedValues)
        keys.forEach { assertThat(tree[it]).isEqualTo("$it") }
        assertThat(tree).isValid()
    }

    @Test
    fun `When inserting elements beyond degree should have correct size and not be empty`() {
        val tree = BPTree<Int, String>(degree = 6)
        val keys = 100..110
        keys.forEach {
            tree[it] = "$it"
            tree.validate()
        }

        val expectedValues = keys.map { "$it" }
        assertThat(tree.values).containsExactlyElementsOf(expectedValues)
        keys.forEach { assertThat(tree[it]).isEqualTo("$it") }

        assertThat(tree).hasSize(11)
        assertThat(tree.isEmpty()).isFalse()
    }

    @Test
    fun `When inserting elements beyond degree cubed should keep all elements and still remain valid`() {
        val tree = BPTree<Int, String>(degree = 3)

        val keys = 0..30
        keys.forEach {
            tree[it] = "$it"
            tree.validate()
        }

        val values = tree.values

        val expectedValues = keys.map { "$it" }
        assertThat(values).containsExactlyElementsOf(expectedValues)
        assertThat(tree).hasSize(31)
        assertThat(tree).isValid()
    }

    @Test
    fun `When removing a key from a single layer tree, its value should be returned`() {
        val tree = BPTree<Int, String>(degree = 3)
        tree[5] = "a"
        tree[2] = "b"

        val result = tree.remove(5)

        assertThat(result).isEqualTo("a")
        assertThat(tree.values).containsExactly("b")
    }

    @Test
    fun `When removing a key from a single layer tree, its key and value should be removed from the tree`() {
        val tree = BPTree<Int, String>(degree = 3)
        tree[5] = "a"
        tree[2] = "b"

        tree.remove(5)

        assertThat(tree.orderedKeys()).containsExactly(2)
        assertThat(tree.values).containsExactly("b")
        assertThat(tree.containsKey(5)).isFalse()
        assertThat(tree.containsValue("a")).isFalse()
    }

    @Test
    fun `When removing a key from a single layer tree, its size should be correct`() {
        val tree = BPTree<Int, String>(degree = 3)
        tree[5] = "a"
        tree[2] = "b"

        tree.remove(5)
        assertThat(tree).hasSize(1)
        assertThat(tree.isEmpty()).isFalse()
    }

    @Test
    fun `When removing a key from a single layer tree, it should remain valid`() {
        val tree = BPTree<Int, String>(degree = 3)
        tree[5] = "a"
        tree[2] = "b"

        tree.remove(5)
        assertThat(tree).isValid()
    }

    @Test
    fun `When removing a key from a multi layered tree, the key and value should be removed from the tree`() {
        val tree = BPTree<Int, String>(degree = 3)
        val keys = 0..10
        keys.forEach { tree[it] = "$it" }

        val result = tree.remove(10)

        assertThat(result).isEqualTo("10")
        assertThat(tree.orderedKeys()).containsExactlyElementsOf(keys.take(10))
        assertThat(tree.values).containsExactlyElementsOf(keys.take(10).map(Int::toString))
    }

    @Test
    fun `When removing multiple keys from a multi layered tree, the keys and values should be removed from the tree`() {
        val tree = BPTree<Int, String>(degree = 3)
        val keys = 0..10
        keys.forEach { tree[it] = "$it" }

        val result8 = tree.remove(8)
        val result9 = tree.remove(9)
        val result10 = tree.remove(10)

        assertThat(result8).isEqualTo("8")
        assertThat(result9).isEqualTo("9")
        assertThat(result10).isEqualTo("10")

        assertThat(tree.orderedKeys()).containsExactlyElementsOf(keys.take(8))
        assertThat(tree.values).containsExactlyElementsOf(keys.take(8).map(Int::toString))
    }

    @Test
    fun `When removing keys from a multi layered tree, it should remain valid`() {
        val tree = BPTree<Int, String>(degree = 3)
        val keys = 0..10
        keys.forEach { tree[it] = "$it" }

        tree.remove(8)
        tree.remove(9)
        tree.remove(10)

        assertThat(tree).isValid()
    }

    @Test
    fun `When deleting causes the root have a single child, that child should become the new root`() {
        val tree = buildBPTree(degree = 6, 10) {  // all nodes at minimum occupancy
            internal(2, 6) {
                leaf(0 to "0", 1 to "1")
                leaf(2 to "2", 4 to "4")
                leaf(6 to "6", 9 to "9")
            }
            internal(15, 17) {
                leaf(10 to "10", 14 to "14")
                leaf(15 to "15", 16 to "16")
                leaf(17 to "17", 22 to "22")
            }
        }

        tree.remove(15)

        assertThat(tree.root).isInternal()
            .containsExactlyKeys(2, 6, 10, 17)
    }

    @Test
    fun `When after deleting the root that is internal has only single leaf child, that leaf should become new root`() {
        val tree = buildBPTree(degree = 6, 2) {  // all nodes at minimum occupancy
            leaf(0 to "0", 1 to "1")
            leaf(2 to "2", 4 to "4")
        }

        tree.remove(4)

        assertThat(tree.root).isLeaf()
            .containsExactly(0 to "0", 1 to "1", 2 to "2")
        assertThat(tree).isValid()
    }

    @Test
    fun `When after deleting the root becomes a leaf it is allowed to have down to zero values`() {
        val tree = buildBPTree(degree = 6, 2) {  // all nodes at minimum occupancy
            leaf(0 to "0", 1 to "1")
            leaf(2 to "2", 4 to "4")
        }

        tree.remove(0)

        assertThat(tree.root).isLeaf()
            .containsExactly(1 to "1", 2 to "2", 4 to "4")
        assertThat(tree).isValid()

        tree.remove(2)
        assertThat(tree.root).isLeaf()
            .containsExactly(1 to "1", 4 to "4")
        assertThat(tree).isValid()

        tree.remove(4)
        assertThat(tree.root).isLeaf()
            .containsExactly(1 to "1")
        assertThat(tree).isValid()

        tree.remove(1)
        assertThat(tree.root).isLeaf().isEmpty()
        assertThat(tree).isValid()
    }

    @Test
    fun `When inserting an item containsKey and containsValue should return true`() {
        val tree = BPTree<Int, String>(4)
        tree[7] = "7"

        assertThat(tree.containsKey(7)).isTrue()
        assertThat(tree.containsValue("7")).isTrue()
    }

    @Test
    fun `When doesn't contain a key or item containsKey and containsValue should return false`() {
        val tree = BPTree<Int, String>(4)
        tree[7] = "7"

        assertThat(tree.containsKey(3)).isFalse()
        assertThat(tree.containsValue("99")).isFalse()
    }

    @Nested
    inner class `When clearing a tree` {
        private val tree = buildBPTree(degree = 8, 3) {
            leaf(0 to "0", 1 to "1", 2 to "2")
            leaf(3 to "3", 4 to "4", 19 to "19")
        }

        @Test
        fun `all keys and values should be deleted`() {
            tree.clear()
            assertThat(tree.keys).isEmpty()
            assertThat(tree.values).isEmpty()
        }

        @Test
        fun `isEmpty should return true`() {
            tree.clear()
            assertThat(tree.isEmpty()).isTrue()
        }

        @Test
        fun `the root should be an empty leaf`() {
            tree.clear()
            assertThat(tree.root).isLeaf().isEmpty()
        }

        @Test
        fun `then getting a key returns null`() {
            tree.clear()
            assertThat(tree[0]).isNull()
            assertThat(tree[19]).isNull()
            assertThat(tree[777]).isNull()
        }

        @Test
        fun `then containsKey and containsValue return false`() {
            tree.clear()
            assertThat(tree.containsKey(0)).isFalse()
            assertThat(tree.containsKey(19)).isFalse()
            assertThat(tree.containsKey(777)).isFalse()
            assertThat(tree.containsValue("0")).isFalse()
            assertThat(tree.containsValue("19")).isFalse()
            assertThat(tree.containsValue("777")).isFalse()
        }

        @Test
        fun `then tree is still valid`() {
            tree.clear()
            assertThat(tree).isValid()
        }
    }
}
