@file:Suppress("unused")

package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder
import kotlin.jvm.JvmName

//Contains

/** Checks whether or not this [Iterable] contains all objects in [values]. */
@JvmName("containsAllIterable")
fun <V, T : Iterable<V>> ValidationBuilder<T>.containsAll(vararg values: V): Constraint<T> =
    addConstraint(
        "must contain all of: {0}",
        values.joinToString("', '", "'", "'")
    ) { tempValue, _ -> values.all(tempValue::contains) }

/** Checks whether or not this [Iterable] contains any object in [values]. */
@JvmName("containsAnyIterable")
fun <V, T : Iterable<V>> ValidationBuilder<T>.containsAny(vararg values: V): Constraint<T> =
    addConstraint(
        "must contain any of: {0}",
        values.joinToString("', '", "'", "'")
    ) { tempValue, _ -> values.any(tempValue::contains) }

/** Checks whether or not this [Iterable] contains all objects in [values]. */
@JvmName("containsAllArray")
fun <T> ValidationBuilder<Array<T>>.containsAll(vararg values: T): Constraint<Array<T>> =
    addConstraint(
        "must contain all of: {0}",
        values.joinToString("', '", "'", "'")
    ) { tempValue, _ -> values.all(tempValue::contains) }

/** Checks whether or not this [Iterable] contains any object in [values]. */
@JvmName("containsAnyArray")
fun <T> ValidationBuilder<Array<T>>.containsAny(vararg values: T): Constraint<Array<T>> =
    addConstraint(
        "must contain any of: {0}",
        values.joinToString("', '", "'", "'")
    ) { tempValue, _ -> values.any(tempValue::contains) }

/** Checks whether or not this [Map] contains all keys in [keys]. */
fun <K, V, T : Map<K, V>> ValidationBuilder<T>.containsAllKeys(vararg keys: K): Constraint<T> =
    addConstraint(
        "keys must contains all keys of: {0}",
        keys.joinToString("', '", "'", "'")
    ) { tempValue, _ -> keys.all(tempValue::contains) }

/** Checks whether or not this [Map] contains any key in [keys]. */
fun <K, V, T : Map<K, V>> ValidationBuilder<T>.containsAnyKey(vararg keys: K): Constraint<T> =
    addConstraint(
        "must contains any key of: {0}",
        keys.joinToString("', '", "'", "'")
    ) { tempValue, _ -> keys.any(tempValue::contains) }

/** Checks whether or not this [Map] contains all values in [values]. */
fun <K, V, T : Map<K, V>> ValidationBuilder<T>.containsAllValues(vararg values: V): Constraint<T> =
    addConstraint(
        "keys must contains all values of: {0}",
        values.joinToString("', '", "'", "'")
    ) { tempValue, _ -> values.all(tempValue.values::contains) }

/** Checks whether or not this [Map] contains any value in [values]. */
fun <K, V, T : Map<K, V>> ValidationBuilder<T>.containsAnyValue(vararg values: V): Constraint<T> =
    addConstraint(
        "must contains any value of: {0}",
        values.joinToString("', '", "'", "'")
    ) { tempValue, _ -> values.any(tempValue.values::contains) }

/** Checks whether or not this [Map] contains all mappings in [mappings]. */
fun <K, V, T : Map<K, V>> ValidationBuilder<T>.containsAllMappings(vararg mappings: Pair<K, V>): Constraint<T> =
    addConstraint(
        "keys must contains all mappings of: {0}",
        mappings.joinToString("', '", "'", "'") { "${it.first} -> ${it.second}" }
    ) { tempValue, _ -> mappings.all { mapping -> tempValue.any { entry -> entry.key == mapping.first && entry.value == mapping.second } } }

/** Checks whether or not this [Map] contains any mapping in [mappings]. */
fun <K, V, T : Map<K, V>> ValidationBuilder<T>.containsAnyMapping(vararg mappings: Pair<K, V>): Constraint<T> =
    addConstraint(
        "must contains any mapping of: {0}",
        mappings.joinToString("', '", "'", "'") { "${it.first} -> ${it.second}" }
    ) { tempValue, _ -> mappings.any { mapping -> tempValue.any { entry -> entry.key == mapping.first && entry.value == mapping.second } } }

//Min / max items

/** Checks whether or not this [Iterable] has at least [minSize] items. */
@JvmName("minItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

/** Checks whether or not this [Iterable] has at most [maxSize] items. */
@JvmName("maxItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

/** Checks whether or not this [Array] has at least [minSize] items. */
@JvmName("minItemsArray")
fun <T> ValidationBuilder<Array<T>>.minItems(minSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

/** Checks whether or not this [Array] has at most [maxSize] items. */
@JvmName("maxItemsArray")
fun <T> ValidationBuilder<Array<T>>.maxItems(maxSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

/** Checks whether or not this [Map] has at least [minSize] entries. */
@JvmName("minItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { tempValue, _ -> tempValue.count() >= minSize }

/** Checks whether or not this [Map] has at most [maxSize] entries. */
@JvmName("maxItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { tempValue, _ -> tempValue.count() <= maxSize }

//Empty / not empty

/** Checks whether or not this [Iterable] is empty. */
@JvmName("emptyIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.empty(): Constraint<T> = maxItems(0) hint "must be empty"

/** Checks whether or not this [Iterable] is not empty. */
@JvmName("notEmptyIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.notEmpty(): Constraint<T> = minItems(1) hint "may not be empty"

/** Checks whether or not this [Array] is empty. */
@JvmName("emptyArray")
fun <T> ValidationBuilder<Array<T>>.empty(): Constraint<Array<T>> = maxItems(0) hint "must be empty"

/** Checks whether or not this [Array] is not empty. */
@JvmName("notEmptyArray")
fun <T> ValidationBuilder<Array<T>>.notEmpty(): Constraint<Array<T>> = minItems(1) hint "may not be empty"

/** Checks whether or not this [Map] is empty. */
@JvmName("emptyMap")
fun <T : Map<*, *>> ValidationBuilder<T>.empty(): Constraint<T> = maxItems(0) hint "must be empty"

/** Checks whether or not this [Map] is empty. */
@JvmName("notEmptyMap")
fun <T : Map<*, *>> ValidationBuilder<T>.notEmpty(): Constraint<T> = minItems(1) hint "may not be empty"

//Uniqueness

/** Checks whether or not this [Iterable] has only unique items. */
@JvmName("uniqueItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.distinct(): Constraint<T> =
    addConstraint("all items must be unique") { tempValue, _ -> tempValue.distinct().count() == tempValue.count() }

/** Checks whether or not this [Array] has only unique items. */
@JvmName("uniqueItemsArray")
fun <T> ValidationBuilder<Array<T>>.distinct(): Constraint<Array<T>> =
    addConstraint("all items must be unique") { tempValue, _ -> tempValue.distinct().count() == tempValue.count() }

/** Checks whether or not this [Map] has only unique values. */
fun <T : Map<*, *>> ValidationBuilder<T>.distinctValues(): Constraint<T> =
    addConstraint("all values must be unique") { tempValue, _ ->
        tempValue.values.distinct().count() == tempValue.values.count()
    }