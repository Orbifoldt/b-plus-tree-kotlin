| :exclamation:  Documentation is a work in progress! |
|-----------------------------------------------------|

<!-- TOC -->
* [B+ Tree definition](#b-tree-definition)
* [B+ Tree operations](#b-tree-operations)
  * [Lookup](#lookup)
    * [Lookup example](#lookup-example)
    * [Implementation: lookup](#implementation-lookup)
  * [Insertion](#insertion)
    * [Insertion example](#insertion-example)
  * [Deletion](#deletion)
    * [Deletion examples](#deletion-examples)
    * [Implementation: delete in `LeafNode`](#implementation-delete-in-leafnode)
    * [Implementation: delete in `InternalNode`](#implementation-delete-in-internalnode)
    * [Implementation: Delete in the tree itself](#implementation-delete-in-the-tree-itself)
<!-- TOC -->

# B+ Tree definition
_Definition:_ A B+ tree of order `M` is a [`M`-way tree](https://en.wikipedia.org/wiki/M-ary_tree) that satisfies the following properties:
1. _Balanced:_ every leaf is at the same depth
2. The root node has at least 2 and at most `M` children, unless the tree contains exactly 0 or 1 value.
3. Any internal node has at least `⌈M/2⌉` and at most `M` children
4. Any leaf node has at least `⌈M/2⌉ - 1` and at most `M - 1` values


To each node we'll also assign keys. If an internal node has `C` children, then it has exactly `C - 1` keys and, consequently, it has at least `⌈M/2⌉ - 1` and at most `M - 1` keys.


Source: https://15445.courses.cs.cmu.edu/fall2021/notes/07-trees.pdf

# B+ Tree operations
A B+ tree supports all operations you would find in a map, like lookup, insertion and deletion, but their implementations will of course need to ensure that the tree remains a valid B+ tree.

## Lookup
To find the value associated to a key, we'll need to traverse the tree downwards.

### Lookup example
See the following thorough video: [YouTube - B+ Tree Basics 1 - Douglas Fisher](https://youtu.be/CYKRMz8yzVU?si=lt4oFuLoguf5RduQ&t=146)

### Implementation: lookup
First, we define this helper function that finds the index of the child that should contain the key we're trying to find:
```kotlin
fun Node.findIndexOf(key: Int): Int = keys.withIndex()
    .firstNotNullOfOrNull { if (it.value > key) it.index else null } 
    ?: keys.size
```
Or, in other words, this simply finds the index of the first key that is larger than our sought after `key`, or `keys.size` if nothing is larger. NB: a more efficient implementation would be to use binary search.

Then, we'll recursively call below `get(key)` method on the child at the index returned by the helper function until we head a leaf. So, on `InternalNode`s we have:
```kotlin
class InternalNode : Node {
    val keys: List<Int>
    val children: List<Node>
    
    fun get(key: Int): String? = children[findIndexOf(key)].get(key)
}
```
When we finally hit a `LeafNode`, we use this method to return the value corresponding to `key`, or null if `key` was not found:
```kotlin
class LeafNode : Node {
    val keys: List<Int>
    val values: List<String>
    
    fun get(key: Int): String? {
        val index = keys.indexOf(key)
        return if (index >= 0) values[index] else null
    }
}
```

## Insertion
TODO: using the lookup algorithm, find the leaf that should the key-value pair. If leaf overflows, we split it. (or move value to siblings?)

### Insertion example
See [YouTube - B+ Trees Basics 2 (insertion) - Douglas Fisher](https://www.youtube.com/watch?v=_nY8yR6iqx4). For the slightly more involved process splitting of internal nodes, see [timestamp `2:57`](https://www.youtube.com/watch?v=_nY8yR6iqx4&t=177). Do note that they only mention the splitting of nodes when they overflow. Additionally, in such a scenario, values or children of a node can also be moved to siblings of that node, for example see: [YouTube - B+Tree Insertions - Stephan Burroughs](https://www.youtube.com/watch?v=h6Mw7_S4ai0).


## Deletion
Deletion of a key and its associated value requires you to first look up the leaf it resides in (as laid out above), whereafter you simply delete the key and its value. Then, one of two situations occurs:
- There are still more than `⌈M/2⌉ - 1` values, so nothing needs to happen.
- The leaf underflows and has less than `⌈M/2⌉ - 1` values, thus the tree requires rebalancing.

To rebalance, we'll need to either borrow some value from neighboring sibling leaves, or merge the leaf into one of those siblings. Consequently, its parent could then also underflow, so we'll need to repeat this borrowing and merging process recursively back up the tree until we hit the root. In the case that the root ends up with a single child, this child will be promoted to be the new root.

To rebalance the tree, these steps are followed:
1. If the node has a left sibling, try to borrow from it.
2. If the left sibling itself has exactly the minimum number of keys (`⌈M/2⌉ - 1`), then merge the current node into that sibling.
3. Else, if the node has a right sibling, try to borrow from that one.
4. If the right sibling has exactly the minimum number of keys (`⌈M/2⌉ - 1`), then merge the current node into that sibling.
5. Else, if the node has no siblings, it must be the root. If it has only a single child, we'll promote that child to be the new root.

| :exclamation:  In borrowing or merging we only look at siblings, i.e. neighboring nodes that share the _same_ parent! |
|-----------------------------------------------------------------------------------------------------------------------|

So, if some key-value pair needs to be deleted from a tree, we'll start at the root and move downwards to delete it from the leaf, after which we move back up while borrowing and merging whenever nodes are under-occupied, and finally possibly re-assigning the root of the tree. 

### Deletion examples
See [YouTube - B+Tree Deletions - Stephan Burroughs](https://www.youtube.com/watch?v=QrbaQDSuxIM) for a plethora of examples. Do note that they use slightly different conventions for the number of allowed values in the leafs. Also, at `1:09` they state that only the left most nodes look to the right for borrowing or merging; in our conventions the more correct statement is that only the left most children of any given node have to look right to borrow or merge (that is, a node only looks at its siblings and not at other neighbors).

### Implementation: delete in `LeafNode`
Deletion of the actual key and its corresponding value is dealt with by the `LeafNode`s, so we can simply do: 
```kotlin
fun remove(key: Int, degree: Int): String? {
    val index = keys.indexOfOrNull(key) ?: return null
    keys.removeAt(index)
    return values.removeAt(index)
}
```
Here, `key` is the key which we want to remove and `degree` is the degree of our tree (this is unused in here).

### Implementation: delete in `InternalNode`
Since we need to consider siblings in borrowing and merging process, it makes it easier to implement if we hand this responsibility over to the parent. So, in particular, this only has to be implemented in the `InternalNode`s. The general structure that we will adhere to is:
```kotlin
fun remove(key: Int, degree: Int): String? {
    // 1. Locate child containing the key, and (recursively) delete from it
    // 2. Check if there was an underflow in that child
    // 3. If so, deal with underflow
}
```

The first two things are straightforward to implement:
```kotlin
fun remove(key: Int, degree: Int): String? {
    val index = findIndexOf(key)
    val child = children[index]
    val value = child.remove(key, degree) 
        ?: return null  // If nothing was deleted just return null

    if(child.keys.size >= minKeys(degree)) return value  // No underflow, so no merge/borrow
    if(keys.size == 0) return value  // We're at the root, so no need to handle underflow
    
    TODO("deal with underflow")
}
```
If we reach this last line of code in the function, it means that an underflow has occurred that needs to be dealt with. Now, we'll need to distinguish several different cases:
1. The `child` is a `LeafNode`
   1. [Borrow key-value pair from its left sibling](#1i-borrow-from-left-sibling-leaf)
   2. [Merge all keys and values into its left sibling](#1ii-merge-into-left-sibling-leaf)
   3. [Borrow key-value pair from its right sibling](#1iii-borrow-from-right-sibling-leaf)
   4. [Merge all keys and values into its right sibling](#1iv-merge-into-right-sibling-leaf)
2. The `child` is a `InternalNode`
   1. [Borrow a child from its left sibling](#2i-borrow-from-left-internal-sibling)
   2. [Merge all children into its left sibling](#2ii-merge-into-left-internal-sibling)
   3. [Borrow a child from its right sibling](#2iii-borrow-from-right-internal-sibling)
   4. [Merge all children into its right sibling](#2iv-merge-into-right-internal-sibling)

As you can imagine, in any of these cases the keys in the siblings and their parents will need to be updated to reflect the new situation. 

#### 1.i. Borrow from left sibling leaf
![Borrowing from left sibling leaf](/bptree_borrow-left-leaf.excalidraw.svg)
To borrow from the left sibling, we simply remove the last key and value from the sibling and append them at beginning of our current child's keys and values. Since now the smallest possible valued contained by the child has changed, we also need to update the key in the parent. In code, we have:
```kotlin
val borrowedKey = leftSibling.keys.removeLast()
val borrowedValue = leftSibling.values.removeLast()
child.keys.add(0, borrowedKey)
child.values.add(0, borrowedValue)
keys[index - 1] = borrowedKey  // update parent's key corresponding to the child
```
<br>

#### 1.ii. Merge into left sibling leaf
![Merging into left sibling leaf](/bptree_merge-left-leaf.excalidraw.svg)
To merge the child into its left sibling, we can simply move all keys and value to the end of the sibling's keys and values. Then, we remove the (now empty) child and its key from the parent.
```kotlin
child.keys.moveInto(leftSibling.keys)
child.values.moveInto(leftSibling.values)
children.removeAt(index)
keys.removeAt(index - 1)  // in the parent, remove key corresponding to the child
```
<br>


#### 1.iii. Borrow from right sibling leaf
![Borrowing from right sibling leaf](/bptree_borrow-right-leaf.excalidraw.svg)
To borrow from the right sibling, we remove the first key and value from the sibling and append them at end of our current child's keys and values. Since now the smallest possible value contained by the right sibling has changed, we also need to update the key in the parent. In code, we have:
```kotlin
val borrowedKey = rightSibling.keys.removeFirst()
val borrowedValue = rightSibling.values.removeFirst()
child.keys.add(borrowedKey)
child.values.add(borrowedValue)
keys[index] = rightSibling.keys.first()  // update parent's key corresponding to right sibling
```
<br>

#### 1.iv. Merge into right sibling leaf
![Merging into right sibling leaf](/bptree_merge-right-leaf.excalidraw.svg)
To merge the child into its right sibling, we can simply move all keys and value to the beginning of the sibling's keys and values. Then, we remove the (now empty) child from the parent, but not its key: the smallest possible value contained by the right sibling has changed, but since that value will be larger or equal to the key of the (removed) child we instead in the parent remove the key corresponding to sibling. And so, the remaining key that corresponded to the child will now correspond to the right sibling. 
```kotlin
child.keys.moveInto(rightSibling.keys, index = 0)
child.values.moveInto(rightSibling.values, index = 0)
children.removeAt(index)
keys.removeAt(index)  // in the parent, remove key corresponding to the right sibling
```
<br>

#### 2.i. Borrow from left internal sibling
![Borrowing from left internal sibling](/bptree_borrow-left-internal.excalidraw.svg)
To borrow from a left sibling that is in an internal node, we simply take its last child and add it to beginning of this child's children. Since this child's smallest contained value has changed, we move down the key in the parent and add it as the first key. In that parent key's place we move up the last key of the sibling.
```kotlin
val currentKey = keys[index-1]
val siblingsLastKey = leftSibling.keys.removeLast()
val siblingsLastChild = leftSibling.children.removeLast()

child.keys.add(0, currentKey)
child.children.add(0, siblingsLastChild)
keys[index - 1] = siblingsLastKey  // move up sibling's key
```
<br>

#### 2.ii. Merge into left internal sibling
![Merge into left internal sibling](/bptree_merge-left-internal.excalidraw.svg)
To merge the internal node into its left sibling, we first take the key from the parent corresponding to the child and insert that into the sibling's keys. Then, we move all the child's keys and children into the sibling's keys and children. In this process, the key and the child are removed from the parent.
```kotlin
val currentKey = keys.removeAt(index - 1)
children.removeAt(index)

leftSibling.keys.add(currentKey)  // add parent's key
child.keys.moveInto(leftSibling.keys)
child.children.moveInto(leftSibling.children)
```
<br>

#### 2.iii. Borrow from right internal sibling
![Borrowing from right internal sibling](/bptree_borrow-right-internal.excalidraw.svg)
To borrow from a right sibling that is in an internal node, we simply take its first child and add it to end of this child's children. Since this child's largest contained value has changed, we move down the key in the parent and add it as the last key. In that parent key's place we move up the first key of the right sibling.
```kotlin
val currentKey = keys[index]
val siblingsFirstKey = rightSibling.keys.removeFirst()
val siblingsFirstChild = rightSibling.children.removeFirst()

child.keys.add(currentKey)
child.children.add(siblingsFirstChild)
keys[index] = siblingsFirstKey  // move up sibling's key
```
<br>

#### 2.iv. Merge into right internal sibling
![Merge into right internal sibling](/bptree_merge-right-internal.excalidraw.svg)
To merge the internal node into its right sibling, we first take the key from the parent corresponding to the child and insert that at the front of the sibling's keys. Then, we move all the child's keys and children in front of the sibling's keys and children. In this process, the key and the child are removed from the parent.
```kotlin
val currentKey = keys.removeAt(index)
children.removeAt(index)

rightSibling.keys.add(0, currentKey)  // prepend parent's key
child.keys.moveInto(rightSibling.keys, index = 0)
child.children.moveInto(rightSibling.children, index = 0)
```
<br>


#### Full implementation
<details>
    <summary>Click to show full implementation of the remove operation in an `InternalNode`:</summary>

    ```kotlin
    fun remove(key: Int, degree: Int): String? {
        val index = findIndexOf(key)
        val child = children[index]
        val value = child.remove(key, degree) ?: return null
    
        if(child.keys.size >= minKeys(degree)) return value  // No underflow, so no need to merge or borrow
        if(keys.size == 0) return value  // We're at the root, so no need to handle underflow
    
        val lookLeft = (index > 0)  // Only consider siblings, not general neighbors
        if(child is LeafNode){
            if(lookLeft){
                val leftSibling = child.leftNeighbor!!
                if(leftSibling.keys.size > minKeys(degree)){
                    // borrow from left sibling
                    val borrowedKey = leftSibling.keys.removeLast()
                    val borrowedValue = leftSibling.values.removeLast()
                    child.keys.add(0, borrowedKey)
                    child.values.add(0, borrowedValue)
                    keys[index - 1] = borrowedKey
                } else {
                    // merge child into left sibling
                    child.keys.moveInto(leftSibling.keys)
                    child.values.moveInto(leftSibling.values)
                    children.removeAt(index)
                    keys.removeAt(index - 1)
                }
            } else {
                val rightSibling = child.rightNeighbor!!
                if(rightSibling.keys.size > minKeys(degree)) {
                    // borrow from right sibling
                    val borrowedKey = rightSibling.keys.removeFirst()
                    val borrowedValue = rightSibling.values.removeFirst()
                    child.keys.add(borrowedKey)
                    child.values.add(borrowedValue)
                    keys[index] = rightSibling.keys.first()
                }  else {
                    // merge child into right sibling
                    child.keys.moveInto(rightSibling.keys, index = 0)
                    child.values.moveInto(rightSibling.values, index = 0)
                    children.removeAt(index)
                    keys.removeAt(index)
                }
            }
    
        } else if(child is InternalNode) {
            if(lookLeft){
                val leftSibling = child.leftNeighbor!!
                if(leftSibling.keys.size > minKeys(degree)){
                    // borrow from left sibling
                    val currentKey = keys[index-1]
                    val siblingsLastKey = leftSibling.keys.removeLast()
                    val siblingsLastChild = leftSibling.children.removeLast()
    
                    child.keys.add(0, currentKey)
                    child.children.add(0, siblingsLastChild)
                    keys[index - 1] = siblingsLastKey
                } else {
                    // merge into left sibling
                    val currentKey = keys.removeAt(index - 1)
                    children.removeAt(index)
    
                    leftSibling.keys.add(currentKey)
                    child.keys.moveInto(leftSibling.keys)
                    child.children.moveInto(leftSibling.children)
                }
            } else {
                val rightSibling = child.rightNeighbor!!
                if(rightSibling.keys.size > minKeys(degree)) {
                    // borrow from right sibling
                    val currentKey = keys[index]
                    val siblingsFirstKey = rightSibling.keys.removeFirst()
                    val siblingsFirstChild = rightSibling.children.removeFirst()
    
                    child.keys.add(currentKey)
                    child.children.add(siblingsFirstChild)
                    keys[index] = siblingsFirstKey
                } else {
                    // merge into right sibling
                    val currentKey = keys.removeAt(index)
                    children.removeAt(index)
    
                    rightSibling.keys.add(0, currentKey)
                    child.keys.moveInto(rightSibling.keys, index = 0)
                    child.children.moveInto(rightSibling.children, index = 0)
                }
            }
    
        } else throw IllegalStateException("Child node was neither leaf nor internal!")
    
        return value
    }
    ```
</details>


### Implementation: Delete in the tree itself
Now, for the tree, when we want to delete some entry we can simply delete it from the root node (which then recursively propagates downwards). Then, finally, we do need to deal with the case that the root is left with only a single child. In that case, this only child is promoted to be the new root. 
```kotlin
class BPTree {
    var root: Node
    
    fun remove(key: Int): String? {
        val result = root.remove(key, degree)

        if (root is InternalNode && root.keys.isEmpty()) {
            assert((root as InternalNode).children.size == 1) { 
                "When there are no keys left there should be exactly 1 child" 
            }
            root = (root as InternalNode).children.first()
        }
        return result
    }
}
```
Note that if the root is a leaf node, we don't reassign it. This nicely deals with the edge-cases where the tree is left with only zero or one values.