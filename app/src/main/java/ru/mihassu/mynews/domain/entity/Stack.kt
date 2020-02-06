@file:JvmName("Stack")

package ru.mihassu.mynews.domain.entity

class Stack<T> : Iterable<T> {

    private val items = mutableListOf<T>()

    override fun iterator(): Iterator<T> {
        return items.iterator()
    }

    fun isEmpty(): Boolean = this.items.isEmpty()

    fun count(): Int = this.items.count()

    fun push(element: T) {
        val position = this.count()
        this.items.add(position, element)
    }

    fun pop(): T? {
        var result: T? = null
        if (!this.isEmpty()) {
            val item = this.items.count() - 1
            result = this.items.removeAt(item)
        }
        return result;
    }

    fun peek(): T? {

        var result: T? = null
        if (!this.isEmpty()) {
            result = this.items[this.items.count() - 1]
        }
        return result;
    }

    fun clear() {
        items.clear()
    }

    override fun toString() = this.items.toString()
}