@file:Suppress("unused")

package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.*
import de.kotlinBerlin.kValidation.internal.ArrayPropKey
import de.kotlinBerlin.kValidation.internal.IterablePropKey
import de.kotlinBerlin.kValidation.internal.MapPropKey
import de.kotlinBerlin.kValidation.internal.PropModifier.Undefined
import de.kotlinBerlin.kValidation.internal.SingleValuePropKey
import kotlin.collections.Map.Entry
import kotlin.jvm.JvmName

/** Performs the specified validation on each item of an iterable. */
@JvmName("onEachIterable")
fun <T : Iterable<R>, R> AndValidationBuilder<T>.onEach(init: AndValidationBuilder<R>.() -> Unit) {
    getOrCreateBuilder<R>(IterablePropKey(thisPath, Undefined)).also(init)
}

/** Performs the specified validation on the item of an iterable at the specified position. */
@JvmName("onIndicesIterable")
fun <T : Iterable<R>, R> AndValidationBuilder<T>.onIndices(
    vararg anIndexList: Int,
    init: AndValidationBuilder<R>.() -> Unit
) {
    anIndexList.forEach {
        getOrCreateBuilder<R>(SingleValuePropKey(IterablePathDescriptor(it), Undefined)).also(init)
    }
}

/** Performs the specified validation on each item of an array. */
@JvmName("onEachArray")
fun <R> AndValidationBuilder<Array<R>>.onEach(init: AndValidationBuilder<R>.() -> Unit) {
    getOrCreateBuilder<R>(ArrayPropKey(thisPath, Undefined)).also(init)
}

/** Performs the specified validation on the item of an array at the specified position. */
@JvmName("onIndicesArray")
fun <R> AndValidationBuilder<Array<R>>.onIndices(vararg anIndexList: Int, init: AndValidationBuilder<R>.() -> Unit) {
    anIndexList.forEach {
        getOrCreateBuilder<R>(SingleValuePropKey(ArrayPathDescriptor(it), Undefined)).also(init)
    }
}

/** Performs the specified validation on each entry of a map. */
@JvmName("onEachMap")
fun <K, V> AndValidationBuilder<Map<K, V>>.onEach(init: AndValidationBuilder<Entry<K, V>>.() -> Unit) {
    getOrCreateBuilder<Entry<K, V>>(MapPropKey(thisPath, Undefined)).also(init)
}

/** Performs the specified validation on the item of an array at the specified position. */
fun <K, V> AndValidationBuilder<Map<K, V>>.onKeys(
    vararg aKeyList: K,
    init: AndValidationBuilder<Entry<K, V>>.() -> Unit
) {
    aKeyList.forEach {
        getOrCreateBuilder<Entry<K, V>>(SingleValuePropKey(MapPathDescriptor(it), Undefined)).also(init)
    }
}

/** Performs the specified validation on each item of an iterable. */
@JvmName("onEachIterable")
fun <T : Iterable<R>, R> OrValidationBuilder<T>.onEach(init: OrValidationBuilder<R>.() -> Unit) {
    getOrCreateBuilder<R>(IterablePropKey(thisPath, Undefined)).also(init)
}

/** Performs the specified validation on the item of an iterable at the specified position. */
@JvmName("onIndicesIterable")
fun <R> OrValidationBuilder<Iterable<R>>.onIndices(vararg anIndexList: Int, init: OrValidationBuilder<R>.() -> Unit) {
    anIndexList.forEach {
        getOrCreateBuilder<R>(SingleValuePropKey(IterablePathDescriptor(it), Undefined)).also(init)
    }
}

/** Performs the specified validation on each item of an array. */
@JvmName("onEachArray")
fun <R> OrValidationBuilder<Array<R>>.onEach(init: OrValidationBuilder<R>.() -> Unit) {
    getOrCreateBuilder<R>(ArrayPropKey(thisPath, Undefined)).also(init)
}

/** Performs the specified validation on the item of an iterable at the specified position. */
@JvmName("onIndicesArray")
fun <R> OrValidationBuilder<Array<R>>.onIndices(vararg anIndexList: Int, init: OrValidationBuilder<R>.() -> Unit) {
    anIndexList.forEach {
        getOrCreateBuilder<R>(SingleValuePropKey(ArrayPathDescriptor(it), Undefined)).also(init)
    }
}

/** Performs the specified validation on each item of a map. */
@JvmName("onEachMap")
fun <K, V> OrValidationBuilder<Map<K, V>>.onEach(init: OrValidationBuilder<Entry<K, V>>.() -> Unit) {
    getOrCreateBuilder<Entry<K, V>>(MapPropKey(thisPath, Undefined)).also(init)
}

/** Performs the specified validation on the item of an array at the specified position. */
fun <K, V> OrValidationBuilder<Map<K, V>>.onKeys(
    vararg aKeyList: K,
    init: OrValidationBuilder<Entry<K, V>>.() -> Unit
) {
    aKeyList.forEach {
        getOrCreateBuilder<Entry<K, V>>(SingleValuePropKey(MapPathDescriptor(it), Undefined)).also(init)
    }
}

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