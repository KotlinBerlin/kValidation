package de.kotlinBerlin.kValidation

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

/** A list of [ValidationError] instances. */
typealias ValidationErrors = List<ValidationError>

internal object NoValidationErrors : ValidationErrors by emptyList()

internal class DefaultValidationErrors(private val errors: List<ValidationError>) : ValidationErrors by errors {
    override fun toString(): String = errors.toString()
}

/** A path from the root object that was validated to an sub object that was validated. */
open class ValidationPath internal constructor(segments: List<PathDescriptor<*, *>>) {
    /** A list of [PathDescriptor] instances that describes the path to a specific value that was validated. */
    val segments: List<PathDescriptor<*, *>> = cleanUp(segments)

    /** Returns a string representation of this [ValidationPath]. */
    override fun toString(): String = segments.fold("this") { previousResult, nextSegment ->
        combineSegments(nextSegment, previousResult)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ValidationPath) {
            if (segments.size != other.segments.size) return false
            segments.forEachIndexed { tempIndex, tempSegment ->
                val tempOtherSegment = other.segments[tempIndex]
                if (tempOtherSegment != tempSegment) return false
            }
            return true
        }
        return false
    }

    override fun hashCode(): Int = segments.toTypedArray().contentHashCode()

    private fun combineSegments(
        nextSegment: PathDescriptor<*, *>,
        previousResult: String
    ): String {
        return when (nextSegment) {
            is PropertyPathDescriptor, is FunctionPathDescriptor -> "$previousResult.${nextSegment.name}"
            ThisPathDescriptor -> previousResult
            is MapPathDescriptor<*, *>, is ArrayPathDescriptor, is IterablePathDescriptor -> "$previousResult${nextSegment.name}"
            is ConditionalPathDescriptor -> combineSegments(nextSegment.descriptor, previousResult)
        }
    }

    private fun cleanUp(aSegmentList: List<PathDescriptor<*, *>>): List<PathDescriptor<*, *>> {
        val tempList = mutableListOf(*aSegmentList.toTypedArray())
        val tempIterator = tempList.listIterator()
        for (tempSegment in tempIterator) {
            if (tempSegment is ThisPathDescriptor) {
                tempIterator.remove()
            } else if (tempSegment is ConditionalPathDescriptor) {
                tempIterator.set(tempSegment.descriptor)
            }
        }
        return tempList
    }
}

internal object EmptyValidationPath : ValidationPath(emptyList())

/** A single error when validating an object. */
interface ValidationError {
    /** The string representation of the [path]. */
    val dataPath: String

    /** The path from the root object to the object that caused this error. */
    val path: ValidationPath

    /** The description of the error. */
    val message: String
}

internal data class BasicValidationError(
    override val path: ValidationPath,
    override val message: String
) : ValidationError {
    override val dataPath: String get() = path.toString()
    override fun toString(): String = "ValidationError(dataPath=$dataPath, message=$message)"
}

internal fun ValidationPath.matches(propertyPath: Array<out Any>): Boolean {
    propertyPath.forEachIndexed { index, pathSegment ->
        val tempPathIdentifier = when (pathSegment) {
            is KProperty1<*, *> -> "prop:${pathSegment.name}"
            is KFunction1<*, *> -> "func:${pathSegment.name}"
            else -> pathSegment
        }
        val tempSegmentIdentifier = when (val tempActual = segments[index]) {
            is PropertyPathDescriptor -> "prop:${tempActual.property.name}"
            is FunctionPathDescriptor -> "func:${tempActual.function.name}"
            is MapPathDescriptor<*, *> -> tempActual.key
            is ArrayPathDescriptor -> tempActual.index
            is IterablePathDescriptor -> tempActual.index
            is ConditionalPathDescriptor, ThisPathDescriptor -> throw IllegalStateException("Should not happen!")
        }
        if (tempSegmentIdentifier != tempPathIdentifier) return false
    }
    return true
}