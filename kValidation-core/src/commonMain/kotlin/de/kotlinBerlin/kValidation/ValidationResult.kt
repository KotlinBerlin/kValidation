@file:Suppress("MemberVisibilityCanBePrivate")

package de.kotlinBerlin.kValidation

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1


/** A result of an validation on an object. */
sealed class ValidationResult<out T>(
    /** The value that was validated. */
    val value: T
) {
    /** A flat list of all [SimpleInvalidResult] instances.*/
    abstract val flatErrors: List<SimpleInvalidResult<*>>

    /** All [SimpleInvalidResult] instances grouped by their [SimpleInvalidResult.dataPath] property. */
    abstract val groupedErrors: Map<ValidationPath, List<SimpleInvalidResult<*>>>

    /**
     * All errors that have a [ValidationPath] that matches the path given.
     * If [includeSubErrors] is true, then also all [SimpleInvalidResult] instances that correspond to sub paths of the
     * given one are returned.
     */
    abstract fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean = false): List<SimpleInvalidResult<*>>

    /** Print this [ValidationResult] in a way that represents the original structure of the [Validation]. */
    abstract fun print(): String
}

/** A result that indicates that the validated object is valid. */
class Valid<out T>(aValue: T) : ValidationResult<T>(aValue) {
    override val flatErrors: List<SimpleInvalidResult<*>> get() = emptyList()
    override val groupedErrors: Map<ValidationPath, List<SimpleInvalidResult<*>>> get() = emptyMap()

    override fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean): List<SimpleInvalidResult<*>> = emptyList()

    override fun print(): String = "valid"

    /** Valid instances are equal, if their [value] properties are equal.*/
    override fun equals(other: Any?): Boolean = other is Valid<*> && value == other.value

    /** The hashcode of the [value] property. */
    override fun hashCode(): Int = value.hashCode()
}

/** A result that indicates that the validated object is not valid. */
sealed class Invalid<out T>(aValue: T, internal open val parent: CompoundResult<*>?) : ValidationResult<T>(aValue) {
    /** A message that describes the invalid state of the object that was validated. */
    abstract val message: String
    override val groupedErrors: Map<ValidationPath, List<SimpleInvalidResult<*>>> get() = flatErrors.groupBy { it.dataPath }

    internal abstract val dataPath: ValidationPath

    /** Simply returns the [message]. */
    override fun toString(): String = message
}

/** A result that represents a single reason for an invalid object. */
sealed class SimpleInvalidResult<out T>(
    override val message: String,
    value: T,
    override val parent: CompoundResult<*>
) :
    Invalid<T>(value, parent) {
    override val flatErrors: List<SimpleInvalidResult<T>> get() = listOf(this)

    /** The path to the root object that was validated. */
    public override val dataPath: ValidationPath get() = BasicValidationPath(mutableListOf(*parent.dataPath.segments))

    override fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean): List<SimpleInvalidResult<T>> =
        if (aSubPath.isEmpty()) flatErrors else emptyList()

    override fun print(): String = message
}

/** Represents an error. */
class Error<out T>(hint: String, value: T, parent: CompoundResult<*>) : SimpleInvalidResult<T>(hint, value, parent)

/** Represents a warning. */
class Warning<out T>(hint: String, value: T, parent: CompoundResult<*>) : SimpleInvalidResult<T>(hint, value, parent)

/** A result wrapping other results inside it. */
sealed class CompoundResult<out T>(value: T, parent: CompoundResult<*>?) : Invalid<T>(value, parent) {
    internal abstract val combinationSign: String

    override val flatErrors: List<SimpleInvalidResult<*>> get() = internalErrors.flatMap(Invalid<*>::flatErrors)
    override val dataPath: ValidationPath
        get() = parent?.let { BasicValidationPath(mutableListOf(*it.dataPath.segments)) } ?: EmptyValidationPath

    internal val internalErrors: MutableList<Invalid<*>> = mutableListOf()

    /** Errors wrapped by this [CompoundResult]. */
    val errors: List<Invalid<*>> get() = internalErrors

    internal abstract fun addError(anError: Invalid<*>)

