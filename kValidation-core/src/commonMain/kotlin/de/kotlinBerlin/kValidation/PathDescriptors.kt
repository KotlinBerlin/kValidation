@file:Suppress("MemberVisibilityCanBePrivate")

package de.kotlinBerlin.kValidation

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

/** A description on how to get the next object that should be validated and its name. */
sealed class PathDescriptor<in T, out R> {
    /** Gets the next object that should be validated. */
    abstract operator fun get(aValue: T): R

    /** Gets the name of the path to the next object that should be validated. */
    abstract val name: String

    /** Compares two [PathDescriptor] instances if they represent the same path. */
    override fun equals(other: Any?): Boolean = other is PathDescriptor<*, *> && other.name == name

    /** The hash of this path. */
    override fun hashCode(): Int = name.hashCode()
}

/** Represents a path to the object itself. */
object ThisPathDescriptor : PathDescriptor<Any?, Any?>() {
    override fun get(aValue: Any?): Any? = aValue
    override val name: String get() = ""
}

/** A custom path to any object that gets returned by the getter. The identifier will be used when comparing instances of it with others. */
class CustomPathDescriptor<in T, out R>(
    /** A unique identifier for this path. The [Any.toString] method  on the [identifier] is used to determine the name of this path. */
    val identifier: Any,
    private val getter: (T) -> R,
) : PathDescriptor<T, R>() {
    override val name: String get() = identifier.toString()
    override fun get(aValue: T): R = getter(aValue)

    override fun equals(other: Any?): Boolean = other is CustomPathDescriptor<*, *> && other.identifier == identifier
    override fun hashCode(): Int = identifier.hashCode()
}

/** Represents the path to a nested property. */
class PropertyPathDescriptor<in T, out R>(
    /** The nested property. */
    val property: KProperty1<in T, R>
) : PathDescriptor<T, R>() {
    override fun get(aValue: T): R = property(aValue)
    override val name: String get() = property.name

    override fun equals(other: Any?): Boolean = other is PropertyPathDescriptor<*, *> && super.equals(other)
    override fun hashCode(): Int = property.hashCode()
}

/** Represents the path to the result of a function. */
class FunctionPathDescriptor<in T, out R>(
    /** The function which results should be validated. */
    val function: KFunction1<T, R>
) : PathDescriptor<T, R>() {
    override fun get(aValue: T): R = function(aValue)
    override val name: String get() = function.name
    override fun equals(other: Any?): Boolean = other is FunctionPathDescriptor<*, *> && super.equals(other)
    override fun hashCode(): Int = function.hashCode()
}

/** Represents the path to an entry in a map given its [key] */
class MapPathDescriptor<T, R>(
    /** The key that should be searched for in the map. */
    val key: T
) : PathDescriptor<Map<T, R>, Map.Entry<T, R>>() {
    override fun get(aValue: Map<T, R>): Map.Entry<T, R> =
        aValue.entries.find { it.key == key } ?: throw NoSuchElementException("No mapping for $key found in map!")

    override val name: String get() = "[${key.toString()}]"
}

/** Represents the path to an element if an [Array] given its [index]. */
class ArrayPathDescriptor<R>(
    /** The index of the object that should be validated. */
    val index: Int
) : PathDescriptor<Array<R>, R>() {
    override fun get(aValue: Array<R>): R = aValue[index]
    override val name: String get() = "[$index]"
}

/** Represents the path to an element if an [Iterable] given its [index]. */
class IterablePathDescriptor<R>(
    /** The index of the object that should be validated. */
    val index: Int
) : PathDescriptor<Iterable<R>, R>() {
    override fun get(aValue: Iterable<R>): R = aValue.iterator().run {
        for (i in 0 until index) {
            next()
        }
        next()
    }

    override val name: String get() = "[$index]"
}

/** Represents the path to an object that should only be validated if the [condition] is valid itself. */
internal class ConditionalPathDescriptor<T, out R>(
    internal val descriptor: PathDescriptor<T, R>,
    /** The condition to check first. */
    val condition: Validation<T>
) : PathDescriptor<T, R>() {
    override val name: String get() = descriptor.name
    override fun get(aValue: T): R = descriptor[aValue]
    private val computedHashCode by lazy { arrayOf(name, condition).contentHashCode() }

    override fun equals(other: Any?): Boolean =
        other is ConditionalPathDescriptor<*, *> && condition == other.condition && super.equals(other)

    override fun hashCode(): Int = computedHashCode
}