@file:Suppress("unused")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.ValidationBuilder.Companion.asPathDescriptorDo
import de.kotlinBerlin.kValidation.constraints.Constraint
import kotlin.jvm.JvmStatic
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class ValidationDsl

@ValidationDsl
interface ValidationBuilder<T> {

    @Suppress("UNCHECKED_CAST")
    val thisPath: PathDescriptor<T, T>
        get() = ThisPathDescriptor as PathDescriptor<T, T>

    fun build(): Validation<T>
    fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T, ValidationContext?) -> Boolean
    ): Constraint<T>

    infix fun Constraint<T>.hint(hint: String): Constraint<T>
    fun run(validation: Validation<T>)

    infix fun <R> PathDescriptor<T, R>.validateIf(init: AndValidationBuilder<T>.() -> Unit): PathDescriptor<T, R> {
        val validation = Validation(init)
        return ConditionalPathDescriptor(this, validation)
    }

    infix fun <R> KProperty1<T, R>.validateIf(init: AndValidationBuilder<T>.() -> Unit): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(init) }

    infix fun <R> KFunction1<T, R>.validateIf(init: AndValidationBuilder<T>.() -> Unit): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(init) }

    infix fun <R> PathDescriptor<T, R>.validateIf(aValidation: Validation<T>): PathDescriptor<T, R> =
        ConditionalPathDescriptor(this, aValidation)

    infix fun <R> KProperty1<T, R>.validateIf(aValidation: Validation<T>): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(aValidation) }

    infix fun <R> KFunction1<T, R>.validateIf(aValidation: Validation<T>): PathDescriptor<T, R> =
        asPathDescriptorDo(this) { validateIf(aValidation) }

    companion object {
        @JvmStatic
        inline fun <T, R, RESULT> asPathDescriptorDo(
            aProperty: KProperty1<T, R>,
            aProcessor: PathDescriptor<T, R>.() -> RESULT
        ): RESULT = aProcessor.invoke(PropertyPathDescriptor(aProperty))

        @JvmStatic
        inline fun <T, R, RESULT> asPathDescriptorDo(
            aFunction: KFunction1<T, R>,
            aProcessor: PathDescriptor<T, R>.() -> RESULT
        ): RESULT = aProcessor.invoke(FunctionPathDescriptor(aFunction))
    }
}

interface AndValidationBuilder<T> : ValidationBuilder<T> {

    infix fun <R> PathDescriptor<T, R>.validate(init: AndValidationBuilder<R>.() -> Unit)
    infix fun <R> PathDescriptor<T, Iterable<R>>.onEachIterable(init: AndValidationBuilder<R>.() -> Unit)
    infix fun <R> PathDescriptor<T, Array<R>>.onEachArray(init: AndValidationBuilder<R>.() -> Unit)
    infix fun <K, V> PathDescriptor<T, Map<K, V>>.onEachMap(init: AndValidationBuilder<Map.Entry<K, V>>.() -> Unit)
    infix fun <R> PathDescriptor<T, R?>.ifPresent(init: AndValidationBuilder<R>.() -> Unit)
    infix fun <R> PathDescriptor<T, R?>.required(init: AndValidationBuilder<R>.() -> Unit)
    val <R> PathDescriptor<T, R>.has: AndValidationBuilder<R>

