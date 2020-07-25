@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.internal.BasicAndValidationBuilder
import de.kotlinBerlin.kValidation.internal.BasicOrValidationBuilder

/** Main interface to perform the validation for an object and to build a validation. */
abstract class Validation<T> {

    /**
     *  Performs the validation of the specified object with the given context. Should not be called from the outside but
     * only from within implementations if the [Validation] interface.
     */
    abstract fun validate(aValue: T, aContext: ValidationContext<*>): Boolean

    /** Performs the validation of the specified object. */
    operator fun invoke(aValue: T): ValidationResult<T> = validate(aValue, emptyMap())

    /**
     * Performs the validation of the specified object and a context map that can contain values to modify the
     * behaviour of the validation process.
     */
    fun validate(value: T, contextMap: Map<String, Any?>): ValidationResult<T> {
        val tempContext = ValidationContext(value, mutableMapOf(*contextMap.toList().toTypedArray()))
        val tempResult = validate(value, tempContext)
        val tempValidationResult = tempContext.finish()
        return if (tempResult) Valid(value) else tempValidationResult
    }

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

/** Basic implementation if an [ValidationContext] */
class ValidationContext<T> internal constructor(aValue: T, private val properties: MutableMap<String, Any?>) :
    MutableMap<String, Any?> by properties {
    private val rootNode = PathResult(ThisPathDescriptor, aValue, null)
    private val stack = mutableListOf<CompoundResult<*>>()

    private val currentNode: CompoundResult<*> get() = stack.lastOrNull() ?: rootNode

    internal fun finish(): ValidationResult<T> {
        if (stack.isNotEmpty()) {
            throw IllegalStateException("Called finish on a stack that is not empty.")
        }
        return rootNode
    }

    /** Creates a new instance of an [CompoundResult] and puts in on the internal stack. */
    fun push(aConstructor: (CompoundResult<*>) -> CompoundResult<*>) {
        stack.add(aConstructor(currentNode))
    }

    /**
     * Creates a new instance of an [Invalid] and adds it to the currently active [CompoundResult] previously added
     * with [push].
     */
    fun addInvalidResult(aConstructor: (CompoundResult<*>) -> Invalid<*>) {
        val tempInvalid = aConstructor.invoke(currentNode)
        when (val tempCurrentNode = currentNode) {
            is PathResult -> {
                when (tempInvalid) {
                    is CompoundResult -> tempCurrentNode.addError(tempInvalid)
                    else -> throw IllegalStateException("Can only add path results currently!")
                }
            }
            is LogicalResult -> tempCurrentNode.addError(tempInvalid)
        }
    }

    /** Pops the last element that was added by the [push] method and discards it. */
    fun pop() {
        stack.removeAt(stack.lastIndex)
    }

    /**
     *  Pops the last element that was added by the [push] method and ads it to the [CompoundResult] added
     * previously to that element.
     */
    fun popAndAddToParent() {
        val tempResult = stack.removeAt(stack.lastIndex)
        addInvalidResult { tempResult }
    }
}