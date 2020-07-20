@file:Suppress("unused")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.internal.*
import kotlin.collections.Map.Entry
import kotlin.jvm.JvmName

@JvmName("onEachIterable")
fun <T : Iterable<R>, R> AndValidationBuilder<T>.onEach(init: AndValidationBuilder<R>.() -> Unit) {
    val tempBuilder = BasicAndValidationBuilder<R>()
    tempBuilder.init()
    val tempIterableValidation = IterableValidation(tempBuilder.build())
    run(UndefinedPropertyValidation(BasicPathDescriptor<T, T>("") { it }, tempIterableValidation))
}

@JvmName("onEachArray")
fun <T> AndValidationBuilder<Array<T>>.onEach(init: AndValidationBuilder<T>.() -> Unit) {
    val tempBuilder = BasicAndValidationBuilder<T>()
    tempBuilder.init()
    val tempArrayValidation = ArrayValidation(tempBuilder.build())
    val tempValidation =
        UndefinedPropertyValidation(BasicPathDescriptor<Array<T>, Array<T>>("") { it }, tempArrayValidation)
    run(tempValidation)
}

@JvmName("onEachMap")
fun <M : Map<K, V>, K, V> AndValidationBuilder<M>.onEach(init: AndValidationBuilder<Entry<K, V>>.() -> Unit) {
    val tempBuilder = BasicAndValidationBuilder<Entry<K, V>>()
    tempBuilder.init()
    val tempMapValidation = MapValidation(tempBuilder.build())
    val tempValidation = UndefinedPropertyValidation(BasicPathDescriptor<M, M>("") { it }, tempMapValidation)
    run(tempValidation)
}

@JvmName("onEachIterable")
fun <T : Iterable<R>, R> OrValidationBuilder<T>.onEach(init: OrValidationBuilder<R>.() -> Unit) {
    val tempBuilder = BasicOrValidationBuilder<R>()
    tempBuilder.init()
    val tempIterableValidation = IterableValidation(tempBuilder.build())
    val tempValidation =
        UndefinedPropertyValidation(BasicPathDescriptor<T, T>("") { it }, tempIterableValidation)
    run(tempValidation)
}

@JvmName("onEachArray")
fun <T> OrValidationBuilder<Array<T>>.onEach(init: OrValidationBuilder<T>.() -> Unit) {
    val tempBuilder = BasicOrValidationBuilder<T>()
    tempBuilder.init()
    val tempArrayValidation = ArrayValidation(tempBuilder.build())
    val tempValidation =
        UndefinedPropertyValidation(BasicPathDescriptor<Array<T>, Array<T>>("") { it }, tempArrayValidation)
    run(tempValidation)
}

@JvmName("onEachMap")
fun <M : Map<K, V>, K, V> OrValidationBuilder<M>.onEach(init: OrValidationBuilder<Entry<K, V>>.() -> Unit) {
    val tempBuilder = BasicOrValidationBuilder<Entry<K, V>>()
    tempBuilder.init()
    val tempMapValidation = MapValidation(tempBuilder.build())
    val tempValidation = UndefinedPropertyValidation(BasicPathDescriptor<M, M>("") { it }, tempMapValidation)
    run(tempValidation)
}

//Min items

@JvmName("maxItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { it.count() >= minSize }

@JvmName("maxItemsArray")
fun <T> ValidationBuilder<Array<T>>.minItems(minSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { it.count() >= minSize }

@JvmName("maxItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} item${if (minSize != 1) "s" else ""}",
        minSize.toString()
    ) { it.count() >= minSize }

// Max items

@JvmName("minItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { it.count() <= maxSize }

@JvmName("minItemsArray")
fun <T> ValidationBuilder<Array<T>>.maxItems(maxSize: Int): Constraint<Array<T>> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { it.count() <= maxSize }

@JvmName("minItemsMap")
fun <T : Map<*, *>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} item${if (maxSize != 1) "s" else ""}",
        maxSize.toString()
    ) { it.count() <= maxSize }

//Uniqueness

@JvmName("uniqueItemsIterable")
fun <T : Iterable<*>> ValidationBuilder<T>.uniqueItems(unique: Boolean): Constraint<T> =
    addConstraint("all items must be unique") { !unique || it.distinct().count() == it.count() }

@JvmName("uniqueItemsArray")
fun <T> ValidationBuilder<Array<T>>.uniqueItems(unique: Boolean): Constraint<Array<T>> =
    addConstraint("all items must be unique") { !unique || it.distinct().count() == it.count() }

fun <T : Map<*, *>> ValidationBuilder<T>.uniqueValues(unique: Boolean): Constraint<T> =
    addConstraint("all values must be unique") { !unique || it.values.distinct().count() == it.values.count() }