    infix fun <R> KProperty1<T, R>.validate(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { validate(init) }

    infix fun <R> KProperty1<T, Iterable<R>>.onEachIterable(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachIterable(init) }

    infix fun <R> KProperty1<T, Array<R>>.onEachArray(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachArray(init) }

    infix fun <K, V> KProperty1<T, Map<K, V>>.onEachMap(init: AndValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachMap(init) }

    infix fun <R> KProperty1<T, R?>.ifPresent(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    infix fun <R> KProperty1<T, R?>.required(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    val <R> KProperty1<T, R>.has: AndValidationBuilder<R> get() = PropertyPathDescriptor(this).has

    infix fun <R> KFunction1<T, R>.validate(init: AndValidationBuilder<R>.() -> Unit) =
        asPathDescriptorDo(this) { validate(init) }

    infix fun <R> KFunction1<T, Iterable<R>>.onEachIterable(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachIterable(init) }

    infix fun <R> KFunction1<T, Array<R>>.onEachArray(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachArray(init) }

    infix fun <K, V> KFunction1<T, Map<K, V>>.onEachMap(init: AndValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachMap(init) }

    infix fun <R> KFunction1<T, R?>.ifPresent(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    infix fun <R> KFunction1<T, R?>.required(init: AndValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    val <R> KFunction1<T, R>.has: AndValidationBuilder<R> get() = FunctionPathDescriptor(this).has

    fun shortCircuit()

    fun shortCircuit(anInitBlock: AndValidationBuilder<T>.() -> Unit)

    fun or(anInitBlock: OrValidationBuilder<T>.() -> Unit)
}

interface OrValidationBuilder<T> : ValidationBuilder<T> {

    infix operator fun <R> PathDescriptor<T, R>.invoke(init: OrValidationBuilder<R>.() -> Unit)
    infix fun <R> PathDescriptor<T, Iterable<R>>.onEachIterable(init: OrValidationBuilder<R>.() -> Unit)
    infix fun <R> PathDescriptor<T, Array<R>>.onEachArray(init: OrValidationBuilder<R>.() -> Unit)
    infix fun <K, V> PathDescriptor<T, Map<K, V>>.onEachMap(init: OrValidationBuilder<Map.Entry<K, V>>.() -> Unit)
    infix fun <R> PathDescriptor<T, R?>.ifPresent(init: OrValidationBuilder<R>.() -> Unit)
    infix fun <R> PathDescriptor<T, R?>.required(init: OrValidationBuilder<R>.() -> Unit)
    val <R> PathDescriptor<T, R>.has: OrValidationBuilder<R>

    infix operator fun <R> KProperty1<T, R>.invoke(init: OrValidationBuilder<R>.() -> Unit) =
        asPathDescriptorDo(this) { invoke(init) }

    infix fun <R> KProperty1<T, Iterable<R>>.onEachIterable(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachIterable(init) }

    infix fun <R> KProperty1<T, Array<R>>.onEachArray(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachArray(init) }

    infix fun <K, V> KProperty1<T, Map<K, V>>.onEachMap(init: OrValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachMap(init) }

    infix fun <R> KProperty1<T, R?>.ifPresent(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    infix fun <R> KProperty1<T, R?>.required(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    val <R> KProperty1<T, R>.has: OrValidationBuilder<R> get() = PropertyPathDescriptor(this).has

    infix operator fun <R> KFunction1<T, R>.invoke(init: OrValidationBuilder<R>.() -> Unit) =
        asPathDescriptorDo(this) { invoke(init) }

    infix fun <R> KFunction1<T, Iterable<R>>.onEachIterable(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachIterable(init) }

    infix fun <R> KFunction1<T, Array<R>>.onEachArray(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachArray(init) }

    infix fun <K, V> KFunction1<T, Map<K, V>>.onEachMap(init: OrValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        asPathDescriptorDo(this) { onEachMap(init) }

    infix fun <R> KFunction1<T, R?>.ifPresent(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { ifPresent(init) }

    infix fun <R> KFunction1<T, R?>.required(init: OrValidationBuilder<R>.() -> Unit): Unit =
        asPathDescriptorDo(this) { required(init) }

    val <R> KFunction1<T, R>.has: OrValidationBuilder<R> get() = FunctionPathDescriptor(this).has

    fun nonShortCircuit()

    fun nonShortCircuit(anInitBlock: OrValidationBuilder<T>.() -> Unit)

    fun and(anInitBlock: AndValidationBuilder<T>.() -> Unit)
}