    override fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean): List<SimpleInvalidResult<*>> {
        return errors.flatMap { it.errorsAt(*aSubPath, includeSubErrors = includeSubErrors) }
    }

    override val message: String
        get() = if (internalErrors.size == 1) {
            internalErrors.first().message
        } else {
            internalErrors.joinToString(separator = " $combinationSign ", prefix = "(", postfix = ")")
        }
}

/** A result that wraps other results inside it that are reachable by the [path]. */
class PathResult<out T>(
    /** The path to the validated sub object. */
    val path: PathDescriptor<*, *>, value: T, parent: CompoundResult<*>?
) :
    CompoundResult<T>(value, parent) {
    override val combinationSign: String get() = ","

    override val dataPath: ValidationPath get() = BasicValidationPath(mutableListOf(*super.dataPath.segments, path))

    override fun errorsAt(vararg aSubPath: Any?, includeSubErrors: Boolean): List<SimpleInvalidResult<*>> {
        if (aSubPath.isEmpty() && !includeSubErrors) return emptyList()
        if (aSubPath.isEmpty() && includeSubErrors) return errors.first().errorsAt(includeSubErrors = includeSubErrors)
        if (path is ThisPathDescriptor) return errors.first().errorsAt(*aSubPath, includeSubErrors = includeSubErrors)

        val tempCurrentSubPath = aSubPath[0]
        val tempRemainingSubPath = aSubPath.sliceArray(1..aSubPath.lastIndex)
        return if (pathMatches(path, tempCurrentSubPath)) {
            errors.first().errorsAt(*tempRemainingSubPath, includeSubErrors = includeSubErrors)
        } else {
            emptyList()
        }
    }

    override fun print(): String {
        if (path is ThisPathDescriptor) return errors.first().print()
        val tempBuilder = StringBuilder(path.name).append(":\n")
        val tempSubPrinted = errors.first().print()
        val tempSubPrintedIndented = tempSubPrinted.indented()
        tempBuilder.append(tempSubPrintedIndented)
        return tempBuilder.toString()
    }

    override fun addError(anError: Invalid<*>) {
        if (internalErrors.isNotEmpty()) throw IllegalStateException("Can not have more than 1 sub path!")
        internalErrors.add(anError)
    }

    private fun pathMatches(aPath: PathDescriptor<*, *>, anObject: Any?): Boolean = when (aPath) {
        ThisPathDescriptor -> throw IllegalStateException("Should not happen!")
        is PropertyPathDescriptor -> anObject is KProperty1<*, *> && anObject.name == aPath.name
        is FunctionPathDescriptor -> anObject is KFunction1<*, *> && anObject.name == aPath.name
        is MapPathDescriptor<*, *> -> anObject == aPath.key
        is ArrayPathDescriptor -> anObject == aPath.index
        is IterablePathDescriptor -> anObject == aPath.index
        is ConditionalPathDescriptor -> pathMatches(aPath.descriptor, anObject)
        is CustomPathDescriptor -> anObject == aPath.identifier
    }
}

/** A result wrapping other results that are combined by a logical operator. */
sealed class LogicalResult<out T>(value: T, override val parent: CompoundResult<*>) : CompoundResult<T>(value, parent) {
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

    override fun addError(anError: Invalid<*>) {
        if (anError is LogicalResult && combinationSign == anError.combinationSign) {
            internalErrors.addAll(anError.internalErrors)
        } else {
            internalErrors.add(anError)
        }
    }
}

/** A result wrapping other results that are combined by the && operator. */
class AndResult<out T>(value: T, parent: CompoundResult<*>) : LogicalResult<T>(value, parent) {
    override val combinationSign: String get() = "and"
}

/** A result wrapping other results that are combined by the || operator. */
class OrResult<out T>(value: T, parent: CompoundResult<*>) : LogicalResult<T>(value, parent) {
    override val combinationSign: String get() = "or"
}

private fun String.indented(): String =
    this.split("\n").foldIndexed("") { tempIndex, tempPreviousResult, tempValue ->
        val tempSeparator = if (tempIndex == 0) "" else "\n"
        "$tempPreviousResult$tempSeparator\t$tempValue"
    }
