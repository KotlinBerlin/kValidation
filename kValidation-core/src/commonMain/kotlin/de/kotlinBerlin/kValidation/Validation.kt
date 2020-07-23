@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.internal.BasicAndValidationBuilder
import de.kotlinBerlin.kValidation.internal.BasicOrValidationBuilder
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

/** Main interface to perform the validation for an object and to build a validation. */
abstract class Validation<in T> {

    /** Performs the validation of the specified object with the given context. */
    internal abstract fun validate(aValue: T, aContext: ValidationContext): Boolean

    /** Performs the validation of the specified object with a default context. */
    operator fun invoke(aValue: T): ValidationResult = validate(aValue, emptyMap())
    fun validate(value: T, contextMap: Map<String, Any?>): ValidationResult {
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
internal class ValidationContext(aValue: Any?, private val contextMap: MutableMap<String, Any?>) {
    private val stack = mutableListOf<PathResult<*>>(SimplePathResult(ThisPathDescriptor, null, aValue))

    private val currentNode: PathResult<*> get() = stack[stack.lastIndex]

    val properties: Map<String, Any?> get() = object : Map<String, Any?> by contextMap {}

    fun finish(): ValidationResult {
        if (stack.size != 1) {
            throw IllegalStateException("Called finish on a stack that does not has exactly 1 element.")
        }
        return currentNode
    }

    fun push(aConstructor: (PathResult<*>) -> PathResult<*>) {
        stack.add(aConstructor(currentNode))
    }

    fun addInvalidResult(anInvalid: Invalid<*>) {
        when (val tempCurrentNode = currentNode) {
            is SimplePathResult -> {
                when (anInvalid) {
                    is PathResult -> tempCurrentNode.setSubPath(anInvalid)
                    else -> throw IllegalStateException("Can only add path results currently!")
                }
            }
            is CombinedResult -> tempCurrentNode.addError(anInvalid)
        }
    }

    fun pop() {
        stack.removeAt(stack.lastIndex)
    }

    fun popAndAddToParent() {
        val tempResult = stack.removeAt(stack.lastIndex)
        addInvalidResult(tempResult)
    }
}

sealed class ValidationResult

class Valid<out T>(val value: T) : ValidationResult()

sealed class Invalid<out T>(val value: T) : ValidationResult() {
    abstract val hint: String
    abstract val flatErrors: List<SimpleInvalidResult<*>>

    abstract fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean = false): List<SimpleInvalidResult<*>>
    abstract fun print(): String

    override fun toString(): String = hint
}


sealed class SimpleInvalidResult<out T>(override val hint: String, value: T) : Invalid<T>(value) {
    override val flatErrors: List<SimpleInvalidResult<T>> get() = listOf(this)

    override fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean): List<SimpleInvalidResult<T>> =
        if (aSubPath.isEmpty()) flatErrors else emptyList()

    override fun print(): String = hint
}

class Error<out T>(hint: String, value: T) : SimpleInvalidResult<T>(hint, value) {
    override val hint: String get() = "error: ${super.hint}"
}

class Warning<out T>(hint: String, value: T) : SimpleInvalidResult<T>(hint, value) {
    override val hint: String get() = "warn: ${super.hint}"
}

internal sealed class PathResult<out T>(private val parent: PathResult<*>?, value: T) : Invalid<T>(value) {
    abstract val path: PathDescriptor<*, *>
}

