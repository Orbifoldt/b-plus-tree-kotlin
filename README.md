| :exclamation:  Documentation is a work in progress! |
|-----------------------------------------------------|

# B+ Tree
_Definition:_ A B+ tree of order `M` is a [`M`-way tree](https://en.wikipedia.org/wiki/M-ary_tree) that satisfies the following properties:
1. _Balanced:_ every leaf is at the same depth
2. The root node has at least 2 and at most `M` children, unless the tree is empty.
3. Any internal node has at least `⌈M/2⌉ - 1` and at most `M` children

To each internal node we associate `k` keys, where `k` is such that the number of children of a node equals `k + 1`. These keys are [_well-ordered_](https://en.wikipedia.org/wiki/Well-order) and all distinct. 

[//]: # (4. Any leaf node has at least `⌈M/2⌉ - 1` and at most `M - 1` children)

Source: https://15445.courses.cs.cmu.edu/fall2021/notes/07-trees.pdf




Insertion example:
TODO

