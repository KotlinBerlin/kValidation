package de.kotlinBerlin.kValidation

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
    if (propertyPath.size != segments.size) return false
    segments.forEachIndexed { index, segment ->
        val tempExpected = propertyPath[index]
        val tempIsEqual = when (segment) {
            is PropertyPathDescriptor -> segment.property == tempExpected
            is FunctionPathDescriptor -> segment.function == tempExpected
            is MapPathDescriptor<*, *> -> segment.entry.key == tempExpected
            is IndexPathDescriptor -> segment.index == tempExpected
            is ConditionalPathDescriptor, ThisPathDescriptor -> throw IllegalStateException("Should not happen!")
        }
        if (!tempIsEqual) return false
    }
    return true
}