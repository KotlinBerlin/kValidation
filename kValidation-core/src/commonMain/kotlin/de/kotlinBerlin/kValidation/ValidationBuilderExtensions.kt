@file:Suppress("unused")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.constraints.type
import kotlin.reflect.KProperty1

/** Shortcut for "thisPath required". */
inline fun <T : Any> AndValidationBuilder<T?>.required(crossinline init: AndValidationBuilder<T>.() -> Unit): Unit =
    thisPath required { init() }

/** Shortcut for "thisPath ifPresent". */
inline fun <T : Any> AndValidationBuilder<T?>.ifPresent(crossinline init: AndValidationBuilder<T>.() -> Unit): Unit =
    thisPath ifPresent { init() }

/** Shortcut for "thisPath required". */
inline fun <T : Any> OrValidationBuilder<T?>.required(crossinline init: OrValidationBuilder<T>.() -> Unit): Unit =
    thisPath required { init() }

/** Shortcut for "thisPath ifPresent". */
inline fun <T : Any> OrValidationBuilder<T?>.ifPresent(crossinline init: OrValidationBuilder<T>.() -> Unit): Unit =
    thisPath ifPresent { init() }

/** If the object is of type [R] then the validation is executed, otherwise the object is valid. */
inline fun <reified R> AndValidationBuilder<*>.ifType(crossinline init: AndValidationBuilder<R>.() -> Unit) {
    or {
        type<R>().not()
        and {
            CustomPathDescriptor<Any?, R>("as ${R::class.simpleName}") { it as R } invoke {
                init()
            }
        }
    }
}

/** If the object is of type [R] then the validation is executed, otherwise the object is invalid. */
inline fun <reified R> AndValidationBuilder<*>.requireType(crossinline init: AndValidationBuilder<R>.() -> Unit) {
    shortCircuit {
        type<R>()
        CustomPathDescriptor<Any?, R>("as ${R::class.simpleName}") { it as R } invoke {
            init()
        }
    }
}

/** If the object is of type [R] then the validation is executed, otherwise the object is valid. */
inline fun <reified R> OrValidationBuilder<*>.ifType(crossinline init: OrValidationBuilder<R>.() -> Unit) {
    type<R>().not()
    CustomPathDescriptor<Any?, R>("as ${R::class.simpleName}") { it as R } invoke {
        init()
    }
}

/** If the object is of type [R] then the validation is executed, otherwise the object is invalid. */
inline fun <reified R> OrValidationBuilder<*>.requireType(crossinline init: OrValidationBuilder<R>.() -> Unit) {
    and {
        shortCircuit()
        type<R>()
        or {
            CustomPathDescriptor<Any?, R>("as ${R::class.simpleName}") { it as R } invoke {
                init()
            }
        }
    }
}

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