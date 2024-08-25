package org.example

internal fun <T> Collection<T>.indexOfOrNull(element: T): Int? {
    val index = indexOf(element)
    return if(index >= 0) index else null
}

/**
 * Inserts all elements of this collection into the specified collection [other], and then
 * clears all elements of this collection.
 */
internal fun <T> MutableCollection<T>.moveInto(other: MutableCollection<T>) {
    other.addAll(this)
    this.clear()
}

/**
 * Inserts all elements of this list into the specified list [other] at the specified [index], and then
 * clears all elements of this list.
 */
internal fun <T> MutableList<T>.moveInto(other: MutableList<T>, index: Int) {
    other.addAll(index, this)
    this.clear()
}