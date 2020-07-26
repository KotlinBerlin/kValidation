@file:Suppress("unused")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.ValidationBuilder.Companion.asPathDescriptorDo
import de.kotlinBerlin.kValidation.constraints.Constraint
import kotlin.collections.Map.Entry
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
@Target(AnnotationTarget.CLASS)
private annotation class ValidationDsl

/** An interface that defines standard method to build a [Validation] instance. */
@ValidationDsl
interface ValidationBuilder<T> {

    /** Returns a [PathDescriptor] that describes a path to the current object. */
    @Suppress("UNCHECKED_CAST")
    val thisPath: PathDescriptor<T, T>
        get() = ThisPathDescriptor as PathDescriptor<T, T>

    /** Creates a [Validation] from this builder. */
    fun build(): Validation<in T>

    /** Adds a custom constraint to the validation. */
    fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T, Map<String, Any?>) -> Boolean
    ): Constraint<T>

    /** Replaces the hint text on the [Constraint]. */
    infix fun Constraint<T>.hint(aNewHint: String): Constraint<T>

    /** Marks the [Constraint] as a warning. */
    fun Constraint<T>.asWarning(): Constraint<T>

    /** Negates the [Constraint]. */
    fun Constraint<T>.not(): Constraint<T>

    /** Runs any other [Validation] in the context of this [Validation]. */
    fun run(validation: Validation<in T>)

    /** Creates a conditional path that only gets followed if the condition validates to true itself. */
    infix fun <R> PathDescriptor<T, R>.validateIf(init: AndValidationBuilder<T>.() -> Unit): PathDescriptor<T, R> {
        val validation = Validation(init)
        return ConditionalPathDescriptor(this, validation)
    }

    /** Creates a conditional path that only executes [validation] if the condition validates to true itself. */
    infix fun <R> PathDescriptor<T, R>.validateIf(validation: Validation<T>): PathDescriptor<T, R> =
        ConditionalPathDescriptor(this, validation)

    /** Create a [PathDescriptor] from the [KProperty1] and calls [validateIf] on it. */
    infix fun <R> KProperty1<T, R>.validateIf(init: AndValidationBuilder<T>.() -> Unit): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [validateIf] on it. */
    infix fun <R> KProperty1<T, R>.validateIf(aValidation: Validation<T>): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(aValidation) }

    /** Create a [PathDescriptor] from the function and calls [validateIf] on it. */
    infix fun <R> KFunction1<T, R>.validateIf(init: AndValidationBuilder<T>.() -> Unit): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(init) }

    /** Create a [PathDescriptor] from the function and calls [validateIf] on it. */
    infix fun <R> KFunction1<T, R>.validateIf(aValidation: Validation<T>): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(aValidation) }

    companion object {
        internal inline fun <T, R, RESULT> asPathDescriptorDo(
            aProperty: KProperty1<T, R>,
            aProcessor: PathDescriptor<T, R>.() -> RESULT
        ): RESULT = aProcessor.invoke(PropertyPathDescriptor(aProperty))

        internal inline fun <T, R, RESULT> asPathDescriptorDo(
            aFunction: KFunction1<T, R>,
            aProcessor: PathDescriptor<T, R>.() -> RESULT
        ): RESULT = aProcessor.invoke(FunctionPathDescriptor(aFunction))
    }
}

/**
 * An interface that defines standard method to build a [Validation] instance that combines its parts with the && operator.
 *
 * The [Validation] by default evaluates all validations defined and wont stop on the first failing one. To change that
 * use the [shortCircuit] methods.
 */
interface AndValidationBuilder<T> : ValidationBuilder<T> {

