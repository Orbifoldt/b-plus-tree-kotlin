| :exclamation:  Documentation is a work in progress! |
|-----------------------------------------------------|

<!-- TOC -->
* [B+ Tree definition](#b-tree-definition)
* [B+ Tree operations](#b-tree-operations)
  * [Finding a key](#finding-a-key)
  * [Insertion](#insertion)
    * [Insertion example:](#insertion-example)
  * [Deletion](#deletion)
    * [Implementation](#implementation)
      * [Delete in `LeafNode`](#delete-in-leafnode)
      * [Delete in `InternalNode`](#delete-in-internalnode)
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
A B+ tree supports all operations you would find in a map, but their implementations will of course need to ensure that the tree remains a valid B+ tree.

## Finding a key
TODO: recursively binary search keys and then go to corresponding child

## Insertion
TODO: using the find algorithm, find the leaf that should the key-value pair. If leaf overflows, we split it. (or move value to siblings?)

### Insertion example:
TODO


## Deletion
Deletion of a key and its associated value requires you to first find the leaf it resided in (as laid out above). Then, you simply delete the key and its value. Now, one of two situations occurs:
- There are still more than `⌈M/2⌉ - 1` values, so nothing needs to happen.
- The leaf underflows and has less than `⌈M/2⌉ - 1` values, so the tree needs to be rebalanced.

To rebalance, we'll need to either borrow some value from neighboring leaves, or merge the leaf into one of its neighbors. Consequently, its parent could then also underflow, so we repeat this borrowing and merging process recursively back up the tree until we hit the root. In the case that the root ends up with a single child, this child will be promoted to be the new root.

To rebalance the tree, these steps are followed:
1. If the node has a left sibling, try to borrow from it.
2. If the left sibling itself has exactly the minimum number of keys (`⌈M/2⌉ - 1`), then merge the current node into that sibling.
3. Else, if the node has a right sibling, try to borrow from that one.
4. If the right sibling has exactly the minimum number of keys (`⌈M/2⌉ - 1`), then merge the current node into that sibling.
5. Else, if the node has no siblings, it must be the root. If it has a child, we'll promote that child to be the new root.

| :exclamation:  In borrowing or merging we only look at siblings, i.e. neighboring nodes that share the _same_ parent! |
|-----------------------------------------------------------------------------------------------------------------------|

### Implementation

#### Delete in `LeafNode`
Deletion of the actual key and its corresponding value is dealt with by the `LeafNode`s, so we can simply do: 
```kotlin
fun remove(key: Int, degree: Int): String? {
    val index = keys.indexOfOrNull(key) ?: return null
    keys.removeAt(index)
    return values.removeAt(index)
}
```
Here, `key` is the key which we want to remove and `degree` is the degree of our tree (this is unused in here).

#### Delete in `InternalNode`
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
    val value = child.remove(key, degree) ?: return null  // If nothing was deleted just return null

    if(child.keys.size >= minKeys(degree)) return value  // No underflow, so no need to merge or borrow
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

##### 1.i. Borrow from left sibling leaf
To borrow from the left sibling, we simply remove the last key and value from the sibling and append them at beginning of our current child's keys and values. Since now the smallest possible valued contained by the child has changed, we also need to update the key in the parent. In code, we have:
```kotlin
val borrowedKey = leftSibling.keys.removeLast()
val borrowedValue = leftSibling.values.removeLast()
child.keys.add(0, borrowedKey)
child.values.add(0, borrowedValue)
keys[index - 1] = borrowedKey  // update parent's key corresponding to the child
```
![Borrowing from left sibling leaf](/bptree_borrow-left-leaf.excalidraw.svg)

##### 1.ii. Merge into left sibling leaf
To merge the child into its left sibling, we can simply move all keys and value to the end of the sibling's keys and values. Then, we remove the (now empty) child and its key from the parent.
```kotlin
child.keys.moveInto(leftSibling.keys)
child.values.moveInto(leftSibling.values)
children.removeAt(index)
keys.removeAt(index - 1)  // in the parent, remove key corresponding to the child
```
![TODO: image]()


##### 1.iii. Borrow from right sibling leaf
To borrow from the right sibling, we remove the first key and value from the sibling and append them at end of our current child's keys and values. Since now the smallest possible value contained by the right sibling has changed, we also need to update the key in the parent. In code, we have:
```kotlin
val borrowedKey = rightSibling.keys.removeFirst()
val borrowedValue = rightSibling.values.removeFirst()
child.keys.add(borrowedKey)
child.values.add(borrowedValue)
keys[index] = rightSibling.keys.first()  // update parent's key corresponding to the right sibling
```
![Borrowing from right sibling leaf](/bptree_borrow-right-leaf.excalidraw.svg)

##### 1.iv. Merge into right sibling leaf
To merge the child into its right sibling, we can simply move all keys and value to the beginning of the sibling's keys and values. Then, we remove the (now empty) child from the parent, but not its key: the smallest possible value contained by the right sibling has changed, but since that value will be larger or equal to the key of the (removed) child we instead in the parent remove the key corresponding to sibling. And so, the remaining key that corresponded to the child will now correspond to the right sibling. 
```kotlin
child.keys.moveInto(rightSibling.keys, index = 0)
child.values.moveInto(rightSibling.values, index = 0)
children.removeAt(index)
keys.removeAt(index)  // in the parent, remove key corresponding to the right sibling
```
![TODO: image]()

##### 2.i. Borrow from left internal sibling
```kotlin
val currentKey = keys[index-1]
val siblingsLastKey = leftSibling.keys.removeLast()
val siblingsLastChild = leftSibling.children.removeLast()

child.keys.add(0, currentKey)
child.children.add(0, siblingsLastChild)
keys[index - 1] = siblingsLastKey
```
![TODO: image]()

##### 2.ii. Merge into left internal sibling
```kotlin
val currentKey = keys.removeAt(index - 1)
children.removeAt(index)
leftSibling.keys.add(currentKey)
child.keys.moveInto(leftSibling.keys)
child.children.moveInto(leftSibling.children)
```
![TODO: image]()

##### 2.iii. Borrow from right internal sibling
```kotlin
val currentKey = keys[index]
val siblingsFirstKey = rightSibling.keys.removeFirst()
val siblingsFirstChild = rightSibling.children.removeFirst()

child.keys.add(currentKey)
child.children.add(siblingsFirstChild)
keys[index] = siblingsFirstKey
```
![TODO: image]()

##### 2.iv. Merge into right internal sibling
```kotlin
val currentKey = keys.removeAt(index)
children.removeAt(index)

rightSibling.keys.add(0, currentKey)
child.keys.moveInto(rightSibling.keys, index = 0)
child.children.moveInto(rightSibling.children, index = 0)
```
![TODO: image]()


##### Full implementation
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


