package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.AndValidationBuilder
import de.kotlinBerlin.kValidation.OrValidationBuilder
import de.kotlinBerlin.kValidation.PathDescriptor
import de.kotlinBerlin.kValidation.internal.PropModifier.*

internal class BasicAndValidationBuilder<T> : BasicValidationBuilder<T>(false), AndValidationBuilder<T> {

    override fun isCombineWithOr(): Boolean = false

    override fun <R> PathDescriptor<T, R>.validate(init: AndValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(Undefined).also(init)
    }

    override fun <R> PathDescriptor<T, Iterable<R>>.onEachIterable(init: AndValidationBuilder<R>.() -> Unit) {
        getOrCreateIterablePropertyBuilder(Undefined).also(init)
    }

    override fun <R> PathDescriptor<T, Array<R>>.onEachArray(init: AndValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder<R>(ArrayPropKey(this, Undefined)).also(init)
    }

    override fun <K, V> PathDescriptor<T, Map<K, V>>.onEachMap(init: AndValidationBuilder<Map.Entry<K, V>>.() -> Unit) {
        getOrCreateBuilder<Map.Entry<K, V>>(MapPropKey(this, Undefined)).also(init)
    }

    override fun <R> PathDescriptor<T, R?>.ifPresent(init: AndValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(Optional).also(init)
    }

    override fun <R> PathDescriptor<T, R?>.required(init: AndValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(Required).also(init)
    }

    override val <R> PathDescriptor<T, R>.has: AndValidationBuilder<R>
        get() = getOrCreateBuilder(Undefined)

    override fun or(anInitBlock: OrValidationBuilder<T>.() -> Unit) {
        val tempBuilder = BasicOrValidationBuilder<T>()
        tempBuilder.anInitBlock()
        val tempValidator = tempBuilder.build()
        run(tempValidator)
    }

    override fun shortCircuit() {
        shortCircuit = true
    }

    override fun shortCircuit(anInitBlock: AndValidationBuilder<T>.() -> Unit) {
        val tempBuilder = BasicAndValidationBuilder<T>()
        tempBuilder.shortCircuit()
        tempBuilder.anInitBlock()
        val tempValidator = tempBuilder.build()
        run(tempValidator)
    }

    private fun <R> PathDescriptor<T, R?>.getOrCreateBuilder(modifier: PropModifier): AndValidationBuilder<R> =
        getOrCreateBuilder(SingleValuePropKey(this, modifier))

    private fun <R> PathDescriptor<T, Iterable<R>>.getOrCreateIterablePropertyBuilder(modifier: PropModifier): AndValidationBuilder<R> =
        getOrCreateBuilder(IterablePropKey(this, modifier))

    @Suppress("UNCHECKED_CAST")
    override fun <R> getOrCreateBuilder(aKey: PropKey<T>): AndValidationBuilder<R> =
        super.getOrCreateBuilder(aKey) { BasicAndValidationBuilder<R>() } as AndValidationBuilder<R>
}