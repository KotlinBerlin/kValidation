package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.PathDescriptor
import de.kotlinBerlin.kValidation.Validation
import de.kotlinBerlin.kValidation.ValidationBuilder
import de.kotlinBerlin.kValidation.internal.PropModifier.*

internal enum class PropModifier {
    Undefined, Optional, Required
}

internal abstract class PropKey<T> {
    abstract val modifier: PropModifier
    abstract val pathDescriptor: PathDescriptor<T, *>
    abstract fun build(builder: ValidationBuilder<*>): Validation<T>
    abstract fun copyWith(aModifier: PropModifier): PropKey<T>
}

internal data class SingleValuePropKey<T, R>(
    override val pathDescriptor: PathDescriptor<T, R>,
    override val modifier: PropModifier
) : PropKey<T>() {
    override fun build(builder: ValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as ValidationBuilder<R>).build()
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(pathDescriptor, validations)
            Optional -> OptionalPropertyValidation(pathDescriptor, validations)
            Required -> RequiredPropertyValidation(pathDescriptor, validations)
        }
    }

    override fun copyWith(aModifier: PropModifier): PropKey<T> {
        return SingleValuePropKey(pathDescriptor, aModifier)
    }
}

internal data class IterablePropKey<T, R>(
    override val pathDescriptor: PathDescriptor<T, Iterable<R>>,
    override val modifier: PropModifier,
    private val anIndexList: IntArray = IntArray(0)
) : PropKey<T>() {
    override fun build(builder: ValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as BasicValidationBuilder<R>).build()
        val tempIterableValidation = IterableValidation(validations, *anIndexList)
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(pathDescriptor, tempIterableValidation)
            Optional -> OptionalPropertyValidation(pathDescriptor, tempIterableValidation)
            Required -> RequiredPropertyValidation(pathDescriptor, tempIterableValidation)
        }
    }

    override fun copyWith(aModifier: PropModifier): PropKey<T> {
        return IterablePropKey(pathDescriptor, aModifier, anIndexList)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IterablePropKey<*, *>) return false

        if (pathDescriptor != other.pathDescriptor) return false
        if (modifier != other.modifier) return false
        if (!anIndexList.contentEquals(other.anIndexList)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pathDescriptor.hashCode()
        result = 31 * result + modifier.hashCode()
        result = 31 * result + anIndexList.contentHashCode()
        return result
    }
}

internal data class ArrayPropKey<T, R>(
    override val pathDescriptor: PathDescriptor<T, Array<R>>,
    override val modifier: PropModifier,
    private val anIndexList: IntArray = IntArray(0)
) : PropKey<T>() {
    override fun build(builder: ValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as BasicValidationBuilder<R>).build()
        val tempArrayValidation = ArrayValidation(validations, *anIndexList)
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(pathDescriptor, tempArrayValidation)
            Optional -> OptionalPropertyValidation(pathDescriptor, tempArrayValidation)
            Required -> RequiredPropertyValidation(pathDescriptor, tempArrayValidation)
        }
    }

    override fun copyWith(aModifier: PropModifier): PropKey<T> {
        return ArrayPropKey(pathDescriptor, aModifier, anIndexList)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayPropKey<*, *>) return false

        if (pathDescriptor != other.pathDescriptor) return false
        if (modifier != other.modifier) return false
        if (!anIndexList.contentEquals(other.anIndexList)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pathDescriptor.hashCode()
        result = 31 * result + modifier.hashCode()
        result = 31 * result + anIndexList.contentHashCode()
        return result
    }
}

internal data class MapPropKey<T, K, V>(
    override val pathDescriptor: PathDescriptor<T, Map<K, V>>,
    override val modifier: PropModifier,
    private val aKeyList: Array<out K>
) : PropKey<T>() {
    override fun build(builder: ValidationBuilder<*>): Validation<T> {
        @Suppress("UNCHECKED_CAST")
        val validations = (builder as BasicValidationBuilder<Map.Entry<K, V>>).build()
        val tempMapValidation = MapValidation(validations, *aKeyList)
        return when (modifier) {
            Undefined -> UndefinedPropertyValidation(pathDescriptor, tempMapValidation)
            Optional -> OptionalPropertyValidation(pathDescriptor, tempMapValidation)
            Required -> RequiredPropertyValidation(pathDescriptor, tempMapValidation)
        }
    }

    override fun copyWith(aModifier: PropModifier): PropKey<T> {
        return MapPropKey(pathDescriptor, aModifier, aKeyList)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapPropKey<*, *, *>) return false

        if (pathDescriptor != other.pathDescriptor) return false
        if (modifier != other.modifier) return false
        if (!aKeyList.contentEquals(other.aKeyList)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pathDescriptor.hashCode()
        result = 31 * result + modifier.hashCode()
        result = 31 * result + aKeyList.contentHashCode()
        return result
    }
}