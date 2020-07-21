package de.kotlinBerlin.kValidation

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

typealias ValidationErrors = List<ValidationError>

internal object NoValidationErrors : ValidationErrors by emptyList()

internal class DefaultValidationErrors(private val errors: List<ValidationError>) : ValidationErrors by errors {
    override fun toString(): String = errors.toString()
}

open class ValidationPath internal constructor(segments: List<PathDescriptor<*, *>>) {
    private val _segments = mutableListOf(*segments.toTypedArray())
    val segments: List<PathDescriptor<*, *>> get() = _segments

    init {
        cleanUp()
    }

    override fun toString(): String = segments.fold("this") { previousResult, nextSegment ->
        combineSegments(nextSegment, previousResult)
    }

    private fun combineSegments(
        nextSegment: PathDescriptor<*, *>,
        previousResult: String
    ): String {
        return when (nextSegment) {
            is PropertyPathDescriptor, is FunctionPathDescriptor -> "$previousResult.${nextSegment.name}"
            ThisPathDescriptor -> previousResult
            is MapPathDescriptor<*, *>, is IndexPathDescriptor -> "$previousResult${nextSegment.name}"
            is ConditionalPathDescriptor -> combineSegments(nextSegment.descriptor, previousResult)
        }
    }

    private fun cleanUp() {
        val tempIterator = _segments.listIterator()
        for (tempSegment in tempIterator) {
            if (tempSegment is ThisPathDescriptor) {
                tempIterator.remove()
            } else if (tempSegment is ConditionalPathDescriptor) {
                tempIterator.set(tempSegment.descriptor)
            }
        }
    }
}

object EmptyValidationPath : ValidationPath(emptyList())

interface ValidationError {
    val dataPath: String
    val path: ValidationPath
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
            is KProperty1<*, *> -> pathSegment.name
            is KFunction1<*, *> -> pathSegment.name
            else -> pathSegment
        }
        val tempSegmentIdentifier = when (val tempActual = segments[index]) {
            is PropertyPathDescriptor -> tempActual.property.name
            is FunctionPathDescriptor -> tempActual.function.name
            is MapPathDescriptor<*, *> -> tempActual.entry.key
            is IndexPathDescriptor -> tempActual.index
            is ConditionalPathDescriptor, ThisPathDescriptor -> throw IllegalStateException("Should not happen!")
        }
        if (tempSegmentIdentifier != tempPathIdentifier) return false
    }
    return true
}