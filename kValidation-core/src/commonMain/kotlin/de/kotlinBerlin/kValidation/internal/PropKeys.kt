package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.PathDescriptor
import de.kotlinBerlin.kValidation.Validation
import de.kotlinBerlin.kValidation.internal.PropModifier.*

internal enum class PropModifier {
    Undefined, Optional, OptionalRequired
}

internal abstract class PropKey<T> {
    abstract fun build(builder: BasicValidationBuilder<*>): Validation<T>
}

internal data class SingleValuePropKey<T, R>(
    val property: PathDescriptor<T, R>,
    val modifier: PropModifier
) : PropKey<T>() {
    override fun build(builder: BasicValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as BasicValidationBuilder<R>).build()
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(property, validations)
            Optional -> OptionalPropertyValidation(property, validations)
            OptionalRequired -> RequiredPropertyValidation(property, validations)
        }
    }
}

internal data class IterablePropKey<T, R>(
    val property: PathDescriptor<T, Iterable<R>>,
    val modifier: PropModifier
) : PropKey<T>() {
    override fun build(builder: BasicValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as BasicValidationBuilder<R>).build()
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(property, IterableValidation(validations))
            Optional -> OptionalPropertyValidation(property, IterableValidation(validations))
            OptionalRequired -> RequiredPropertyValidation(property, IterableValidation(validations))
        }
    }
}

internal data class ArrayPropKey<T, R>(
    val property: PathDescriptor<T, Array<R>>,
    val modifier: PropModifier
) : PropKey<T>() {
    override fun build(builder: BasicValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as BasicValidationBuilder<R>).build()
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(property, ArrayValidation(validations))
            Optional -> OptionalPropertyValidation(property, ArrayValidation(validations))
            OptionalRequired -> RequiredPropertyValidation(property, ArrayValidation(validations))
        }
    }
}

internal data class MapPropKey<T, K, V>(
    val property: PathDescriptor<T, Map<K, V>>,
    val modifier: PropModifier
) : PropKey<T>() {
    override fun build(builder: BasicValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as BasicValidationBuilder<Map.Entry<K, V>>).build()
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(property, MapValidation(validations))
            Optional -> OptionalPropertyValidation(property, MapValidation(validations))
            OptionalRequired -> RequiredPropertyValidation(property, MapValidation(validations))
        }
    }
}