internal class SimplePathResult<out T>(override val path: PathDescriptor<*, *>, parent: PathResult<*>?, value: T) :
    PathResult<T>(parent, value) {
    override val flatErrors: List<SimpleInvalidResult<*>> get() = subPath.flatErrors
    override val hint: String get() = subPath.hint

    private lateinit var internalSubPath: PathResult<*>
    val subPath: PathResult<*> get() = internalSubPath

    override fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean): List<SimpleInvalidResult<*>> {
        if (aSubPath.isEmpty() && !includeSubErrors) return emptyList()
        if (aSubPath.isEmpty() && includeSubErrors) return subPath.errorsAt(includeSubErrors = includeSubErrors)
        if (path is ThisPathDescriptor) return subPath.errorsAt(*aSubPath, includeSubErrors = includeSubErrors)

        val tempCurrentSubPath = aSubPath[0]
        val tempRemainingSubPath = aSubPath.sliceArray(1..aSubPath.lastIndex)
        return if (pathMatches(path, tempCurrentSubPath)) {
            subPath.errorsAt(*tempRemainingSubPath, includeSubErrors = includeSubErrors)
        } else {
            emptyList()
        }
    }

    override fun print(): String {
        if (path is ThisPathDescriptor) return subPath.print()
        val tempBuilder = StringBuilder(path.name).append(":\n")
        val tempSubPrinted = subPath.print()
        val tempSubPrintedIndented = tempSubPrinted.indented()
        tempBuilder.append(tempSubPrintedIndented)
        return tempBuilder.toString()
    }

    internal fun setSubPath(aSubPath: PathResult<*>) {
        if (::internalSubPath.isInitialized) throw IllegalStateException("Can not have more than 1 sub path!")
        internalSubPath = aSubPath
    }

    private fun pathMatches(aPath: PathDescriptor<*, *>, anObject: Any?): Boolean = when (aPath) {
        ThisPathDescriptor -> throw IllegalStateException("Should not happen!")
        is PropertyPathDescriptor -> anObject is KProperty1<*, *> && anObject.name == aPath.name
        is FunctionPathDescriptor -> anObject is KFunction1<*, *> && anObject.name == aPath.name
        is MapPathDescriptor<*, *> -> anObject == aPath.key
        is ArrayPathDescriptor -> anObject == aPath.index
        is IterablePathDescriptor -> anObject == aPath.index
        is ConditionalPathDescriptor -> pathMatches(aPath.descriptor, anObject)
    }
}

internal sealed class CombinedResult<out T>(parent: PathResult<*>?, value: T) :
    PathResult<T>(parent, value) {
    override val path: PathDescriptor<*, *> get() = ThisPathDescriptor
    override val flatErrors: List<SimpleInvalidResult<*>> get() = internalErrors.flatMap(Invalid<*>::flatErrors)

    protected val internalErrors = mutableListOf<Invalid<*>>()
    val errors: List<Invalid<*>> get() = internalErrors

    abstract val combinationSign: String
    override val hint: String
        get() = if (internalErrors.size == 1) {
            internalErrors.first().hint
        } else {
            internalErrors.joinToString(separator = " $combinationSign ", prefix = "(", postfix = ")")
        }

    override fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean): List<SimpleInvalidResult<*>> {
        return errors.flatMap { it.errorsAt(*aSubPath, includeSubErrors = includeSubErrors) }
    }

    override fun print(): String {
        if (internalErrors.size == 1) return internalErrors[0].print()

        val tempBuilder = StringBuilder(combinationSign).append(" {")
        for (tempError in errors) {
            val tempPrinted = tempError.print()
            val tempPrintedIndented = tempPrinted.indented()
            tempBuilder.append("\n").append(tempPrintedIndented)
        }
        tempBuilder.append("\n}")
        return tempBuilder.toString()
    }

    internal fun addError(anError: Invalid<*>) {
        if (anError is CombinedResult && combinationSign == anError.combinationSign) {
            internalErrors.addAll(anError.internalErrors)
        } else {
            internalErrors.add(anError)
        }
    }
}

internal class AndResult<out T>(parent: PathResult<*>?, value: T) :
    CombinedResult<T>(parent, value) {
    override val combinationSign: String get() = "and"
}

internal class OrResult<out T>(parent: PathResult<*>?, value: T) :
    CombinedResult<T>(parent, value) {
    override val combinationSign: String get() = "or"
}

private fun String.indented(): String =
    this.split("\n").foldIndexed("") { tempIndex, tempPreviousResult, tempValue ->
        val tempSeparator = if (tempIndex == 0) "" else "\n"
        "$tempPreviousResult$tempSeparator\t$tempValue"
    }
