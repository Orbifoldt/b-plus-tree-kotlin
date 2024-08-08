import org.assertj.core.api.Assertions.assertThat
import org.example.BPTree
import org.example.InternalNode
import org.example.LeafNode
import org.junit.jupiter.api.Test

class BPTreeTest {

    @Test
    fun `Should be able to create an empty B+ Tree`() {
        val tree = BPTree(3)
        val values = tree.listValues()
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

        val values = tree.listValues()
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

        val values = tree.listValues()

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

        val values = tree.listValues()

        val expectedValues = keys.map { "$it" }
        assertThat(values).containsExactlyElementsOf(expectedValues)
        tree.validate()
    }

    // TODO: test with even degree
}