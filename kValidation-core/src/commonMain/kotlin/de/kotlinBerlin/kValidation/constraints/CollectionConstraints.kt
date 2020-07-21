@file:Suppress("unused")

package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder
import kotlin.jvm.JvmName

//Min items

@JvmName("maxItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

@JvmName("maxItemsArray")
fun <T> ValidationBuilder<Array<T>>.minItems(minSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

@JvmName("maxItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

// Max items

@JvmName("minItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

@JvmName("minItemsArray")
fun <T> ValidationBuilder<Array<T>>.maxItems(maxSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

@JvmName("minItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

//Uniqueness

@JvmName("uniqueItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.uniqueItems(unique: Boolean): Constraint<T> =
    addConstraint("all items must be unique") { tempValue, _ ->
        !unique || tempValue.distinct().count() == tempValue.count()
    }

@JvmName("uniqueItemsArray")
fun <T> ValidationBuilder<Array<T>>.uniqueItems(unique: Boolean): Constraint<Array<T>> =
    addConstraint("all items must be unique") { tempValue, _ ->
        !unique || tempValue.distinct().count() == tempValue.count()
    }

fun <T : Map<*, *>> ValidationBuilder<T>.uniqueValues(unique: Boolean): Constraint<T> =
    addConstraint("all values must be unique") { tempValue, _ ->
        !unique || tempValue.values.distinct().count() == tempValue.values.count()
    }