@file:Suppress("unused")

package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder
import kotlin.jvm.JvmName

//Min items

/** Checks whether or not this [Iterable] has at least [minSize] items. */
@JvmName("minItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

/** Checks whether or not this [Array] has at least [minSize] items. */
@JvmName("minItemsArray")
fun <T> ValidationBuilder<Array<T>>.minItems(minSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

/** Checks whether or not this [Map] has at least [minSize] entries. */
@JvmName("minItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

// Max items

/** Checks whether or not this [Iterable] has at most [maxSize] items. */
@JvmName("maxItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

/** Checks whether or not this [Array] has at most [maxSize] items. */
@JvmName("maxItemsArray")
fun <T> ValidationBuilder<Array<T>>.maxItems(maxSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

/** Checks whether or not this [Map] has at most [maxSize] entries. */
@JvmName("maxItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

//Uniqueness

/** Checks whether or not this [Iterable] has only unique items. */
@JvmName("uniqueItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.uniqueItems(): Constraint<T> =
    addConstraint("all items must be unique") { tempValue, _ -> tempValue.distinct().count() == tempValue.count() }

/** Checks whether or not this [Array] has only unique items. */
@JvmName("uniqueItemsArray")
fun <T> ValidationBuilder<Array<T>>.uniqueItems(): Constraint<Array<T>> =
    addConstraint("all items must be unique") { tempValue, _ -> tempValue.distinct().count() == tempValue.count() }

/** Checks whether or not this [Map] has only unique values. */
fun <T : Map<*, *>> ValidationBuilder<T>.uniqueValues(): Constraint<T> =
    addConstraint("all values must be unique") { tempValue, _ ->
        tempValue.values.distinct().count() == tempValue.values.count()
    }