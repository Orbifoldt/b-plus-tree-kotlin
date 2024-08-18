import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.example.BPTree
import org.junit.jupiter.api.Test

class BPTreeTest {

    @Test
    fun `Should be able to create an empty B+ Tree`() {
        val tree = BPTree(3)
        val values = tree.values
        assertThat(values).isEmpty()
    }

    @Test
    fun `Should be able to insert an item into an empty tree and then fetch it again`() {
        val tree = BPTree(4)
        tree.insert(7, "7")
        assertThat(tree.get(7)).isEqualTo("7")
    }

    @Test
    fun `Should be able to insert multiple items and then fetch them again`() {
        val tree = BPTree(99)
        tree.insert(88, "88")
        tree.insert(5, "5")
        tree.insert(13, "13")

        assertThat(tree.get(5)).isEqualTo("5")
        assertThat(tree.get(13)).isEqualTo("13")
        assertThat(tree.get(88)).isEqualTo("88")
    }

    @Test
    fun `When listing all items in a tree they should be returned in a sorted manner`() {
        val tree = BPTree(99)
        tree.insert(88, "88")
        tree.insert(5, "5")
        tree.insert(13, "13")

        val values = tree.values
        assertThat(values).containsExactly("5", "13", "88")
    }

    @Test
    fun `When inserting elements beyond degree should keep all elements and still remain valid`() {
        val tree = BPTree(degree = 3)

        val keys = 0..50 step 10
        keys.forEach {
            tree.insert(it, "$it")
            println(tree)
            tree.validate()
        }

        val values = tree.values

        val expectedValues = keys.map { "$it" }
        assertThat(values).containsExactlyElementsOf(expectedValues)
        keys.forEach {
            println("getting $it")
            assertThat(tree.get(it)).isEqualTo("$it")
        }
        tree.validate()
    }

    @Test
    fun `When inserting elements beyond degree cubed should keep all elements and still remain valid`() {
        val tree = BPTree(degree = 3)

        val keys = 0..30
        keys.forEach {
            tree.insert(it, "$it")
            tree.validate()
        }

        val values = tree.values

        val expectedValues = keys.map { "$it" }
        assertThat(values).containsExactlyElementsOf(expectedValues)
        tree.validate()
    }

    @Test
    fun `When removing a key from a single layer tree, its value should be returned`(){
        val tree = BPTree(degree = 3)
        tree.insert(5, "a")
        tree.insert(2, "b")

        val result = tree.remove(5)

        assertThat(result).isEqualTo("a")
        assertThat(tree.values).containsExactly("b")
    }

    @Test
    fun `When removing a key from a single layer tree, its key and value should be removed from the tree`(){
        val tree = BPTree(degree = 3)
        tree.insert(5, "a")
        tree.insert(2, "b")

        tree.remove(5)

        assertThat(tree.orderedKeys()).containsExactly(2)
        assertThat(tree.values).containsExactly("b")
    }

    @Test
    fun `When removing a key from a multi layered tree, the key and value should be removed from the tree`(){
        val tree = BPTree(degree = 3)
        val keys = 0..10
        keys.forEach { tree.insert(it, "$it") }

        val result = tree.remove(10)

        assertThat(result).isEqualTo("10")

        assertThat(tree.orderedKeys()).containsExactlyElementsOf(keys.take(10))
        assertThat(tree.values).containsExactlyElementsOf(keys.take(10).map(Int::toString))
    }

    @Test
    fun `When removing multiple keys from a multi layered tree, the keys and values should be removed from the tree`(){
        val tree = BPTree(degree = 3)
        val keys = 0..10
        keys.forEach { tree.insert(it, "$it") }

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
    fun `When removing keys from a multi layered tree, it should remain valid`(){
        val tree = BPTree(degree = 3)
        val keys = 0..10
        keys.forEach { tree.insert(it, "$it") }

        tree.remove(8)
        tree.remove(9)
        tree.remove(10)

        assertThatNoException().isThrownBy { tree.validate() }
    }

    // TODO: test with even degree
}