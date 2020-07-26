@file:Suppress("unused")

package de.kotlinBerlin.kValidation

import kotlin.reflect.KProperty1

/** Shortcut for "thisPath required". */
inline fun <T : Any> AndValidationBuilder<T?>.required(crossinline init: AndValidationBuilder<T>.() -> Unit) =
    thisPath required { init() }

/** Shortcut for "thisPath ifPresent". */
inline fun <T : Any> AndValidationBuilder<T?>.ifPresent(crossinline init: AndValidationBuilder<T>.() -> Unit) =
    thisPath ifPresent { init() }

/** Shortcut for "thisPath required". */
inline fun <T : Any> OrValidationBuilder<T?>.required(crossinline init: OrValidationBuilder<T>.() -> Unit) =
    thisPath required { init() }

/** Shortcut for "thisPath ifPresent". */
inline fun <T : Any> OrValidationBuilder<T?>.ifPresent(crossinline init: OrValidationBuilder<T>.() -> Unit) =
    thisPath ifPresent { init() }

/** Simple was to reference the key in a map validation. */
val <K, V> ValidationBuilder<Map.Entry<K, V>>.key: PathDescriptor<Map.Entry<K, V>, K> get() = PropertyPathDescriptor(Map.Entry<K, V>::key)

/** Simple was to reference the value in a map validation. */
val <K, V> ValidationBuilder<Map.Entry<K, V>>.value: PathDescriptor<Map.Entry<K, V>, V>
    get() = PropertyPathDescriptor(
        Map.Entry<K, V>::value
    )

/** Simple was to reference the key in a map validation when retrieving the results via the [ValidationResult.errorsAt] method */
val MAP_KEY_SELECTOR: KProperty1<Map.Entry<Any?, Any?>, Any?> = Map.Entry<Any?, Any?>::key

/** Simple was to reference the value in a map validation when retrieving the results via the [ValidationResult.errorsAt] method */
val MAP_VALUE_SELECTOR: KProperty1<Map.Entry<Any?, Any?>, Any?> = Map.Entry<Any?, Any?>::value