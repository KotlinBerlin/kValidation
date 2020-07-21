@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.internal.BasicAndValidationBuilder
import de.kotlinBerlin.kValidation.internal.BasicOrValidationBuilder

/** Main interface to perform the validation for an object and to build a validation. */
interface Validation<in T> {

    /** Performs the validation of the specified object with the given context. */
    fun validate(aValue: T, aContext: ValidationContext): ValidationResult<T>

    /** Performs the validation of the specified object with a default context. */
    operator fun invoke(value: T): ValidationResult<T> = validate(value, BasicValidationContext())

    companion object {
        /** Builds a new [Validation] with the root builder performing an "and" operation on the added constraints. */
        operator fun <T> invoke(init: AndValidationBuilder<T>.() -> Unit): Validation<T> =
            and(init)

        /** Builds a new [Validation] with the root builder performing an "and" operation on the added constraints. */
        fun <T> and(init: AndValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = BasicAndValidationBuilder<T>()
            return builder.apply(init).build()
        }

        /** Builds a new [Validation] with the root builder performing an "or" operation on the added constraints. */
        fun <T> or(init: OrValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = BasicOrValidationBuilder<T>()
            return builder.apply(init).build()
        }
    }
}

/** A context that gets passed to every [Validation] and every [de.kotlinBerlin.kValidation.constraints.Constraint]]. */
interface ValidationContext {
    /** Gets a property of this [ValidationContext]*/
    operator fun get(aKey: String): Any?

    /** Sets a property in this [ValidationContext]*/
    operator fun set(aKey: String, aValue: Any?)

    /** Checks weather or not this context contains a property for the given key. */
    fun contains(aKey: String): Boolean
}

/** Basic implementation if an [ValidationContext] */
open class BasicValidationContext : ValidationContext {
    private val properties = mutableMapOf<String, Any?>()
    override fun get(aKey: String): Any? = properties[aKey]
    override fun set(aKey: String, aValue: Any?): Unit = properties.set(aKey, aValue)
    override fun contains(aKey: String): Boolean = properties.containsKey(aKey)
}

/** A [ValidationContext] that wraps another one. */
class WrappingValidationContext(
    private val original: ValidationContext,
    /** The wrapped context. */
    val wrapped: ValidationContext
) : ValidationContext {
    override fun get(aKey: String): Any? = if (original.contains(aKey)) original[aKey] else wrapped[aKey]
    override fun set(aKey: String, aValue: Any?): Unit = original.set(aKey, aValue)
    override fun contains(aKey: String): Boolean = original.contains(aKey) || wrapped.contains(aKey)
}