    /** Builds a [Validation] for an object described by this [PathDescriptor]. */
    infix operator fun <R> PathDescriptor<T, R>.invoke(init: AndValidationBuilder<R>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each object in the [Iterable] described by this [PathDescriptor]. */
    infix fun <R> PathDescriptor<T, Iterable<R>>.allInIterable(init: AndValidationBuilder<R>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each object at the specified indices in the [Iterable] described by this [PathDescriptor]. */
    fun <R> PathDescriptor<T, Iterable<R>>.allIndicesInIterable(
        vararg anIndexList: Int,
        init: AndValidationBuilder<R>.() -> Unit
    )

    /** Builds a [Validation] that gets invoked for each object in the [Array] described by this [PathDescriptor]. */
    infix fun <R> PathDescriptor<T, Array<R>>.allInArray(init: AndValidationBuilder<R>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each object at the specified indices in the [Array] described by this [PathDescriptor]. */
    fun <R> PathDescriptor<T, Array<R>>.allIndicesInArray(
        vararg anIndexList: Int,
        init: AndValidationBuilder<R>.() -> Unit
    )

    /** Builds a [Validation] that gets invoked for each entry in the [Map] described by this [PathDescriptor]. */
    infix fun <K, V> PathDescriptor<T, Map<K, V>>.allInMap(init: AndValidationBuilder<Entry<K, V>>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each entry at the specified keys in the [Map] described by this [PathDescriptor]. */
    fun <K, V> PathDescriptor<T, Map<K, V>>.allKeysInMap(
        vararg aKeyList: K,
        init: AndValidationBuilder<Entry<K, V>>.() -> Unit
    )

    /** Builds a [Validation] for an object described by this [PathDescriptor] that only gets executed if the object is not null. */
    infix fun <R> PathDescriptor<T, R?>.ifPresent(init: AndValidationBuilder<R>.() -> Unit)

    /**
     * Builds a [Validation] for an object described by this [PathDescriptor] that gets executed if the object is not null.
     * Otherwise an error is created and added to the [ValidationResult].
     */
    infix fun <R> PathDescriptor<T, R?>.required(init: AndValidationBuilder<R>.() -> Unit)

    /** Create a [PathDescriptor] from the [KProperty1] and calls [invoke] on it. */
    infix operator fun <R> KProperty1<T, R>.invoke(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { invoke(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allInIterable] on it. */
    infix fun <R> KProperty1<T, Iterable<R>>.allInIterable(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInIterable(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allIndicesInIterable] on it. */
    fun <R> KProperty1<T, Iterable<R>>.allIndicesInIterable(
        vararg anIndexList: Int,
        init: AndValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInIterable(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allInArray] on it. */
    infix fun <R> KProperty1<T, Array<R>>.allInArray(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInArray(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allIndicesInArray] on it. */
    fun <R> KProperty1<T, Array<R>>.allIndicesInArray(
        vararg anIndexList: Int,
        init: AndValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInArray(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allInMap] on it. */
    infix fun <K, V> KProperty1<T, Map<K, V>>.allInMap(init: AndValidationBuilder<Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInMap(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allKeysInMap] on it. */
    fun <K, V> KProperty1<T, Map<K, V>>.allKeysInMap(
        vararg aKeyList: K,
        init: AndValidationBuilder<Entry<K, V>>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allKeysInMap(*aKeyList, init = init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [ifPresent] on it. */
    infix fun <R> KProperty1<T, R?>.ifPresent(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [required] on it. */
    infix fun <R> KProperty1<T, R?>.required(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    /** Create a [PathDescriptor] from the function and calls [invoke] on it. */
    infix operator fun <R> KFunction1<T, R>.invoke(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { invoke(init) }

    /** Create a [PathDescriptor] from the function and calls [allInIterable] on it. */
    infix fun <R> KFunction1<T, Iterable<R>>.allInIterable(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInIterable(init) }

    /** Create a [PathDescriptor] from the function and calls [allIndicesInIterable] on it. */
    fun <R> KFunction1<T, Iterable<R>>.allIndicesInIterable(
        vararg anIndexList: Int,
        init: AndValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInIterable(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the function and calls [allInArray] on it. */
    infix fun <R> KFunction1<T, Array<R>>.allInArray(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInArray(init) }

    /** Create a [PathDescriptor] from the function and calls [allIndicesInArray] on it. */
    fun <R> KFunction1<T, Array<R>>.allIndicesInArray(
        vararg anIndexList: Int,
        init: AndValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInArray(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the function and calls [allInMap] on it. */
    infix fun <K, V> KFunction1<T, Map<K, V>>.allInMap(init: AndValidationBuilder<Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInMap(init) }

    /** Create a [PathDescriptor] from the function and calls [allKeysInMap] on it. */
    fun <K, V> KFunction1<T, Map<K, V>>.allKeysInMap(
        vararg aKeyList: K,
        init: AndValidationBuilder<Entry<K, V>>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allKeysInMap(*aKeyList, init = init) }

    /** Create a [PathDescriptor] from the function and calls [ifPresent] on it. */
    infix fun <R> KFunction1<T, R?>.ifPresent(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    /** Create a [PathDescriptor] from the function and calls [required] on it. */
    infix fun <R> KFunction1<T, R?>.required(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    /** Makes this [Validation] short circuit, which means it will stop validating on the first invalid result encountered. */
    fun shortCircuit()

    /** Adds a [Validation] to this [Validation] that is short circuit. */
    fun shortCircuit(anInitBlock: AndValidationBuilder<T>.() -> Unit)

    /** Adds a [Validation] to this [Validation] that combines its results with the || operator. */
    fun or(anInitBlock: OrValidationBuilder<T>.() -> Unit)
}

/**
 * An interface that defines standard method to build a [Validation] instance that combines its parts with the || operator.
 *
 * The [Validation] by default stops evaluating on the first succeeding one as it would bring no value to the validation to validate the others.
 * To change that use the [nonShortCircuit] methods.
 */
interface OrValidationBuilder<T> : ValidationBuilder<T> {

    /** Builds a [Validation] for an object described by this [PathDescriptor]. */
    infix operator fun <R> PathDescriptor<T, R>.invoke(init: OrValidationBuilder<R>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each object in the [Iterable] described by this [PathDescriptor]. */
    infix fun <R> PathDescriptor<T, Iterable<R>>.allInIterable(init: OrValidationBuilder<R>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each object at the specified indices in the [Iterable] described by this [PathDescriptor]. */
    fun <R> PathDescriptor<T, Iterable<R>>.allIndicesInIterable(
        vararg anIndexList: Int,
        init: OrValidationBuilder<R>.() -> Unit
    )

    /** Builds a [Validation] that gets invoked for each object in the [Array] described by this [PathDescriptor]. */
    infix fun <R> PathDescriptor<T, Array<R>>.allInArray(init: OrValidationBuilder<R>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each object at the specified indices in the [Array] described by this [PathDescriptor]. */
    fun <R> PathDescriptor<T, Array<R>>.allIndicesInArray(
        vararg anIndexList: Int,
        init: OrValidationBuilder<R>.() -> Unit
    )

    /** Builds a [Validation] that gets invoked for each entry in the [Map] described by this [PathDescriptor]. */
    infix fun <K, V> PathDescriptor<T, Map<K, V>>.allInMap(init: OrValidationBuilder<Entry<K, V>>.() -> Unit)

    /** Builds a [Validation] that gets invoked for each entry at the specified keys in the [Map] described by this [PathDescriptor]. */
    fun <K, V> PathDescriptor<T, Map<K, V>>.allKeysInMap(
        vararg aKeyList: K,
        init: OrValidationBuilder<Entry<K, V>>.() -> Unit
    )

    /** Builds a [Validation] for an object described by this [PathDescriptor] that only gets executed if the object is not null. */
    infix fun <R> PathDescriptor<T, R?>.ifPresent(init: OrValidationBuilder<R>.() -> Unit)

    /**
     * Builds a [Validation] for an object described by this [PathDescriptor] that gets executed if the object is not null.
     * Otherwise an error is created and added to the [ValidationResult].
     */
    infix fun <R> PathDescriptor<T, R?>.required(init: OrValidationBuilder<R>.() -> Unit)

    /** Create a [PathDescriptor] from the [KProperty1] and calls [invoke] on it. */
    infix operator fun <R> KProperty1<T, R>.invoke(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { invoke(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allInIterable] on it. */
    infix fun <R> KProperty1<T, Iterable<R>>.allInIterable(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInIterable(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allIndicesInIterable] on it. */
    fun <R> KProperty1<T, Iterable<R>>.allIndicesInIterable(
        vararg anIndexList: Int,
        init: OrValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInIterable(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allInArray] on it. */
    infix fun <R> KProperty1<T, Array<R>>.allInArray(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInArray(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allIndicesInArray] on it. */
    fun <R> KProperty1<T, Array<R>>.allIndicesInArray(
        vararg anIndexList: Int,
        init: OrValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInArray(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allInMap] on it. */
    infix fun <K, V> KProperty1<T, Map<K, V>>.allInMap(init: OrValidationBuilder<Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInMap(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [allKeysInMap] on it. */
    fun <K, V> KProperty1<T, Map<K, V>>.allKeysInMap(
        vararg aKeyList: K,
        init: OrValidationBuilder<Entry<K, V>>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allKeysInMap(*aKeyList, init = init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [ifPresent] on it. */
    infix fun <R> KProperty1<T, R?>.ifPresent(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    /** Create a [PathDescriptor] from the [KProperty1] and calls [required] on it. */
    infix fun <R> KProperty1<T, R?>.required(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    /** Create a [PathDescriptor] from the function and calls [invoke] on it. */
    infix operator fun <R> KFunction1<T, R>.invoke(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { invoke(init) }

    /** Create a [PathDescriptor] from the function and calls [allInIterable] on it. */
    infix fun <R> KFunction1<T, Iterable<R>>.allInIterable(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInIterable(init) }

    /** Create a [PathDescriptor] from the function and calls [allIndicesInIterable] on it. */
    fun <R> KFunction1<T, Iterable<R>>.allIndicesInIterable(
        vararg anIndexList: Int,
        init: OrValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInIterable(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the function and calls [allInArray] on it. */
    infix fun <R> KFunction1<T, Array<R>>.allInArray(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInArray(init) }

    /** Create a [PathDescriptor] from the function and calls [allIndicesInArray] on it. */
    fun <R> KFunction1<T, Array<R>>.allIndicesInArray(
        vararg anIndexList: Int,
        init: OrValidationBuilder<R>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allIndicesInArray(*anIndexList, init = init) }

    /** Create a [PathDescriptor] from the function and calls [allInMap] on it. */
    infix fun <K, V> KFunction1<T, Map<K, V>>.allInMap(init: OrValidationBuilder<Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { allInMap(init) }

    /** Create a [PathDescriptor] from the function and calls [allKeysInMap] on it. */
    fun <K, V> KFunction1<T, Map<K, V>>.allKeysInMap(
        vararg aKeyList: K,
        init: OrValidationBuilder<Entry<K, V>>.() -> Unit
    ): Unit = asPathDescriptorDo(this) { allKeysInMap(*aKeyList, init = init) }

    /** Create a [PathDescriptor] from the function and calls [ifPresent] on it. */
    infix fun <R> KFunction1<T, R?>.ifPresent(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    /** Create a [PathDescriptor] from the function and calls [required] on it. */
    infix fun <R> KFunction1<T, R?>.required(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    /** Makes this [Validation] non short circuit, which means it will validate all validations defined. */
    fun nonShortCircuit()

    /** Adds a [Validation] to this [Validation] that is not short circuit. */
    fun nonShortCircuit(anInitBlock: OrValidationBuilder<T>.() -> Unit)

    /** Adds a [Validation] to this [Validation] that combines its results with the && operator. */
    fun and(anInitBlock: AndValidationBuilder<T>.() -> Unit)
}
