package bptree

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BPTreeBuilderTest {
    private val tree = buildBPTree(degree = 3, 8) {
        internal(4) {
            internal(1, 2) {
                leaf(0 to "0")
                leaf(1 to "1")
                leaf(2 to "2")
            }
            internal(6) {
                leaf(4 to "4", 5 to "5")
                leaf(6 to "6", 7 to "7")
            }
        }
        internal(12) {
            internal(10) {
                leaf {  // alternative syntax
                    keyValue(8 to "8")
                    keyValue(9 to "9")
                }
                leaf(10 to "10")
            }
            internal(14) {
                leaf(12 to "12")
                leaf(14 to "14")
            }
        }
    }

    @Test
    fun `Should be able to fetch values from the created tree`() {
        assertThat(tree[2]).isEqualTo("2")
        assertThat(tree[6]).isEqualTo("6")
        assertThat(tree[7]).isEqualTo("7")
        assertThat(tree[8]).isEqualTo("8")
        assertThat(tree[3]).isNull()
        assertThat(tree[666]).isNull()
    }

    @Test
    fun `Should be able to list all values in the tree`() {
        // This also verifies that the neighbor are set correctly
        assertThat(tree.values).containsExactly("0", "1", "2", "4", "5", "6", "7", "8", "9", "10", "12", "14")
    }

    @Test
    fun `Should be able to delete values from the created tree`() {
        assertThat(tree.remove(4)).isEqualTo("4")
        assertThat(tree.remove(1)).isEqualTo("1")
        assertThat(tree.remove(7)).isEqualTo("7")
        assertThat(tree.remove(3)).isNull()
        assertThat(tree.remove(4)).isNull()
        assertThat(tree.remove(0)).isEqualTo("0")
        assertThat(tree.values).containsExactly("2", "5", "6", "8", "9", "10", "12", "14")
    }

    @Nested
    inner class `While creating an invalid tree, should throw IllegalArgumentException` {
        @Test
        fun `when number of keys doesn't match number of children`() {
            assertThatThrownBy {
                buildBPTree(degree = 4, 3, 5) {
                    leaf(0 to "0")
                }
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `when a mix of leaf and internal nodes is provided`() {
            assertThatThrownBy {
                buildBPTree(degree = 3, 8) {
                    leaf(6 to "6")
                    internal(9) {
                        leaf(8 to "8")
                        leaf(33 to "33")
                    }
                }
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `when number of values per leaf is too low for the degree`() {
            assertThatThrownBy {
                buildBPTree(degree = 5, 8, 33) {
                    leaf(6 to "6")
                    leaf(8 to "8")
                    leaf(33 to "33")
                }
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `when the keys are not sorted correctly`() {
            assertThatThrownBy {
                buildBPTree(degree = 3, 33, 8) {
                    leaf(6 to "6")
                    leaf(8 to "8")
                    leaf(33 to "33")
                }
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}