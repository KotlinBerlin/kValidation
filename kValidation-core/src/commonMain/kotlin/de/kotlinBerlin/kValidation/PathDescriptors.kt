package de.kotlinBerlin.kValidation

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

sealed class PathDescriptor<in T, out R> {
    abstract operator fun get(aValue: T): R
    abstract val name: String
    override fun equals(other: Any?): Boolean = other is PathDescriptor<*, *> && other.name == name
    override fun hashCode(): Int = name.hashCode()
}

object ThisPathDescriptor : PathDescriptor<Any?, Any?>() {
    override fun get(aValue: Any?): Any? = aValue
    override val name: String get() = ""
}

class PropertyPathDescriptor<in T, out R>(val property: KProperty1<in T, R>) : PathDescriptor<T, R>() {
    override fun get(aValue: T): R = property(aValue)
    override val name: String get() = property.name
}

class FunctionPathDescriptor<in T, out R>(val function: KFunction1<T, R>) : PathDescriptor<T, R>() {
    override fun get(aValue: T): R = function(aValue)
    override val name: String get() = function.name
}

class MapPathDescriptor<T, R>(val entry: Map.Entry<T, R>) : PathDescriptor<Nothing, Map.Entry<T, R>>() {
    override fun get(aValue: Nothing): Map.Entry<T, R> = entry
    override val name: String get() = entry.key.toString()
}

class IndexPathDescriptor<R>(val index: Int, val value: R) : PathDescriptor<Nothing, R>() {
    override fun get(aValue: Nothing): R = value
    override val name: String get() = "[$index]"
}

class ConditionalPathDescriptor<T, out R>(
    internal val descriptor: PathDescriptor<T, R>,
    val condition: Validation<T>
) : PathDescriptor<T, R>() {
    override val name: String get() = descriptor.name
    override fun get(aValue: T): R = descriptor[aValue]
    private val computedHashCode by lazy { arrayOf(name, condition).contentHashCode() }

    override fun equals(other: Any?): Boolean =
        other is ConditionalPathDescriptor<*, *> && condition == other.condition && super.equals(other)

    override fun hashCode(): Int = computedHashCode
}