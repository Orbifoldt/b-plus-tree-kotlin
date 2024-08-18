package org.example


fun main(){
    val x = bPTree(degree = 3, 8) {
        internal(4) {
            internal(2) {
                leaf(0 to "0")
                leaf { keyValue(2, "2") }
            }
            internal(6) {
                leaf(4 to "4", 5 to "5")
                leaf {
                    keyValue(6, "6")
                    keyValue(7, "7")
                }
            }
        }
        internal(12) {
            internal(10) {
                leaf { keyValue(8, "8") }
                leaf { keyValue(10, "10") }
            }
            internal(14) {
                leaf { keyValue(12, "12") }
                leaf { keyValue(14, "14") }
            }
        }
    }
    println(x)
}

fun bPTree(degree: Int, vararg keys: Int, f: BPTreeBuilder.() -> Unit) =
    BPTreeBuilder(degree, *keys)
        .apply(f)
        .build()

class BPTreeBuilder internal constructor(private val degree: Int, vararg keys: Int) {
    private val root = InternalNodeBuilder(degree, *keys, isRoot = true)

    fun build() = BPTree(degree, root.build())

    fun internal(vararg keys: Int, f: InternalNodeBuilder.() -> Unit){
        root.internal(*keys) {
            f()
        }
    }
}

internal sealed interface NodeBuilder {
    fun build(): Node
}

class InternalNodeBuilder internal constructor(
    private val degree: Int,
    vararg keys: Int,
    private val isRoot: Boolean = false,
): NodeBuilder {
    private val keys = mutableListOf<Int>().apply { addAll(keys.toList()) }
    private val children = mutableListOf<NodeBuilder>()

    fun internal(vararg keys: Int, f: InternalNodeBuilder.() -> Unit){
        children += InternalNodeBuilder(degree, *keys)
            .apply(f)
    }

    fun leaf(f: LeafBuilder.() -> Unit){
        this.children += LeafBuilder(degree).apply(f)
    }

    fun leaf(vararg keyValuePairs: Pair<Int, String>){
        children.add(LeafBuilder(degree, keyValuePairs.toList()))
    }

    override fun build(): InternalNode {
        val children: MutableList<Node> = if(children.all { it is InternalNodeBuilder }){
             children.filterIsInstance<InternalNodeBuilder>()  // to please the compiler
                 .map(InternalNodeBuilder::build)
                 .also { it.zipWithNext { left, right -> left.rightNeighbor = right; right.leftNeighbor = left } }
                 .toMutableList()
        } else if(children.all { it is LeafBuilder }){
            children.filterIsInstance<LeafBuilder>()  // to please the compiler
                 .map(LeafBuilder::build)
                 .also { it.zipWithNext { left, right -> left.rightNeighbor = right; right.leftNeighbor = left } }
                 .toMutableList()
        } else throw IllegalArgumentException("Node may only contain one type of nodes, found both internal and leaves")

        return InternalNode(keys, children).also { it.checkIsValid(degree, isRoot) }
    }
}

class LeafBuilder internal constructor(
    private val degree: Int,
    keyValuePairs: List<Pair<Int, String>> = emptyList(),
): NodeBuilder {
    private val keys = mutableListOf<Int>().apply { addAll(keyValuePairs.map { (key, _) -> key }) }
    private val values = mutableListOf<String>().apply { addAll(keyValuePairs.map { (_, value) -> value }) }

    fun keyValue(key: Int, value: String){
        keys.add(key)
        values.add(value)
    }

    override fun build(): LeafNode {
        keys.sort()
        return LeafNode(keys, values).also { it.checkIsValid(degree, false) }
    }
}
