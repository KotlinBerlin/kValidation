package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.AndValidationBuilder
import de.kotlinBerlin.kValidation.OrValidationBuilder
import de.kotlinBerlin.kValidation.PathDescriptor
import de.kotlinBerlin.kValidation.internal.PropModifier.*

internal class BasicOrValidationBuilder<T> : BasicValidationBuilder<T>(true), OrValidationBuilder<T> {

    override fun isCombineWithOr(): Boolean = true

    override fun <R> PathDescriptor<T, R>.invoke(init: OrValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(NonNull).also(init)
    }

    override fun <R> PathDescriptor<T, Iterable<R>>.onEachIterable(init: OrValidationBuilder<R>.() -> Unit) {
        getOrCreateIterablePropertyBuilder(NonNull).also(init)
    }

    override fun <R> PathDescriptor<T, Array<R>>.onEachArray(init: OrValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder<R>(ArrayPropKey(this, NonNull)).also(init)
    }

    override fun <K, V> PathDescriptor<T, Map<K, V>>.onEachMap(init: OrValidationBuilder<Map.Entry<K, V>>.() -> Unit) {
        getOrCreateBuilder<Map.Entry<K, V>>(MapPropKey(this, NonNull)).also(init)
    }

    override fun <R> PathDescriptor<T, R?>.ifPresent(init: OrValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(Optional).also(init)
    }

    override fun <R> PathDescriptor<T, R?>.required(init: OrValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(OptionalRequired).also(init)
    }

    override val <R> PathDescriptor<T, R>.has: OrValidationBuilder<R>
        get() = getOrCreateBuilder(NonNull)

    override fun and(anInitBlock: AndValidationBuilder<T>.() -> Unit) {
        val tempBuilder = BasicAndValidationBuilder<T>()
        tempBuilder.anInitBlock()
        val tempValidator = tempBuilder.build()
        run(tempValidator)
    }

    override fun nonShortCircuit() {
        shortCircuit = false
    }

    override fun nonShortCircuit(anInitBlock: OrValidationBuilder<T>.() -> Unit) {
        val tempBuilder = BasicOrValidationBuilder<T>()
        tempBuilder.nonShortCircuit()
        tempBuilder.anInitBlock()
        val tempValidator = tempBuilder.build()
        run(tempValidator)
    }

    private fun <R> PathDescriptor<T, R?>.getOrCreateBuilder(modifier: PropModifier): OrValidationBuilder<R> =
        getOrCreateBuilder(SingleValuePropKey(this, modifier))

    private fun <R> PathDescriptor<T, Iterable<R>>.getOrCreateIterablePropertyBuilder(modifier: PropModifier): OrValidationBuilder<R> =
        getOrCreateBuilder(IterablePropKey(this, modifier))

    @Suppress("UNCHECKED_CAST")
    private fun <R> getOrCreateBuilder(aKey: PropKey<T>): OrValidationBuilder<R> =
        super.getOrCreateBuilder(aKey) { BasicOrValidationBuilder<R>() } as OrValidationBuilder<R>
}